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

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.DexFileInputSource;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexFile;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.FilterIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class DexConfuser extends Confuser {

    private final ArrayCollection<DexConfuseTask> mTaskList;

    public DexConfuser(Protector protector, DexConfuseTask ... tasks) {
        super(protector, "DexConfuser: ");
        this.mTaskList = new ArrayCollection<>(tasks);
    }
    public DexConfuser(Protector protector) {
        this(protector,
                new DexArrayPayloadConfuser().setLogger(protector),
                new DexStringFogger().setLogger(protector)
        );
    }

    @Override
    public void confuse() {
        if (!isEnabled()) {
            logMessage("Skip");
            return;
        }
        ApkModule apkModule = getApkModule();
        List<DexFileInputSource> dexFiles = getApkModule().listDexFiles();
        if (dexFiles.isEmpty()) {
            logMessage("Dex files not found");
            return;
        }
        logMessage("Confusing " + dexFiles.size() + " dex files ...");
        for (DexFileInputSource inputSource : dexFiles) {
            InputSource modified = confuse(inputSource);
            if (modified != null) {
                apkModule.add(modified);
            }
        }
        List<DexConfuseTask> taskList = getTasks();
        for (DexConfuseTask task : taskList) {
            task.logSummary();
        }
    }
    private InputSource confuse(InputSource inputSource) {
        logMessage(inputSource.getAlias());
        DexFile dexFile = load(inputSource);
        InputSource result = null;
        if (confuse(dexFile)) {
            result = new ByteInputSource(save(dexFile), inputSource.getAlias());
            result.copyAttributes(inputSource);
        }
        dexFile.close();
        return result;
    }
    private byte[] save(DexFile dexFile) {
        dexFile.refresh();
        dexFile.shrink();
        dexFile.refreshFull();
        return dexFile.getBytes();
    }
    private boolean confuse(DexFile dexFile) {
        List<DexConfuseTask> taskList = getTasks();
        boolean result = false;
        Iterator<DexClass> iterator = dexFile.getDexClasses();
        while (iterator.hasNext()) {
            DexClass dexClass = iterator.next();
            for (DexConfuseTask task : taskList) {
                if (task.apply(dexClass)) {
                    result = true;
                }
            }
        }
        return result;
    }
    private DexFile load(InputSource inputSource) {
        try {
            DexFile dexFile = DexFile.read(inputSource.openStream());
            dexFile.setSimpleName(inputSource.getSimpleName());
            return dexFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private int getLevel() {
        return getProtector().getOptions().dexLevel;
    }

    public Iterator<DexConfuseTask> getAllTasks() {
        return mTaskList.iterator();
    }
    public boolean isEnabled() {
        if (getLevel() <= 0) {
            return false;
        }
        return !getTasks().isEmpty();
    }
    public List<DexConfuseTask> getTasks() {
        int level = getLevel();
        return CollectionUtil.toList(FilterIterator.of(
                getAllTasks(), task -> task.isEnabled(level)));
    }
}
