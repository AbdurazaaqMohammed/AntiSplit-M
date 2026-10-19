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
package com.reandroid.arsc.refactor;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.arsc.item.TypeString;
import com.reandroid.arsc.list.EntryItemList;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.TypeStringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.FilterIterator;

import java.util.*;
import com.reandroid.utils.function.Predicate;

public class ResourceBuilder {

    private ResourceMergeOption mMergeOption;
    private final TableBlock sourceTable;
    private ApkModule resultModule;
    private final Map<Integer, Integer> resourceIdMap;
    private boolean mRebuilt;

    public ResourceBuilder(ResourceMergeOption mergeOption, TableBlock sourceTable) {
        this.mMergeOption = mergeOption;
        this.sourceTable = sourceTable;
        this.resourceIdMap = new HashMap<>();
    }
    public ResourceBuilder(TableBlock sourceTable) {
        this(new ResourceMergeOption(), sourceTable);
    }

    public void rebuildTo(ApkModule apkModule) {
        rebuild();
        rebuildManifest(apkModule);
        transferResFiles(apkModule);
        transferTableBlock(apkModule);
    }

    public void rebuild() {
        if(mRebuilt) {
            return;
        }
        mRebuilt = true;
        resultModule = new ApkModule();
        TableBlock resultTable = new TableBlock();
        resultModule.setTableBlock(resultTable);
        initializeTable();
        mergePackages();
        resultTable.refreshFull();
    }

    public int applyIdChanges(Iterator<IntegerReference> iterator) {
        int count = 0;
        Map<Integer, Integer> idMap = getResourceIdMap();
        while (iterator.hasNext()){
            IntegerReference reference = iterator.next();
            Integer value = idMap.get(reference.get());
            if(value != null) {
                reference.set(value);
                count ++;
            }
        }
        return count;
    }
    public Map<Integer, Integer> getResourceIdMap() {
        return resourceIdMap;
    }
    private void addIdMap(int search, int replace){
        if(search == replace ||
                search == 0 || replace == 0 ||
                search == -1 || replace == -1){
            return;
        }
        resourceIdMap.put(search, replace);
    }
    public TableBlock getResultTable() {
        return getResultModule().getTableBlock();
    }
    public ApkModule getResultModule() {
        return resultModule;
    }
    public TableBlock getSourceTable() {
        return sourceTable;
    }

    public ResourceMergeOption getMergeOption() {
        ResourceMergeOption mergeOption = this.mMergeOption;
        if(mergeOption == null){
            mergeOption = new ResourceMergeOption();
            this.mMergeOption = mergeOption;
        }
        return mergeOption;
    }

    public void setMergeOption(ResourceMergeOption mergeOption) {
        this.mMergeOption = mergeOption;
    }

    private void transferTableBlock(ApkModule apkModule) {
        ApkModule resultModule = getResultModule();
        TableBlock tableBlock = resultModule.getTableBlock();
        if(tableBlock == null){
            return;
        }
        apkModule.setTableBlock(tableBlock);
        resultModule.setTableBlock(null);
    }
    private void transferResFiles(ApkModule apkModule) {
        ApkModule resultModule = getResultModule();
        List<ResFile> resFileList = resultModule.listResFiles();
        Set<String> transferred = new HashSet<>();
        for(ResFile resFile : resFileList) {
            InputSource inputSource = resFile.getInputSource();
            String path = inputSource.getAlias();
            if(!transferred.contains(path)){
                apkModule.add(inputSource);
                transferred.add(path);
            }
        }
        ZipEntryMap zipEntryMap = resultModule.getZipEntryMap();
        for(String path : transferred) {
            zipEntryMap.remove(path);
        }
    }
    public void rebuildManifest(ApkModule apkModule) {
        AndroidManifestBlock moduleManifest = apkModule.getAndroidManifest();
        if(moduleManifest == null){
            return;
        }
        TableBlock resultTable = getResultTable();
        if(resultTable == null){
            return;
        }
        PackageBlock documentPackage = moduleManifest.getPackageBlock();
        if(documentPackage.getTableBlock() == resultTable){
            return;
        }

        AndroidManifestBlock resultManifest = new AndroidManifestBlock();
        resultManifest.setPackageBlock(resultTable.pickOne(documentPackage.getId()));

        resultManifest.mergeWithName(getMergeOption(), moduleManifest);

        apkModule.setManifest(resultManifest);
        apkModule.keepManifestChanges();
    }

