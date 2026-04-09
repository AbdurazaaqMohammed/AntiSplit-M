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
package com.reandroid.apkeditor.compile;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ApkModuleJsonEncoder;
import com.reandroid.apk.ApkModuleRawEncoder;
import com.reandroid.apk.ApkModuleXmlEncoder;
import com.reandroid.apk.DexProfileEncoder;
import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Options;
import com.reandroid.apkeditor.smali.SmaliCompiler;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.block.ApkSignatureBlock;
import com.reandroid.archive.writer.ApkFileWriter;
import com.reandroid.arsc.coder.xml.XmlCoder;

import java.io.File;
import java.io.IOException;

public class Builder extends CommandExecutor<BuildOptions> {

    public Builder(BuildOptions options){
        super(options, "[BUILD] ");
    }

    @Override
    public void runCommand() throws IOException {
        BuildOptions options = getOptions();
        delete(options.outputFile);
        String type = options.type;
        if (Options.TYPE_SIG.equals(type)) {
            restoreSignatures();
        } else if(Options.TYPE_RAW.equals(type)) {
            buildRaw();
        } else if(Options.TYPE_XML.equals(type)) {
            buildXml();
        } else if(Options.TYPE_JSON.equals(type)) {
            buildJson();
        }
    }
    private void restoreSignatures() throws IOException {
        logMessage("Restoring signatures ...");
        BuildOptions options = getOptions();
        ArchiveFile archive = new ArchiveFile(options.inputFile);
        ApkFileWriter apkWriter = new ApkFileWriter(options.outputFile, archive.getInputSources());
        apkWriter.setAPKLogger(this);
        ApkSignatureBlock apkSignatureBlock = new ApkSignatureBlock();
        apkSignatureBlock.scanSplitFiles(options.signaturesDirectory);
        apkWriter.setApkSignatureBlock(apkSignatureBlock);
        logMessage("Writing apk...");
        apkWriter.write();
        logMessage("Saved to: " + options.outputFile);
        apkWriter.close();
    }
    public void buildJson() throws IOException {
        logMessage("Scanning JSON directory ...");
        ApkModuleJsonEncoder encoder=new ApkModuleJsonEncoder();
        encoder.setApkLogger(this);


        BuildOptions options = getOptions();

        encoder.setDexEncoder(getSmaliCompiler());
        encoder.setDexProfileEncoder(getDexProfileEncoder());

        encoder.scanDirectory(options.inputFile);
        ApkModule loadedModule = encoder.getApkModule();
        loadedModule.setAPKLogger(this);
        if(options.resDirName!=null){
            logMessage("Renaming resources root dir: "+options.resDirName);
            loadedModule.setResourcesRootDir(options.resDirName);
        }
        if(options.validateResDir){
            logMessage("Validating resources dir ...");
            loadedModule.validateResourcesDir();
        }
        loadedModule.getZipEntryMap().autoSortApkFiles();
        writeApk(loadedModule);
    }
    public void buildXml() throws IOException {
        logMessage("Scanning XML directory ...");
        XmlCoder.getInstance().getSetting().setLogger(this);
        ApkModuleXmlEncoder encoder=new ApkModuleXmlEncoder();
        encoder.setApkLogger(this);

        BuildOptions options = getOptions();

        encoder.setDexEncoder(getSmaliCompiler());
        encoder.setDexProfileEncoder(getDexProfileEncoder());

        ApkModule loadedModule = encoder.getApkModule();
        loadedModule.setAPKLogger(this);

        loadedModule.setPreferredFramework(options.frameworkVersion);
        for (File file : options.frameworks) {
            loadedModule.addExternalFramework(file);
        }
        encoder.scanDirectory(options.inputFile);
        loadedModule = encoder.getApkModule();
        writeApk(loadedModule);
    }
    public void buildRaw() throws IOException {
        logMessage("Scanning Raw directory ...");
        ApkModuleRawEncoder encoder = new ApkModuleRawEncoder();
        encoder.setApkLogger(this);

        BuildOptions options = getOptions();
        if(!options.validateResDir && options.resDirName == null) {
            encoder.setKeepOriginal(true);
            logMessage("Keep original binaries");
        }

        encoder.setDexEncoder(getSmaliCompiler());
        encoder.setDexProfileEncoder(getDexProfileEncoder());

        ApkModule loadedModule = encoder.getApkModule();
        loadedModule.setAPKLogger(this);

        loadedModule.setPreferredFramework(options.frameworkVersion);
        for(File file : options.frameworks){
            loadedModule.addExternalFramework(file);
        }
        encoder.scanDirectory(options.inputFile);
        loadedModule = encoder.getApkModule();
        writeApk(loadedModule);
    }
    private void writeApk(ApkModule apkModule) throws IOException {
        BuildOptions options = getOptions();
        if (!Options.TYPE_RAW.equals(options.type) && !Options.TYPE_SIG.equals(options.type)) {
            applyExtractNativeLibs(apkModule, options.getExtractNativeLibs());
        }
        logMessage("Writing apk...");
        apkModule.writeApk(options.outputFile, null);
        apkModule.close();
        logMessage("Saved to: " + options.outputFile);
    }
    private SmaliCompiler getSmaliCompiler() {
        SmaliCompiler smaliCompiler = new SmaliCompiler(getOptions());
        smaliCompiler.setApkLogger(this);
        return smaliCompiler;
    }
    private DexProfileEncoder getDexProfileEncoder() {
        DexProfileEncoderImpl encoder = new DexProfileEncoderImpl(getOptions());
        encoder.setApkLogger(this);
        return encoder;
    }
}
