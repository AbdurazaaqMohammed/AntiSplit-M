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
package com.reandroid.graph;

import com.reandroid.arsc.refactor.ResourceMergeOption;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class ApkBuildOption {

    private boolean minifyResources = true;
    private boolean minifyClasses = true;
    private boolean minifyFields = true;
    private boolean minifyMethods = false;

    private boolean cleanAnnotations = true;

    private boolean processClassNamesOnStrings = true;

    private ResourceMergeOption mMergeOption;
    private Predicate<? super TypeKey> keepClassesFilter;
    private final Set<TypeKey> keepClassesList = new HashSet<>();

    public ApkBuildOption() {

    }

    public boolean isMinifyResources() {
        return minifyResources;
    }
    public void setMinifyResources(boolean minifyResources) {
        this.minifyResources = minifyResources;
    }
    public boolean isMinifyClasses() {
        return minifyClasses;
    }
    public void setMinifyClasses(boolean minifyClasses) {
        this.minifyClasses = minifyClasses;
    }

    public boolean isMinifyFields() {
        return minifyFields;
    }
    public void setMinifyFields(boolean minifyFields) {
        this.minifyFields = minifyFields;
    }
    public boolean isMinifyMethods() {
        return minifyMethods;
    }
    public void setMinifyMethods(boolean minifyMethods) {
        this.minifyMethods = minifyMethods;
    }

    public boolean isCleanAnnotations() {
        return cleanAnnotations;
    }
    public void setCleanAnnotations(boolean cleanAnnotations) {
        this.cleanAnnotations = cleanAnnotations;
    }

    public boolean isProcessClassNamesOnStrings() {
        return processClassNamesOnStrings;
    }
    public void setProcessClassNamesOnStrings(boolean processClassNamesOnStrings) {
        this.processClassNamesOnStrings = processClassNamesOnStrings;
    }


    public ResourceMergeOption getResourceMergeOption() {
        ResourceMergeOption mergeOption = this.mMergeOption;
        if(mergeOption == null){
            mergeOption = new ResourceMergeOption();
            this.mMergeOption = mergeOption;
        }
        return mergeOption;
    }
    public void setResourceMergeOption(ResourceMergeOption mergeOption) {
        this.mMergeOption = mergeOption;
    }

    public Predicate<? super TypeKey> getKeepClasses() {
        return CollectionUtil.orFilter(getKeepClassesFilter(),
                getKeepClassesListFilter());
    }
    public Predicate<? super TypeKey> getKeepClassesFilter() {
        return keepClassesFilter;
    }
    public void setKeepClassesFilter(Predicate<? super TypeKey> filter) {
        this.keepClassesFilter = filter;
    }

    public Predicate<? super TypeKey> getKeepClassesListFilter() {
        Set<TypeKey> keepClassesList = this.keepClassesList;
        if(!keepClassesList.isEmpty()) {
            return (Predicate<TypeKey>) keepClassesList::contains;
        }
        return null;
    }
    public void clearKeepClasses() {
        this.keepClassesList.clear();
    }
    public void addKeepClasses(TypeKey typeKey) {
        this.keepClassesList.add(typeKey.getDeclaring());
    }
    public void readKeepClassesList(File keepClassesListFile) throws IOException {
        SmaliReader reader = SmaliReader.of(keepClassesListFile);
        Set<TypeKey> keepClassesList = this.keepClassesList;
        while (!reader.finished()) {
            keepClassesList.add(TypeKey.read(reader).getDeclaring());
            reader.skipWhitespacesOrComment();
        }
    }
    public void readKeepResourceNameList(File keepResourceNameListFile) throws IOException {
        getResourceMergeOption().readKeepResourceNameList(keepResourceNameListFile);
    }
}
