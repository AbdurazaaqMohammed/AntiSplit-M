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

import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Util;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.apk.ApkModule;

import java.io.IOException;

public class Refactor extends CommandExecutor<RefactorOptions> {
    public Refactor(RefactorOptions options){
        super(options, "[REFACTOR] ");
    }
    @Override
    public void runCommand() throws IOException {
        RefactorOptions options = getOptions();
        delete(options.outputFile);
        logMessage("Loading apk: " + options.inputFile);
        ApkModule module = ApkModule.loadApkFile(this, options.inputFile);
        if(!module.hasTableBlock()){
            throw new IOException("Don't have " + TableBlock.FILE_NAME);
        }
        String protect = Util.isProtected(module);
        if(protect!=null){
            logMessage(options.inputFile.getAbsolutePath());
            logMessage(protect);
            return;
        }
        if(options.fixTypeNames){
            TypeNameRefactor typeNameRefactor=new TypeNameRefactor(module);
            typeNameRefactor.setApkLogger(this);
            typeNameRefactor.refactor();
        }
        if(options.publicXml != null){
            logMessage("Renaming from: " + options.publicXml);
            PublicXmlRefactor publicXmlRefactor =
                    new PublicXmlRefactor(module, options.publicXml);
            publicXmlRefactor.refactor();
        }else {
            logMessage("Auto refactoring ...");
            AutoRefactor autoRefactor=new AutoRefactor(module);
            autoRefactor.refactor();
            logMessage("Auto renamed entries");
            StringValueNameGenerator generator = new StringValueNameGenerator(module.getTableBlock());
            generator.refactor();
        }
        if(options.cleanMeta){
            logMessage("Clearing META-INF ...");
            clearMeta(module);
        }
        String message = module.refreshTable();
        if(message != null){
            logMessage(message);
        }
        logMessage("Writing apk ...");
        module.writeApk(options.outputFile);
        logMessage("Saved to: "+options.outputFile);
    }
}
