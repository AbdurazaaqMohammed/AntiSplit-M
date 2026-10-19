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
package com.reandroid.dex.data;

import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.container.CountedBlockList;
import com.reandroid.arsc.item.IntegerItem;
import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.common.SectionTool;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.ArrayKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyList;
import com.reandroid.dex.reference.ShortIdReference;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ComputeIterator;

import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class ShortIdList<T extends IdItem> extends DataItem
        implements Comparable<ShortIdList<T>> {

    private final CountedBlockList<ShortIdReference<T>> referenceList;
    private final DexPositionAlign positionAlign;

    public ShortIdList(SectionType<T> sectionType, int usageType) {
        super(3);

        IntegerItem countReference = new IntegerItem();

        this.referenceList = new CountedBlockList<>(
                new ReferenceCreator<>(sectionType, usageType), countReference);

        positionAlign = new DexPositionAlign();

        addChildBlock(0, countReference);
        addChildBlock(1, referenceList);
        addChildBlock(2, positionAlign);
    }

    @Override
    public KeyList<?> getKey() {
        Key[] elements = new Key[size()];
        getItemKeys(elements);
        return ArrayKey.create(elements);
    }
    public void setKey(Key key) {
        KeyList<?> old = getKey();
        KeyList<?> keyList = (KeyList<?>) key;
        if (KeyList.equalsIgnoreEmpty(keyList, ObjectsUtil.cast(old))) {
            return;
        }
        int size = keyList.size();
        setSize(size);
        for (int i = 0; i < size; i++) {
            getReference(i).setKey(keyList.get(i));
        }
        keyChanged(old);
    }
    protected boolean equalsKey(KeyList<?> keyList) {
        if (keyList == null || keyList.isEmpty()) {
            return isEmpty();
        }
        int size = this.size();
        if (size != keyList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!ObjectsUtil.equals(getItemKey(i), keyList.get(i))) {
                return false;
            }
        }
        return true;
    }
    public int size() {
        return referenceList.size();
    }
    public void setSize(int size) {
        referenceList.setSize(size);
    }
    public void clear() {
        setSize(0);
    }
    private void ensureSize(int size) {
        referenceList.ensureSize(size);
    }
    private ShortIdReference<T> createNext() {
        return referenceList.createNext();
    }
    private ShortIdReference<T> getReference(int i) {
        return referenceList.get(i);
    }
    private ShortIdReference<T> getOrCreateReference(int i) {
        ensureSize(i + 1);
        return referenceList.get(i);
    }
    public T addNewItem(Key key) {
        ShortIdReference<T> item = createNext();
        item.setKey(key);
        return item.getItem();
    }
    public void addNewItem(T item) {
        ShortIdReference<T> reference = createNext();
        reference.setItem(item);
    }
    public Key getItemKey(int i) {
        ShortIdReference<T> reference = getReference(i);
        if (reference != null) {
            return reference.getKey();
        }
        return null;
    }
    public T setItemKeyAt(int index, Key key) {
        ShortIdReference<T> reference = getReference(index);
        if (reference != null) {
            reference.setKey(key);
            return reference.getItem();
        }
        return null;
    }
    void getItemKeys(Key[] out) {
        int length = out.length;
        for (int i = 0; i < length; i++) {
            out[i] = getItemKey(i);
        }
    }

    @Override
    public void removeSelf() {
        clear();
        super.removeSelf();
    }

    public boolean remove(T item) {
        return removeIf(t -> t == item);
    }
    public boolean removeIf(Predicate<? super T> filter) {
        return referenceList.removeIf(reference -> filter.test(reference.getItem()));
    }
    void removeNulls() {
        removeIf(item -> item == null);
    }

    public Iterator<T> iterator() {
        return ComputeIterator.of(referenceList.iterator(), ShortIdReference::getItem);
    }
    public T getItem(int i){
        ShortIdReference<T> reference = getReference(i);
        if (reference != null) {
            return reference.getItem();
        }
        return null;
    }
    public boolean isEmpty() {
        return !iterator().hasNext();
    }
    public boolean sort(Comparator<? super T> comparator) {
        return referenceList.sort((ref1, ref2) -> comparator.compare(ref1.getItem(), ref2.getItem()));
    }

    @Override
    protected void onPreRefresh() {
        super.onPreRefresh();
        removeNulls();
    }

    public DexPositionAlign getPositionAlign() {
        return positionAlign;
    }


    @Override
    public int compareTo(ShortIdList<T> shortIdList) {
        if (shortIdList == this) {
            return 0;
        }
        int size1 = this.size();
        int size2 = shortIdList.size();
        int size = NumbersUtil.min(size1, size2);
        for (int i = 0; i < size; i++) {
            int c = SectionTool.compareIdx(this.getItem(i), shortIdList.getItem(i));
            if (c != 0) {
                return c;
            }
        }
        return CompareUtil.compare(size1, size2);
    }
    @Override
    public int hashCode() {
        int hash = 1;
        int size = size();
        for(int i = 0; i < size; i++){
            hash = hash * 31 + ObjectsUtil.hash(getItem(i));
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj == null || getClass() != obj.getClass()){
            return false;
        }
        ShortIdList<?> itemList = (ShortIdList<?>) obj;
        int size = size();
        if(size != itemList.size()){
            return false;
        }
        for(int i = 0; i < size; i++) {
            if(!ObjectsUtil.equals(getItem(i), itemList.getItem(i))){
                return false;
            }
        }
        return true;
    }

    static class ReferenceCreator<T extends IdItem> implements Creator<ShortIdReference<T>> {

        private final SectionType<T> sectionType;
        private final int usageType;

        ReferenceCreator(SectionType<T> sectionType, int usageType) {
            this.sectionType = sectionType;
            this.usageType = usageType;
        }

        @Override
        public ShortIdReference<T> newInstance() {
            return new ShortIdReference<>(sectionType, usageType);
        }
    }
}
