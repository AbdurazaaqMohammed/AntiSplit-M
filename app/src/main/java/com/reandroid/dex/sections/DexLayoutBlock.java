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

import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.dex.base.DexException;
import com.reandroid.dex.common.DexUtils;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.header.DexHeader;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.model.SmaliClass;
import com.reandroid.utils.collection.*;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class DexLayoutBlock extends FixedBlockContainer implements FullRefresh {

    private final SectionList sectionList;

    private final MultiMap<TypeKey, ClassId> extendingClassMap;
    private final MultiMap<TypeKey, ClassId> interfaceMap;

    private Object mTag;

    public DexLayoutBlock() {
        super(1);
        this.sectionList = new SectionList();
        addChild(0, sectionList);

        this.extendingClassMap = new MultiMap<>();
        this.interfaceMap = new MultiMap<>();
    }

    public int getVersion() {
        return getHeader().getVersion();
    }
    public void setVersion(int version) {
        getHeader().setVersion(version);
    }
    public Iterator<Marker> getMarkers() {
        return Marker.parse(getSection(SectionType.STRING_ID));
    }

    public Iterator<ClassId> getExtendingOrImplementing(TypeKey typeKey) {
        Iterator<ClassId> iterator = CombiningIterator.two(getExtendingClassIds(typeKey),
                getImplementationIds(typeKey));
        return new IterableIterator<ClassId, ClassId>(iterator) {
            @Override
            public Iterator<ClassId> iterator(ClassId element) {
                return CombiningIterator.singleOne(element, getExtendingOrImplementing(element.getKey()));
            }
        };
    }
    public Iterator<ClassId> getExtendingClassIds(TypeKey typeKey) {
        if (extendingClassMap.size() == 0) {
            loadExtendingClassMap();
        }
        return this.extendingClassMap.getAll(typeKey);
    }
    public Iterator<ClassId> getImplementationIds(TypeKey interfaceClass) {
        if (interfaceMap.size() == 0) {
            loadInterfacesMap();
        }
        return this.interfaceMap.getAll(interfaceClass);
    }

    public void clear() {
        extendingClassMap.clear();
        interfaceMap.clear();
        getSectionList().clear();
    }
    private void loadExtendingClassMap() {
        MultiMap<TypeKey, ClassId> superClassMap = this.extendingClassMap;
        superClassMap.clear();
        Section<ClassId> section = getSectionList().getSection(SectionType.CLASS_ID);
        if (section == null) {
            return;
        }
        superClassMap.setInitialSize(section.getCount());
        for (ClassId classId : section) {
            TypeKey typeKey = classId.getSuperClassKey();
            if (!DexUtils.isJavaFramework(typeKey.getTypeName())) {
                superClassMap.put(typeKey, classId);
            }
        }
    }
    private void loadInterfacesMap() {
        MultiMap<TypeKey, ClassId> interfaceMap = this.interfaceMap;
        interfaceMap.clear();
        Section<ClassId> section = getSectionList().getSection(SectionType.CLASS_ID);
        if (section == null) {
            return;
        }
        for (ClassId classId : section) {
            for (TypeKey typeKey : classId.getInterfacesKey()) {
                if (!DexUtils.isJavaFramework(typeKey.getTypeName())) {
                    interfaceMap.put(typeKey, classId);
                }
            }
        }
    }
    public Iterator<StringId> getStrings() {
        return getItems(SectionType.STRING_ID);
    }
    public<T1 extends SectionItem> Iterator<T1> getClonedItems(SectionType<T1> sectionType) {
        Section<T1> section = getSectionList().getSection(sectionType);
        if (section != null) {
            return section.clonedIterator();
        }
        return EmptyIterator.of();
    }
    public<T1 extends SectionItem> Iterator<T1> getItems(SectionType<T1> sectionType) {
        Section<T1> section = getSectionList().getSection(sectionType);
        if (section != null) {
            return section.iterator();
        }
        return EmptyIterator.of();
    }
    @Override
    public void refreshFull() throws DexException {
        this.onPreRefresh();
        sortStrings();
        getSectionList().refreshFull();
        this.onRefreshed();
    }
    @Override
    protected void onPreRefresh() {
        super.onPreRefresh();
        interfaceMap.clear();
        extendingClassMap.clear();
        this.updateHeaderOffset();
    }
    @Override
    protected void onRefreshed() {
        this.updateChecksumAndSignature();
    }

    // Updating checksum/signature is expensive operation, but
    // checksum (alder32) is a lot faster than signature (sha1), thus our logic is:
    //   * Update checksum, if the value changes then repeat with refresh
    //   * If checksum is not changed at first attempt, then no need of other action
    //   * If checksum is changed after the first attempt, then update sig & cs
    //   * Normally it requires not more than 3 trials to update but throws unreachable after
    //     trying 10 times
    private void updateChecksumAndSignature() {
        DexHeader dexHeader = getHeader();
        SectionList sectionList = getSectionList();
        int maximumTrials = 10;
        int i = 0;
        while (i < maximumTrials) {
            if (dexHeader.updateChecksum()) {
                sectionList.refresh();
            } else {
                if (i != 0) {
                    dexHeader.updateSignature();
                    dexHeader.updateChecksum();
                }
                return;
            }
            i ++;
        }
        throw new RuntimeException("Failed to update checksums, trial = " + i);
    }
    private void updateHeaderOffset() {
        DexLayoutBlock previousLayoutBlock = getPreviousLayoutBlock();
        int offset = 0;
        if (previousLayoutBlock != null) {
            DexHeader header = previousLayoutBlock.getHeader();
            offset = header.getOffsetReference().get() + header.fileSize.get();
        }
        getHeader().getOffsetReference().set(offset);
    }
    private DexLayoutBlock getPreviousLayoutBlock() {
        DexContainerBlock containerBlock = getParentInstance(DexContainerBlock.class);
        if (containerBlock != null) {
            return containerBlock.get(getIndex() - 1);
        }
        return null;
    }
    public void sortSection(SectionType<?>[] order) {
        refresh();
        getSectionList().sortSection(order);
        refresh();
    }
    public int clearEmptySections() {
        return getSectionList().clearEmptySections();
    }
    public void clearPoolMap(SectionType<?> sectionType) {
        getSectionList().clearPoolMap(sectionType);
    }
    public void clearPoolMap() {
        extendingClassMap.clear();
        interfaceMap.clear();
        getSectionList().clearPoolMap();
    }
    public boolean sortStrings() {
        return getSectionList().sortStrings();
    }

    public <T1 extends SectionItem> Iterator<T1> getAll(SectionType<T1> sectionType, Key key) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.getAll(key);
        }
        return EmptyIterator.of();
    }
    public <T1 extends SectionItem> boolean removeEntries(SectionType<T1> sectionType, Predicate<T1> filter) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.removeEntries(filter);
        }
        return false;
    }
    public <T1 extends SectionItem> boolean removeWithKeys(SectionType<T1> sectionType, Predicate<? super Key> filter) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.removeWithKeys(filter);
        }
        return false;
    }
    public <T1 extends SectionItem> boolean removeWithKey(SectionType<T1> sectionType, Key key) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.remove(key);
        }
        return false;
    }
    public <T1 extends SectionItem> T1 getItem(SectionType<T1> sectionType, Key key) {
        Section<T1> section = getSection(sectionType);
        if (section != null) {
            return section.getSectionItem(key);
        }
        return null;
    }
    public<T1 extends SectionItem> Section<T1> getSection(SectionType<T1> sectionType) {
        return getSectionList().getSection(sectionType);
    }
    public<T1 extends SectionItem> Section<T1> getOrCreateSection(SectionType<T1> sectionType) {
        return getSectionList().getOrCreateSection(sectionType);
    }
    public DexHeader getHeader() {
        return getSectionList().getHeader();
    }
    public int getFileSize() {
        return getHeader().getFileSize();
    }
    public SectionList getSectionList() {
        return sectionList;
    }
    public MapList getMapList() {
        return getSectionList().getMapList();
    }
    public boolean isEmpty() {
        SectionList sectionList = getSectionList();
        Section<?> section = sectionList.getOwnedSection(SectionType.CLASS_ID);
        if (section != null && !section.isEmpty()) {
            return false;
        }
        DexContainerBlock containerBlock = getDexContainerBlock();
        if (containerBlock != null && containerBlock.isMultiLayout()) {
            section = sectionList.getOwnedSection(SectionType.STRING_ID);
            return section == null || section.isEmpty();
        }
        return false;
    }
    void transferSharedSectionsFrom(DexLayoutBlock layoutBlock) {
        if (layoutBlock != this) {
            this.getSectionList().transferSharedSectionsFrom(layoutBlock.getSectionList());
        }
    }
    public void removeSelf() {
        DexContainerBlock containerBlock = getDexContainerBlock();
        if (containerBlock != null) {
            containerBlock.remove(this);
        }
    }
    public DexContainerBlock getDexContainerBlock() {
        return getParentInstance(DexContainerBlock.class);
    }
    public boolean merge(MergeOptions options, ClassId classId) {
        return getSectionList().merge(options, classId);
    }
    public boolean merge(MergeOptions options, DexLayoutBlock layoutBlock) {
        if (layoutBlock == this) {
            options.onMergeError(this, getSectionList(), "Can not merge dex file to self");
            return false;
        }
        return getSectionList().merge(options, layoutBlock.getSectionList());
    }
    public ClassId fromSmali(SmaliClass smaliClass) throws IOException {
        return getSectionList().fromSmali(smaliClass);
    }
    @Override
    public byte[] getBytes() {
        BytesOutputStream outputStream = new BytesOutputStream(getFileSize());
        try {
            writeBytes(outputStream);
            outputStream.close();
        } catch (IOException ignored) {
        }
        return outputStream.toByteArray();
    }

    public void readBytes(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        getSectionList().readSections(reader, filter);
    }
    public void write(File file) throws IOException {
        OutputStream outputStream = FileUtil.outputStream(file);
        writeBytes(outputStream);
        outputStream.close();
    }


    public Object getTag() {
        return mTag;
    }
    public void setTag(Object tag) {
        this.mTag = tag;
    }

    void initialize(SectionType<?> ... sectionTypes) {
        SectionList sectionList = getSectionList();
        MapList mapList = sectionList.getMapList();
        mapList.getOrCreate(SectionType.HEADER);
        mapList.getOrCreate(SectionType.MAP_LIST);
        sectionList.ensureInitialized(sectionTypes);
        sectionList.getMapList().linkHeader(sectionList.getHeader());
    }
    public static DexLayoutBlock createDefault(SectionType<?>[] sectionTypes) {
        DexLayoutBlock dexLayoutBlock = new DexLayoutBlock();
        SectionList sectionList = dexLayoutBlock.getSectionList();
        MapList mapList = sectionList.getMapList();
        mapList.getOrCreate(SectionType.HEADER);
        mapList.getOrCreate(SectionType.MAP_LIST);
        for (SectionType<?> sectionType : sectionTypes) {
            sectionList.getOrCreateSection(sectionType);
        }
        sectionList.getMapList().linkHeader(sectionList.getHeader());
        return dexLayoutBlock;
    }
}
