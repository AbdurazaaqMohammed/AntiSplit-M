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
package com.reandroid.arsc.pool;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.Chunk;
import com.reandroid.arsc.header.StringPoolHeader;
import com.reandroid.arsc.item.*;
import com.reandroid.arsc.list.OffsetReferenceList;
import com.reandroid.arsc.list.StringItemList;
import com.reandroid.arsc.list.StyleItemList;
import com.reandroid.arsc.list.StyleItemListEnd;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.collection.MultiMap;
import com.reandroid.xml.StyleDocument;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public abstract class StringPool<T extends StringItem> extends Chunk<StringPoolHeader>
        implements Iterable<T>, JSONConvert<JSONArray> {

    private final Object mLock = new Object();
    private final StringItemList<T> mArrayStrings;
    private final StyleItemList mArrayStyles;

    private final MultiMap<String, T> poolMap;
    private boolean stringLinkLocked;

    StringPool(boolean is_utf8, boolean stringLinkLocked, StringCreator<T> creator) {
        super(new StringPoolHeader(), 6);

        StringPoolHeader header = getHeaderBlock();
        header.setEncodingChangedListener(null);
        header.setUtf8(is_utf8);

        OffsetReferenceList<OffsetItem> offsetStrings = new OffsetReferenceList<>(
                header.getCountStrings(),
                OffsetItem.CREATOR_OFFSET32);

        OffsetReferenceList<OffsetItem> offsetStyles = new OffsetReferenceList<>(
                header.getCountStyles(),
                OffsetItem.CREATOR_OFFSET32);

        AlignItem stringsAlign = new AlignItem(true);

        this.mArrayStrings = new StringItemList<>(
                stringsAlign,
                header,
                offsetStrings,
                creator);

        this.mArrayStyles = new StyleItemList(
                header.getStartStyles(),
                offsetStyles);

        StyleItemListEnd styleItemListEnd = new StyleItemListEnd(header.getCountStyles());

        addChild(offsetStrings);
        addChild(offsetStyles);
        addChild(mArrayStrings);
        addChild(stringsAlign);
        addChild(mArrayStyles);
        addChild(styleItemListEnd);

        this.stringLinkLocked = stringLinkLocked;

        this.poolMap = new MultiMap<>();
        this.poolMap.setFavouriteObjectsSorter((item1, item2) -> {
            int i = item1.compareTo(item2);
            if (i == 0) {
                i = CompareUtil.compareUnsigned(item1.getIndex(), item2.getIndex());
            }
            return i;
        });
    }
    StringPool(boolean is_utf8, StringCreator<T> creator) {
        this(is_utf8, true, creator);
    }

    @Override
    public Iterator<T> iterator() {
        return mArrayStrings.iterator();
    }
    public final T get(int index) {
        return mArrayStrings.get(index);
    }
    public int size() {
        return mArrayStrings.size();
    }
    public boolean isEmpty() {
        return size() == 0;
    }
    public void clear() {
        getStyleArray().clear();
        getStringsArray().clear();
        poolMap.clear();
    }
    public void sort() {
        ensureStringLinkUnlockedInternal();
        getStringsArray().sort();
    }
    public boolean isStringLinkLocked() {
        return stringLinkLocked;
    }
    public void ensureStringLinkUnlockedInternal() {
        synchronized (mLock) {
            if (!stringLinkLocked) {
                return;
            }
            stringLinkLocked = false;
            linkStrings();
            reloadPoolMap();
        }
    }
    void linkStrings() {
        getStyleArray().linkStyleStringsInternal();
    }
    public void linkStylesInternal() {
        StyleItemList styleArray = getStyleArray();
        StringItemList<T> stringsArray = getStringsArray();
        int size = NumbersUtil.min(stringsArray.size(), styleArray.size());
        for( int i = 0; i < size; i++) {
            StyleItem styleItem = styleArray.get(i);
            StringItem stringItem = stringsArray.get(i);
            stringItem.linkStyleItemInternal(styleItem);
        }
        getStyleArray().linkStyleStringsInternal();
    }
    public void removeString(T item) {
        getStringsArray().remove(item);
    }
    public Iterator<String> getStrings() {
        return ComputeIterator.of(iterator(), T::getXml);
    }
    public void addStrings(Collection<String> stringList) {
        if (stringList == null || stringList.size() == 0) {
            return;
        }
        for(String str : stringList) {
            createNewString(str);
        }
    }
    private void reloadPoolMap() {
        if (poolMap.size() == 0) {
            poolMap.clear();
            poolMap.setInitialSize(size());
            poolMap.putAll(StringItem::getXml, iterator());
        }
    }
    public void compressDuplicates() {
        ensureStringLinkUnlockedInternal();
        poolMap.findDuplicates(CompareUtil.getComparableComparator(), list -> {
            T first = list.get(0);
            for(int i = 1; i < list.size(); i++) {
                T item = list.get(i);
                first.transferReferences(item);
            }
        });
    }
    public boolean removeUnusedStrings() {
        return getStringsArray().removeIf(getUnusedStringsFilter());
    }
    public List<T> listUnused() {
        return getStringsArray().subListIf(getUnusedStringsFilter());
    }
    public int countUnused() {
        return getStringsArray().countIf(getUnusedStringsFilter());
    }
    private Predicate<T> getUnusedStringsFilter() {
        return item -> !item.hasReference();
    }
    public StyleItemList getStyleArray() {
        return mArrayStyles;
    }
    public StringItemList<T> getStringsArray() {
        return mArrayStrings;
    }
    public T removeReference(ReferenceItem ref) {
        if (ref == null) {
            return null;
        }
        T item = get(ref.get());
        if (item != null) {
            item.removeReference(ref);
            return item;
        }
        return null;
    }
    public boolean contains(String str) {
        return poolMap.containsKey(str);
    }
    public void onStringChanged(String old, T stringItem) {
        if (!stringLinkLocked) {
            poolMap.updateKey(old, stringItem.getXml(), stringItem);
        }
    }
    public void onStringRemoved(T stringItem) {
        if (!stringLinkLocked) {
            poolMap.remove(stringItem.getXml(), stringItem);
        }
    }
    public final T getLast() {
        return mArrayStrings.getLast();
    }
    public<E extends Block> Iterator<E> getUsers(Class<E> parentClass, String value) {
        return new IterableIterator<T, E>(getAll(value)) {
            @Override
            public Iterator<E> iterator(T element) {
                return element.getUsers(parentClass);
            }
        };
    }
    public final Iterator<T> getAll(String str) {
        ensureStringLinkUnlockedInternal();
        if (str == null) {
            return FilterIterator.of(poolMap.getAll(StringsUtil.EMPTY),
                    StringItem::isNull);
        }
        return poolMap.getAll(str);
    }
    public final T get(String str, Predicate<? super T> predicate) {
        ensureStringLinkUnlockedInternal();
        if (str == null) {
            str = StringsUtil.EMPTY;
        }
        return poolMap.get(str, predicate);
    }
    public final T getString(String str) {
        return CollectionUtil.getFirst(getAll(str));
    }
    public T getOrCreate(String str) {
        ensureStringLinkUnlockedInternal();
        T item = get(str, stringItem -> stringItem.equalsValue(str));
        if (item == null) {
            item = createNewString(str);
        }
        return item;
    }
    public T getOrCreate(StyleDocument styleDocument) {
        ensureStringLinkUnlockedInternal();
        String xml = styleDocument.getXml();
        T item = get(xml, StringItem::hasStyle);
        if (item != null) {
            return item;
        }
        item = createNewString();
        item.set(styleDocument);
        return item;
    }
    public T getOrCreate(JSONObject jsonObject) {
        ensureStringLinkUnlockedInternal();
        JSONObject style = jsonObject.optJSONObject(StringItem.NAME_style);
        if (style != null) {
            T item = createNewString();
            item.set(jsonObject);
            StyleDocument styleDocument = item.getStyleDocument();
            if (styleDocument != null) {
                T exist = getString(styleDocument.getXml());
                if (exist != null) {
                    if (item.hasReference()) {
                        item.set((String) null);
                        item.setNull(true);
                    }
                    return exist;
                }
                return item;
            }
        }
        return getOrCreate(jsonObject.getString(StringItem.NAME_string));

    }
    public T createNewString(String str) {
        T item = createNewString();
        item.set(str);
        return item;
    }
    public T createNewString() {
        return mArrayStrings.createNext();
    }

    public final int countStyles() {
        return mArrayStyles.size();
    }
    public boolean isUtf8() {
        return getHeaderBlock().isUtf8();
    }
    public void setUtf8(boolean utf8) {
        getHeaderBlock().setUtf8(utf8);
    }
    public String getEncoding() {
        if (isUtf8()) {
            return "utf-8";
        }
        return "utf-16";
    }
    public void setEncoding(String encoding) {
        setUtf8(encoding != null && !StringsUtil.toLowercase(encoding).startsWith("utf-16"));
    }

    @Override
    protected void onPreRefresh() {
        super.onPreRefresh();
        ensureStringLinkUnlockedInternal();
    }

    @Override
    protected void onChunkRefreshed() {
    }
    @Override
    public void onChunkLoaded() {
        linkStylesInternal();
        reloadPoolMap();
    }

    public void onPreAddInternal(int index, T item) {
    }
    public void onSortedInternal() {
        getStyleArray().sort();
    }
    @Override
    public byte[] getBytes() {
        BytesOutputStream outputStream = new BytesOutputStream(
                getHeaderBlock().getChunkSize());
        try {
            writeBytes(outputStream);
            outputStream.close();
        } catch (IOException ignored) {
        }
        return outputStream.toByteArray();
    }
    @Override
    public JSONArray toJson() {
        return getStringsArray().toJson();
    }
    @Override
    public void fromJson(JSONArray json) {
        if (json == null) {
            return;
        }
        getStringsArray().fromJson(json);
        refresh();
    }

    private boolean containsInternal(T item) {
        return poolMap.containsValue(item.getXml(),
                stringItem -> stringItem.compareTo(item) == 0);
    }
    public void merge(StringPool<T> stringPool) {
        if (stringPool == null || stringPool == this || stringPool.isEmpty()) {
            return;
        }
        ensureStringLinkUnlockedInternal();
        getStringsArray().ensureCapacity(stringPool.size());
        for (T stringItem : stringPool) {
            if (!containsInternal(stringItem)) {
                createNewString().merge(stringItem);
            }
        }
    }
}
