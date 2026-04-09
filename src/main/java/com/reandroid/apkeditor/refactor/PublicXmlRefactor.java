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
package com.reandroid.apkeditor.refactor;

import com.reandroid.apk.APKLogger;
import com.reandroid.apk.ApkModule;
import com.reandroid.identifiers.TableIdentifier;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

public class PublicXmlRefactor {
    private final ApkModule apkModule;
    private final File pubXmlFile;
    private APKLogger apkLogger;
    public PublicXmlRefactor(ApkModule apkModule, File pubXmlFile){
        this.apkModule = apkModule;
        this.pubXmlFile = pubXmlFile;
        this.apkLogger = apkModule.getApkLogger();
    }
    public void refactor() throws IOException {
        logMessage("Loading: " + pubXmlFile);
        TableIdentifier tableIdentifier = new TableIdentifier();
        try {
            tableIdentifier.loadPublicXml(pubXmlFile);
        } catch (XmlPullParserException ex) {
            throw new IOException(ex);
        }
        logMessage("Applying from public xml ...");
        tableIdentifier.setTableBlock(apkModule.getTableBlock());
        int count = tableIdentifier.renameSpecs();
        if(count == 0){
            logMessage("Nothing renamed !");
        }
        logMessage("Renamed: " + count);
        apkModule.getTableBlock().removeUnusedSpecs();
    }
    public void setApkLogger(APKLogger apkLogger) {
        this.apkLogger = apkLogger;
    }
    void logMessage(String msg) {
        APKLogger apkLogger = this.apkLogger;
        if(apkLogger!=null){
            apkLogger.logMessage(msg);
        }
    }
    void logError(String msg, Throwable tr) {
        APKLogger apkLogger = this.apkLogger;
        if(apkLogger == null || (msg == null && tr == null)){
            return;
        }
        apkLogger.logError(msg, tr);
    }
    void logVerbose(String msg) {
        APKLogger apkLogger = this.apkLogger;
        if(apkLogger!=null){
            apkLogger.logVerbose(msg);
        }
    }
}
