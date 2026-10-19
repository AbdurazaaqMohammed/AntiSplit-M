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
package com.reandroid.dex.ins;

import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.ByteArray;
import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.base.Ule128Item;
import com.reandroid.dex.common.IdUsageIterator;
import com.reandroid.dex.data.CodeItem;
import com.reandroid.dex.data.FixedDexContainerWithTool;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelSet;
import com.reandroid.dex.program.TryItem;
import com.reandroid.dex.smali.model.SmaliTryItem;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public class InsTryBlock extends FixedDexContainerWithTool implements
        Creator<InsTryItem>, Iterable<InsTryItem>, InstructionLabelSet, IdUsageIterator {

    private final CodeItem codeItem;
    private HandlerOffsetArray handlerOffsetArray;
    private Ule128Item tryItemsCount;
    private ByteArray unknownBytes;
    private BlockList<InsTryItem> tryItemArray;
    private DexPositionAlign positionAlign;

    public InsTryBlock(CodeItem codeItem) {
        super(5);
        this.codeItem = codeItem;
    }

    InstructionList getInstructionList() {
        return getCodeItem().getInstructionList();
    }
    private CodeItem getCodeItem() {
        return codeItem;
    }
    public int getTryItemCount() {
        if (isNull()) {
            return 0;
        }
        return tryItemArray.getCount();
    }
    
    @Override
    public Iterator<InstructionLabel> getLabels() {
        return IterableIterator.of(iterator(), TryItem::getLabels);
    }

    public InsTryItem createNext() {
        initialize();
        InsTryItem tryItem = newInstance();
        add(tryItem);
        return tryItem;
    }
    public InsTryItem createNextCopy(InsTryItem base) {
        InsTryItem tryItem = base.newCompact();
        add(tryItem);
        return tryItem;
    }
    private void add(InsTryItem tryItem) {
        if (tryItemArray != null) {
            tryItemArray.add(tryItem);
            handlerOffsetArray.ensureSize(tryItemArray.size());
        }
    }
    public InsTryItem get(int i) {
        if (tryItemArray != null) {
            return tryItemArray.get(i);
        }
        return null;
    }
    public Iterator<InsTryItem> getTriesForAddress(int address) {
        return FilterIterator.of(iterator(),
                tryItem -> tryItem.hasExceptionHandlersForAddress(address));
    }
    @Override
    public Iterator<InsTryItem> iterator() {
        if (isNull()) {
            return EmptyIterator.of();
        }
        return tryItemArray.iterator();
    }
    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        if (isNull()) {
            return;
        }
        BlockList<InsTryItem> array = this.tryItemArray;
        array.removeIf(InsTryItem::isEmpty);
        updateHandlerOffsets();
        if (isEmpty()) {
            setNull(true);
        } else {
            positionAlign.align(this);
        }
    }
    private void updateHandlerOffsets() {
        Ule128Item tryItemsCount = this.tryItemsCount;
        BlockList<InsTryItem> array = this.tryItemArray;
        if ( array == null || tryItemsCount == null) {
            return;
        }
        int size = array.size();
        int realTryItemCount = 0;
        for (int i = 0; i < size; i++) {
            InsTryItem tryItem = array.get(i);
            if (!tryItem.isCompact()) {
                realTryItemCount ++;
            }
        }

        tryItemsCount.set(realTryItemCount);

        int baseOffset = tryItemsCount.countBytes();
        HandlerOffsetArray offsetArray = this.handlerOffsetArray;
        offsetArray.setSize(size);
        for (int i = 0; i < size; i++) {
            InsTryItem tryItem = array.get(i);
            HandlerOffset handlerOffset = offsetArray.get(i);
            int offset = array.countUpTo(tryItem);
            offset += baseOffset;
            handlerOffset.setOffset(offset);
        }
    }
    private HandlerOffsetArray initHandlersOffset() {
        if (handlerOffsetArray == null) {
            handlerOffsetArray = new HandlerOffsetArray(getCodeItem().getTryCountReference());
            addChild(INDEX_offsetArray, handlerOffsetArray);
        }
        return handlerOffsetArray;
    }
    private void initTryItemArray() {
        if (tryItemArray != null) {
            return;
        }
        tryItemsCount = new Ule128Item();
        addChild(INDEX_itemsCount, tryItemsCount);
        tryItemArray = new BlockList<>(this);
        addChild(INDEX_itemArray, tryItemArray);
    }
    public boolean isEmpty() {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        return tryItemArray == null || tryItemArray.size() == 0;
    }
    @Override
    public boolean isNull() {
        return tryItemArray == null;
    }
    @Override
    public void setNull(boolean is_null) {
        if (is_null == this.isNull()) {
            return;
        }
        if (is_null) {
            clear();
        } else {
            initialize();
        }
    }
    private void initialize() {
        initHandlersOffset();
        initTryItemArray();
        if (positionAlign == null) {
            positionAlign = new DexPositionAlign();
            addChild(INDEX_positionAlign, positionAlign);
        }
    }
    private void clear() {
        if (handlerOffsetArray != null) {
            handlerOffsetArray.setParent(null);
            handlerOffsetArray.setIndex(-1);
            handlerOffsetArray = null;
        }
        if (tryItemsCount != null) {
            tryItemsCount.setParent(null);
            tryItemsCount.setIndex(-1);
            tryItemArray = null;
        }
        if (tryItemArray != null) {
            tryItemArray.clearChildes();
            tryItemArray.setParent(null);
            tryItemArray.setIndex(-1);
            tryItemArray = null;
        }
        if (positionAlign != null) {
            positionAlign.setParent(null);
            positionAlign.setIndex(-1);
            positionAlign = null;
        }
        addChild(INDEX_offsetArray, null);
        addChild(INDEX_itemsCount, null);
        addChild(INDEX_unknownBytes, null);
        addChild(INDEX_itemArray, null);
        addChild(INDEX_positionAlign, null);
    }

    public void remove(InsTryItem tryItem) {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        if (tryItemArray != null) {
            if (tryItemArray.remove(tryItem)) {
                tryItem.onRemove();
            }
        }
    }
    public void moveTo(InsTryItem tryItem, int index) {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        if (tryItemArray != null && tryItem.getIndex() != index) {
            tryItemArray.moveTo(tryItem, index);
            this.handlerOffsetArray.moveTo(tryItem.getHandlerOffset(), index);
        }
    }
    public void onRemove() {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        if (tryItemArray != null) {
            this.tryItemArray = null;
            int count = tryItemArray.getCount();
            for (int i = 0; i < count; i++) {
                InsTryItem tryItem = tryItemArray.getLast();
                tryItem.onRemove();
            }
            tryItemArray.clearChildes();
        }
        HandlerOffsetArray array = this.handlerOffsetArray;
        if (array != null) {
            array.setSize(0);
            this.handlerOffsetArray = null;
        }
        DexPositionAlign positionAlign = this.positionAlign;
        if (positionAlign != null) {
            this.positionAlign = null;
        }
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        boolean is_null = getCodeItem().getTryCountReference().get() == 0;
        setNull(is_null);
        if (is_null) {
            return;
        }
        this.handlerOffsetArray.onReadBytes(reader);
        this.tryItemsCount.onReadBytes(reader);
        readUnknownBytes(reader);
        this.tryItemArray.setSize(handlerOffsetArray.size());
        this.tryItemArray.readChildes(reader);
        this.positionAlign.onReadBytes(reader);
    }
    private void setUnknownBytes(int count) {
        if (count <= 0 || isNull()) {
            ByteArray unknown = this.unknownBytes;
            if (unknown != null) {
                unknown.setParent(null);
                unknown.setIndex(-1);
                unknown.setSize(0);
                this.unknownBytes = null;
            }
            return;
        }
        ByteArray unknown = this.unknownBytes;
        if (unknown == null) {
            unknown = new ByteArray(count);
            this.unknownBytes = unknown;
            unknown.setParent(this);
            addChild(INDEX_unknownBytes, unknown);
        } else {
            unknown.setSize(count);
        }
    }
    private void readUnknownBytes(BlockReader reader) throws IOException {
        int minStart = this.handlerOffsetArray.getMinStart();
        minStart = minStart - this.tryItemsCount.countBytes();
        setUnknownBytes(minStart);
        ByteArray unknown = this.unknownBytes;
        if (unknown != null) {
            unknown.readBytes(reader);
        }
    }

    public DexPositionAlign getPositionAlign() {
        return positionAlign;
    }

    @Override
    public InsTryItem newInstance() {
        return new InsTryItem(initHandlersOffset());
    }

    @Override
    public InsTryItem newInstanceAt(int index) {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        HandlerOffsetArray offsetArray = initHandlersOffset();
        if (tryItemArray.size() < 2) {
            return new InsTryItem(offsetArray);
        }
        int i = offsetArray.indexOf(offsetArray.getOffset(index));
        InsTryItem tryItem = null;
        if (i >= 0 && i < index) {
            tryItem = tryItemArray.get(i);
            if (tryItem != null) {
                tryItem = tryItem.newCompact();
            }
        }
        if (tryItem == null) {
            tryItem = new InsTryItem(offsetArray);
        }
        return tryItem;
    }

    public boolean compactSimilarCatches() {
        BlockList<InsTryItem> array = this.tryItemArray;
        if (array == null) {
            return false;
        }
        boolean result = false;
        int size = array.size();
        for (int i = 0; i < size; i++) {
            InsTryItem base =  array.get(i);
            if (!base.isCompact()) {
                for (int j = i + 1; j < size; j++) {
                    InsTryItem tryItem = array.get(j);
                    if (base.compactWith(tryItem)) {
                        result = true;
                    }
                }
            }
        }
        if (result) {
            refresh();
        }
        return result;
    }
    public boolean flattenCompactCatches() {
        BlockList<InsTryItem> array = this.tryItemArray;
        if (array == null) {
            return false;
        }
        boolean result = false;
        int size = array.size();
        for (int i = 0; i < size; i++) {
            if (array.get(i).flatten()) {
                result = true;
            }
        }
        if (result) {
            refresh();
        }
        return result;
    }
    public boolean splitTryHandlers() {
        BlockList<InsTryItem> array = this.tryItemArray;
        if (array == null) {
            return false;
        }
        flattenCompactCatches();
        boolean result = false;
        for (int i = 0; i < array.size(); i++) {
            InsTryItem tryItem = array.get(i);
            if (tryItem.splitHandlers()) {
                result = true;
            }
        }
        if (result) {
            refresh();
        }
        return result;
    }
    public boolean combineTries() {
        BlockList<InsTryItem> array = this.tryItemArray;
        if (array == null) {
            return false;
        }
        boolean result = false;
        for (int i = 0; i < array.size(); i++) {
            InsTryItem base = array.get(i);
            if (!base.isCompact()) {
                for (int j = i + 1; j < array.size(); j++) {
                    InsTryItem tryItem = array.get(j);
                    if (base.combineWith(tryItem)) {
                        tryItem.removeSelf();
                        result = true;
                    }
                }
            }
        }
        if (result) {
            refresh();
        }
        return result;
    }
    public void merge(InsTryBlock tryBlock) {
        boolean is_null = tryBlock.isNull();
        setNull(is_null);
        if (is_null) {
            return;
        }
        int count = tryBlock.getTryItemCount();
        for (int i = 0; i < count; i++) {
            InsTryItem coming = tryBlock.get(i);
            InsTryItem comingSource = coming.getTryItem();
            InsTryItem tryItem;
            if (coming != comingSource) {
                tryItem = get(comingSource.getIndex()).newCompact();
            } else {
                tryItem = newInstance();
            }
            add(tryItem);
            tryItem.merge(coming);
        }
        updateHandlerOffsets();
    }
    public void fromProgram(TryItem tryItem) {
        createNext().fromProgram(tryItem);
        updateHandlerOffsets();
    }

    @Override
    public boolean uses(Key key) {
        Iterator<InsTryItem> iterator = iterator();
        while (iterator.hasNext()) {
            if (iterator.next().uses(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<IdItem> usedIds() {
        return IterableIterator.of(iterator(), InsTryItem::usedIds);
    }

    public boolean sortTryItems() {
        BlockList<InsTryItem> tryItemArray = this.tryItemArray;
        HandlerOffsetArray handlerOffsetArray = this.handlerOffsetArray;
        if (tryItemArray == null || handlerOffsetArray == null
                || handlerOffsetArray.size() < 2
                || !tryItemArray.needsSort(CompareUtil.getComparableComparator())) {
            return false;
        }
        return tryItemArray.sort(CompareUtil.getComparableComparator(), handlerOffsetArray);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InsTryBlock tryBlock = (InsTryBlock) obj;
        if (isNull()) {
            return tryBlock.isNull();
        }
        return Objects.equals(handlerOffsetArray, tryBlock.handlerOffsetArray) &&
                Objects.equals(tryItemArray, tryBlock.tryItemArray);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        Object obj = handlerOffsetArray;
        hash = hash * 31;
        if (obj != null) {
            hash = hash + obj.hashCode();
        }
        obj = tryItemArray;
        hash = hash * 31;
        if (obj != null) {
            hash = hash + obj.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if (isNull()) {
            return "NULL";
        }
        return "tryItems = " + tryItemArray.toString() + ", bytes="+countBytes();
    }

    private static final int INDEX_offsetArray = 0;
    private static final int INDEX_itemsCount = 1;
    private static final int INDEX_unknownBytes = 2;
    private static final int INDEX_itemArray = 3;
    private static final int INDEX_positionAlign = 4;
}
