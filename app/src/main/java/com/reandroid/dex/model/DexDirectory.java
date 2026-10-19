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

import com.reandroid.archive.ZipEntryMap;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.FieldId;
import com.reandroid.dex.id.MethodId;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyPair;
import com.reandroid.dex.key.KeyReference;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.sections.MergeOptions;
import com.reandroid.dex.sections.Section;
import com.reandroid.dex.sections.SectionArray;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.SmaliWriterSetting;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.EmptyList;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class DexDirectory implements Iterable<DexFile>, Closeable,
        DexClassRepository, FullRefresh {

    private final DexFileSourceSet dexSourceSet;
    private Object mTag;
    private final ArrayCollection<KeyReference> externalKeyReferenceList;

    public DexDirectory() {
        this.dexSourceSet = new DexFileSourceSet();
        this.externalKeyReferenceList = new ArrayCollection<>();
    }

    public Object getTag() {
        return mTag;
    }
    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public int mergeAll(MergeOptions options, Iterable<DexClass> iterable){
        return mergeAll(options, iterable.iterator());
    }
    public int mergeAll(MergeOptions options, Iterator<DexClass> iterator){
        int result = 0;
        while (iterator.hasNext()){
            boolean merged = merge(options, iterator.next());
            if(merged){
                result ++;
            }
        }
        return result;
    }
    public boolean merge(DexClass dexClass){
        return merge(new DexMergeOptions(), dexClass);
    }
    public boolean merge(MergeOptions options, DexClass dexClass){
        if(dexClass.isInSameDirectory(this)){
            return false;
        }
        if(containsClass(dexClass.getKey())){
            options.onDuplicate(dexClass.getId());
            return false;
        }
        boolean startChanged = false;
        int start = options.getMergeStartDexFile();
        for(int i = start; i < size(); i ++){
            DexFile dexFile = get(i);
            if(dexFile.merge(options, dexClass)){
                if(startChanged){
                    options.setMergeStartDexFile(i);
                }
                return true;
            }
            startChanged = true;
        }
        return false;
    }
    public void merge(DexDirectory directory){
        merge(new DexMergeOptions(false), directory);
    }
    public void merge(MergeOptions options, DexDirectory directory){
        if(directory == this){
            throw new IllegalArgumentException("Cyclic merge");
        }
        int start = options.getMergeStartDexFile();
        int i = start;
        while (true){
            DexFile dexFile = this.get(i);
            DexFile last = directory.getLastNonEmpty(options,0);
            if(dexFile == null || last == null){
                break;
            }
            if(!dexFile.merge(options, last)){
                i ++;
            }
        }
        if(i != start){
            options.setMergeStartDexFile(i);
        }
        shrink();
        directory.merge(options);
        getDexSourceSet().merge(directory.getDexSourceSet());
    }
    public void merge(){
        merge(new DexMergeOptions());
    }
    public void merge(MergeOptions options){
        if(size() < 2){
            return;
        }
        int i = 0;
        while (true){
            DexFile dexFile = get(i);
            DexFile last = getLastNonEmpty(options,i + 1);
            if(dexFile == null || last == null){
                break;
            }
            if(!dexFile.merge(options, last)){
                i ++;
            }
        }
        shrink();
    }
    private DexFile getLastNonEmpty(MergeOptions options, int limit){
        int size = size() - 1;
        for(int i = size; i >= limit; i--){
            DexFile dexFile = get(i);
            if(!options.isEmptyDexFile(dexFile.getContainerBlock())){
                return dexFile;
            }
        }
        return null;
    }
    public int clearDuplicateData(){
        int result = 0;
        for(DexFile dexFile : this){
            result += dexFile.clearDuplicateData();
        }
        return result;
    }
    public int clearUnused(){
        int result = 0;
        for(DexFile dexFile : this){
            result += dexFile.clearUnused();
        }
        return result;
    }
    public void save() throws IOException {
        dexSourceSet.saveAll();
    }
    public void save(File dir) throws IOException {
        dexSourceSet.saveAll(dir);
    }
    public Iterator<ClassId> getClassIds() {
        return getItems(SectionType.CLASS_ID);
    }
    public <T1 extends SectionItem> T1 get(SectionType<T1> sectionType, Key key){
        for (DexFile dexFile : this) {
            T1 item = dexFile.getItem(sectionType, key);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
    public Iterator<DexInstruction> getDexInstructions() {
        return new IterableIterator<DexFile, DexInstruction>(iterator()) {
            @Override
            public Iterator<DexInstruction> iterator(DexFile element) {
                return element.getDexInstructions();
            }
        };
    }
    public Iterator<DexInstruction> getDexInstructionsCloned() {
        return new IterableIterator<DexFile, DexInstruction>(clonedIterator()) {
            @Override
            public Iterator<DexInstruction> iterator(DexFile element) {
                return element.getDexInstructionsCloned();
            }
        };
    }
    @Override
    public Iterator<DexFile> iterator() {
        return dexSourceSet.getDexFiles();
    }
    public Iterator<DexFile> clonedIterator() {
        return dexSourceSet.getClonedDexFiles();
    }

    @Override
    public void refreshFull() {
        for(DexFile dexFile : this){
            dexFile.setDexDirectory(this);
            dexFile.refreshFull();
        }
    }
    @Override
    public void refresh(){
        for(DexFile dexFile : this){
            dexFile.setDexDirectory(this);
            dexFile.refresh();
        }
    }
    public void updateDexFileList(){
        for(DexFile dexFile : this){
            dexFile.setDexDirectory(this);
        }
    }
    public void addDirectory(File dir) throws IOException {
        getDexSourceSet().addAll(dir);
        for(DexFile dexFile : this){
            dexFile.setDexDirectory(this);
        }
    }
    public void addApk(ZipEntryMap zipEntryMap) throws IOException {
        addZip(zipEntryMap, "");
    }
    public void addZip(ZipEntryMap zipEntryMap, String root) throws IOException {
        getDexSourceSet().addAll(zipEntryMap, root);
        for(DexFile dexFile : this){
            dexFile.setDexDirectory(this);
        }
    }
    public void addFile(File file) throws IOException {
        DexSource<DexFile> source = getDexSourceSet().add(file);
        if(file.isFile()){
            source.get().setDexDirectory(this);
        }
    }
    public ZipEntryMap getZipEntryMap() {
        return getDexSourceSet().getZipEntryMap();
    }
    public void setZipEntryMap(ZipEntryMap zipEntryMap) {
        getDexSourceSet().setZipEntryMap(zipEntryMap);
    }
    public DexFile createDefault(){
        DexFileSourceSet sourceSet = getDexSourceSet();
        if(size() == 0 && sourceSet.getZipEntryMap() == null) {
            sourceSet.setZipEntryMap(new ZipEntryMap());
        }
        DexSource<DexFile> source = sourceSet.createNext();
        DexFile dexFile = DexFile.createDefault();
        source.set(dexFile);
        dexFile.setDexDirectory(this);
        dexFile.setSimpleName(source.toString());
        int version = getVersion();
        if(version != 0) {
            dexFile.setVersion(version);
        }
        return dexFile;
    }
    public DexFileSourceSet getDexSourceSet() {
        return dexSourceSet;
    }

    public int rename(TypeKey search, TypeKey replace){
        if(containsClass(replace)){
            throw new RuntimeException("Duplicate: " + search + " --> " + replace);
        }
        int count = 0;
        Iterator<?> iterator = renameTypes(search, replace, true, true);
        while (iterator.hasNext()){
            iterator.next();
            count++;
        }
        return count;
    }

    public Iterator<StringId> renameTypes(TypeKey search, TypeKey replace){
        return renameTypes(search, replace, true, true);
    }
    public Iterator<StringId> renameTypes(TypeKey search, TypeKey replace, boolean renameInner, boolean renameJava){
        return renameTypes(new KeyPair<>(search, replace), renameInner, renameJava);
    }
    public Iterator<StringId> renameTypes(KeyPair<TypeKey, TypeKey> pair, boolean renameInner, boolean renameJava){
        return FilterIterator.of(getClonedItems(SectionType.STRING_ID),
                stringId -> renameTypes(stringId, pair, renameInner, renameJava));
    }
    public Iterator<StringId> renameTypes(Iterable<KeyPair<TypeKey, TypeKey>> iterable, boolean renameInner, boolean renameJava){
        return FilterIterator.of(getClonedItems(SectionType.STRING_ID),
                stringId -> renameTypes(stringId, iterable, renameInner, renameJava));
    }
    boolean renameTypes(StringId stringId, Iterable<KeyPair<TypeKey, TypeKey>> iterable, boolean renameInner, boolean renameJava){
        for(KeyPair<TypeKey, TypeKey> pair : iterable){
            boolean renamed = renameTypes(stringId, pair, renameInner, renameJava);
            if(renamed){
                return true;
            }
        }
        return false;
    }
    boolean renameTypes(StringId stringId, KeyPair<TypeKey, TypeKey> pair, boolean renameInner, boolean renameJava){
        boolean renamed = renameTypeString(stringId, pair, renameInner, renameJava);
        if(renamed){
            DexClass dexClass = getDexClass(TypeKey.create(stringId.getString()));
            if(dexClass != null){
                dexClass.fixDalvikInnerClassName();
            }
        }
        return renamed;
    }
    private boolean renameTypeString(StringId stringId, KeyPair<TypeKey, TypeKey> pair, boolean renameInner, boolean renameJava){

        String text = stringId.getString();

        TypeKey search = pair.getFirst();
        TypeKey replace = pair.getSecond();
        String type = search.getTypeName();
        String type2 = replace.getTypeName();

        if(type.equals(text)){
            stringId.setString(type2);
            return true;
        }
        if(renameInner){
            type = type.replace(';', '$');
            if(text.startsWith(type)){
                type2 = replace.getTypeName();
                type2 = type2.replace(';', '$');
                text = text.substring(type.length());
                stringId.setString(type2 + text);
                return true;
            }
        }
        type = search.getSignatureTypeName();
        if(type.equals(text)){
            type2 = replace.getSignatureTypeName();
            stringId.setString(type2);
            return true;
        }
        type = search.getArrayType(1);
        if(type.equals(text)){
            type2 = replace.getArrayType(1);
            stringId.setString(type2);
            return true;
        }
        if(renameInner){
            type = type.replace(';', '$');
            if(text.startsWith(type)){
                type2 = replace.getArrayType(1);
                type2 = type2.replace(';', '$');
                text = text.substring(type.length());
                stringId.setString(type2 + text);
                return true;
            }
        }
        type = search.getArrayType(2);
        if(type.equals(text)){
            type2 = replace.getArrayType(2);
            stringId.setString(type2);
            return true;
        }
        if(renameInner){
            type = type.replace(';', '$');
            if(text.startsWith(type)){
                type2 = replace.getArrayType(2);
                type2 = type2.replace(';', '$');
                text = text.substring(type.length());
                stringId.setString(type2 + text);
                return true;
            }
        }
        type = search.getArrayType(3);
        if(type.equals(text)){
            type2 = replace.getArrayType(3);
            stringId.setString(type2);
            return true;
        }
        if(renameInner){
            type = type.replace(';', '$');
            if(text.startsWith(type)){
                type2 = replace.getArrayType(3);
                type2 = type2.replace(';', '$');
                text = text.substring(type.length());
                stringId.setString(type2 + text);
                return true;
            }
        }
        if(renameJava){
            type = search.getSourceName();
            if(type.equals(text)){
                type2 = replace.getSourceName();
                stringId.setString(type2);
                return true;
            }
            if(renameInner){
                type = type + "$";
                if(text.startsWith(type)){
                    type2 = replace.getSourceName();
                    type2 = type2 + "$";
                    text = text.substring(type.length());
                    stringId.setString(type2 + text);
                    return true;
                }
                type = type + ".";
                if(text.startsWith(type)){
                    type2 = replace.getSourceName();
                    type2 = type2 + ".";
                    text = text.substring(type.length());
                    stringId.setString(type2 + text);
                    return true;
                }
            }
        }
        return false;
    }
    public List<MethodKey> replace(MethodKey methodKey, String name){
        List<MethodKey> results = rename(methodKey, name);
        if(!results.isEmpty()){
            return results;
        }
        List<MethodId> methodIdList = CollectionUtil.toList(getItems(SectionType.METHOD_ID, methodKey));
        int size = methodIdList.size();
        if(size == 0){
            return EmptyList.of();
        }
        results = new ArrayCollection<>(size);
        for(MethodId methodId : methodIdList){
            methodId.setName(name);
            results.add(methodId.getKey());
        }
        return results;
    }
    public List<MethodKey> rename(MethodKey methodKey, String name){
        if(containsDeepSearch(methodKey.changeName(name))){
            return EmptyList.of();
        }
        ArrayCollection<MethodId> methodIdList = new ArrayCollection<>();
        methodIdList.addAll(getMethodIds(methodKey));
        if(methodIdList.size() == 0){
            return EmptyList.of();
        }
        MethodKey renamed = methodKey.changeName(name);
        for(MethodId methodId : methodIdList){
            if(renamed.equals(methodId.getKey())){
                throw new IllegalArgumentException("Duplicate: " + renamed);
            }
        }
        List<MethodKey> results = new ArrayCollection<>(methodIdList.size());
        for(MethodId methodId : methodIdList){
            methodId.setName(name);
            results.add(methodId.getKey());
        }
        return results;
    }
    public List<FieldKey> replace(FieldKey fieldKey, String name){
        List<FieldKey> results = rename(fieldKey, name);
        if(!results.isEmpty()){
            return results;
        }
        List<FieldId> fieldIdList = CollectionUtil.toList(getItems(SectionType.FIELD_ID, fieldKey));
        int size = fieldIdList.size();
        if(size == 0){
            return EmptyList.of();
        }
        results = new ArrayCollection<>(size);
        for(FieldId fieldId : fieldIdList){
            fieldId.setName(name);
            results.add(fieldId.getKey());
        }
        return results;
    }
    public List<FieldKey> rename(FieldKey fieldKey, String name){
        ArrayCollection<FieldKey> existingFields = ArrayCollection.of(findEquivalentFields(fieldKey.changeName(name)));
        ArrayCollection<FieldId> fieldIdList = ArrayCollection.of(getItems(SectionType.FIELD_ID, fieldKey));
        if(fieldIdList.isEmpty()){
            return EmptyList.of();
        }
        if(!existingFields.isEmpty()){
            throw new IllegalArgumentException("Conflicting fields: " + existingFields.getFirst());
        }
        FieldKey renamed = fieldKey.changeName(name);
        for(FieldId fieldId : fieldIdList){
            if(renamed.equals(fieldId.getKey())){
                throw new IllegalArgumentException("Duplicate: " + renamed);
            }
        }
        List<FieldKey> results = new ArrayCollection<>(fieldIdList.size());
        for(FieldId fieldId : fieldIdList){
            fieldId.setName(name);
            results.add(fieldId.getKey());
        }
        return results;
    }
    public boolean containsDeepSearch(MethodKey methodKey){
        DexClass startClass = getDexClass(methodKey.getDeclaring());
        if(startClass == null){
            return false;
        }
        if(startClass.containsDeclaredMethod(methodKey)){
            return true;
        }
        Iterator<DexClass> iterator = startClass.getOverridingAndSuperTypes();
        while (iterator.hasNext()){
            DexClass dexClass = iterator.next();
            if(dexClass.containsDeclaredMethod(methodKey)){
                return true;
            }
        }
        return false;
    }
    public boolean containsDeepSearch(FieldKey fieldKey){
        DexClass startClass = getDexClass(fieldKey.getDeclaring());
        if(startClass == null){
            return false;
        }
        Iterator<DexClass> iterator = startClass.getOverridingAndSuperTypes();
        while (iterator.hasNext()){
            DexClass dexClass = iterator.next();
            FieldKey key = fieldKey.changeDeclaring(dexClass.getKey());
            if(fieldKey.equals(key)){
                return true;
            }
        }
        return false;
    }

    @Override
    public DexClassRepository getRootRepository() {
        return this;
    }
    @Override
    public Iterator<DexClassModule> modules() {
        return new IterableIterator<DexFile, DexClassModule>(iterator()) {
            @Override
            public Iterator<DexClassModule> iterator(DexFile element) {
                return element.modules();
            }
        };
    }

    public int distributeClasses(int maxClassesPerDex) {
        if(maxClassesPerDex <= 0){
            throw new IllegalArgumentException(
                    "Classes per dex must be greater than zero: " + maxClassesPerDex);
        }
        int size = this.size();
        if(size == 0){
            return 0;
        }
        int count = this.getDexClassesCount();
        int classesPerDex = count / size;
        while (classesPerDex > maxClassesPerDex){
            this.createDefault();
            int check = this.size();
            if(check <= size){
                throw new IllegalArgumentException("Failed to create next dex");
            }
            size = check;
            classesPerDex = count / size;
        }
        int result = 0;
        for(int i = 0; i < size; i++){
            result += distributeClasses(this.get(i), classesPerDex);
        }
        return result;
    }
    private int distributeClasses(DexFile source, int classesPerDex){
        int result = 0;
        DexDirectory directory = source.getDexDirectory();
        for(int i = 0; i < directory.size(); i++) {
            DexLayout sourceLayout = source.getFirst();
            if (sourceLayout == null) {
                continue;
            }
            DexLayout destinationLayout = directory.get(i).getOrCreateFirst();
            result += distributeClasses(sourceLayout, destinationLayout, classesPerDex);
        }
        return result;
    }
    private int distributeClasses(DexLayout source, DexLayout destination, int classesPerDex){
        int result = 0;
        if(source.getDexLayoutBlock() == destination.getDexLayoutBlock()){
            return result;
        }
        Section<ClassId> classSection = source.getSection(SectionType.CLASS_ID);
        ClassId previous = null;
        SectionArray<ClassId> array = classSection.getItemArray();
        while (source.getDexClassesCount() > classesPerDex && destination.getDexClassesCount() < classesPerDex){
            ClassId classId = array.getLast();
            if(classId == previous){
                break;
            }
            destination.merge(classId);
            previous = classId;
            result ++;
        }
        return result;
    }
    public DexFile get(int i){
        return dexSourceSet.getDexFile(i);
    }
    public int indexOf(DexFile dexFile){
        int size = size();
        for(int i = 0; i < size; i++){
            if(dexFile == get(i)){
                return i;
            }
        }
        return -1;
    }
    public int size() {
        return dexSourceSet.size();
    }
    public DexFile getFirst(){
        DexSource<DexFile> source = dexSourceSet.getFirst();
        if(source != null) {
            return source.get();
        }
        return null;
    }
    public DexFile getLast(){
        DexSource<DexFile> source = dexSourceSet.getLast();
        if(source != null) {
            return source.get();
        }
        return null;
    }

    @Override
    public Iterator<? extends KeyReference> getExternalReferences() {
        return externalKeyReferenceList.iterator();
    }
    public void addExternalKeyReference(KeyReference reference) {
        if (reference != null && !externalKeyReferenceList.contains(reference)) {
            externalKeyReferenceList.add(reference);
        }
    }
    public void clearExternalTypeKeyReferences() {
        externalKeyReferenceList.clear();
    }

    @Override
    public void close() throws IOException {
        this.dexSourceSet.close();
        this.clearExternalTypeKeyReferences();
    }

    @Deprecated
    public void writeSmali(SmaliWriter writer, File root) throws IOException {
        SmaliWriterSetting writerSetting = writer.getWriterSetting();
        for (DexFile dexFile : this) {
            File dir = new File(root, dexFile.buildSmaliDirectoryName());
            dexFile.writeSmali(writerSetting, dir);
        }
    }
    public void writeSmali(SmaliWriterSetting writerSetting, File root) throws IOException {
        writeSmali(writerSetting, root, null);
    }
    public void writeSmali(SmaliWriterSetting writerSetting, File root,
                           Predicate<? super DexFile> predicate) throws IOException {
        for (DexFile dexFile : this) {
            if (predicate == null || predicate.test(dexFile)) {
                File dir = new File(root, dexFile.buildSmaliDirectoryName());
                dexFile.writeSmali(writerSetting, dir);
            }
        }
    }

    @Override
    public String toString() {
        return "DexFiles = " + size();
    }

    public static DexDirectory fromZip(ZipEntryMap zipEntryMap) throws IOException {
        return fromZip(zipEntryMap, null);
    }
    public static DexDirectory fromZip(ZipEntryMap zipEntryMap, Predicate<SectionType<?>> readFilter) throws IOException {
        DexDirectory dexDirectory = new DexDirectory();
        DexFileSourceSet sourceSet = dexDirectory.getDexSourceSet();
        sourceSet.setReadFilter(readFilter);
        sourceSet.addAll(zipEntryMap);
        dexDirectory.updateDexFileList();
        return dexDirectory;
    }
    public static DexDirectory fromDexFilesDirectory(File dir, Predicate<SectionType<?>> readFilter) throws IOException {
        DexDirectory dexDirectory = new DexDirectory();
        DexFileSourceSet sourceSet = dexDirectory.getDexSourceSet();
        sourceSet.setReadFilter(readFilter);
        sourceSet.addAll(dir);
        dexDirectory.updateDexFileList();
        return dexDirectory;
    }

    public static DexDirectory readMapList(ZipEntryMap zipEntryMap) throws IOException {
        return fromZip(zipEntryMap, CollectionUtil.getRejectAll());
    }
    public static DexDirectory readStrings(ZipEntryMap zipEntryMap) throws IOException {
        return fromZip(zipEntryMap, sectionType ->
                sectionType == SectionType.STRING_ID || sectionType == SectionType.STRING_DATA);
    }
}
