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
package com.reandroid.graph;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.model.ResourceName;
import com.reandroid.arsc.value.*;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class RequiredEntriesScanner extends BaseApkModuleProcessor{

    private final ApkBuildOption buildOption;
    private final Set<ResourceName> requiredResources;
    private final Set<String> requiredFiles;
    private final Set<String> processedFiles;
    private final Set<Integer> processedNumbers;

    public RequiredEntriesScanner(ApkBuildOption buildOption, ApkModule apkModule, DexClassRepository classRepository) {
        super(apkModule, classRepository);

        this.buildOption = buildOption;
        this.requiredResources = new HashSet<>();
        this.requiredFiles = new HashSet<>();
        this.processedFiles = new HashSet<>();
        this.processedNumbers = new HashSet<>();
    }

    @Override
    public void apply() {
        scanUserConfigs();
        scanIdOnXml(getApkModule().getAndroidManifest());
        scanIdOnDexClasses();
        reset();
    }

    public Set<ResourceName> getRequiredResources() {
        return requiredResources;
    }
    public Set<String> getRequiredFiles() {
        return requiredFiles;
    }

    private void scanUserConfigs() {
        Predicate<? super ResourceName> filter = buildOption.getResourceMergeOption()
                .getKeepResourceName();
        if(filter == null) {
            return;
        }
        Iterator<ResourceEntry> iterator = FilterIterator.of(getTableBlock().getResources(),
                resourceEntry -> {
                    ResourceName resourceName = resourceEntry.toResourceName();
                    return resourceName != null && filter.test(resourceName);
                });
        while (iterator.hasNext()) {
            add(iterator.next());
        }
    }
    private void scanIdOnResXml(String path) {
        if(path == null || !processedFiles.add(path)) {
            return;
        }
        ResFile resFile = getApkModule().getResFile(path);
        if(resFile != null) {
            this.requiredFiles.add(path);
            scanIdOnXml(resFile.getResXmlDocument());
        }
    }
    private void scanIdOnXml(ResXmlDocument resXmlDocument) {
        if(resXmlDocument != null) {
            Iterator<ResXmlAttribute> iterator = resXmlDocument.recursiveAttributes();
            while (iterator.hasNext()) {
                addAttribute(iterator.next());
            }
        }
    }
    private void scanIdOnDexClasses() {
        Iterator<ResourceEntry> iterator = ComputeIterator.of(getClassRepository().visitIntegers(),
                reference -> RequiredEntriesScanner.this.getLocalResource(reference.get()));
        while (iterator.hasNext()) {
            ResourceEntry resourceEntry = iterator.next();
            add(resourceEntry);
        }
    }
    private void add(ResourceEntry resourceEntry) {
        if(resourceEntry == null || !resourceEntry.isContext(getTableBlock())) {
            return;
        }
        ResourceName resourceName = resourceEntry.toResourceName();
        if(resourceName == null || !requiredResources.add(resourceName)) {
            return;
        }
        Iterator<Entry> iterator = resourceEntry.iterator(true);
        while (iterator.hasNext()) {
            addEntry(iterator.next());
        }
    }
    private void addEntry(Entry entry) {
        if(entry == null || entry.isNull()) {
            return;
        }
        if(entry.isScalar()) {
            addValue(entry.getResValue());
        }else if(entry.isComplex()) {
            addComplex(entry.getResTableMapEntry());
        }
    }
    private void addComplex(ResTableMapEntry mapEntry) {
        if(mapEntry == null) {
            return;
        }
        add(getLocalResource(mapEntry.getParentId()));
        for(ResValueMap valueMap : mapEntry) {
            addAttribute(valueMap);
        }
    }
    private void addAttribute(AttributeValue attribute) {
        if(attribute == null) {
            return;
        }
        add(attribute.resolveName());
        addValue(attribute);
    }
    private void addValue(Value value) {
        if(value == null) {
            return;
        }
        String path = value.getValueAsString();
        if (path != null) {
            scanIdOnResXml(path);
        } else {
            add(value.getValueAsReference());
        }
    }
    ResourceEntry getLocalResource(int id) {
        if(PackageBlock.isResourceId(id) && processedNumbers.add(id)) {
            TableBlock tableBlock = getTableBlock();
            ResourceEntry resourceEntry = tableBlock.getLocalResource(id);
            if(resourceEntry != null && resourceEntry.isContext(tableBlock)) {
                return resourceEntry;
            }
        }
        return null;
    }
    private TableBlock getTableBlock() {
        return getApkModule().getTableBlock();
    }
    public void reset() {
        this.processedFiles.clear();
        this.processedNumbers.clear();
    }
}
