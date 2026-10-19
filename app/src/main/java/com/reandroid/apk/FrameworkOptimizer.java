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
package com.reandroid.apk;

import com.reandroid.app.AndroidManifest;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.chunk.xml.ResXmlNode;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.model.FrameworkTable;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.ResXmlStringPool;
import com.reandroid.arsc.value.*;

import java.io.IOException;
import java.util.*;
import com.reandroid.utils.function.Predicate;
import java.util.zip.ZipEntry;

 public class FrameworkOptimizer {
    private final ApkModule frameworkApk;
    private APKLogger apkLogger;
    private boolean mOptimizing;
    public FrameworkOptimizer(ApkModule frameworkApk){
        this.frameworkApk = frameworkApk;
        this.apkLogger = frameworkApk.getApkLogger();
    }
    public void optimize(){
        if(mOptimizing){
            return;
        }
        mOptimizing = true;
        if(!frameworkApk.hasTableBlock()){
            logMessage("Don't have: "+TableBlock.FILE_NAME);
            mOptimizing = false;
            return;
        }
        FrameworkTable frameworkTable = getFrameworkTable();
        AndroidManifestBlock manifestBlock = null;
        if(frameworkApk.hasAndroidManifest()){
            manifestBlock = frameworkApk.getAndroidManifest();
        }
        optimizeTable(frameworkTable, manifestBlock);
        UncompressedFiles uncompressedFiles = frameworkApk.getUncompressedFiles();
        uncompressedFiles.clearExtensions();
        uncompressedFiles.clearPaths();
        clearFiles(frameworkApk.getZipEntryMap());
        logMessage("Optimized");
    }
    private void clearFiles(ZipEntryMap zipEntryMap){
        int size = zipEntryMap.size();
        if(size == 2){
            return;
        }
        logMessage("Removing files from: " + size);
        InputSource tableSource = zipEntryMap.getInputSource(TableBlock.FILE_NAME);
        InputSource manifestSource = zipEntryMap.getInputSource(AndroidManifestBlock.FILE_NAME);
        zipEntryMap.clear();
        if(tableSource!=null){
            tableSource.setMethod(ZipEntry.DEFLATED);
        }
        if(manifestSource!=null){
            manifestSource.setMethod(ZipEntry.DEFLATED);
        }
        zipEntryMap.add(tableSource);
        zipEntryMap.add(manifestSource);
        size = size - zipEntryMap.size();
        logMessage("Removed files: "+size);
    }
    private void optimizeTable(FrameworkTable table, AndroidManifestBlock manifestBlock){
        if(table.isOptimized()){
            return;
        }
        logMessage("Optimizing ...");
        int prev = table.countBytes();
        int version = 0;
        String name = "framework";
        if(manifestBlock !=null){
            Integer code = manifestBlock.getVersionCode();
            if(code!=null){
                version = code;
            }
            name = manifestBlock.getPackageName();
            compressManifest(manifestBlock);
            backupManifestValue(manifestBlock, table);
        }
        logMessage("Optimizing table ...");
        table.optimize(name, version);
        long diff=prev - table.countBytes();
        long percent=(diff*100L)/prev;
        logMessage("Table size reduced by: "+percent+" %");
        mOptimizing = false;
    }

    private FrameworkTable getFrameworkTable(){
        TableBlock tableBlock = frameworkApk.getTableBlock();
        if(tableBlock instanceof FrameworkTable){
            return (FrameworkTable) tableBlock;
        }
        FrameworkTable frameworkTable = toFramework(tableBlock);
        frameworkApk.setTableBlock(frameworkTable);
        return frameworkTable;
    }
    private FrameworkTable toFramework(TableBlock tableBlock){
        logMessage("Converting to framework ...");
        BlockReader reader = new BlockReader(tableBlock.getBytes());
        FrameworkTable frameworkTable = new FrameworkTable();
        try {
            frameworkTable.readBytes(reader);
        } catch (IOException exception) {
            logError("Error re-loading framework: ", exception);
        }
        return frameworkTable;
    }
    private void compressManifest(AndroidManifestBlock manifestBlock){
        logMessage("Compressing manifest ...");
        int prev = manifestBlock.countBytes();
        ResXmlElement manifest = manifestBlock.getDocumentElement();
        manifest.removeIf(new Predicate<ResXmlNode>() {
            @Override
            public boolean test(ResXmlNode xmlNode) {
                return !(xmlNode instanceof ResXmlElement) ||
                        !((ResXmlElement) xmlNode).equalsName(AndroidManifest.TAG_application);
            }
        });
        ResXmlElement application = manifestBlock.getApplicationElement();
        if(application!=null){
            application.clear();
        }
        ResXmlStringPool stringPool = manifestBlock.getStringPool();
        stringPool.removeUnusedStrings();
        manifestBlock.refresh();
        long diff=prev - manifestBlock.countBytes();
        long percent=(diff*100L)/prev;
        logMessage("Manifest size reduced by: "+percent+" %");
    }
    private void backupManifestValue(AndroidManifestBlock manifestBlock, TableBlock tableBlock){
        logMessage("Backup manifest values ...");
        ResXmlElement application = manifestBlock.getApplicationElement();
        ResXmlAttribute iconAttribute = null;
        int iconReference = 0;
        if(application!=null){
            ResXmlAttribute attribute = application
                    .searchAttributeByResourceId(AndroidManifestBlock.ID_icon);
            if(attribute!=null && attribute.getValueType()==ValueType.REFERENCE){
                iconAttribute = attribute;
                iconReference = attribute.getData();
            }
        }

        ResXmlElement element = manifestBlock.getDocumentElement();
        backupAttributeValues(tableBlock, element);

        if(iconAttribute!=null){
            iconAttribute.setTypeAndData(ValueType.REFERENCE, iconReference);
        }
    }
    private void backupAttributeValues(TableBlock tableBlock, ResXmlElement element){
        if(element == null){
            return;
        }
        Iterator<ResXmlAttribute> attributes = element.getAttributes();
        while (attributes.hasNext()){
            ResXmlAttribute attribute = attributes.next();
            backupAttributeValues(tableBlock, attribute);
        }
        Iterator<ResXmlElement> iterator = element.getElements();
        while (iterator.hasNext()){
            backupAttributeValues(tableBlock, iterator.next());
        }
    }
    private void backupAttributeValues(TableBlock tableBlock, ResXmlAttribute attribute){
        if(attribute==null){
            return;
        }
        ValueType valueType = attribute.getValueType();
        if(valueType!=ValueType.REFERENCE && valueType!=ValueType.ATTRIBUTE){
            return;
        }
        int reference = attribute.getData();
        Entry entry = getEntryWithValue(tableBlock, reference);
        if(entry == null || isReferenceEntry(entry) || entry.isComplex()){
            return;
        }
        ResTableEntry resTableEntry = (ResTableEntry) entry.getTableEntry();
        ResValue resValue = resTableEntry.getValue();
        valueType = resValue.getValueType();
        if(valueType==ValueType.STRING){
            String value = resValue.getValueAsString();
            attribute.setValueAsString(value);
        }else {
            int data = resValue.getData();
            attribute.setTypeAndData(valueType, data);
        }
    }
    private Entry getEntryWithValue(TableBlock tableBlock, int resourceId){
        Set<Integer> circularReference = new HashSet<>();
        return getEntryWithValue(tableBlock, resourceId, circularReference);
    }
    private Entry getEntryWithValue(TableBlock tableBlock, int resourceId, Set<Integer> circularReference){
        if(circularReference.contains(resourceId)){
            return null;
        }
        circularReference.add(resourceId);
        ResourceEntry entryGroup = tableBlock.getResource(resourceId);
        Entry entry = entryGroup.get();
        if(entry==null){
            return null;
        }
        if(isReferenceEntry(entry)){
            return getEntryWithValue(
                    tableBlock,
                    ((ResValue)entry.getTableEntry().getValue()).getData(),
                    circularReference);
        }
        if(!entry.isNull()){
            return entry;
        }
        Iterator<Entry> itr = entryGroup.iterator(true);
        while (itr.hasNext()){
            entry = itr.next();
            if(!isReferenceEntry(entry)){
                if(!entry.isNull()){
                    return entry;
                }
            }
        }
        return null;
    }
    private boolean isReferenceEntry(Entry entry){
        if(entry==null || entry.isNull()){
            return false;
        }
        TableEntry<?, ?> tableEntry = entry.getTableEntry();
        if(tableEntry instanceof CompoundEntry){
            return false;
        }
        if(!(tableEntry instanceof ResTableEntry)){
            return false;
        }
        ResTableEntry resTableEntry = (ResTableEntry) tableEntry;
        ResValue resValue = resTableEntry.getValue();

        ValueType valueType = resValue.getValueType();

        return valueType == ValueType.REFERENCE
                || valueType == ValueType.ATTRIBUTE;
    }

    APKLogger getApkLogger(){
        return apkLogger;
    }
    public void setAPKLogger(APKLogger logger) {
        this.apkLogger = logger;
    }
    void logMessage(String msg) {
        if(apkLogger!=null){
            apkLogger.logMessage(msg);
        }
    }
    private void logError(String msg, Throwable tr) {
        if(apkLogger!=null){
            apkLogger.logError(msg, tr);
        }
    }
    private void logVerbose(String msg) {
        if(apkLogger!=null){
            apkLogger.logVerbose(msg);
        }
    }
}
