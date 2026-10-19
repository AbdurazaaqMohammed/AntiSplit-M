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

import com.reandroid.archive.ArchiveBytes;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.BlockInputSource;
import com.reandroid.archive.FileInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.WriteProgress;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.archive.block.ApkSignatureBlock;
import com.reandroid.archive.io.ArchiveFileEntrySource;
import com.reandroid.archive.writer.ApkByteWriter;
import com.reandroid.archive.writer.ApkFileWriter;
import com.reandroid.archive.writer.ApkStreamWriter;
import com.reandroid.archive.writer.ApkWriter;
import com.reandroid.archive.writer.DataDescriptorFactory;
import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.array.PackageArray;
import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.item.TableString;
import com.reandroid.arsc.model.FrameworkTable;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.identifiers.PackageIdentifier;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.xml.XMLDocument;
import com.reandroid.xml.XMLElement;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.reandroid.utils.function.Predicate;
import java.util.zip.ZipEntry;

public class ApkModule implements ApkFile, Closeable {
    
    private String moduleName;
    private final ZipEntryMap zipEntryMap;
    private boolean loadDefaultFramework = true;
    private boolean mDisableLoadFramework = false;
    private TableBlock mTableBlock;
    private InputSource mTableOriginalSource;
    private AndroidManifestBlock mManifestBlock;
    private InputSource mManifestOriginalSource;
    private final UncompressedFiles mUncompressedFiles;
    private APKLogger apkLogger;
    private ApkType mApkType;
    private ApkSignatureBlock apkSignatureBlock;
    private Integer preferredFramework;
    private Closeable mCloseable;
    private final List<TableBlock> mExternalFrameworks;
    private int extractNativeLibs;
    private int compressionLevel = java.util.zip.Deflater.DEFAULT_COMPRESSION;

    private final Map<Object, Object> mTagMaps;

    public int getCompressionLevel() {
        return compressionLevel;
    }
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public ApkModule(String moduleName, ZipEntryMap zipEntryMap) {
        this.moduleName = moduleName;
        this.zipEntryMap = zipEntryMap;
        this.mUncompressedFiles=new UncompressedFiles();
        this.mUncompressedFiles.addPath(zipEntryMap);
        this.mExternalFrameworks = new ArrayCollection<>();
        this.zipEntryMap.setModuleName(moduleName);
        this.mTagMaps = new HashMap<>();
    }
    public ApkModule(ZipEntryMap zipEntryMap) {
        this("base", zipEntryMap);
    }
    public ApkModule() {
        this("base", new ZipEntryMap());
    }

    public void putTag(Object key, Object item) {
        mTagMaps.put(key, item);
    }
    public Object getTag(Object key) {
        return mTagMaps.get(key);
    }
    public Object removeTag(Object key) {
        return mTagMaps.remove(key);
    }
    public void clearTags() {
        mTagMaps.clear();
    }
    public void addExternalFramework(File frameworkFile) throws IOException {
        if (frameworkFile == null) {
            return;
        }
        logMessage("Loading external framework: " + frameworkFile);
        FrameworkApk framework = FrameworkApk.loadTableBlock(frameworkFile);
        framework.setAPKLogger(getApkLogger());
        addExternalFramework(framework);
    }
    public void addExternalFramework(ApkModule apkModule) {
        if (apkModule == null || apkModule == this || !apkModule.hasTableBlock()) {
            return;
        }
        addExternalFramework(apkModule.getTableBlock());
    }
    public void addExternalFramework(TableBlock tableBlock) {
        if (tableBlock == null
                || tableBlock.getApkFile() == this
                || mExternalFrameworks.contains(tableBlock)) {
            return;
        }
        mExternalFrameworks.add(tableBlock);
        updateExternalFramework();
    }
    public String refreshTable() {
        TableBlock tableBlock = this.mTableBlock;
        if (tableBlock != null) {
            return tableBlock.refreshFull();
        }
        return null;
    }
    public void refreshManifest() {
        AndroidManifestBlock manifestBlock = this.mManifestBlock;
        if (manifestBlock != null) {
            manifestBlock.refreshFull();
        }
    }
    public void validateResourceNames() {
        if (!hasTableBlock()) {
            return;
        }
        logMessage("Validating resource names ...");
        TableBlock tableBlock = getTableBlock();
        for (PackageBlock packageBlock : tableBlock.listPackages()) {
            validateResourceNames(packageBlock);
        }
    }
    public void validateResourceNames(PackageBlock packageBlock) {
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.load(packageBlock);
        if (!packageIdentifier.hasDuplicateResources()) {
            return;
        }
        logMessage("Renaming duplicate resources ... ");
        packageIdentifier.ensureUniqueResourceNames();
        packageIdentifier.setResourceNamesToPackage(packageBlock);
    }
    public ApkSignatureBlock getApkSignatureBlock() {
        return apkSignatureBlock;
    }
    public void setApkSignatureBlock(ApkSignatureBlock apkSignatureBlock) {
        this.apkSignatureBlock = apkSignatureBlock;
    }

    public boolean hasSignatureBlock() {
        return getApkSignatureBlock() != null;
    }

    public void dumpSignatureInfoFiles(File directory) throws IOException{
        ApkSignatureBlock apkSignatureBlock = getApkSignatureBlock();
        if (apkSignatureBlock == null) {
            throw new IOException("Don't have signature block");
        }
        apkSignatureBlock.writeSplitRawToDirectory(directory);
    }
    public void dumpSignatureBlock(File file) throws IOException{
        ApkSignatureBlock apkSignatureBlock = getApkSignatureBlock();
        if (apkSignatureBlock == null) {
            throw new IOException("Don't have signature block");
        }
        apkSignatureBlock.writeRaw(file);
    }

