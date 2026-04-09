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

import com.reandroid.apk.APKLogger;
import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexMethod;

import java.util.Iterator;

public abstract class DexConfuseTask implements APKLogger {

    private APKLogger logger;
    private String logTag;
    private long mTotalCount;

    public DexConfuseTask(String logTag) {
        this.logTag = logTag;
    }
    public DexConfuseTask() {
        this.logTag = getClass().getSimpleName() + ": ";
    }

    public abstract int confuseLevel();
    public boolean isEnabled(int level) {
        return level >= confuseLevel();
    }
    public boolean apply(DexClass dexClass) {
        boolean result = false;
        dexClass.edit();
        Iterator<DexMethod> iterator = dexClass.declaredMethods();
        while (iterator.hasNext()) {
            if (apply(iterator.next())) {
                addCount();
                result = true;
            }
        }
        return result;
    }
    public abstract boolean apply(DexMethod dexMethod);

    public long getTotalCount() {
        return mTotalCount;
    }
    public void setTotalCount(long count) {
        this.mTotalCount = count;
    }
    public void addCount() {
        setTotalCount(getTotalCount() + 1);
    }

    public void logSummary() {
        logMessage("total = " + getTotalCount());
    }
    public void setLogTag(String logTag) {
        this.logTag = logTag;
    }
    public DexConfuseTask setLogger(APKLogger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public void logMessage(String message) {
        APKLogger logger = this.logger;
        if (logger != null) {
            logger.logMessage(logTag + message);
        }
    }
    @Override
    public void logError(String message, Throwable throwable) {
        APKLogger logger = this.logger;
        if (logger != null) {
            logger.logError(logTag + message, throwable);
        }
    }
    @Override
    public void logVerbose(String message) {
        APKLogger logger = this.logger;
        if (logger != null) {
            logger.logVerbose(message);
        }
    }
}
