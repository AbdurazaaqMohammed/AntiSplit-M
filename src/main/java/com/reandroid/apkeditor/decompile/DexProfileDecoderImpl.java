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

import com.reandroid.apk.APKLogger;
import com.reandroid.apk.ApkModule;
import com.reandroid.apk.DexProfileDecoder;
import com.reandroid.dex.dexopt.ProfileDirectory;
import com.reandroid.dex.dexopt.ProfileFile;

import java.io.File;
import java.io.IOException;

public class DexProfileDecoderImpl implements DexProfileDecoder {

    private final DecompileOptions options;
    private APKLogger apkLogger;

    public DexProfileDecoderImpl(DecompileOptions options) {
        this.options = options;
    }

    @Override
    public void decodeDexProfile(ApkModule apkModule, File mainDirectory) throws IOException {
        if (!isEnabled(apkModule)) {
            return;
        }
        logMessage("Decoding assets/dexopt ...");
        ProfileDirectory profileDirectory = new ProfileDirectory();
        profileDirectory.readApk(apkModule.getZipEntryMap());
        logMessage("Scanning dex files for profile ...");
        profileDirectory.linkApk(apkModule.getZipEntryMap());
        profileDirectory.decodeToJsonDir(new File(mainDirectory, ProfileFile.DECODE_DIR_NAME));
    }
    private boolean isEnabled(ApkModule apkModule) {
        if (!options.dexProfile) {
            return false;
        }
        return apkModule.getInputSource(ProfileFile.PATH_PROF) != null ||
                apkModule.getInputSource(ProfileFile.PATH_PROFM) != null;
    }
    public void setApkLogger(APKLogger apkLogger) {
        this.apkLogger = apkLogger;
    }

    private void logMessage(String message) {
        APKLogger apkLogger = this.apkLogger;
        if (apkLogger != null) {
            apkLogger.logMessage(message);
        }
    }
}
