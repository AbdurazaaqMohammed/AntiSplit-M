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
package com.reandroid.dex.model;

import com.reandroid.common.Origin;
import com.reandroid.dex.base.DexException;
import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.common.DexUtils;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.sections.*;
import com.reandroid.dex.smali.*;
import com.reandroid.dex.smali.model.SmaliClass;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.*;
import com.reandroid.utils.io.FileByteSource;
import com.reandroid.utils.io.FileIterator;

import java.io.*;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class DexLayout implements DexClassModule, Closeable,
        Iterable<DexClass> {

    private final DexFile dexFile;
    private final DexLayoutBlock dexLayoutBlock;
    private boolean closed;

    public DexLayout(DexFile dexFile, DexLayoutBlock dexLayoutBlock) {
        this.dexFile = dexFile;
        this.dexLayoutBlock = dexLayoutBlock;
        dexLayoutBlock.setTag(this);
    }

    @Override
    public boolean isMultiLayoutEntry() {
        DexContainerBlock containerBlock = getDexLayoutBlock()
                .getDexContainerBlock();
        if (containerBlock != null) {
            return containerBlock.isMultiLayout();
        }
        return false;
    }
    @Override
    public int getOffset() {
        return getDexLayoutBlock().getHeader().getOffset();
    }
    @Override
    public boolean sort() {
        return getDexLayoutBlock().sortStrings();
    }

    public String getName() {
        return DIRECTORY_PREFIX + getIndex();
    }

    @Override
    public int getVersion() {
        return getDexLayoutBlock().getVersion();
    }
    @Override
    public void setVersion(int version) {
        getDexLayoutBlock().setVersion(version);
    }

    @Override
    public int shrink() {
        return getDexLayoutBlock().getSectionList().shrink();
    }
    public int clearDuplicateData() {
        return getDexLayoutBlock().getSectionList().clearDuplicateData();
    }

    public DexClassRepository getClassRepository() {
        return getDexFile();
    }
    public int getIndex() {
        return getDexLayoutBlock().getIndex();
    }
    public DexFile getDexFile() {
        return this.dexFile;
    }

    @Override
    public Iterator<DexClass> getExtendingClasses(TypeKey typeKey) {
        return ComputeIterator.of(getDexLayoutBlock().getExtendingClassIds(typeKey), this::create);
    }
    @Override
    public Iterator<DexClass> getImplementClasses(TypeKey typeKey) {
        return ComputeIterator.of(getDexLayoutBlock().getImplementationIds(typeKey), this::create);
    }
    @Override
    public DexClass getOrCreateClass(TypeKey key) {
        DexClass dexClass = searchClass(key);
        if (dexClass != null) {
            return dexClass;
        }
        ClassId classId = getOrCreateClassId(key);
        return create(classId);
    }

    @Override
    public Iterator<DexClassModule> modules() {
        return SingleIterator.of(this);
    }

    @Override
    public Iterator<DexClass> iterator() {
        return getDexClasses();
    }
    @Override
    public boolean removeClasses(Predicate<? super DexClass> filter) {
        Predicate<ClassId> classIdFilter = classId -> filter.test(DexLayout.this.create(classId));
        return getDexLayoutBlock().removeEntries(SectionType.CLASS_ID, classIdFilter);
    }
    @Override
    public <T1 extends SectionItem> boolean removeEntries(SectionType<T1> sectionType, Predicate<T1> filter) {
        return getDexLayoutBlock().removeEntries(sectionType, filter);
    }
    @Override
    public <T1 extends SectionItem> boolean removeEntriesWithKey(SectionType<T1> sectionType, Predicate<? super Key> filter) {
        return getDexLayoutBlock().removeWithKeys(sectionType, filter);
    }
    @Override
    public <T1 extends SectionItem> boolean removeEntry(SectionType<T1> sectionType, Key key) {
        return getDexLayoutBlock().removeWithKey(sectionType, key);
    }
    @Override
    public DexClass getDexClass(TypeKey key) {
        ClassId classId = getItem(SectionType.CLASS_ID, key);
        if (classId == null) {
            return null;
        }
        return create(classId);
    }
    @Override
    public Iterator<DexClass> getDexClasses(Predicate<? super TypeKey> filter) {
        return ComputeIterator.of(getItemsIfKey(SectionType.CLASS_ID, ObjectsUtil.cast(filter)), this::create);
    }
    @Override
    public Iterator<DexClass> getDexClassesCloned(Predicate<? super TypeKey> filter) {
        return ComputeIterator.of(
                getClonedItemsIfKey(SectionType.CLASS_ID, ObjectsUtil.cast(filter)),
                this::create);
    }

    @Override
    public DexClassRepository getRootRepository() {
        return getDexFile().getRootRepository();
    }
    public ClassId getOrCreateClassId(TypeKey key) {
        Section<ClassId> section = getOrCreateSection(SectionType.CLASS_ID);
        ClassId classId = section.get(key);
        if (classId != null) {
            return classId;
        }
        classId = section.getOrCreate(key);
        classId.getOrCreateClassData();
        classId.setSuperClass(TypeKey.OBJECT);
        classId.setSourceFile(DexUtils.toSourceFileName(key.getTypeName()));
        classId.addAccessFlag(AccessFlag.PUBLIC);
        return classId;
    }
    private DexClass create(ClassId classId) {
        return new DexClass(this, classId);
    }
    public Marker getOrCreateMarker() {
        Marker marker = CollectionUtil.getFirst(getMarkers());
        if (marker != null) {
            return marker;
        }
        marker = Marker.createR8();
        Section<StringId> stringSection = getSection(SectionType.STRING_ID);

        StringId stringId = stringSection.createItem();
        marker.setStringId(stringId);

        marker.save();

        return marker;
    }
    @Override
    public void addMarker(Marker marker) {
        StringId stringId = marker.getStringId();
        if (stringId == null) {
            Section<StringId> stringSection = getSection(SectionType.STRING_ID);
            stringId = stringSection.createItem();
            marker.setStringId(stringId);
        }
        marker.save();
    }
    @Override
    public Iterator<Marker> getMarkers() {
        return getDexLayoutBlock().getMarkers();
    }
    @Override
    public Iterator<DexSectionInfo> getSectionInfo() {
        MapList mapList = getDexLayoutBlock().getMapList();
        return ComputeIterator.of(mapList.iterator(),
                mapItem -> new DexSectionInfo(DexLayout.this, mapItem));
    }
    @Override
    public void refreshFull() throws DexException {
        getDexLayoutBlock().refreshFull();
    }
    public void sortSection(SectionType<?>[] order) {
        refresh();
        getDexLayoutBlock().sortSection(order);
        refresh();
    }
    @Override
    public void clearPoolMap() {
        getDexLayoutBlock().clearPoolMap();
    }
    public Iterator<DexInstruction> getDexInstructions() {
        return new IterableIterator<DexClass, DexInstruction>(getDexClasses()) {
            @Override
            public Iterator<DexInstruction> iterator(DexClass element) {
                return element.getDexInstructions();
            }
        };
    }
    public Iterator<DexInstruction> getDexInstructionsCloned() {
        return new IterableIterator<DexClass, DexInstruction>(getDexClassesCloned()) {
            @Override
            public Iterator<DexInstruction> iterator(DexClass element) {
                return element.getDexInstructions();
            }
        };
    }
    @Override
    public <T1 extends SectionItem> Section<T1> getSection(SectionType<T1> sectionType) {
        return getDexLayoutBlock().getSection(sectionType);
    }
    @Override
    public <T1 extends SectionItem> Section<T1> getOrCreateSection(SectionType<T1> sectionType) {
        return getDexLayoutBlock().getOrCreateSection(sectionType);
    }
    @Override
    public void refresh() {
        getDexLayoutBlock().refresh();
    }
    public DexLayoutBlock getDexLayoutBlock() {
        return dexLayoutBlock;
    }

    public boolean isEmpty() {
        return getDexLayoutBlock().isEmpty();
    }
    public boolean merge(DexClass dexClass) {
        return merge(new DexMergeOptions(true), dexClass);
    }
    public boolean merge(MergeOptions options, DexClass dexClass) {
        return this.merge(options, dexClass.getId());
    }
    public boolean merge(ClassId classId) {
        return merge(new DexMergeOptions(true), classId);
    }
    public boolean merge(MergeOptions options, ClassId classId) {
        return getDexLayoutBlock().merge(options, classId);
    }
    public boolean merge(MergeOptions options, DexLayout dexLayout) {
        if (dexLayout == null || dexLayout.isEmpty()) {
            return false;
        }
        return getDexLayoutBlock().merge(options, dexLayout.getDexLayoutBlock());
    }
    public void parseSmaliDirectory(File dir) throws IOException {
        parseSmaliDirectory(null, dir);
    }
    public void parseSmaliDirectory(SmaliReaderSetting readerSetting, File dir) throws IOException {
        requireNotClosed();
        if (!dir.isDirectory()) {
            throw new FileNotFoundException("No such directory: " + dir);
        }
        FileIterator iterator = new FileIterator(dir, FileIterator.getExtensionFilter(".smali"));
        FileByteSource byteSource = new FileByteSource();
        SmaliReader reader = new SmaliReader(byteSource);
        reader.setReaderSetting(readerSetting);
        DexLayoutBlock layout = getDexLayoutBlock();
        Section<ClassId> classIdSection = null;
        while (iterator.hasNext()) {
            reader.reset();
            File file = iterator.next();
            byteSource.setFile(file);
            reader.setOrigin(Origin.createNew(file));
            SmaliClass smaliClass = new SmaliClass();
            smaliClass.parse(reader);
            if (classIdSection == null) {
                classIdSection = getSection(SectionType.CLASS_ID);
            }
            if (classIdSection != null && classIdSection.contains(smaliClass.getKey())) {
                throw new IOException(smaliClass.getOrigin() + " Class: "
                        + smaliClass.getKey() + " has already been interned");
            }
            layout.fromSmali(smaliClass);
        }
        sort();
        shrink();
    }

    public void parseSmaliFile(File file) throws IOException {
        requireNotClosed();
        fromSmali(SmaliReader.of(file));
    }
    public void fromSmaliAll(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        while (!reader.finished()) {
            fromSmali(reader);
            reader.skipWhitespacesOrComment();
        }
    }
    public DexClass fromSmali(SmaliReader reader) throws IOException {
        requireNotClosed();
        SmaliClass smaliClass = new SmaliClass();
        smaliClass.parse(reader);
        DexClass dexClass = fromSmali(smaliClass);
        reader.skipWhitespacesOrComment();
        return dexClass;
    }
    public DexClass fromSmali(SmaliClass smaliClass) throws IOException {
        requireNotClosed();
        ClassId classId = getDexLayoutBlock().fromSmali(smaliClass);
        return create(classId);
    }
    @Deprecated
    public void writeSmali(SmaliWriter writer, File root) throws IOException {
        SmaliFileNameFactory fileNameFactory = new SmaliFileNameFactory(root);
        Iterator<ClassId> iterator = getItems(SectionType.CLASS_ID);
        while (iterator.hasNext()) {
            ClassId classId = iterator.next();
            File file = fileNameFactory.getUniqueFilenameForClass(classId.getKey());
            writer.setWriter(file);
            classId.append(writer);
            writer.close();
        }
    }
    public void writeSmali(SmaliWriterSetting writerSetting, File root) throws IOException {
        SmaliFileNameFactory fileNameFactory = new SmaliFileNameFactory(root);
        Iterator<ClassId> iterator = getItems(SectionType.CLASS_ID);
        while (iterator.hasNext()) {
            ClassId classId = iterator.next();
            SmaliWriter writer = new SmaliWriter(writerSetting);
            writer.setWriter(fileNameFactory.getUniqueFilenameForClass(classId.getKey()));
            classId.append(writer);
            writer.close();
        }
    }


    public byte[] getBytes() {
        if (isClosed()) {
            return null;
        }
        if (isEmpty()) {
            return new byte[0];
        }
        return getDexLayoutBlock().getBytes();
    }
    public String printSectionInfo(boolean hex) {
        StringBuilder builder = new StringBuilder();
        boolean appendOnce = false;
        if (isMultiLayoutEntry()) {
            builder.append(getName());
            builder.append(", offset = ");
            builder.append(getOffset());
            appendOnce = true;
        }
        Iterator<DexSectionInfo> iterator = getSectionInfo();
        while (iterator.hasNext()) {
            DexSectionInfo sectionInfo = iterator.next();
            if (appendOnce) {
                builder.append('\n');
            }
            builder.append(' ');
            int index = sectionInfo.getIndex();
            if (index < 10) {
                builder.append(' ');
            }
            builder.append(index);
            builder.append(") ");
            builder.append(sectionInfo.print(hex));
            appendOnce = true;
        }
        return builder.toString();
    }
    private void requireNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("Closed");
        }
    }
    public boolean isClosed() {
        return closed;
    }
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            getDexLayoutBlock().clear();
        }
    }

    public int getDexClassesCountForDebug() {
        Section<?> section = getSection(SectionType.CLASS_ID);
        if (section != null) {
            return section.getCount();
        } else {
            DexSectionInfo sectionInfo = getSectionInfo(SectionType.CLASS_ID);
            if (sectionInfo != null) {
                return sectionInfo.getCount();
            }
        }
        return 0;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isMultiLayoutEntry()) {
            builder.append("offset = ");
            builder.append(getOffset());
            builder.append(", ");
        }
        builder.append("version = ");
        builder.append(getVersion());
        builder.append(", classes = ");
        int classesCount;
        if (getSections(SectionType.CLASS_ID).hasNext()) {
            classesCount = getDexClassesCount();
        } else {
            DexSectionInfo sectionInfo = getSectionInfo(SectionType.CLASS_ID);
            if (sectionInfo != null) {
                classesCount = sectionInfo.getCount();
            } else {
                classesCount = 0;
            }
        }
        builder.append(classesCount);
        return builder.toString();
    }

    public static DexLayout findDexFile(ClassId classId) {
        if (classId == null) {
            return null;
        }
        return DexLayout.findDexFile(classId.getParentInstance(DexLayoutBlock.class));
    }
    public static DexLayout findDexFile(DexLayoutBlock dexLayoutBlock) {
        if (dexLayoutBlock == null) {
            return null;
        }
        Object obj = dexLayoutBlock.getTag();
        if (!(obj instanceof DexLayout)) {
            return null;
        }
        return  (DexLayout) obj;
    }

    public static final String DIRECTORY_PREFIX = ObjectsUtil.of("layout.");
}
