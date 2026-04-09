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

import com.reandroid.apk.APKLogger;
import com.reandroid.apk.ApkUtil;
import com.reandroid.apk.DexProfileEncoder;
import com.reandroid.dex.dexopt.ProfileDirectory;
import com.reandroid.dex.dexopt.ProfileFile;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;

public class DexProfileEncoderImpl implements DexProfileEncoder {

    private final BuildOptions options;
    private APKLogger apkLogger;

    public DexProfileEncoderImpl(BuildOptions options) {
        this.options = options;
    }

    @Override
    public void encodeDexProfile(File mainDirectory) throws IOException {
        File decodeDir = new File(mainDirectory, ProfileFile.DECODE_DIR_NAME);
        if (!isEnabled(decodeDir)) {
            return;
        }
        logMessage("Encoding assets/dexopt ...");
        ProfileDirectory profileDirectory = new ProfileDirectory();
        profileDirectory.encodeJsonDir(decodeDir);
        File dex = new File(mainDirectory, ".cache");
        if (!dex.isDirectory()) {
            dex = new File(mainDirectory, "dex");
        }
        profileDirectory.syncDexDirectory(dex);
        profileDirectory.updateDexDirectory(dex);
        profileDirectory.refresh();
        File dexOpt = toDexOptDir(mainDirectory);
        logMessage("Writing: " + FileUtil.shortPath(dexOpt, 3));
        profileDirectory.writeTo(dexOpt);
        profileDirectory.close();
    }
    private File toDexOptDir(File mainDirectory) {
        File dexOpt = new File(mainDirectory, ApkUtil.ROOT_NAME);
        dexOpt = new File(dexOpt, "assets");
        dexOpt = new File(dexOpt, "dexopt");
        return dexOpt;
    }
    private boolean isEnabled(File decodeDir) {
        if (!options.dexProfile) {
            return false;
        }
        return decodeDir.isDirectory();
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
