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
package com.reandroid.apkeditor.protect;

import com.reandroid.apkeditor.CommandExecutor;
import com.reandroid.apkeditor.Util;
import com.reandroid.apk.*;

import java.io.IOException;

public class Protector extends CommandExecutor<ProtectorOptions> {

    private ApkModule mApkModule;

    public Protector(ProtectorOptions options) {
        super(options, "[PROTECT] ");
    }

    public ApkModule getApkModule() {
        return this.mApkModule;
    }

    public void setApkModule(ApkModule apkModule) {
        this.mApkModule = apkModule;
    }

    @Override
    public ProtectorOptions getOptions() {
        return super.getOptions();
    }

    @Override
    public void runCommand() throws IOException {
        ProtectorOptions options = getOptions();
        delete(options.outputFile);
        ApkModule module = ApkModule.loadApkFile(this, options.inputFile);
        module.setLoadDefaultFramework(false);
        String protect = Util.isProtected(module);
        if(protect != null){
            logMessage(options.inputFile.getAbsolutePath());
            logMessage(protect);
            return;
        }
        setApkModule(module);
        new ManifestConfuser(this).confuse();
        new DirectoryConfuser(this).confuse();
        new FileNameConfuser(this).confuse();
        new TableConfuser(this).confuse();
        new DexConfuser(this).confuse();
        module.getTableBlock().refresh();
        logMessage("Writing apk ...");
        if (options.confuse_zip) {
            logMessage("Confusing zip structure ...");
            new ProtectedFileWriter(module, options.outputFile).write();
        } else {
            module.writeApk(options.outputFile);
        }
        module.close();
        logMessage("Saved to: " + options.outputFile);
    }
}