    public void scanSignatureInfoFiles(File directory) throws IOException{
        if (!directory.isDirectory()) {
            throw new IOException("No such directory: " + directory);
        }
        ApkSignatureBlock apkSignatureBlock = this.apkSignatureBlock;
        if (apkSignatureBlock == null) {
            apkSignatureBlock = new ApkSignatureBlock();
        }
        apkSignatureBlock.scanSplitFiles(directory);
        setApkSignatureBlock(apkSignatureBlock);
    }
    public void loadSignatureBlock(File file) throws IOException{
        if (!file.isFile()) {
            throw new IOException("No such file: " + file);
        }
        ApkSignatureBlock apkSignatureBlock = this.apkSignatureBlock;
        if (apkSignatureBlock == null) {
            apkSignatureBlock = new ApkSignatureBlock();
        }
        apkSignatureBlock.read(file);
        setApkSignatureBlock(apkSignatureBlock);
    }

    public String getSplit() {
        if (!hasAndroidManifest()) {
            return null;
        }
        return getAndroidManifest().getSplit();
    }
    public List<TableBlock> getLoadedFrameworks() {
        List<TableBlock> results = new ArrayCollection<>();
        if (!hasTableBlock()) {
            return results;
        }
        TableBlock tableBlock = getTableBlock(false);
        results.addAll(tableBlock.getFrameWorks());
        return results;
    }
    public boolean isFrameworkVersionLoaded(Integer version) {
        if (version == null) {
            return false;
        }
        for (TableBlock tableBlock : getLoadedFrameworks()) {
            if (tableBlock instanceof FrameworkTable) {
                if (version.equals(((FrameworkTable) tableBlock).getVersionCode())) {
                    return true;
                }
            }
        }
        return false;
    }
    public FrameworkApk getLoadedFramework(Integer version, boolean onlyAndroid) {
        for (TableBlock tableBlock : getLoadedFrameworks()) {
            if (!(tableBlock instanceof FrameworkTable)) {
                continue;
            }
            FrameworkTable frame = (FrameworkTable) tableBlock;
            if (onlyAndroid && !isAndroid(frame)) {
                continue;
            }
            if (version == null || version.equals(frame.getVersionCode())) {
                return (FrameworkApk) frame.getApkFile();
            }
        }
        return null;
    }
    public FrameworkApk initializeAndroidFramework(Integer version) throws IOException {
        TableBlock tableBlock = getTableBlock(false);
        return initializeAndroidFramework(tableBlock, version);
    }
    public FrameworkApk initializeAndroidFramework(TableBlock tableBlock, Integer version) throws IOException {
        if (mDisableLoadFramework || tableBlock == null || isAndroid(tableBlock)) {
            return null;
        }
        FrameworkApk exist = getLoadedFramework(version, true);
        if (exist != null) {
            return exist;
        }
        logMessage("Initializing android framework ...");
        FrameworkApk frameworkApk;
        if (version == null) {
            logMessage("Can not read framework version, loading latest");
            frameworkApk = AndroidFrameworks.getLatest();
        } else {
            logMessage("Loading android framework for version: " + version);
            frameworkApk = AndroidFrameworks.getBestMatch(version);
        }
        FrameworkTable frameworkTable = frameworkApk.getTableBlock();
        tableBlock.addFramework(frameworkTable);
        logMessage("Initialized framework: " + frameworkApk.getName()
                + " (" + frameworkApk.getVersionName() + ")");
        return frameworkApk;
    }
    public FrameworkApk initializeAndroidFramework(XMLDocument xmlDocument) throws IOException {
        if (this.preferredFramework != null) {
            return initializeAndroidFramework(preferredFramework);
        }
        if (isAndroidCoreApp(xmlDocument)) {
            logMessage("Looks framework itself, skip loading frameworks");
            return null;
        }
        Integer version = readVersionCode(xmlDocument);
        return initializeAndroidFramework(version);
    }
    private boolean isAndroid(TableBlock tableBlock) {
        if (tableBlock instanceof FrameworkTable) {
            FrameworkTable frameworkTable = (FrameworkTable) tableBlock;
            return frameworkTable.isAndroid();
        }
        return false;
    }
    private boolean isAndroidCoreApp(XMLDocument manifestDocument) {
        XMLElement root = manifestDocument.getDocumentElement();
        if (root == null) {
            return false;
        }
        if (!"android".equals(root.getAttributeValue("package"))) {
            return false;
        }
        String coreApp = root.getAttributeValue("coreApp");
        return "true".equals(coreApp);
    }
    private Integer readVersionCode(XMLDocument xmlDocument) {
        if (xmlDocument == null) {
            return null;
        }
        XMLElement manifestRoot = xmlDocument.getDocumentElement();
        if (manifestRoot == null) {
            logMessage("WARN: Manifest root not found");
            return null;
        }
        String versionString = manifestRoot.getAttributeValue("android:compileSdkVersion");
        Integer version = null;
        if (versionString != null) {
            version = safeParseInteger(versionString);
        }
        if (version == null) {
            versionString = manifestRoot.getAttributeValue("platformBuildVersionCode");
            if (versionString != null) {
                version = safeParseInteger(versionString);
            }
        }
        Integer target = null;
        Iterator<XMLElement> iterator = manifestRoot
                .getElements(AndroidManifestBlock.TAG_uses_sdk);
        while (iterator.hasNext()) {
            XMLElement element = iterator.next();
            versionString = element.getAttributeValue("android:targetSdkVersion");
            if (versionString != null) {
                target = safeParseInteger(versionString);
            }
        }
        if (version == null) {
            version = target;
        } else if (target != null && target > version) {
            version = target;
        }
        return version;
    }
    private Integer safeParseInteger(String versionString) {
        try{
            return Integer.parseInt(versionString);
        }catch (NumberFormatException exception) {
            logMessage("NumberFormatException on manifest version reading: '"
                    +versionString+"': "+exception.getMessage());
            return null;
        }
    }

    public void setPreferredFramework(Integer version) {
        if (version != null && version.equals(preferredFramework)) {
            return;
        }
        this.preferredFramework = version;
        if (version == null || mTableBlock == null) {
            return;
        }
        if (isFrameworkVersionLoaded(version)) {
            return;
        }
        logMessage("Initializing preferred framework: " + version);
        mTableBlock.clearFrameworks();
        FrameworkApk frameworkApk = AndroidFrameworks.getBestMatch(version);
        AndroidFrameworks.setCurrent(frameworkApk);
        mTableBlock.addFramework(frameworkApk.getTableBlock());
        logMessage("Initialized framework: " + frameworkApk.getVersionCode());
    }

