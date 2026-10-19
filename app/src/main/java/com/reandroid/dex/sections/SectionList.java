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
package com.reandroid.dex.sections;

import com.reandroid.arsc.base.OffsetSupplier;
import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.common.ArraySupplier;
import com.reandroid.dex.base.BlockListArray;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.common.SectionTool;
import com.reandroid.dex.header.DexHeader;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.model.SmaliClass;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.ArraySupplierIterator;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.reandroid.utils.function.Predicate;

public class SectionList extends FixedBlockContainer
        implements SectionTool, OffsetSupplier, Iterable<Section<?>> ,
        ArraySupplier<Section<?>>, FullRefresh {

    private final IntegerReference baseOffset;
    private final DexHeader dexHeader;
    private final BlockList<Section<?>> sectionArray;
    private final Map<SectionType<?>, Section<?>> typeMap;
    private final MapList mapList;

    public SectionList() {
        super(1);

        BlockList<Section<?>> sectionArray = new BlockList<>();
        this.sectionArray = sectionArray;

        DexHeader dexHeader = new DexHeader();
        this.baseOffset = dexHeader.getOffsetReference();

        Section<DexHeader> dexHeaderSection = SectionType
                .HEADER.createSpecialSection(baseOffset);
        dexHeaderSection.add(dexHeader);

        Section<MapList> mapListSection = SectionType.MAP_LIST.createSpecialSection(dexHeader.map);
        MapList mapList = new MapList(dexHeader.map);
        mapListSection.add(mapList);

        sectionArray.add(dexHeaderSection);
        sectionArray.add(mapListSection);

        this.typeMap = new HashMap<>();

        addChild(0, sectionArray);

        this.dexHeader = dexHeader;
        this.mapList = mapList;

        typeMap.put(SectionType.HEADER, dexHeaderSection);
        typeMap.put(SectionType.MAP_LIST, mapListSection);
    }

    public int shrink() {
        int result = 0;
        while (true) {
            int count = clearDuplicateData();
            if (count == 0) {
                break;
            }
            result += count;
        }
        result += clearEmptySections();
        return result;
    }
    public int clearDuplicateData() {
        refresh();
        int result = 0;
        SectionType<?>[] remove = SectionType.getRemoveOrderList();
        for (SectionType<?> sectionType : remove) {
            Section<?> section = getSection(sectionType);
            if (section != null) {
                result += section.clearDuplicates();
            }
        }
        if (result != 0) {
            refresh();
        }
        return result;
    }
    public int clearUnused() {
        int result = 0;
        SectionType<?>[] remove = SectionType.getRemoveOrderList();
        for (SectionType<?> sectionType : remove) {
            Section<?> section = getSection(sectionType);
            if (section != null) {
                result += section.clearUnused();
            }
        }
        return result;
    }
    public int clearEmptySections() {
        int result = 0;
        List<Section<?>> sections = CollectionUtil.toList(getSections());
        for (Section<?> section : sections) {
            if (section.isEmpty()) {
                remove(section);
                result ++;
            }
        }
        return result;
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        mapList.refresh();
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        readSections(reader, null);
    }
    void readSections(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        int position = reader.getPosition();
        DexHeader header = getHeader();
        header.getOffsetReference().set(position);
        readSpecialSections(reader);
        readBody(reader, filter);
        reader.seek(position + header.getFileSize());
    }
    private void readSpecialSections(BlockReader reader) throws IOException {
        getSection(SectionType.HEADER).readBytes(reader);
        getSection(SectionType.MAP_LIST).readBytes(reader);
        ensureMapList(getSection(SectionType.HEADER));
        ensureMapList(getSection(SectionType.MAP_LIST));
    }
    private void readBody(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        MapItem[] mapItemList = mapList.getBodyReaderSorted();
        int length = mapItemList.length;
        for (int i = 0; i < length; i++) {
            MapItem mapItem = mapItemList[i];
            SectionType<SectionItem> sectionType = mapItem.getSectionType();
            if (filter == null || filter.test(sectionType)) {
                loadSection(mapItem, reader);
            }
        }
        sectionArray.sort(getOffsetComparator());
        mapList.linkHeader(dexHeader);
    }
    private void loadSection(MapItem mapItem, BlockReader reader) throws IOException {
        Section<?> section = getSection(mapItem.getSectionType());
        if (section == null) {
            section = mapItem.createNewSection();
            add(section);
        }
        if (ownsSection(section)) {
            section.readBytes(reader);
        } else {
            ensureMapList(section);
        }
    }
    @Override
    public boolean isReading() {
        DexContainerBlock containerBlock = getDexContainerBlock();
        if (containerBlock != null) {
            return containerBlock.isReading();
        }
        return true;
    }
    public DexContainerBlock getDexContainerBlock() {
        DexLayoutBlock layoutBlock = getLayoutBlock();
        if (layoutBlock != null) {
            return layoutBlock.getDexContainerBlock();
        }
        return ObjectsUtil.getNull();
    }
    public DexLayoutBlock getLayoutBlock() {
        return getParentInstance(DexLayoutBlock.class);
    }

    public<T1 extends SectionItem> Section<T1> add(Section<T1> section) {
        SectionType<T1> sectionType = section.getSectionType();
        Section<T1> existing = getSection(sectionType);
        if (existing == section) {
            return existing;
        }
        if (existing != null) {
            throw new IllegalArgumentException("Already contains section type: "
                    + sectionType + ", existing = " + existing
                    + ", section = " + section);
        }
        BlockList<Section<?>> sectionArray = this.sectionArray;
        int size = sectionArray.size();
        int index;
        if (sectionType == SectionType.HEADER) {
            index = 0;
        } else if (sectionType == SectionType.MAP_LIST) {
            index = sectionArray.size();
        } else if (section.getOffset() == 0) {
            index = 1;
            for (int i = 1; i < size; i++) {
                SectionType<?> current = sectionArray.get(i).getSectionType();
                if ((sectionType.isIdSection() && current.isIdSection()) ||
                        (sectionType.isDataSection() && current.isDataSection())) {
                    index ++;
                }
            }
        } else {
            index = 1;
            int offset = section.getOffset();
            for (int i = 1; i < size; i++) {
                int current = sectionArray.get(i).getOffset();
                if (offset > current) {
                    index ++;
                }
            }
        }
        sectionArray.add(index, section);
        typeMap.put(section.getSectionType(), section);
        return section;
    }
    public DexHeader getHeader() {
        return dexHeader;
    }
    public MapList getMapList() {
        return mapList;
    }

    public void remove(Section<?> section) {
        if (section == null) {
            return;
        }
        SectionType<?> sectionType = section.getSectionType();
        if (typeMap.remove(sectionType) != section) {
            return;
        }
        section.onRemove(this);
        sectionArray.remove(section);
    }
    private Section<?> cut(Section<?> section) {
        if (sectionArray.remove(section)) {
            typeMap.remove(section.getSectionType());
            return section;
        }
        return null;
    }
    public void clear() {
        Iterator<Section<?>> iterator = getSections();
        while (iterator.hasNext()) {
            Section<?> section = iterator.next();
            section.onRemove(this);
        }
        sectionArray.clearChildes();
        typeMap.clear();
    }
    public void clearPoolMap(SectionType<?> sectionType) {
        Section<?> section = getSection(sectionType);
        if (section != null) {
            section.clearPoolMap();
        }
    }
    public void clearPoolMap() {
        for(Section<?> section : this) {
            section.clearPoolMap();
        }
    }
    public void sortSection(SectionType<?>[] order) {
        //WARN: DO NOT CALL refresh() HERE
        sectionArray.sort(SectionType.comparator(order, Section::getSectionType));
        mapList.sort();
    }
    @Override
    public void refreshFull() {
        SectionType<?>[] sortOrder = SectionType.getSortSectionsOrder();
        for(SectionType<?> sectionType : sortOrder) {
            Section<?> section = getSection(sectionType);
            if (section != null) {
                section.refreshFull();
            }
        }
        clearDuplicateData();
        if (clearEmptySections() != 0) {
            refresh();
        }
    }
    public boolean sortStrings() {
        boolean result = sortItems(SectionType.STRING_DATA);
        if (sortItems(SectionType.STRING_ID)) {
            result = true;
        }
        if (sortItems(SectionType.TYPE_ID)) {
            result = true;
        }
        if (sortItems(SectionType.PROTO_ID)) {
            result = true;
        }
        if (sortItems(SectionType.FIELD_ID)) {
            result = true;
        }
        if (sortItems(SectionType.METHOD_ID)) {
            result = true;
        }
        if (sortItems(SectionType.CLASS_ID)) {
            result = true;
        }
        if (sortItems(SectionType.ANNOTATION_SET)) {
            result = true;
        }
        return result;
    }
    private boolean sortItems(SectionType<?> sectionType) {
        Section<?> section = getSection(sectionType);
        if (section != null) {
            return section.sort();
        }
        return false;
    }
    public<T1 extends SectionItem> T1 getLoaded(SectionType<T1> sectionType, Key key) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.getSectionItem(key);
        }
        return null;
    }
    @Override
    public<T1 extends SectionItem> Section<T1> getSection(SectionType<T1> sectionType) {
        if (sectionType != null) {
            Section<T1> section = getOwnedSection(sectionType);
            if (section == null && sectionType.isSharedSection()) {
                section = getForeignSection(sectionType);
            }
            return section;
        }
        return null;
    }
    private <T1 extends SectionItem> Section<T1> getForeignSection(SectionType<T1> sectionType) {
        DexContainerBlock containerBlock = getDexContainerBlock();
        if (containerBlock != null) {
            Iterator<SectionList> iterator = containerBlock.getSectionLists();
            while (iterator.hasNext()) {
                SectionList sectionList = iterator.next();
                if (sectionList != this) {
                    Section<T1> section = sectionList.getOwnedSection(sectionType);
                    if (section != null) {
                        return section;
                    }
                }
            }
        }
        return null;
    }
    <T1 extends SectionItem> Section<T1> getOwnedSection(SectionType<T1> sectionType) {
        return ObjectsUtil.cast(typeMap.get(sectionType));
    }
    private boolean ownsSection(Section<?> section) {
        return section != null && section.getParent(this.getClass()) == this;
    }
    @Override
    public SectionList getSectionList() {
        return this;
    }
    @Override
    public<T1 extends SectionItem> Section<T1> getOrCreateSection(SectionType<T1> sectionType) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section;
        }
        if (sectionType == SectionType.MAP_LIST || sectionType == SectionType.HEADER) {
            return null;
        }
        MapList mapList = getMapList();
        MapItem mapItem = mapList.getOrCreate(sectionType);
        section = mapItem.createNewSection();
        add(section);
        if (!isReading()) {
            sortSection(SectionType.getR8Order());
            mapItem.link(getHeader());
        }
        return section;
    }
    void ensureInitialized(SectionType<?> ... sectionTypes) {
        if (isReading() || sectionTypes == null || sectionTypes.length == 0) {
            return;
        }
        for (SectionType<?> type : sectionTypes) {
            if (type != null) {
                ensureMapList(getOrCreateSection(type));
            }
        }
        getMapList().linkHeader(getHeader());
    }
    private void ensureMapList(Section<?> section) {
        SectionType<?> sectionType = section.getSectionType();
        MapList mapList = getMapList();
        MapItem mapItem;
        if (isReading()) {
            mapItem = mapList.get(sectionType);
        } else {
            mapItem = mapList.getOrCreate(sectionType);
        }
        section.addCountAndOffset(mapItem.getCountAndOffset());
    }

    public int indexOf(Section<?> section) {
        return sectionArray.indexOf(section);
    }
    @Override
    public Section<?> get(int i) {
        return sectionArray.get(i);
    }

    public boolean contains(Key key) {
        if (key == null) {
            return false;
        }
        SectionType<?> sectionType = SectionType.getSectionType(key);
        if (sectionType != null) {
            return contains(sectionType, key);
        }
        throw new IllegalArgumentException("Unknown key type: " + key.getClass() + ", '" + key + "'");
    }
    private boolean contains(SectionType<?> sectionType, Key key) {
        Section<?> section = getSection(sectionType);
        if (section != null) {
            return section.contains(key);
        }
        return false;
    }

    public void keyChangedInternal(SectionItem item, SectionType<?> sectionType, Key oldKey) {
        Section<?> section = getSection(sectionType);
        if (section == null) {
            return;
        }
        section.keyChanged(item, oldKey);
        if (sectionType == SectionType.TYPE_ID) {
            ClassId classId = getLoaded(SectionType.CLASS_ID, oldKey);
            if (classId != null) {
                // getKey() call triggers keyChanged event
                classId.getKey();
            }
            //TODO: notify to all uses TypeKey
        }
    }
    public Iterator<Section<?>> getSections() {
        return sectionArray.arrayIterator();
    }
    public Iterator<IdSection<?>> getIdSections() {
        return ObjectsUtil.cast(sectionArray.iterator(IdSection.class));
    }
    @Override
    public int getCount() {
        return sectionArray.size();
    }
    public Iterator<Section<?>> getSharedSections() {
        return CollectionUtil.copyOf(sectionArray.iterator(
                section -> section.getSectionType().isSharedSection()));
    }
    @Override
    public Iterator<Section<?>> iterator() {
        return ArraySupplierIterator.of(this);
    }
    @Override
    public IntegerReference getOffsetReference() {
        return baseOffset;
    }
    void transferSharedSectionsFrom(SectionList sectionList) {
        if (sectionList !=  this) {
            Iterator<Section<?>> iterator = sectionList.getSharedSections();
            while (iterator.hasNext()) {
                transferSharedSection(sectionList, iterator.next());
            }
        }
    }
    private void transferSharedSection(SectionList sectionList, Section<?> section) {
        section = sectionList.cut(section);
        if (section != null) {
            this.add(section);
            section.addCountAndOffset(mapList.get(section.getSectionType()).getCountAndOffset());
        }
    }

    private boolean canAddAll(Collection<IdItem> idItemCollection) {
        int reserveSpace = 200;
        Iterator<IdSection<?>> idSections = getIdSections();
        while (idSections.hasNext()) {
            IdSection<?> section = idSections.next();
            if (!section.canAddAll(idItemCollection, reserveSpace)) {
                return false;
            }
        }
        return true;
    }
    public boolean merge(MergeOptions options, ClassId classId) {
        if (classId == null) {
            options.onMergeError(getParentInstance(DexLayoutBlock.class), classId, "Null class id");
            return false;
        }
        if (classId.getParent() == null) {
            options.onMergeError(getParentInstance(DexLayoutBlock.class), classId, "Destroyed class id");
            return false;
        }
        if (classId.getParent(SectionList.class) == this) {
            options.onMergeError(getParentInstance(DexLayoutBlock.class), classId, "Class id is on same section");
            return false;
        }
        if (options.skipMerging(classId, classId.getKey())) {
            return false;
        }
        if (contains(SectionType.CLASS_ID, classId.getKey())) {
            options.onDuplicate(classId);
            return false;
        }
        ArrayCollection<IdItem> collection = classId.listUsedIds();
        if (!canAddAll(collection)) {
            options.onDexFull(getParentInstance(DexLayoutBlock.class), classId);
            return false;
        }
        Section<ClassId> mySection = getOrCreateSection(SectionType.CLASS_ID);

        ClassId myClass = mySection.getOrCreate(classId.getKey());
        myClass.merge(classId);
        if (options.relocateClass()) {
            classId.removeSelf();
        }
        options.onMergeSuccess(classId, classId.getKey());
        return true;
    }
    public boolean merge(MergeOptions options, SectionList sectionList) {
        if (sectionList == this) {
            options.onMergeError(getParentInstance(DexLayoutBlock.class), sectionList, "Can not merge with self");
            return false;
        }
        if (sectionList.getParent() == null) {
            options.onMergeError(getParentInstance(DexLayoutBlock.class), sectionList, "Destroyed section list");
            return false;
        }
        Section<ClassId> comingSection = sectionList.getSection(SectionType.CLASS_ID);
        if (comingSection == null || comingSection.getCount() == 0) {
            return false;
        }
        boolean mergedOnce = false;
        boolean mergedAll = true;
        Section<ClassId> mySection = getOrCreateSection(SectionType.CLASS_ID);
        BlockListArray<ClassId> comingArray = comingSection.getItemArray();
        int size = comingArray.size() - 1;
        for (int i = size; i >= 0; i--) {
            ClassId coming = comingArray.get(i);
            TypeKey key = coming.getKey();
            if (options.skipMerging(coming, key)) {
                continue;
            }
            if (mySection.contains(key)) {
                options.onDuplicate(coming);
                continue;
            }
            ArrayCollection<IdItem> collection = coming.listUsedIds();
            if (!canAddAll(collection)) {
                mergedAll = false;
                options.onDexFull(this.getParentInstance(DexLayoutBlock.class), coming);
                break;
            }
            ClassId classId = mySection.getOrCreate(coming.getKey());
            classId.merge(coming);
            options.onMergeSuccess(coming, key);
            if (options.relocateClass()) {
                coming.removeSelf();
            }
            mergedOnce = true;
        }
        if (comingSection.getCount() == 0) {
            SectionList comingSectionSectionList = comingSection.getSectionList();
            DexLayoutBlock dexLayoutBlock = comingSectionSectionList
                    .getParentInstance(DexLayoutBlock.class);
            dexLayoutBlock.clear();
        }
        if (mergedOnce) {
            sortStrings();
            refresh();
        }
        return mergedAll;
    }
    public ClassId fromSmali(SmaliClass smaliClass) throws IOException {
        ClassId classId = getOrCreateSectionItem(SectionType.CLASS_ID, smaliClass.getKey());
        classId.fromSmali(smaliClass);
        return classId;
    }
    private static<T1 extends Section<?>> Comparator<T1> getOffsetComparator() {
        return (section1, section2) -> {
            if (section1 == section2) {
                return 0;
            }
            if (section1 == null) {
                return 1;
            }
            return section1.compareOffset(section2);
        };
    }
}
