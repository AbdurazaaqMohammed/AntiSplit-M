/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.dex.debug;

import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.base.FixedDexContainer;
import com.reandroid.dex.data.DebugInfo;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.smali.model.SmaliCodeSet;
import com.reandroid.dex.smali.model.SmaliDebugElement;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class DebugSequence extends FixedDexContainer implements Iterable<DebugElementBlock> {

    private final IntegerReference lineStart;
    private BlockList<DebugElementBlock> elementList;

    public DebugSequence(IntegerReference lineStart) {
        super(2);
        this.lineStart = lineStart;
        addChild(1, DebugEndSequence.INSTANCE);
    }

    public<T1 extends DebugElementBlock> T1 getOrCreateAtAddress(DebugElementType<T1> elementType, int address) {
        T1 prev = null;
        Iterator<T1> iterator = iterator(elementType);
        while (iterator.hasNext()) {
            T1 element = iterator.next();
            int a = element.getTargetAddress();
            if (a == address) {
                return element;
            }
            if (a > address) {
                break;
            }
            prev = element;
        }
        int index = 0;
        if (prev != null) {
            index = prev.getIndex() + 1;
        }
        T1 element = createAtPosition(elementType, index);
        element.setTargetAddress(address);
        return element;
    }
    public Iterator<DebugElementBlock> getAtAddress(int address) {
        return FilterIterator.of(iterator(), element -> address == element.getTargetAddress());
    }
    public void removeInvalid() {
        int size = size();
        for(int i = size - 1; i >= 0; i --) {
            DebugElementBlock element = get(i);
            if (!element.isValid()) {
                remove(element);
            }
        }
    }
    public int getLineStart() {
        return lineStart.get();
    }
    public void setLineStart(int start) {
        if (start == getLineStart()) {
            return;
        }
        setLineStartInternal(start);
        cacheValues();
    }
    void setLineStartInternal(int start) {
        lineStart.set(start);
    }

    public Iterator<DebugElementBlock> getExtraLines() {
        return new FilterIterator<>(iterator(),
                element -> (!(element instanceof DebugAdvance)));
    }
    public boolean removeIf(Predicate<? super DebugElementBlock> filter) {
        boolean removedOnce = false;
        Iterator<DebugElementBlock> iterator = FilterIterator.of(clonedIterator(), filter);
        while (iterator.hasNext()) {
            boolean removed = removeInternal(iterator.next());
            if (removed) {
                removedOnce = true;
            }
        }
        if (removedOnce) {
            updateValues();
        }
        return removedOnce;
    }
    public boolean remove(DebugElementBlock element) {
        if (element == null || element.getParent(getClass()) != this) {
            return false;
        }
        boolean removed = removeInternal(element);
        if (removed) {
            updateValues();
        }
        return removed;
    }
    private boolean removeInternal(DebugElementBlock element) {
        element.onPreRemove(this);
        boolean removed = getElementList().remove(element);
        if (removed) {
            element.setParent(null);
            element.setIndex(-1);
        }
        return removed;
    }
    public<T1 extends DebugElementBlock> T1 createAtPosition(DebugElementType<T1> type, int index) {
        T1 element = type.newDebugBlock();
        add(index, element);
        return element;
    }
    @SuppressWarnings("unchecked")
    public<T1 extends DebugElementBlock> T1 createNext(DebugElementType<T1> type) {
        if (type == DebugElementType.END_SEQUENCE) {
            return (T1) DebugEndSequence.INSTANCE;
        }
        T1 element = type.newDebugBlock();
        add(element);
        return element;
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        int position = reader.getPosition();
        int count = 0;
        while (reader.read() != 0) {
            count++;
        }
        if (count == 0) {
            return;
        }
        reader.seek(position);
        BlockList<DebugElementBlock> elementList = unlockElementList();
        unlockElementList().ensureCapacity(count);
        DebugElementType<?> type = readNext(reader);
        while (!type.is(DebugElementType.END_SEQUENCE)) {
            type = readNext(reader);
        }
        elementList.trimToSize();
        cacheValues();
    }

    private DebugElementType<?> readNext(BlockReader reader) throws IOException {
        DebugElementType<?> type = DebugElementType.readFlag(reader);
        DebugElementBlock debugElement;
        if (type == DebugElementType.END_SEQUENCE) {
            debugElement = DebugEndSequence.INSTANCE;
        } else {
            debugElement = type.newDebugBlock();
            unlockElementList().add(debugElement);
        }
        debugElement.readBytes(reader);
        return type;
    }

    private void cacheValues() {
        DebugElementBlock previous = null;
        for(DebugElementBlock element : this) {
            element.cacheValues(this, previous);
            previous = element;
        }
    }
    private void updateValues() {
        DebugElementBlock previous = null;
        Iterator<DebugElementBlock> iterator = clonedIterator();
        while (iterator.hasNext()) {
            DebugElementBlock element = iterator.next();
            element.updateValues(this, previous);
            previous = element;
        }
    }

    public DebugElementBlock get(int i) {
        return getElementList().get(i);
    }
    public void add(int i, DebugElementBlock element) {
        unlockElementList().add(i, element);
    }
    @SuppressWarnings("unchecked")
    public<T1 extends DebugElementBlock> Iterator<T1> iterator(DebugElementType<T1> type) {
        return ComputeIterator.of(iterator(), element -> {
            if (element.getElementType() == type) {
                return (T1) element;
            }
            return null;
        });
    }
    public int size() {
        return getElementList().size();
    }
    public boolean isEmpty() {
        return !getVisible().hasNext();
    }
    public int visibleSize() {
        return CollectionUtil.count(getVisible());
    }
    @Override
    public Iterator<DebugElementBlock> iterator() {
        return getElementList().iterator();
    }
    public Iterator<DebugElementBlock> getVisible() {
        return getElementList().iterator(DebugElementBlock::isVisible);
    }
    public Iterator<DebugElementBlock> clonedIterator() {
        return getElementList().clonedIterator();
    }
    public boolean add(DebugElementBlock element) {
        if (element == null || element.getClass() == DebugEndSequence.class) {
            return false;
        }
        return unlockElementList().add(element);
    }
    public void clear() {
        getElementList().clearChildes();
    }
    public boolean isRemoved() {
        DebugInfo debugInfo = getParentInstance(DebugInfo.class);
        if (debugInfo != null) {
            return debugInfo.isRemoved();
        }
        return true;
    }


    public Iterator<IdItem> usedIds() {
        return new IterableIterator<DebugElementBlock, IdItem>(iterator()) {
            @Override
            public Iterator<IdItem> iterator(DebugElementBlock element) {
                return element.usedIds();
            }
        };
    }

    private BlockList<DebugElementBlock> unlockElementList() {
        BlockList<DebugElementBlock> elementList = this.elementList;
        if (elementList == null || BlockList.isImmutableEmpty(elementList)) {
            elementList = new BlockList<>();
            this.elementList = elementList;
            addChild(0, elementList);
        }
        return elementList;
    }
    private BlockList<DebugElementBlock> getElementList() {
        BlockList<DebugElementBlock> elementList = this.elementList;
        if (elementList == null || isRemoved()) {
            elementList = BlockList.empty();
        }
        return elementList;
    }

    public void merge(DebugSequence sequence) {
        this.lineStart.set(sequence.lineStart.get());
        int size = sequence.size();
        if (size == 0) {
            return;
        }
        unlockElementList().ensureCapacity(size);
        for(int i = 0; i < size; i++) {
            DebugElementBlock coming = sequence.get(i);
            DebugElementBlock element = createNext(coming.getElementType());
            element.merge(coming);
        }
        cacheValues();
        getElementList().trimToSize();
    }
    public void fromSmali(SmaliCodeSet smaliCodeSet) {
        Iterator<SmaliDebugElement> iterator = smaliCodeSet.getDebugElements();
        while (iterator.hasNext()) {
            SmaliDebugElement smaliDebug = iterator.next();
            createNext(smaliDebug.getDebugElementType()).fromSmali(smaliDebug);
        }
    }

    public int compareSequence(DebugSequence sequence) {
        if (sequence == null) {
            return -1;
        }
        if (sequence == this) {
            return 0;
        }
        int i;
        Iterator<DebugElementBlock> iterator1 = this.getVisible();
        Iterator<DebugElementBlock> iterator2 = sequence.getVisible();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            DebugElementBlock element1 = iterator1.next();
            DebugElementBlock element2 = iterator2.next();
            i = element1.compareElement(element2);
            if (i != 0) {
                return i;
            }
        }
        if (iterator1.hasNext()) {
            return 1;
        }
        if (iterator2.hasNext()) {
            return -1;
        }
        return 0;
    }
    public boolean startsWith(DebugSequence sequence) {
        if (sequence == null) {
            return false;
        }
        if (sequence == this) {
            return true;
        }
        Iterator<DebugElementBlock> iterator1 = this.getVisible();
        Iterator<DebugElementBlock> iterator2 = sequence.getVisible();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            DebugElementBlock element1 = iterator1.next();
            DebugElementBlock element2 = iterator2.next();
            if (!element1.equals(element2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }
    @Override
    public int hashCode() {
        int hash = 1;
        Iterator<DebugElementBlock> iterator = getVisible();
        while (iterator.hasNext()) {
            hash = hash * 31 + iterator.next().hashCode() * 31;
        }
        return hash;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DebugSequence sequence = (DebugSequence) obj;
        Iterator<DebugElementBlock> iterator1 = this.getVisible();
        Iterator<DebugElementBlock> iterator2 = sequence.getVisible();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            Object item1 = iterator1.next();
            Object item2 = iterator2.next();
            if (!item1.equals(item2)) {
                return false;
            }
        }
        return !iterator1.hasNext() && !iterator2.hasNext();
    }
    @Override
    public String toString() {
        return "start=" + lineStart + ", elements=" + getElementList();
    }
}
