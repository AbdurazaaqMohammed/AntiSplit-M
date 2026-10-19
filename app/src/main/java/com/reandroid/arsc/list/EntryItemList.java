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
package com.reandroid.arsc.list;

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.header.TypeHeader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.arsc.item.OffsetItem;
import com.reandroid.arsc.pool.SpecStringPool;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.CompareUtil;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class EntryItemList extends OffsetBlockList<Entry> implements JSONConvert<JSONArray> {

    private final TypeHeader header;

    public EntryItemList(TypeHeader header, EntryItemOffsetList offsetList) {
        super(header.getEntriesStart(), offsetList, Entry.CREATOR);
        this.header = header;
        header.setOffsetTypeChangedListener(EntryItemList.this::onOffsetTypeChanged);
    }

    public Entry getEntry(int entryId) {
        int index;
        if (isSparse()) {
            index = getEntryIndex(entryId);
        } else {
            index = entryId;
        }
        return get(index);
    }
    public Entry getOrCreate(int entryId) {
        Entry entry = getEntry(entryId);
        if (entry == null) {
            if (isSparse()) {
                entry = createSparse(entryId);
            } else {
                entry = createAt(entryId);
            }
        }
        return entry;
    }
    private Entry createSparse(int entryId) {
        EntryItemOffsetList offsetList = getOffsetReferenceList();
        int index = offsetList.findSortPoint(entryId);
        OffsetItem offsetItem = offsetList.createAt(index);
        offsetItem.setIdx(entryId);
        return this.createAt(index);
    }
    public Entry getEntry(String entryName){
        if (entryName == null) {
            return null;
        }
        TypeBlock typeBlock = getParentInstance(TypeBlock.class);
        if (typeBlock == null) {
            return null;
        }
        PackageBlock packageBlock = typeBlock.getPackageBlock();
        if (packageBlock == null) {
            return null;
        }
        Iterator<Entry> iterator = packageBlock.getEntries(
                typeBlock.getTypeName(), entryName);
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.getParentInstance(EntryItemList.class) == this) {
                return entry;
            }
        }
        return null;
    }

    public boolean isEmptyEntries() {
        return !iterator(true).hasNext();
    }
    public int countNonNull() {
        return countIf(NON_NULL_PREDICATE);
    }

    public void removeAllNull(int start) {
        trimLastIf(start, Entry::isNull);
    }
    public Boolean hasComplexEntry() {
        Iterator<Entry> iterator = iterator(true);
        while (iterator.hasNext()){
            Entry entry = iterator.next();
            if (entry.isComplex()) {
                return true;
            }
            ResValue resValue = entry.getResValue();
            ValueType valueType = resValue.getValueType();
            if (valueType == null || valueType == ValueType.REFERENCE
                    || valueType == ValueType.NULL) {
                continue;
            }
            return false;
        }
        return null;
    }
    public Iterator<Entry> iterator(boolean skipNullBlock) {
        if (!skipNullBlock) {
            return iterator();
        }
        return iterator(NON_NULL_PREDICATE);
    }

    public int getHighestEntryId() {
        if (isSparse()) {
            return getOffsetReferenceList().getHighestIdx();
        }
        return size() - 1;
    }
    public int getEntryId(int index) {
        if (isSparse()) {
            return getOffsetReferenceList().get(index).getIdx();
        }
        return index;
    }
    public int getEntryIndex(int entryId) {
        if (isSparse()) {
            return getOffsetReferenceList().indexOfIdx(entryId);
        }
        return entryId;
    }

    public boolean isSparse() {
        return getOffsetReferenceList().isSparse();
    }
    public int getOffsetType() {
        return getOffsetReferenceList().getOffsetType();
    }
    public void setOffsetType(int offsetType) {
        header.setOffsetType(offsetType);
    }
    public void sort() {
        if (isSparse()) {
            EntryItemOffsetList offsetList = getOffsetReferenceList();
            offsetList.sort(CompareUtil.getComparableComparator(), this);
        }
    }
    @Override
    public EntryItemOffsetList getOffsetReferenceList() {
        return (EntryItemOffsetList) super.getOffsetReferenceList();
    }
    void onOffsetTypeChanged(int offsetType) {
        if (getOffsetReferenceList().setOffsetType(offsetType, this)) {
            buildOffsetList();
        }
    }

    @Override
    public void onPreRemove(Entry item) {
        item.setNull(true);
        super.onPreRemove(item);
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        IntegerReference flags = header.getFlags();
        flags.set((flags.get() & 0xfffffffc) | getOffsetType());
    }

    public void linkTableStringsInternal(TableStringPool tableStringPool) {
        Iterator<Entry> iterator = iterator(true);
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            entry.linkTableStringsInternal(tableStringPool);
        }
    }
    public void linkSpecStringsInternal(SpecStringPool specStringPool) {
        Iterator<Entry> iterator = iterator(true);
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            entry.linkSpecStringsInternal(specStringPool);
        }
    }

    public void merge(EntryItemList itemList) {
        if (itemList != null && itemList != this) {
            int capacity;
            if (this.isSparse()) {
                capacity = itemList.size();
            } else {
                capacity = itemList.getHighestEntryId() + 1 - size();
            }
            ensureCapacity(capacity);
            Iterator<Entry> iterator = itemList.iterator(true);
            while (iterator.hasNext()) {
                Entry comingBlock = iterator.next();
                Entry existingBlock = this.getOrCreate(comingBlock.getId());
                existingBlock.merge(comingBlock);
            }
            buildOffsetList();
        }
    }
    @Override
    public JSONArray toJson() {
        JSONArray jsonArray = new JSONArray(size());
        Iterator<Entry> iterator = iterator(true);
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            JSONObject jsonObject = entry.toJson();
            if(jsonObject != null) {
                jsonArray.put(jsonObject);
            }
        }
        return jsonArray;
    }
    @Override
    public void fromJson(JSONArray json) {
        clear();
        if (json != null) {
            int length = json.length();
            if (!isSparse()) {
                setSize(length);
            }
            String idKey = Entry.NAME_id;
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                int entryId = jsonObject.getInt(idKey);
                getOrCreate(entryId).fromJson(jsonObject);
            }
        }
        buildOffsetList();
    }


    private static final Predicate<Entry> NON_NULL_PREDICATE = entry -> !entry.isNull();
}