    public Integer getAndroidFrameworkVersion() {
        if (preferredFramework != null) {
            return preferredFramework;
        }
        if (!hasAndroidManifest()) {
            return null;
        }
        AndroidManifestBlock manifest = getAndroidManifest();
        Integer version = manifest.getCompileSdkVersion();
        if (version == null) {
            version = manifest.getPlatformBuildVersionCode();
        }
        Integer target = manifest.getTargetSdkVersion();
        if (version == null) {
            version = target;
        } else if (target != null && target > version) {
            version = target;
        }
        return version;
    }
    public void removeResFilesWithEntry(int resourceId) {
        removeResFilesWithEntry(resourceId, null, true);
    }
    public void removeResFilesWithEntry(int resourceId, ResConfig resConfig, boolean trimEntryArray) {
        List<Entry> removedList = removeResFiles(resourceId, resConfig);
        SpecTypePair specTypePair = null;
        for (Entry entry:removedList) {
            if (entry == null || entry.isNull()) {
                continue;
            }
            if (trimEntryArray && specTypePair == null) {
                specTypePair = entry.getTypeBlock().getParentSpecTypePair();
            }
            entry.setNull(true);
        }
        if (specTypePair != null) {
            specTypePair.removeNullEntries(resourceId);
        }
    }
    public List<Entry> removeResFiles(int resourceId) {
        return removeResFiles(resourceId, null);
    }
    public List<Entry> removeResFiles(int resourceId, ResConfig resConfig) {
        ArrayCollection<Entry> results = new ArrayCollection<>();
        if (resourceId == 0 && resConfig == null) {
            return results;
        }
        List<ResFile> resFileList = listResFiles(resourceId, resConfig);
        ZipEntryMap zipEntryMap = getZipEntryMap();
        for (ResFile resFile:resFileList) {
            results.addAll(resFile.iterator());
            zipEntryMap.remove(resFile.getInputSource());
        }
        return results;
    }
    public XMLDocument decodeXMLFile(String path) throws IOException {
        ResXmlDocument resXmlDocument = loadResXmlDocument(path);
        AndroidManifestBlock manifest = getAndroidManifest();
        int pkgId = manifest.guessCurrentPackageId();
        if (pkgId != 0 && hasTableBlock()) {
            PackageBlock packageBlock = getTableBlock().pickOne(pkgId);
            if (packageBlock != null) {
                resXmlDocument.setPackageBlock(packageBlock);
            }
        }
        throw new RuntimeException("Method not implemented");
    }
    public List<DexFileInputSource> listDexFiles() {
        List<DexFileInputSource> results = new ArrayCollection<>();
        for (InputSource source: getInputSources()) {
            if (DexFileInputSource.isDexName(source.getAlias())) {
                DexFileInputSource inputSource;
                if (source instanceof DexFileInputSource) {
                    inputSource = (DexFileInputSource)source;
                } else {
                    inputSource = new DexFileInputSource(source.getAlias(), source);
                }
                results.add(inputSource);
            }
        }
        DexFileInputSource.sort(results);
        return results;
    }
    public List<InputSource> listNativeLibraryFiles() {
        return CollectionUtil.toList(getZipEntryMap().iteratorWithPath(
                path -> path != null && path.startsWith("lib/")
                        && path.endsWith(".so")));
    }
    public boolean isBaseModule() {
        if (!hasAndroidManifest()) {
            return false;
        }
        AndroidManifestBlock manifest;
        try {
            manifest= getAndroidManifest();
            return !(manifest.isSplit() || manifest.getMainActivity() == null);
        } catch (Exception ignored) {
            return false;
        }
    }
    public String getModuleName() {
        return moduleName;
    }
    public void setModuleName(String moduleName) {
        if (moduleName == null) {
            throw new NullPointerException();
        }
        this.moduleName = moduleName;
        this.zipEntryMap.setModuleName(moduleName);
    }
    public void writeApk(File file) throws IOException {
        writeApk(file, null);
    }
    public void writeApk(File file, WriteProgress progress) throws IOException {
        ApkFileWriter writer = createApkFileWriter(file);
        writer.setWriteProgress(progress);
        writer.write();
    }
    public byte[] writeApkBytes() throws IOException {
        ApkByteWriter writer = createApkByteWriter();
        writer.write();
        return writer.toByteArray();
    }
    public void writeApk(OutputStream outputStream) throws IOException {
        createApkStreamWriter(outputStream).write();
    }
    public ApkFileWriter createApkFileWriter(File file) throws IOException {
        updateUncompressedFiles();
        ApkFileWriter writer = new ApkFileWriter(file, getZipEntryMap().toArray(true));
        applyDefaultApkWriterSetting(writer);
        return writer;
    }
    public ApkByteWriter createApkByteWriter() {
        updateUncompressedFiles();
        ApkByteWriter writer = new ApkByteWriter(getZipEntryMap().toArray(true));
        applyDefaultApkWriterSetting(writer);
        return writer;
    }
    public ApkStreamWriter createApkStreamWriter(OutputStream outputStream) {
        updateUncompressedFiles();
        ApkStreamWriter writer = new ApkStreamWriter(outputStream,
                getZipEntryMap().toArray(true));
        applyDefaultApkWriterSetting(writer);
        return writer;
    }
    private void applyDefaultApkWriterSetting(ApkWriter<?, ?> writer) {
        writer.setAPKLogger(getApkLogger());
        writer.setApkSignatureBlock(getApkSignatureBlock());
        writer.setArchiveInfo(getZipEntryMap().getArchiveInfo());
        writer.setDataDescriptorFactory(DataDescriptorFactory.NO_ACTION);
        writer.setCompressionLevel(getCompressionLevel());
    }
    public void uncompressNonXmlResFiles() {
        for (ResFile resFile:listResFiles()) {
            if (resFile.isBinaryXml()) {
                continue;
            }
            resFile.getInputSource().setMethod(ZipEntry.STORED);
        }
    }
    public UncompressedFiles getUncompressedFiles() {
        return mUncompressedFiles;
    }
    public void setExtractNativeLibs(Boolean extractNativeLibs) {
        int i;
        if (extractNativeLibs == null) {
            i = 1;
        } else if (!extractNativeLibs) {
            i = 2;
        } else {
            i = 3;
        }
        this.extractNativeLibs = i;
    }
    private void applyExtractNativeLibs() {
        Boolean extractNativeLibs;
        int i = this.extractNativeLibs;
        if (i == 1) {
            extractNativeLibs = null;
        } else if (i == 2) {
            extractNativeLibs = Boolean.FALSE;
        } else if (i == 3) {
            extractNativeLibs = Boolean.TRUE;
        } else {
            return;
        }
        applyExtractNativeLibsOnLoadedManifest(extractNativeLibs);
        unCompressNativeLibs(Boolean.FALSE.equals(extractNativeLibs));
    }
    private void applyExtractNativeLibsOnLoadedManifest(Boolean value) {
        AndroidManifestBlock manifestBlock = this.mManifestBlock;
        if (manifestBlock != null) {
            manifestBlock.setExtractNativeLibs(value);
        }
    }
    private void unCompressNativeLibs(boolean unCompressed) {
        List<InputSource> nativeLibraryFiles = listNativeLibraryFiles();
        UncompressedFiles uncompressedFiles = getUncompressedFiles();
        for (InputSource inputSource : nativeLibraryFiles) {
            inputSource.setUncompressed(unCompressed);
            if (unCompressed) {
                uncompressedFiles.addPath(inputSource);
            } else {
                uncompressedFiles.removePath(inputSource.getName());
                uncompressedFiles.removePath(inputSource.getAlias());
            }
        }
    }
    public void updateUncompressedFiles() {
        getUncompressedFiles().apply(getZipEntryMap());
        applyExtractNativeLibs();
    }
    public void removeDir(String dirName) {
        getZipEntryMap().removeDir(dirName);
    }
    public void validateResourcesDir() {
        List<ResFile> resFileList = listResFiles();
        Set<String> existPaths=new HashSet<>();
        InputSource[] sourceList = getInputSources();
        for (InputSource inputSource:sourceList) {
            existPaths.add(inputSource.getAlias());
        }
        for (ResFile resFile:resFileList) {
            String path = resFile.getFilePath();
            String pathNew = resFile.validateTypeDirectoryName();
            if (pathNew == null || pathNew.equals(path)) {
                continue;
            }
            if (existPaths.contains(pathNew)) {
                continue;
            }
            existPaths.remove(path);
            existPaths.add(pathNew);
            resFile.setFilePath(pathNew);
            if (resFile.getInputSource().getMethod() == ZipEntry.STORED) {
                getUncompressedFiles().replacePath(path, pathNew);
            }
            logVerbose("Dir validated: '"+path+"' -> '"+pathNew+"'");
        }
        getTableBlock().refresh();
    }
    public void setResourcesRootDir(String dirName) {
        List<ResFile> resFileList = listResFiles();
        Set<String> existPaths=new HashSet<>();
        InputSource[] sourceList = getInputSources();
        for (InputSource inputSource:sourceList) {
            existPaths.add(inputSource.getAlias());
        }
        for (ResFile resFile:resFileList) {
            String path=resFile.getFilePath();
            String pathNew=ApkUtil.replaceRootDir(path, dirName);
            if (existPaths.contains(pathNew)) {
                continue;
            }
            existPaths.remove(path);
            existPaths.add(pathNew);
            resFile.setFilePath(pathNew);
            if (resFile.getInputSource().getMethod() == ZipEntry.STORED) {
                getUncompressedFiles().replacePath(path, pathNew);
            }
            logVerbose("Root changed: '"+path+"' -> '"+pathNew+"'");
        }
        getTableBlock().refresh();
    }
    public List<ResFile> listResFiles() {
        return listResFiles(0, null);
    }
    public List<ResFile> listResFiles(int resourceId, ResConfig resConfig) {
        List<ResFile> results = new ArrayCollection<>();
        TableBlock tableBlock = getTableBlock();
        if (tableBlock == null) {
            return results;
        }
        TableStringPool stringPool= tableBlock.getStringPool();
        for (InputSource inputSource : getInputSources()) {
            String name = inputSource.getAlias();
            Iterator<TableString> iterator = stringPool.getAll(name);
            while (iterator.hasNext()) {
                TableString tableString = iterator.next();
                List<Entry> entryList = filterResFileEntries(tableString, resourceId, resConfig);
                if (!entryList.isEmpty()) {
                    ResFile resFile = new ResFile(inputSource, entryList);
                    results.add(resFile);
                }
            }
        }
        return results;
    }
    public boolean removeResFile(String path) {
        return removeResFile(path, true);
    }
    public boolean removeResFile(String path, boolean keepResourceId) {
        InputSource inputSource = getInputSource(path);
        if (inputSource == null) {
            return false;
        }
        ResFile resFile = getResFile(path);
        if (resFile == null) {
            return false;
        }
        resFile.delete(keepResourceId);
        removeInputSource(path);
        return true;
    }
    public ResFile getResFile(String path) {
        InputSource inputSource = getInputSource(path);
        if (inputSource == null) {
            return null;
        }
        List<Entry> entryList = listReferencedEntries(path);
        if (entryList.isEmpty()) {
            return null;
        }
        return new ResFile(inputSource, entryList);
    }

