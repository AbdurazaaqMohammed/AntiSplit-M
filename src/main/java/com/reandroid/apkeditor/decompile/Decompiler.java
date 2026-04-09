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
package com.reandroid.apkeditor.decompile;

import com.reandroid.apk.*;
import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Util;
import com.reandroid.apkeditor.smali.SmaliDecompiler;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.block.ApkSignatureBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.coder.xml.XmlCoder;

import java.io.File;
import java.io.IOException;

public class Decompiler extends CommandExecutor<DecompileOptions> {
    public Decompiler(DecompileOptions options){
        super(options, "[DECOMPILE] ");
    }
    @Override
    public void runCommand() throws IOException {
        DecompileOptions options = getOptions();
        delete(options.outputFile);
        logMessage("Loading ...");
        ApkModule apkModule=ApkModule.loadApkFile(this,
                options.inputFile, options.getFrameworks());
        apkModule.setPreferredFramework(options.frameworkVersion);
        if(options.signaturesDirectory != null){
            dumpSignatureBlock();
            return;
        }
        String protect = Util.isProtected(apkModule);
        if(protect!=null){
            logMessage(options.inputFile.getAbsolutePath());
            logMessage(protect);
            return;
        }
        if(options.resDirName!=null){
            logMessage("Renaming resources root dir: "+options.resDirName);
            apkModule.setResourcesRootDir(options.resDirName);
        }
        if(options.validateResDir){
            logMessage("Validating resources dir ...");
            apkModule.validateResourcesDir();
        }
        logMessage("Decompiling to " + options.type + " ...");

        ApkModuleDecoder decoder = getApkModuleDecoder(apkModule);
        decoder.decode(options.outputFile);
        logMessage("Saved to: "+options.outputFile);
    }
    private ApkModuleDecoder getApkModuleDecoder(ApkModule apkModule) throws IOException {
        DecompileOptions options = getOptions();
        ApkModuleDecoder decoder;
        if (DecompileOptions.TYPE_JSON.equals(options.type)) {
            decoder = new ApkModuleJsonDecoder(apkModule, options.splitJson);
        } else if (DecompileOptions.TYPE_RAW.equals(options.type)){
            decoder = new ApkModuleRawDecoder(apkModule);
        } else {
            ApkModuleXmlDecoder xmlDecoder = new ApkModuleXmlDecoder(apkModule);
            xmlDecoder.setKeepResPath(options.keepResPath);
            decoder = xmlDecoder;
            XmlCoder.getInstance().getSetting().setLogger(this);
        }
        decoder.sanitizeFilePaths();
        decoder.setDexDecoder(getSmaliDecompiler(apkModule));
        DexProfileDecoderImpl dexProfileDecoder = new DexProfileDecoderImpl(options);
        dexProfileDecoder.setApkLogger(this);
        decoder.setDexProfileDecoder(dexProfileDecoder);
        return decoder;
    }
    private SmaliDecompiler getSmaliDecompiler(ApkModule apkModule) throws IOException {
        if (getOptions().dex) {
            return null;
        }
        TableBlock tableBlock = getTableBlockForDexComment(apkModule);
        SmaliDecompiler smaliDecompiler = new SmaliDecompiler(tableBlock, getOptions());
        smaliDecompiler.setApkLogger(this);
        return smaliDecompiler;
    }
    private TableBlock getTableBlockForDexComment(ApkModule apkModule) throws IOException {
        if (apkModule.listDexFiles().isEmpty()) {
            return null;
        }
        if (apkModule.hasTableBlock()) {
            return apkModule.getTableBlock();
        }
        TableBlock tableBlock = getUserFrameworkForDexComment();
        if (tableBlock == null) {
            tableBlock = getInternalFrameworkForDexComment();
        }
        return tableBlock;
    }
    private TableBlock getUserFrameworkForDexComment() throws IOException {
        DecompileOptions options = getOptions();

        File[] files = options.getFrameworks();
        if (files.length == 1 && options.frameworkVersion == null) {
            logMessage("Loading framework: " + files[0]);
            return ApkModule.loadApkFile(files[0]).getTableBlock();
        }
        TableBlock tableBlock = null;
        if (files.length != 0) {
            tableBlock = TableBlock.createEmpty();
            for (File file : files) {
                logMessage("Loading framework: " + file);
                tableBlock.addFramework(ApkModule.loadApkFile(file)
                        .getTableBlock());
            }
        }
        if (tableBlock != null) {
            if (options.frameworkVersion != null) {
                FrameworkApk frameworkApk = AndroidFrameworks.getBestMatch(options.frameworkVersion);
                if (frameworkApk != null) {
                    tableBlock.addFramework(frameworkApk.getTableBlock());
                }
            }
            return tableBlock;
        }
        return null;
    }
    private TableBlock getInternalFrameworkForDexComment() {
        DecompileOptions options = getOptions();
        FrameworkApk frameworkApk = null;
        if (options.frameworkVersion != null) {
            frameworkApk = AndroidFrameworks.getBestMatch(options.frameworkVersion);
        }
        if (frameworkApk == null) {
            frameworkApk = AndroidFrameworks.getCurrent();
        }
        if (frameworkApk == null) {
            frameworkApk = AndroidFrameworks.getLatest();
        }
        if (frameworkApk != null) {
            logMessage("Using internal framework: " + frameworkApk.getName());
            return frameworkApk.getTableBlock();
        }
        return null;
    }
    private void dumpSignatureBlock() throws IOException {
        logMessage("Dumping signature blocks ...");
        DecompileOptions options = getOptions();
        ArchiveFile archive = new ArchiveFile(options.inputFile);
        ApkSignatureBlock apkSignatureBlock = archive.getApkSignatureBlock();
        if(apkSignatureBlock == null){
            logMessage("Don't have signature block");
            return;
        }
        apkSignatureBlock.writeSplitRawToDirectory(options.signaturesDirectory);
        logMessage("Signatures dumped to: " + options.signaturesDirectory);
    }
}