    private void mergePackages(){
        TableBlock sourceTable = this.sourceTable;
        TableBlock resultTable = this.getResultTable();
        int size = sourceTable.size();
        for(int i = 0; i < size; i++) {
            PackageBlock sourcePackage = sourceTable.get(i);
            PackageBlock resultPackage = resultTable.get(i);
            mergePackage(sourcePackage, resultPackage);
        }
    }
    private void mergePackage(PackageBlock sourcePackage, PackageBlock resultPackage) {
        ResourceMergeOption mergeOption = this.getMergeOption();
        Predicate<? super ResourceEntry> keepEntries = mergeOption.getKeepEntries();
        Iterator<ResourceEntry> iterator = FilterIterator.of(sourcePackage.getResources(), keepEntries);
        while (iterator.hasNext()){
            ResourceEntry sourceEntry = iterator.next();
            if(sourceEntry.isEmpty()) {
                continue;
            }

            ResourceEntry resultEntry = resultPackage.mergeWithName(mergeOption, sourceEntry);
            addIdMap(sourceEntry.getResourceId(), resultEntry.getResourceId());
        }
    }
    private void initializeTable(){
        TableBlock sourceTable = this.getSourceTable();
        sourceTable.refresh();
        TableBlock resultTable = this.getResultTable();

        resultTable.addFrameworks(sourceTable.frameworks());

        for(PackageBlock sourcePackage : sourceTable) {
            resultTable.newPackage(sourcePackage.getId(), sourcePackage.getName());
        }

        resultTable.getStringPool().merge(sourceTable.getStringPool());
        initializePackages();
    }
    private void initializePackages(){
        TableBlock sourceTable = this.getSourceTable();
        TableBlock resultTable = this.getResultTable();
        int size = sourceTable.size();
        for(int i = 0; i < size; i++) {
            PackageBlock sourcePackage = sourceTable.get(i);
            PackageBlock resultPackage = resultTable.get(i);
            initializePackage(sourcePackage, resultPackage);
        }
    }
    private void initializePackage(PackageBlock sourcePackage, PackageBlock resultPackage) {
        initializeTypeString(sourcePackage, resultPackage);
        initializeSpecString(sourcePackage, resultPackage);
        initializeEntries(sourcePackage, resultPackage);
    }
    private void initializeTypeString(PackageBlock sourcePackage, PackageBlock resultPackage) {
        Predicate<? super ResourceEntry> keepEntries = getMergeOption().getKeepEntries();
        TypeStringPool sourcePool = sourcePackage.getTypeStringPool();
        Set<String> typeSet = new HashSet<>(sourcePool.size());
        for(TypeString typeString : sourcePool) {
            String typeName = typeString.get();
            Iterator<ResourceEntry> iterator = FilterIterator.of(sourcePackage.getResources(typeName),
                    keepEntries);
            if(iterator.hasNext()){
                typeSet.add(typeName);
            }
        }
        List<String> typeList = new ArrayCollection<>(typeSet);
        java.util.Collections.sort(typeList, CompareUtil.getComparableComparator());
        resultPackage.getTypeStringPool().addStrings(typeList);
    }
    private void initializeSpecString(PackageBlock sourcePackage, PackageBlock resultPackage) {
        resultPackage.getSpecStringPool().merge(sourcePackage.getSpecStringPool());
    }
    private void initializeEntries(PackageBlock sourcePackage, PackageBlock resultPackage) {
        TypeStringPool resultPool = resultPackage.getTypeStringPool();
        for(TypeString typeString : resultPool) {
            String typeName = typeString.get();
            initializeEntries(typeName, sourcePackage, resultPackage);
        }
    }
    private void initializeEntries(String typeName, PackageBlock sourcePackage, PackageBlock resultPackage) {
        Predicate<? super ResourceEntry> keepEntries = getMergeOption().getKeepEntries();
        ArrayCollection<ResourceEntry> sourceEntryList = new ArrayCollection<>();
        Iterator<ResourceEntry> iterator = FilterIterator.of(
                sourcePackage.getResources(typeName),
                resourceEntry -> resourceEntry.isDefined() &&
                        keepEntries.test(resourceEntry));
        sourceEntryList.addAll(iterator);

        sourceEntryList.sort(this::compareEntryNames);

        TypeBlock typeBlock = resultPackage.getOrCreateTypeBlock(ResConfig.getDefault(), typeName);
        EntryItemList entryArray = typeBlock.getEntryArray();
        int size = sourceEntryList.size();
        entryArray.setSize(sourceEntryList.size());

        for (int i = 0; i < size; i++) {
            ResourceEntry resourceEntry = sourceEntryList.get(i);
            Entry entry = entryArray.get(i);
            entry.setName(resourceEntry.getName(), true);
        }
    }
    int compareEntryNames(ResourceEntry entry1, ResourceEntry entry2) {
        String name1 = entry1.getName();
        String name2 = entry2.getName();
        if(name1 == null && name2 != null){
            return 1;
        }
        if(name1 != null && name2 == null){
            return -1;
        }
        if(name1 == null){
            return 0;
        }
        if(name1.startsWith("$")){
            name1 = name1.substring(1);
        }
        if(name2.startsWith("$")){
            name2 = name2.substring(1);
        }
        int i = name1.compareTo(name2);
        return CompareUtil.compare(i, 0);
    }
}