    public List<Entry> listReferencedEntries(String path) {
        ArrayCollection<Entry> results = new ArrayCollection<>();
        TableBlock tableBlock = getTableBlock();
        if (tableBlock != null) {
            TableStringPool stringPool = tableBlock.getStringPool();
            Iterator<TableString> iterator = stringPool.getAll(path);
            Predicate<Entry> filter = entry -> entry.isScalar() &&
                    TypeBlock.canHaveResourceFile(entry.getTypeName());
            while (iterator.hasNext()) {
                results.addAll(iterator.next().getEntries(filter));
            }
        }
        return results;
    }
    private List<Entry> filterResFileEntries(TableString tableString, int resourceId, ResConfig resConfig) {
        Iterator<Entry> itr = tableString.getEntries(item -> {
            if (!item.isScalar() ||
                    !TypeBlock.canHaveResourceFile(item.getTypeName())) {
                return false;
            }
            if (resourceId != 0 && resourceId != item.getResourceId()) {
                return false;
            }
            return resConfig == null || resConfig.equals(item.getResConfig());
        });
        return CollectionUtil.toList(itr);
    }
    public int getVersionCode() {
        AndroidManifestBlock manifestBlock = getAndroidManifest();
        if (manifestBlock != null) {
            Integer versionCode = manifestBlock.getVersionCode();
            if (versionCode != null) {
                return versionCode;
            }
        }
        return 0;
    }
    public String getPackageName() {
        if (hasAndroidManifest()) {
            return getAndroidManifest().getPackageName();
        }
        if (!hasTableBlock()) {
            return null;
        }
        TableBlock tableBlock=getTableBlock();
        PackageArray pkgArray = tableBlock.getPackageArray();
        PackageBlock pkg = pkgArray.get(0);
        if (pkg == null) {
            return null;
        }
        return pkg.getName();
    }
    public void setPackageName(String name) {
        String old=getPackageName();
        if (hasAndroidManifest()) {
            getAndroidManifest().setPackageName(name);
        }
        if (!hasTableBlock()) {
            return;
        }
        TableBlock tableBlock=getTableBlock();
        PackageArray pkgArray = tableBlock.getPackageArray();
        for (PackageBlock pkg:pkgArray.listItems()) {
            if (pkgArray.size()==1) {
                pkg.setName(name);
                continue;
            }
            String pkgName=pkg.getName();
            if (pkgName.startsWith(old)) {
                pkgName=pkgName.replace(old, name);
                pkg.setName(pkgName);
            }
        }
    }
    // Use hasAndroidManifest
    @Deprecated
    public boolean hasAndroidManifestBlock() {
        return hasAndroidManifest();
    }
    public boolean hasAndroidManifest() {
        return mManifestBlock != null
                || getZipEntryMap().getInputSource(AndroidManifestBlock.FILE_NAME) != null;
    }
    public boolean hasTableBlock() {
        return mTableBlock != null
                || getZipEntryMap().getInputSource(TableBlock.FILE_NAME) != null;
    }
    public void destroy() {
        getZipEntryMap().clear();
        AndroidManifestBlock manifestBlock = this.mManifestBlock;
        if (manifestBlock != null) {
            manifestBlock.clear();
            this.mManifestBlock = null;
        }
        TableBlock tableBlock = this.mTableBlock;
        if (tableBlock != null) {
            mExternalFrameworks.clear();
            tableBlock.clear();
            this.mTableBlock = null;
        }
        try {
            close();
        } catch (IOException ignored) {
        }
    }
    public void setManifest(AndroidManifestBlock manifestBlock) {
        ZipEntryMap archive = getZipEntryMap();
        if (manifestBlock == null) {
            mManifestBlock = null;
            mManifestOriginalSource = null;
            archive.remove(AndroidManifestBlock.FILE_NAME);
            return;
        }
        manifestBlock.setApkFile(this);
        BlockInputSource<AndroidManifestBlock> source =
                new BlockInputSource<>(AndroidManifestBlock.FILE_NAME, manifestBlock);
        source.setMethod(ZipEntry.STORED);
        source.setSort(0);
        archive.add(source);
        mManifestBlock = manifestBlock;
    }
    public void setTableBlock(TableBlock tableBlock) {
        ZipEntryMap archive = getZipEntryMap();
        if (tableBlock == null) {
            mTableBlock = null;
            mTableOriginalSource = null;
            archive.remove(TableBlock.FILE_NAME);
            unlinkLoadedManifest();
            return;
        }
        tableBlock.setApkFile(this);
        BlockInputSource<TableBlock> source =
                new BlockInputSource<>(TableBlock.FILE_NAME, tableBlock);
        archive.add(source);
        source.setMethod(ZipEntry.STORED);
        source.setSort(1);
        getUncompressedFiles().addPath(source);
        mTableBlock = tableBlock;
        updateExternalFramework();
        ensureLoadedManifestLinked();
    }
    public boolean ensureTableBlock() {
        if (!hasTableBlock()) {
            setTableBlock(TableBlock.createEmpty());
            return true;
        }
        return false;
    }
    /**
     * Use getAndroidManifest()
     * */
    @Deprecated
    public AndroidManifestBlock getAndroidManifestBlock() {
        return getAndroidManifest();
    }
    @Override
    public AndroidManifestBlock getAndroidManifest() {
        if (mManifestBlock != null) {
            return mManifestBlock;
        }
        InputSource inputSource = getInputSource(AndroidManifestBlock.FILE_NAME);
        if (inputSource == null) {
            return null;
        }
        setManifestOriginalSource(inputSource);
        InputStream inputStream;
        try {
            inputStream = inputSource.openStream();
            AndroidManifestBlock manifestBlock = AndroidManifestBlock.load(inputStream);
            inputStream.close();
            this.mManifestBlock = manifestBlock;
            BlockInputSource<AndroidManifestBlock> blockInputSource = new BlockInputSource<>(
                    inputSource.getName(),manifestBlock);
            blockInputSource.copyAttributes(inputSource);
            addInputSource(blockInputSource);
            ensureLoadedManifestLinked();
            onManifestBlockLoaded(manifestBlock);
        } catch (IOException exception) {
            throw new IllegalArgumentException(exception);
        }
        return mManifestBlock;
    }
    private void onManifestBlockLoaded(AndroidManifestBlock manifestBlock) {
        initializeApkType(manifestBlock);
    }
    public TableBlock getTableBlock(boolean initFramework) {
        TableBlock tableBlock = this.mTableBlock;
        if (tableBlock == null) {
            if (!hasTableBlock()) {
                return null;
            }
            try {
                tableBlock = loadTableBlock();
                this.mTableBlock = tableBlock;
                if (initFramework && loadDefaultFramework) {
                    Integer version = getAndroidFrameworkVersion();
                    initializeAndroidFramework(tableBlock, version);
                }
                updateExternalFramework();
            } catch (IOException exception) {
                throw new IllegalArgumentException(exception);
            }
            ensureLoadedManifestLinked();
        }
        return tableBlock;
    }
    private void ensureLoadedManifestLinked() {
        TableBlock tableBlock = this.mTableBlock;
        if (tableBlock == null) {
            return;
        }
        AndroidManifestBlock manifestBlock = this.mManifestBlock;
        if (manifestBlock == null) {
            return;
        }
        PackageBlock packageBlock = manifestBlock.getPackageBlock();
        if (packageBlock != null) {
            TableBlock linkedTable = packageBlock.getTableBlock();
            if (linkedTable == tableBlock) {
                return;
            }
        }
        packageBlock = tableBlock.pickOne(manifestBlock.guessCurrentPackageId());
        if (packageBlock == null) {
            packageBlock = tableBlock.pickOne();
        }
        if (packageBlock != null) {
            manifestBlock.setPackageBlock(packageBlock);
        }
        manifestBlock.setApkFile(this);
        ensureFrameworkLinked();
    }
    private void unlinkLoadedManifest() {
        AndroidManifestBlock manifestBlock = this.mManifestBlock;
        if (manifestBlock == null) {
            return;
        }
        manifestBlock.setPackageBlock(null);
        manifestBlock.setApkFile(null);
    }
    private void ensureFrameworkLinked() {
        if (mDisableLoadFramework) {
            return;
        }
        TableBlock tableBlock = this.mTableBlock;
        if (tableBlock == null ||
                tableBlock instanceof FrameworkTable ||
                isAndroid(tableBlock)) {
            return;
        }
        Integer preferred = this.preferredFramework;
        if (preferred != null || (mManifestBlock != null && !tableBlock.hasFramework())) {
            try {
                initializeAndroidFramework(tableBlock, preferred);
            } catch (IOException ignored) {
            }
        }
    }
    private void updateExternalFramework() {
        TableBlock tableBlock = mTableBlock;
        if (tableBlock == null) {
            return;
        }
        for (TableBlock framework : mExternalFrameworks) {
            tableBlock.addFramework(framework);
        }
    }
    public void discardManifestChanges() {
        getZipEntryMap().add(getManifestOriginalSource());
    }
    public void keepManifestChanges() {
        mManifestOriginalSource = null;
    }
    public InputSource getManifestOriginalSource() {
        InputSource inputSource = this.mManifestOriginalSource;
        if (inputSource == null) {
            inputSource = getInputSource(AndroidManifestBlock.FILE_NAME);
            mManifestOriginalSource = inputSource;
        }
        return inputSource;
    }
    private void setManifestOriginalSource(InputSource inputSource) {
        if (mManifestOriginalSource == null
                && !(inputSource instanceof BlockInputSource)) {
            mManifestOriginalSource = inputSource;
        }
    }
    public void discardTableBlockChanges() {
        getZipEntryMap().add(getTableOriginalSource());
    }
    public void keepTableBlockChanges() {
        mTableOriginalSource = null;
    }
    public InputSource getTableOriginalSource() {
        InputSource inputSource = this.mTableOriginalSource;
        if (inputSource == null) {
            inputSource = getInputSource(TableBlock.FILE_NAME);
            mTableOriginalSource = inputSource;
        }
        return inputSource;
    }
    private void setTableOriginalSource(InputSource inputSource) {
        if (mTableOriginalSource == null
                && !(inputSource instanceof BlockInputSource)) {
            mTableOriginalSource = inputSource;
        }
    }
    @Override
    public TableBlock getTableBlock() {
        if (mTableBlock != null) {
            return mTableBlock;
        }
        checkExternalFramework();
        checkSelfFramework();
        return getTableBlock(!mDisableLoadFramework);
    }
    @Override
    public TableBlock getLoadedTableBlock() {
        return mTableBlock;
    }
    private void checkExternalFramework() {
        if (mDisableLoadFramework || preferredFramework != null) {
            return;
        }
        if (mExternalFrameworks.size() == 0) {
            return;
        }
        mDisableLoadFramework = true;
    }
    private void checkSelfFramework() {
        if (mDisableLoadFramework || preferredFramework != null) {
            return;
        }
        AndroidManifestBlock manifest = getAndroidManifest();
        if (manifest == null) {
            return;
        }
        if (manifest.isCoreApp() == null
                || !"android".equals(manifest.getPackageName())) {
            return;
        }
        if (manifest.guessCurrentPackageId() != 0x01) {
            return;
        }
        logMessage("Looks like framework apk, skip loading framework");
        mDisableLoadFramework = true;
    }

