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
package com.reandroid.apkeditor.merge;

import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Util;
import com.reandroid.apkeditor.common.AndroidManifestHelper;
import com.reandroid.app.AndroidManifest;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.archive.ArchiveEntry;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.utils.HexUtil;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ValueType;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.Predicate;

public class Merger extends CommandExecutor<MergerOptions> {

    public Merger(MergerOptions options){
        super(options, "[MERGE] ");
    }

    @Override
    public void runCommand() throws IOException {
        MergerOptions options = getOptions();
        delete(options.outputFile);
        File dir = options.inputFile;
        boolean extracted = false;
        if(dir.isFile()){
            dir = extractFile(dir);
            extracted = true;
        }
        logMessage("Searching apk files ...");
        ApkBundle bundle = new ApkBundle();
        bundle.setAPKLogger(this);
        bundle.loadApkDirectory(dir, extracted);
        logMessage("Found modules: " + bundle.getApkModuleList().size());
        for(ApkModule apkModule:bundle.getApkModuleList()){
            String protect = Util.isProtected(apkModule);
            if(protect != null){
                logMessage(options.inputFile.getAbsolutePath());
                logMessage(protect);
                return;
            }
        }
        ApkModule mergedModule = bundle.mergeModules(options.validateModules);
        if (options.resDirName != null) {
            logMessage("Renaming resources root dir: " + options.resDirName);
            mergedModule.setResourcesRootDir(options.resDirName);
        }
        if (options.validateResDir) {
            logMessage("Validating resources dir ...");
            mergedModule.validateResourcesDir();
        }
        if (options.cleanMeta) {
            logMessage("Clearing META-INF ...");
            clearMeta(mergedModule);
        }
        sanitizeManifest(mergedModule);
        mergedModule.refreshTable();
        mergedModule.refreshManifest();
        logMessage("Writing apk ...");
        mergedModule.writeApk(options.outputFile);
        mergedModule.close();
        bundle.close();
        if(extracted){
            Util.deleteDir(dir);
            dir.deleteOnExit();
        }
        logMessage("Saved to: " + options.outputFile);
    }
    private File extractFile(File file) throws IOException {
        File tmp = toTmpDir(file);
        logMessage("Extracting to: " + tmp);
        if(tmp.exists()){
            logMessage("Delete: " + tmp);
            Util.deleteDir(tmp);
        }
        tmp.deleteOnExit();
        ArchiveFile archive = new ArchiveFile(file);
        fixFilePermissions(archive);
        Predicate <ArchiveEntry> filter = archiveEntry -> archiveEntry.getName().endsWith(".apk");
        int count = archive.extractAll(tmp, filter, this);
        archive.close();
        if(count == 0){
            throw new IOException("No *.apk files found on: " + file);
        }
        return tmp;
    }
    private void fixFilePermissions(ArchiveFile archive) {
        int rw_all = 438; // equivalent to chmod 666
        Iterator<ArchiveEntry> iterator = archive.iterator();
        while (iterator.hasNext()) {
            ArchiveEntry entry = iterator.next();
            entry.getCentralEntryHeader()
                    .getFilePermissions().permissions(rw_all);
        }
    }
    private File toTmpDir(File file){
        String name = file.getName();
        name = HexUtil.toHex8("tmp_", name.hashCode());
        File dir = file.getParentFile();
        File tmp;
        if(dir == null){
            tmp = new File(name);
        }else {
            tmp = new File(dir, name);
        }
        tmp = Util.ensureUniqueFile(tmp);
        return tmp;
    }
    private void sanitizeManifest(ApkModule apkModule) {
        if(!apkModule.hasAndroidManifest()){
            return;
        }
        AndroidManifestBlock manifest = apkModule.getAndroidManifest();
        logMessage("Sanitizing manifest ...");

        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_splitTypes, this);

        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
            AndroidManifest.NAME_requiredSplitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
            AndroidManifest.NAME_splitTypes, this);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                AndroidManifest.ID_extractNativeLibs,
                this, AndroidManifest.NAME_extractNativeLibs);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                AndroidManifest.ID_isSplitRequired,
                this, AndroidManifest.NAME_isSplitRequired);
        ResXmlElement application = manifest.getApplicationElement();
        List<ResXmlElement> splitMetaDataElements =
                AndroidManifestHelper.listSplitRequired(application);
        boolean splits_removed = false;
        for(ResXmlElement meta : splitMetaDataElements){
            if(!splits_removed){
                splits_removed = removeSplitsTableEntry(meta, apkModule);
            }
            logMessage("Removed-element : <" + meta.getName() + "> name=\""
                    + AndroidManifestBlock.getAndroidNameValue(meta) + "\"");
            application.remove(meta);
        }
        manifest.refresh();
    }
    private boolean removeSplitsTableEntry(ResXmlElement metaElement, ApkModule apkModule) {
        ResXmlAttribute nameAttribute = metaElement.searchAttributeByResourceId(AndroidManifest.ID_name);
        if(nameAttribute == null){
            return false;
        }
        if(!"com.android.vending.splits".equals(nameAttribute.getValueAsString())){
            return false;
        }
        ResXmlAttribute valueAttribute=metaElement.searchAttributeByResourceId(
                AndroidManifest.ID_value);
        if(valueAttribute==null){
            valueAttribute=metaElement.searchAttributeByResourceId(
                    AndroidManifest.ID_resource);
        }
        if(valueAttribute == null
                || valueAttribute.getValueType() != ValueType.REFERENCE){
            return false;
        }
        if(!apkModule.hasTableBlock()){
            return false;
        }
        TableBlock tableBlock = apkModule.getTableBlock();
        ResourceEntry resourceEntry = tableBlock.getResource(valueAttribute.getData());
        if(resourceEntry == null){
            return false;
        }
        ZipEntryMap zipEntryMap = apkModule.getZipEntryMap();
        for(Entry entry : resourceEntry){
            if(entry == null){
                continue;
            }
            ResValue resValue = entry.getResValue();
            if(resValue == null){
                continue;
            }
            String path = resValue.getValueAsString();
            logMessage("Removed-table-entry : " + path);
            //Remove file entry
            zipEntryMap.remove(path);
            // It's not safe to destroy entry, resource id might be used in dex code.
            // Better replace it with boolean value.
            entry.setNull(true);
            SpecTypePair specTypePair = entry.getTypeBlock()
                    .getParentSpecTypePair();
            specTypePair.removeNullEntries(entry.getId());
        }
        return true;
    }
}
