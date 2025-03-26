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
package com.reandroid.apkeditor.smali;

import com.reandroid.apk.APKLogger;
import com.reandroid.apk.ApkModuleEncoder;
import com.reandroid.apk.DexEncoder;
import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.archive.FileInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.dex.model.DexFile;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.io.FileUtil;
import org.jf.dexlib2.extra.DexMarker;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SmaliCompiler implements DexEncoder {

    private final BuildOptions buildOptions;
    private APKLogger apkLogger;
    private Integer minSdkVersion;

    public SmaliCompiler(BuildOptions buildOptions) {
        this.buildOptions = buildOptions;
    }

    @Override
    public List<InputSource> buildDexFiles(ApkModuleEncoder apkModuleEncoder, File mainDir) throws IOException {
        File smaliDir = new File(mainDir, "smali");
        if(!smaliDir.isDirectory()){
            return null;
        }
        AndroidManifestBlock manifestBlock =  apkModuleEncoder.getApkModule().getAndroidManifest();
        if (manifestBlock != null) {
            this.minSdkVersion = manifestBlock.getMinSdkVersion();
        }
        if(minSdkVersion == null){
            minSdkVersion = 24;
        }
        List<InputSource> results = new ArrayList<>();
        List<File> classesDirList = listClassesDirectories(smaliDir);
        int i = 0;
        int size = classesDirList.size();
        for(File classesDir : classesDirList){
            i++;
            String progress = "(" + StringsUtil.formatNumber(i, size) + "/" + size + ") ";
            InputSource inputSource = build(progress, classesDir);
            results.add(inputSource);
        }
        return results;
    }
    private InputSource build(String progress, File classesDir) throws IOException {
        File dexCacheFile = toDexCache(classesDir);
        if(isModified(classesDir, dexCacheFile)){
            return build(progress, classesDir, dexCacheFile);
        }else {
            logMessage(progress + "Cached: " + dexCacheFile.getName());
            return new FileInputSource(dexCacheFile, dexCacheFile.getName());
        }
    }
    private InputSource build(String progress, File classesDir, File dexCacheFile) throws IOException {
        if(BuildOptions.DEX_LIB_INTERNAL.equals(buildOptions.dexLib)) {
            return buildWithInternalLib(progress, classesDir, dexCacheFile);
        }
        return buildWithJesusFreke(progress, classesDir, dexCacheFile);
    }
    private InputSource buildWithJesusFreke(String progress, File classesDir, File dexCacheFile) throws IOException {
        logMessage(progress + "Smali<JF>: " + dexCacheFile.getName());
        SmaliOptions smaliOptions = new SmaliOptions();
        FileUtil.ensureParentDirectory(dexCacheFile);
        smaliOptions.outputDexFile = dexCacheFile.getAbsolutePath();
        File marker = new File(classesDir, DexMarker.FILE_NAME);
        if(marker.isFile()){
            smaliOptions.markersListFile = marker.getAbsolutePath();
        }
        if(smaliOptions.jobs <= 0){
            smaliOptions.jobs = 1;
        }
        if (this.minSdkVersion != null) {
            smaliOptions.apiLevel = this.minSdkVersion;
        }
        boolean success = Smali.assemble(smaliOptions, classesDir.getAbsolutePath());
        if(!success){
            throw new IOException("Failed to build smali, check the logs");
        }
        return new FileInputSource(dexCacheFile, dexCacheFile.getName());
    }
    private InputSource buildWithInternalLib(String progress, File classesDir, File dexCacheFile) throws IOException {
        logMessage(progress + "Smali<INTERNAL>: " + dexCacheFile.getName());
        DexFile dexFile = DexFile.createDefault();
        int version = 0;
        if (this.minSdkVersion != null) {
            version = minSdkVersion;
        }
        version = apiToDexVersion(version);
        dexFile.setVersion(version);
        dexFile.parseSmaliDirectory(classesDir);
        dexFile.refreshFull();
        dexFile.write(dexCacheFile);
        dexFile.close();
        return new FileInputSource(dexCacheFile, dexCacheFile.getName());
    }

    private boolean isModified(File classesDir, File dexCacheFile){
        if(buildOptions.noCache || !dexCacheFile.isFile()){
            return true;
        }
        long dexMod = dexCacheFile.lastModified();
        return isModified(classesDir, dexMod);
    }
    private boolean isModified(File dir, long dexMod){
        if(dir.lastModified() > dexMod){
            return true;
        }
        if(!dir.isDirectory()){
            return false;
        }
        File[] files = dir.listFiles();
        if(files == null){
            return false;
        }
        for(File file : files){
            if(isModified(file, dexMod)){
                return true;
            }
        }
        return false;
    }
    private File toDexCache(File classesDir){
        File mainDir = classesDir.getParentFile().getParentFile();
        File dir = new File(mainDir, SmaliUtil.CACHE_DIR);
        return new File(dir, SmaliUtil.getDexFileName(SmaliUtil.getDexNumber(classesDir.getName())));
    }
    private List<File> listClassesDirectories(File smaliDir){
        List<File> results = new ArrayList<>();
        if(!smaliDir.isDirectory()){
            return results;
        }
        File[] files = smaliDir.listFiles();
        if(files == null){
            return results;
        }
        for(File file : files){
            if(!SmaliUtil.isClassesDir(file)){
                logMessage("WARN: Ignore: " + file);
                continue;
            }
            results.add(file);
        }
        SmaliUtil.sortDexFiles(results);
        return results;
    }
    public void setApkLogger(APKLogger apkLogger) {
        this.apkLogger = apkLogger;
    }
    private void logMessage(String msg){
        APKLogger apkLogger = this.apkLogger;
        if(apkLogger != null){
            apkLogger.logMessage(msg);
        }
    }

    public static int apiToDexVersion(int api) {
        if (api <= 23) {
            return 35;
        }
        switch (api) {
            case 24:
            case 25:
                return 37;
            case 26:
            case 27:
                return 38;
            case 28:
                return 39;
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
                return 40;
            case 35:
                return 41;
        }
        return 39;
    }
}
