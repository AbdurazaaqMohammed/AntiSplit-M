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
package com.reandroid.graph.cleaners;

import com.reandroid.apk.ApkModule;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.Dex;
import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.model.DexDeclaration;
import com.reandroid.graph.ApkBuildOption;

import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public abstract class UnusedClassComponentCleaner<T extends Dex> extends UnusedCleaner<T> {

    public UnusedClassComponentCleaner(ApkBuildOption buildOption, ApkModule apkModule,
                                       DexClassRepository classRepository) {
        super(buildOption, apkModule, classRepository);
    }

    @Override
    public void apply() {
        if(!isEnabled()) {
            debug("Skip");
            return;
        }
        verbose("Searching for unused ...");
        cleanUnusedInCleanableClasses();
        verbose("Cleaned: " + getCount());
    }

    protected abstract List<T> listUnusedInClass(DexClass dexClass);
    protected void cleanUnusedInCleanableClasses() {
        Iterator<DexClass> iterator = getCleanableClasses();
        while (iterator.hasNext()) {
            cleanUnusedInClass(iterator.next());
        }
    }
    protected void cleanUnusedInClass(DexClass dexClass) {
        List<T> unusedList = listUnusedInClass(dexClass);
        if(unusedList != null) {
            boolean debugEnabled = isDebugEnabled();
            for(T item : unusedList) {
                if(debugEnabled) {
                    debug(getDebugString(item));
                }
                item.removeSelf();
                addCount();
            }
        }
    }

    protected Iterator<DexClass> getCleanableClasses() {
        return getDexClasses(this::isCleanableClass);
    }
    protected boolean isCleanableClass(DexClass dexClass) {
        if(dexClass.usesNative() || dexClass.isEnum()) {
            return false;
        }
        Predicate<? super TypeKey> filter = getBuildOption().getKeepClasses();
        if(filter != null && filter.test(dexClass.getKey())) {
            return false;
        }
        // TODO: add user rules here
        return true;
    }
    protected String getDebugString(T item) {
        if(item instanceof DexDeclaration) {
            return ((DexDeclaration) item).getKey().toString();
        }
        return item.toSmaliString();
    }
}
