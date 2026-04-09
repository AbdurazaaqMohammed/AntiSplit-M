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
package com.reandroid.apkeditor;

import com.reandroid.apk.APKLogger;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.ARSCLib;
import com.reandroid.arsc.coder.xml.XmlCoderLogger;
import com.reandroid.commons.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class CommandExecutor<T extends Options> implements APKLogger, XmlCoderLogger {

    private final T options;
    private String mLogTag;
    private boolean mEnableLog;

    public CommandExecutor(T options, String logTag){
        this.options = options;
        this.mLogTag = logTag;
        this.mEnableLog = true;
    }
    /**
     * use run()
     * */
    @Deprecated
    public void run() throws IOException {
        runCommand();
    }
    public void runCommand() throws IOException {
        throw new RuntimeException("Method not implemented");
    }
    protected void delete(File file) {
        if(file == null || !file.exists()) {
            return;
        }
        logMessage("Delete: " + file);
        if(file.isFile()) {
            file.delete();
        } else if(file.isDirectory()) {
            Util.deleteDir(file);
        }
    }
    protected T getOptions() {
        return options;
    }

    protected void applyExtractNativeLibs(ApkModule apkModule, String extractNativeLibs) {
        if (extractNativeLibs != null) {
            Boolean value;
            if ("manifest".equalsIgnoreCase(extractNativeLibs)) {
                if (apkModule.hasAndroidManifest()) {
                    value = apkModule.getAndroidManifest().isExtractNativeLibs();
                } else {
                    value = null;
                }
            } else if ("true".equalsIgnoreCase(extractNativeLibs)) {
                value = Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(extractNativeLibs)) {
                value = Boolean.FALSE;
            } else {
                value = null;
            }
            logMessage("Applying: extractNativeLibs=" + value);
            apkModule.setExtractNativeLibs(value);
        }
    }

    protected void setLogTag(String tag) {
        if(tag == null){
            tag = "";
        }
        this.mLogTag = tag;
    }
    public void setEnableLog(boolean enableLog) {
        this.mEnableLog = enableLog;
    }
    @Override
    public void logMessage(String msg) {
        if(!mEnableLog){
            return;
        }
        Logger.i(mLogTag + msg);
    }
    @Override
    public void logError(String msg, Throwable tr) {
        if(!mEnableLog){
            return;
        }
        Logger.e(mLogTag + msg, tr);
    }
    @Override
    public void logVerbose(String msg) {
        if(!mEnableLog){
            return;
        }
        Logger.sameLine(mLogTag + msg);
    }
    @Override
    public void logMessage(String tag, String msg) {
        if(!mEnableLog){
            return;
        }
        Logger.sameLine(mLogTag + msg);
    }

    @Override
    public void logVerbose(String tag, String msg) {
        if(!mEnableLog){
            return;
        }
        Logger.sameLine(mLogTag + msg);
    }
    public void logWarn(String msg) {
        Logger.e(mLogTag + msg);
    }

    public void logVersion() {
        logMessage("Using: " + APKEditor.getName() + " version " + APKEditor.getVersion() + ", " + ARSCLib.getName() + " version " + ARSCLib.getVersion());
    }

    protected static void clearMeta(ApkModule module){
        removeSignature(module);
        module.setApkSignatureBlock(null);
    }
    protected static void removeSignature(ApkModule module){
        ZipEntryMap archive = module.getZipEntryMap();
        archive.removeIf(Pattern.compile("^META-INF/.+\\.(([MS]F)|(RSA))"));
        archive.remove("stamp-cert-sha256");
    }
}

