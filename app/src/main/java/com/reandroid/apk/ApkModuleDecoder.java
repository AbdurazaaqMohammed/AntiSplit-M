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

import com.reandroid.archive.ArchiveInfo;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.archive.block.ApkSignatureBlock;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.dex.model.DexDirectory;
import com.reandroid.dex.sections.Marker;
import com.reandroid.identifiers.PackageIdentifier;
import com.reandroid.identifiers.TableIdentifier;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class ApkModuleDecoder extends ApkModuleCoder{
    private final ApkModule apkModule;
    private final Set<String> mDecodedPaths;
    private DexDecoder mDexDecoder;
    private DexProfileDecoder mDexProfileDecoder;
    private boolean mLogErrors;
    private DecodeFilter mDecodeFilter;
    private boolean mSanitizePaths;
    private boolean mInitialized;

    public ApkModuleDecoder(ApkModule apkModule){
        super();
        this.apkModule = apkModule;
        this.mDecodedPaths = new HashSet<>();
        this.mSanitizePaths = true;
        setApkLogger(apkModule.getApkLogger());
    }

    public final void decode(File mainDirectory) throws IOException{
        initialize();
        sanitizeFilePaths();
        decodeArchiveInfo(mainDirectory);
        decodeUncompressedFiles(mainDirectory);

        decodeAndroidManifest(mainDirectory);
        decodeResourceTable(mainDirectory);
        decodeDexFiles(mainDirectory);
        extractRootFiles(mainDirectory);
        decodeDexProfile(mainDirectory);
        decodePathMap(mainDirectory);
        dumpSignatures(mainDirectory);
    }
    public abstract void decodeResourceTable(File mainDirectory) throws IOException;
    abstract void decodeAndroidManifest(File mainDirectory) throws IOException;

    public void extractRootFiles(File mainDirectory) throws IOException {
        logMessage("Extracting root files ...");
        File rootDir = new File(mainDirectory, ApkUtil.ROOT_NAME);
        for(InputSource inputSource:apkModule.getInputSources()){
            if(containsDecodedPath(inputSource.getAlias())){
                continue;
            }
            extractRootFile(rootDir, inputSource);
            addDecodedPath(inputSource.getAlias());
        }
    }
    public void decodeDexProfile(File mainDirectory) throws IOException {
        DexProfileDecoder decoder = getDexProfileDecoder();
        if (decoder != null) {
            decoder.decodeDexProfile(apkModule, mainDirectory);
        }
    }
    public void decodeDexInfo(File mainDirectory)
            throws IOException {
        File file = new File(mainDirectory, "dex-info.json");
        logMessage("Decode: " + file.getName());
        ZipEntryMap zipEntryMap = apkModule.getZipEntryMap();
        DexDirectory dexDirectory = DexDirectory.readStrings(zipEntryMap);
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Iterator<Marker> markers = dexDirectory.getMarkers();
        while (markers.hasNext()){
            jsonArray.put(markers.next().getJsonObject());
        }
        jsonObject.put("markers", jsonArray);
        jsonObject.write(file);
    }
    public void decodeArchiveInfo(File mainDirectory)
            throws IOException {
        File file = new File(mainDirectory, ArchiveInfo.JSON_FILE);
        logMessage("Decode: " + file.getName());
        ZipEntryMap zipEntryMap = apkModule.getZipEntryMap();
        ArchiveInfo archiveInfo = zipEntryMap.getOrCreateArchiveInfo();
        archiveInfo.writeToDirectory(mainDirectory);
    }
    public void decodeUncompressedFiles(File mainDirectory)
            throws IOException {
        File file = new File(mainDirectory, UncompressedFiles.JSON_FILE);
        logMessage("Decode: " + file.getName());
        UncompressedFiles uncompressedFiles = new UncompressedFiles();
        uncompressedFiles.addCommonExtensions();
        uncompressedFiles.addPath(getApkModule().getZipEntryMap());
        uncompressedFiles.toJson().write(file);
    }
    public void decodeDexFiles(File mainDir) throws IOException {
        ApkModule apkModule = getApkModule();
        List<DexFileInputSource> dexList = apkModule.listDexFiles();
        DexDecoder dexDecoder = getDexDecoder();
        dexDecoder.decodeDex(apkModule, mainDir);
        for(DexFileInputSource inputSource : dexList) {
            addDecodedPath(inputSource.getAlias());
        }
    }
    @Override
    public ApkModule getApkModule() {
        return apkModule;
    }
    public DexDecoder getDexDecoder() {
        if(mDexDecoder == null){
            DexFileRawDecoder rawDecoder = new DexFileRawDecoder();
            rawDecoder.setApkLogger(getApkLogger());
            mDexDecoder = rawDecoder;
        }
        return mDexDecoder;
    }
    public void setDexDecoder(DexDecoder dexDecoder) {
        this.mDexDecoder = dexDecoder;
    }

    public DexProfileDecoder getDexProfileDecoder() {
        return mDexProfileDecoder;
    }
    public void setDexProfileDecoder(DexProfileDecoder dexProfileDecoder) {
        this.mDexProfileDecoder = dexProfileDecoder;
    }

    public void setSanitizePaths(boolean sanitizePaths) {
        this.mSanitizePaths = sanitizePaths;
    }

    public void sanitizeFilePaths() {
        if (mInitialized && mSanitizePaths) {
            PathSanitizer sanitizer = PathSanitizer.create(getApkModule());
            sanitizer.setApkLogger(getApkLogger());
            sanitizer.sanitize();
        }
    }
    public void dumpSignatures(File mainDirectory) throws IOException {
        ApkModule apkModule = getApkModule();
        ApkSignatureBlock signatureBlock = apkModule.getApkSignatureBlock();
        if(signatureBlock == null){
            return;
        }
        File sigDir = new File(mainDirectory, ApkUtil.SIGNATURE_DIR_NAME);
        logMessage("Dumping signatures ...");
        signatureBlock.writeSplitRawToDirectory(sigDir);
    }
    public void decodePathMap(File mainDirectory) throws IOException {
        File file = new File(mainDirectory, PathMap.JSON_FILE);
        PathMap pathMap = new PathMap();
        pathMap.add(getApkModule().getZipEntryMap());
        pathMap.toJson().write(file);
    }
    public boolean containsDecodedPath(String path){
        return mDecodedPaths.contains(path);
    }
    public void addDecodedPath(String path){
        mDecodedPaths.add(path);
    }

    public DecodeFilter getDecodeFilter(){
        if(mDecodeFilter == null){
            mDecodeFilter = new DecodeFilter();
        }
        return mDecodeFilter;
    }
    public void setDecodeFilter(DecodeFilter decodeFilter) {
        this.mDecodeFilter = decodeFilter;
    }
    boolean isExcluded(String path){
        return getDecodeFilter().isExcluded(path);
    }

    private void extractRootFile(File rootDir, InputSource inputSource) throws IOException {
        File file = inputSource.toFile(rootDir);
        inputSource.write(file);
    }

    void logOrThrow(String message, Throwable exception) throws IOException{
        if(isLogErrors()){
            logError(message, exception);
            return;
        }
        if(message == null && exception == null){
            return;
        }
        if(exception == null){
            exception = new IOException(message);
        }
        if(exception instanceof IOException){
            throw (IOException) exception;
        }
        throw new IOException(exception);
    }


    public void validateResourceNames(){
        logMessage("Validating resource names ...");
        TableBlock tableBlock = apkModule.getTableBlock();
        TableIdentifier tableIdentifier = new TableIdentifier();
        tableIdentifier.load(tableBlock);
        String msg = tableIdentifier.validateSpecNames();
        if(msg == null){
            logMessage("All resource names are valid");
            return;
        }
        if(tableBlock.removeUnusedSpecs()) {
            msg = msg + ", removed specs";
        }
        logMessage(msg);
    }
    public void validateResourceNames(PackageBlock packageBlock){
        logMessage("Validating: " + packageBlock.getName());
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.load(packageBlock);
        String msg = packageIdentifier.validateSpecNames();
        if(msg == null){
            logMessage("[" + packageBlock.getName() + "] All resource names are valid");
            return;
        }
        if(packageBlock.removeUnusedSpecs()) {
            msg = "[" + packageBlock.getName() + "]" + msg + ", removed specs";
            logMessage(msg);
        }
    }
    void initialize(){
        mDecodedPaths.clear();
        ensureTableBlock();
        mInitialized = true;
    }
    private void ensureTableBlock(){
        ApkModule apkModule = getApkModule();
        if(apkModule.ensureTableBlock()){
            logMessage("Missing " + TableBlock.FILE_NAME + ", created empty");
        }
    }

    public boolean isLogErrors() {
        return mLogErrors;
    }
    public void setLogErrors(boolean logErrors) {
        this.mLogErrors = logErrors;
    }


    static File toPackageDirectory(File mainDir, PackageBlock packageBlock){
        File dir = new File(mainDir, TableBlock.DIRECTORY_NAME);
        return new File(dir, packageBlock.buildDecodeDirectoryName());
    }
}
