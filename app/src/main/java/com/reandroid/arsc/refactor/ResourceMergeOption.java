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

import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.item.TypeString;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.model.ResourceName;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.io.FileUtil;
import com.reandroid.utils.io.StringLineStream;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class ResourceMergeOption {

    private Predicate<? super ResourceEntry> keepEntries;
    private Predicate<? super ResourceName> keepResourceNameFilter;
    private Predicate<? super ResConfig> keepConfigs;

    private final Set<ResourceName> keepResourceNameList;

    public ResourceMergeOption() {
        this.keepResourceNameList = new HashSet<>();
    }

    public Predicate<? super Entry> getKeepEntryConfigs() {
        Predicate<? super ResConfig> keepConfigs = this.getKeepConfigs();
        return (Predicate<Entry>) entry -> keepConfigs.test(entry.getResConfig());
    }
    public Predicate<? super ResConfig> getKeepConfigs() {
        Predicate<? super ResConfig> keepConfigs = this.keepConfigs;
        if(keepConfigs == null) {
            keepConfigs = CollectionUtil.getAcceptAll();
        }
        return keepConfigs;
    }
    public void setKeepConfigs(Predicate<? super ResConfig> keepConfigs) {
        this.keepConfigs = keepConfigs;
    }
    public Predicate<? super ResourceEntry> getKeepEntries() {
        Predicate<? super ResourceEntry> keepEntries = this.getKeepEntriesInternal();
        Predicate<? super ResourceName> keepResourceNames = this.getKeepResourceName();
        Predicate<? super ResourceEntry> result = keepEntries;
        if(keepResourceNames != null) {
            result = (Predicate<ResourceEntry>) resourceEntry -> {
                if(keepEntries.test(resourceEntry)) {
                    return true;
                }
                ResourceName resourceName = resourceEntry.toResourceName();
                if(resourceName != null) {
                    return keepResourceNames.test(resourceName);
                }
                return false;
            };
        }
        return result;
    }
    private Predicate<? super ResourceEntry> getKeepEntriesInternal() {
        Predicate<? super ResourceEntry> keepEntries = this.keepEntries;
        if(keepEntries == null) {
            keepEntries = resourceEntry -> !resourceEntry.isEmpty();
            this.keepEntries = keepEntries;
        }
        return CollectionUtil.orFilter(keepEntries, getKeepStyleEntries());
    }
    private Predicate<? super ResourceEntry> getKeepStyleEntries() {
        return (Predicate<ResourceEntry>) resourceEntry ->
                TypeString.isTypeStyle(resourceEntry.getType()) &&
                resourceEntry.getName().indexOf('.') > 0;
    }
    public void setKeepEntries(Predicate<? super ResourceEntry> keepEntries) {
        this.keepEntries = keepEntries;
    }

    public Predicate<? super ResourceName> getKeepResourceName() {
        return CollectionUtil.orFilter(getKeepResourceNameFilter(),
                getKeepResourceNameListFilter());
    }
    public void setKeepResourceNameFilter(Predicate<? super ResourceName> keepResourceNameFilter) {
        this.keepResourceNameFilter = keepResourceNameFilter;
    }
    private Predicate<? super ResourceName> getKeepResourceNameFilter() {
        return keepResourceNameFilter;
    }
    private Predicate<? super ResourceName> getKeepResourceNameListFilter() {
        Set<ResourceName> keepResourceNameList = this.keepResourceNameList;
        if(!keepResourceNameList.isEmpty()) {
            return (Predicate<ResourceName>) keepResourceNameList::contains;
        }
        return null;
    }
    public void clearKeepResourceNameList() {
        this.keepResourceNameList.clear();
    }
    public void addKeepResourceName(ResourceName resourceName) {
        this.keepResourceNameList.add(resourceName);
    }
    public void readKeepResourceNameList(File keepResourceNameListFile) throws IOException {
        StringLineStream stringLineStream = new StringLineStream(FileUtil.inputStream(keepResourceNameListFile));
        while (stringLineStream.hasNext()) {
            String line = stringLineStream.next();
            line = line.trim();
            addKeepResourceName(ResourceName.parse(line));
        }
        IOException exception = stringLineStream.getError();
        if(exception != null) {
            throw exception;
        }
        stringLineStream.close();
    }

    public ResourceEntry resolveUndeclared(PackageBlock currentContext, ResourceEntry undeclared) {
        ResourceEntry entry = undeclared.getLast();
        String name = entry.getName();
        while (name == null){
            ResourceEntry prev = entry.previous();
            if(prev == null){
                break;
            }
            entry = prev;
            if(prev.isEmpty()){
                continue;
            }
            name = entry.getName();
        }
        String type = undeclared.getType();
        if(name == null){
            name = type;
        }
        int i = 0;
        String entryName = name;
        while (currentContext.getResource(type, entryName) != null) {
            entryName = name + i;
            i ++;
        }
        Entry result = currentContext.getOrCreate(ResConfig.getDefault(), type, entryName);
        result.ensureComplex(false);
        ResValue resValue = result.getResValue();
        resValue.setTypeAndData(ValueType.REFERENCE, 0);
        return currentContext.getResource(result.getResourceId());
    }

    public void mergeFileWithName(ApkFile source, ApkFile destination, String path) {
        if(!source.containsFile(path) || destination.containsFile(path)) {
            return;
        }
        if(!mergeResXmlDocument(source, destination, path)) {
            mergeRawFile(source, destination, path);
        }
    }
    private boolean mergeResXmlDocument(ApkFile source, ApkFile destination, String path) {
        if(!path.endsWith(".xml")){
            return false;
        }
        if(!ResXmlDocument.isResXmlBlock(source.getInputSource(path))){
            return false;
        }
        ResXmlDocument document;
        try {
            document = source.loadResXmlDocument(path);
        } catch (IOException exception) {
            return false;
        }
        ResXmlDocument xmlDocument = new ResXmlDocument();
        PackageBlock preferred = destination.getTableBlock().pickOne(document.getPackageBlock().getId());
        if(preferred == null) {
            preferred = destination.getTableBlock().pickOne();
        }
        xmlDocument.setPackageBlock(preferred);
        xmlDocument.mergeWithName(this, document);
        xmlDocument.refreshFull();
        ByteInputSource byteInputSource = new ByteInputSource(xmlDocument.getBytes(), path);
        destination.add(byteInputSource);
        return true;
    }
    private void mergeRawFile(ApkFile source, ApkFile destination, String path) {
        InputSource inputSource = source.getInputSource(path);
        BytesOutputStream outputStream = new BytesOutputStream();
        try {
            outputStream.write(inputSource.openStream());
            outputStream.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        ByteInputSource byteInputSource = new ByteInputSource(outputStream.toByteArray(),
                inputSource.getAlias());
        byteInputSource.setSort(inputSource.getSort());
        byteInputSource.setMethod(inputSource.getMethod());
        destination.add(byteInputSource);
    }

}