    @Override
    public ResXmlDocument getResXmlDocument(String path) {
        InputSource inputSource = getInputSource(path);
        if (inputSource != null) {
            try {
                return loadResXmlDocument(inputSource);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
    @Override
    public ResXmlDocument loadResXmlDocument(String path) throws IOException{
        InputSource inputSource = getInputSource(path);
        if (inputSource == null) {
            throw new FileNotFoundException("No such file in apk: " + path);
        }
        return loadResXmlDocument(inputSource);
    }
    public ResXmlDocument loadResXmlDocument(InputSource inputSource) throws IOException{
        ResXmlDocument resXmlDocument = null;
        if (inputSource instanceof BlockInputSource) {
            Block block = ((BlockInputSource<?>) inputSource).getBlock();
            if (block instanceof ResXmlDocument) {
                resXmlDocument = (ResXmlDocument) block;
            }
        }
        if (resXmlDocument == null) {
            resXmlDocument = new ResXmlDocument();
            resXmlDocument.readBytes(inputSource.openStream());
        }
        resXmlDocument.setApkFile(this);
        if (resXmlDocument.getPackageBlock() == null) {
            resXmlDocument.setPackageBlock(findPackageForPath(inputSource.getAlias()));
        }
        return resXmlDocument;
    }
    private PackageBlock findPackageForPath(String path) {
        TableBlock tableBlock = getTableBlock();
        if (tableBlock == null) {
            return null;
        }
        if (tableBlock.size() == 1) {
            return tableBlock.get(0);
        }
        PackageBlock packageBlock = CollectionUtil.getFirst(
                tableBlock.getStringPool().getUsers(PackageBlock.class, path));
        if (packageBlock == null) {
            packageBlock = tableBlock.pickOne();
        }
        return packageBlock;
    }
    public ApkType getApkType() {
        if (mApkType != null) {
            return mApkType;
        }
        return initializeApkType(mManifestBlock);
    }
    public void setApkType(ApkType apkType) {
        this.mApkType = apkType;
    }
    private ApkType initializeApkType(AndroidManifestBlock manifestBlock) {
        if (mApkType != null) {
            return mApkType;
        }
        ApkType apkType = null;
        if (manifestBlock != null) {
            apkType = manifestBlock.guessApkType();
        }
        if (apkType != null) {
            mApkType = apkType;
        } else {
            apkType = ApkType.UNKNOWN;
        }
        return apkType;
    }

    // If we need TableStringPool only, this loads pool without
    // loading packages and other chunk blocks for faster and less memory usage
    public TableStringPool getVolatileTableStringPool() throws IOException{
        if (mTableBlock != null) {
            return mTableBlock.getStringPool();
        }
        InputSource inputSource = getInputSource(TableBlock.FILE_NAME);
        if (inputSource == null) {
            throw new IOException("Module don't have: "+TableBlock.FILE_NAME);
        }
        if ((inputSource instanceof ArchiveFileEntrySource)
                ||(inputSource instanceof FileInputSource)) {
            InputStream inputStream = inputSource.openStream();
            TableStringPool stringPool = TableStringPool.readFromTable(inputStream);
            inputStream.close();
            return stringPool;
        }
        return getTableBlock().getStringPool();
    }
    TableBlock loadTableBlock() throws IOException {
        InputSource inputSource = getInputSource(TableBlock.FILE_NAME);
        if (inputSource == null) {
            throw new IOException("Entry not found: "+TableBlock.FILE_NAME);
        }
        TableBlock tableBlock;
        if (inputSource instanceof BlockInputSource) {
            tableBlock = (TableBlock) ((BlockInputSource<?>) inputSource).getBlock();
        } else {
            setTableOriginalSource(inputSource);
            InputStream inputStream = inputSource.openStream();
            tableBlock = TableBlock.load(inputStream);
            inputStream.close();
        }
        BlockInputSource<TableBlock> blockInputSource = new BlockInputSource<>(
                inputSource.getName(), tableBlock);
        blockInputSource.copyAttributes(inputSource);
        getZipEntryMap().add(blockInputSource);
        tableBlock.setApkFile(this);
        return tableBlock;
    }
    @Override
    public void add(InputSource inputSource) {
        if (inputSource == null) {
            return;
        }
        String path = inputSource.getAlias();
        if (AndroidManifestBlock.FILE_NAME.equals(path)) {
            InputSource manifestSource = getManifestOriginalSource();
            if (manifestSource != inputSource) {
                mManifestBlock = null;
            }
            setManifestOriginalSource(inputSource);
        } else if (TableBlock.FILE_NAME.equals(path)) {
            InputSource table = getTableOriginalSource();
            if (inputSource != table) {
                mTableBlock = null;
            }
            setTableOriginalSource(inputSource);
        }
        addInputSource(inputSource);
    }

    @Override
    public boolean containsFile(String path) {
        return getZipEntryMap().contains(path);
    }

    @Override
    public InputSource getInputSource(String path) {
        return getZipEntryMap().getInputSource(path);
    }
    public InputSource removeInputSource(String path) {
        return getZipEntryMap().remove(path);
    }
    private void addInputSource(InputSource inputSource) {
        getZipEntryMap().add(inputSource);
    }
    public List<InputSource> listInputSources() {
        return getZipEntryMap().listInputSources();
    }
    public InputSource[] getInputSources() {
        return getZipEntryMap().toArray();
    }
    public ZipEntryMap getZipEntryMap() {
        return zipEntryMap;
    }
    public void setLoadDefaultFramework(boolean loadDefaultFramework) {
        this.loadDefaultFramework = loadDefaultFramework;
        this.mDisableLoadFramework = !loadDefaultFramework;
    }

    public void merge(ApkModule module) throws IOException {
        merge(module, false);
    }
    public void merge(ApkModule module, boolean force) throws IOException {
        if (module == null || module == this) {
            return;
        }
        logMessage("Merging: " + module.getModuleName());
        validateMerge(module, force);
        mergeDexFiles(module);
        mergeTable(module);
        mergeFiles(module);
        getUncompressedFiles().merge(module.getUncompressedFiles());
        mergeFusedModules(module);
    }
    private void mergeFusedModules(ApkModule module) {
        if (!this.hasAndroidManifest() || !module.hasAndroidManifest()) {
            return;
        }
        AndroidManifestBlock baseManifest = this.getAndroidManifest();
        AndroidManifestBlock manifest = module.getAndroidManifest();
        if (!manifest.isFusingInclude()) {
            return;
        }
        String[] fusedModules = manifest.getFusedModuleNames();
        if (fusedModules != null && fusedModules.length != 0) {
            baseManifest.addFusedModuleNames(fusedModules);
            logMessage("Fused modules added [" +
                    StringsUtil.join(fusedModules, ',') + "]");
        }
        String split = manifest.getSplit();
        if (split != null) {
            baseManifest.addFusedModuleNames(split);
            logMessage("Added as fused module <" + split + ">");
        }
    }
    private void validateMerge(ApkModule apkModule, boolean force) throws IOException{
        if (!hasTableBlock()) {
            return;
        }
        String packageName = getPackageName();
        int code = getVersionCode();
        if (packageName == null || code == 0) {
            return;
        }
        String packageName2 = apkModule.getPackageName();
        int code2 = apkModule.getVersionCode();
        if (packageName2 == null || code2 == 0) {
            return;
        }
        if (!packageName.equals(packageName2)) {
            return;
        }
        if (code == code2) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (!force) {
            builder.append("WARN: ");
        }
        builder.append("Incompatible to merge: {");
        builder.append(packageName);
        builder.append(", ");
        builder.append(code);
        builder.append("}, with {");
        builder.append(packageName2);
        builder.append(", ");
        builder.append(code2);
        builder.append("}");
        String msg = builder.toString();
        if (force) {
            throw new IOException(msg);
        }
        logMessage(msg);
    }
    private void mergeTable(ApkModule module) {
        if (!module.hasTableBlock()) {
            return;
        }
        TableBlock exist;
        if (!hasTableBlock()) {
            exist=new TableBlock();
            BlockInputSource<TableBlock> inputSource=new BlockInputSource<>(TableBlock.FILE_NAME, exist);
            addInputSource(inputSource);
        } else {
            exist=getTableBlock();
        }
        TableBlock coming=module.getTableBlock();
        exist.merge(coming);
    }
    private void mergeFiles(ApkModule module) {
        ZipEntryMap entryMapExist = getZipEntryMap();
        ZipEntryMap entryMapComing = module.getZipEntryMap();
        Map<String, InputSource> comingAlias = entryMapComing.toAliasMap();
        Map<String, InputSource> existAlias = entryMapExist.toAliasMap();
        UncompressedFiles uncompressedFiles = module.getUncompressedFiles();
        for (InputSource inputSource:comingAlias.values()) {
            if (existAlias.containsKey(inputSource.getAlias())
                    || existAlias.containsKey(inputSource.getName())) {
                continue;
            }
            if (DexFileInputSource.isDexName(inputSource.getName())) {
                continue;
            }
            if (inputSource.getAlias().startsWith("lib/")) {
                uncompressedFiles.removePath(inputSource.getAlias());
            }
            logVerbose("Added: " + inputSource.getAlias());
            entryMapExist.add(inputSource);
        }
    }
    private void mergeDexFiles(ApkModule module) {
        UncompressedFiles uncompressedFiles = module.getUncompressedFiles();
        List<DexFileInputSource> existList = listDexFiles();
        List<DexFileInputSource> comingList = module.listDexFiles();
        ZipEntryMap zipEntryMap = getZipEntryMap();
        int index=0;
        if (existList.size()>0) {
            index=existList.get(existList.size()-1).getDexNumber();
            if (index==0) {
                index=2;
            } else {
                index++;
            }
        }
        for (DexFileInputSource source : comingList) {
            uncompressedFiles.removePath(source.getAlias());
            String name = DexFileInputSource.getDexName(index);
            DexFileInputSource add = new DexFileInputSource(name, source.getInputSource());
            zipEntryMap.add(add);
            logMessage("Added [" + module.getModuleName() +"] "
                    + source.getAlias() + " -> " + name);
            index++;
            if (index==1) {
                index=2;
            }
        }
    }
    public APKLogger getApkLogger() {
        return apkLogger;
    }
    public void setAPKLogger(APKLogger logger) {
        this.apkLogger = logger;
    }
    void logMessage(String msg) {
        if (apkLogger != null) {
            apkLogger.logMessage(msg);
        }
    }
    private void logError(String msg, Throwable tr) {
        if (apkLogger != null) {
            apkLogger.logError(msg, tr);
        }
    }
    private void logVerbose(String msg) {
        if (apkLogger != null) {
            apkLogger.logVerbose(msg);
        }
    }
    public void setCloseable(Closeable closeable) {
        this.mCloseable = closeable;
    }
    @Override
    public void close() throws IOException {
        Closeable closeable = this.mCloseable;
        if (closeable != null) {
            closeable.close();
        }
    }
    @Override
    public String toString() {
        return getModuleName();
    }
    public static ApkModule loadApkFile(File apkFile) throws IOException {
        return loadApkFile(apkFile, ApkUtil.DEF_MODULE_NAME);
    }
    public static ApkModule loadApkFile(File apkFile, String moduleName) throws IOException {
        ArchiveFile archive = new ArchiveFile(apkFile);
        ApkModule apkModule = new ApkModule(moduleName, archive.createZipEntryMap());
        apkModule.setApkSignatureBlock(archive.getApkSignatureBlock());
        apkModule.setCloseable(archive);
        return apkModule;
    }
    public static ApkModule loadApkFile(File apkFile, File ... externalFrameworks) throws IOException {
        return loadApkFile(null, apkFile, externalFrameworks);
    }
    public static ApkModule loadApkFile(APKLogger logger, File apkFile, File ... externalFrameworks) throws IOException {
        ArchiveFile archive = new ArchiveFile(apkFile);
        ApkModule apkModule = new ApkModule(ApkUtil.DEF_MODULE_NAME, archive.createZipEntryMap());
        apkModule.setAPKLogger(logger);
        apkModule.setApkSignatureBlock(archive.getApkSignatureBlock());
        apkModule.setCloseable(archive);
        if (externalFrameworks == null || externalFrameworks.length == 0) {
            return apkModule;
        }
        for (File frameworkFile : externalFrameworks) {
            if (frameworkFile == null) {
                continue;
            }
            if (apkFile.equals(frameworkFile)) {
                throw new IOException("External framework should be different: " + apkFile);
            }
            apkModule.addExternalFramework(frameworkFile);
        }
        return apkModule;
    }
    public static ApkModule readApkBytes(byte[] bytes) throws IOException {
        ArchiveBytes archiveBytes = new ArchiveBytes(bytes);
        ApkModule apkModule = new ApkModule(archiveBytes.createZipEntryMap());
        apkModule.setModuleName("byte_" + System.currentTimeMillis());
        return apkModule;
    }

}
