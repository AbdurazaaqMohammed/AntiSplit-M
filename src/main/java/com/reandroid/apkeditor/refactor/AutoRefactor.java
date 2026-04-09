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
import com.reandroid.apk.ResFile;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.identifiers.PackageIdentifier;
import com.reandroid.identifiers.TableIdentifier;
import com.reandroid.identifiers.TypeIdentifier;

import java.io.IOException;
import java.util.List;

public class AutoRefactor {
    private final ApkModule mApkModule;
    private APKLogger apkLogger;
    public AutoRefactor(ApkModule apkModule){
        this.mApkModule = apkModule;
        this.apkLogger = apkModule.getApkLogger();
    }
    public void refactor() throws IOException {
        refactorResourceNames();
        refactorFilePaths();
    }
    public int refactorFilePaths(){
        logMessage("Validating file paths ...");
        int renameCount = 0;
        List<ResFile> resFileList = mApkModule.listResFiles();
        for(ResFile resFile:resFileList){
            String path = resFile.buildPath(PackageBlock.RES_DIRECTORY_NAME);
            if(path.equals(resFile.getFilePath())){
                continue;
            }
            resFile.setFilePath(path);
            renameCount++;
        }
        return renameCount;
    }
    private void refactorResourceNames(){
        logMessage("Validating resource names ...");
        TableIdentifier tableIdentifier = new TableIdentifier();
        TableBlock tableBlock = mApkModule.getTableBlock();
        tableIdentifier.load(tableBlock);
        String msg = tableIdentifier.validateSpecNames();
        if(msg == null){
            logMessage("All resource names are valid");
            return;
        }
        int count = 0;
        for(PackageIdentifier pi: tableIdentifier.getPackages()){
            for(TypeIdentifier ti:pi.list()){
                EntryRefactor entryRefactor = new EntryRefactor(ti);
                count += entryRefactor.refactorAll();
            }
        }
        logMessage(msg);
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
