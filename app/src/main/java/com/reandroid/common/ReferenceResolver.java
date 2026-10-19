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
package com.reandroid.common;

import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.collection.ArrayCollection;

import java.util.*;
import com.reandroid.utils.function.Predicate;

public class ReferenceResolver{
    private final TableBlock entryStore;
    private final List<Entry> results;
    private final Set<Integer> resolvedIds;
    private int limit;
    public ReferenceResolver(TableBlock entryStore){
        this.entryStore = entryStore;
        this.results = new ArrayCollection<>();
        this.resolvedIds = new HashSet<>();
        this.limit = -1;
    }
    public Entry resolve(int referenceId){
        return resolve(referenceId, null);
    }
    public synchronized Entry resolve(int referenceId, Predicate<Entry> filter){
        resolveReference(referenceId, filter);
        List<Entry> results = new ArrayCollection<>(this.results);
        reset();
        if(results.size() > 0){
            return results.get(0);
        }
        return null;
    }

    public List<Entry> resolveWithConfig(int referenceId, ResConfig resConfig){
        ConfigFilter configFilter = new ConfigFilter(resConfig);
        List<Entry> results = resolveAll(referenceId, configFilter);
        java.util.Collections.sort(results, configFilter);
        return results;
    }
    public List<Entry> resolveAll(int referenceId){
        return resolveAll(referenceId, (Predicate<Entry>)null);
    }
    public synchronized List<Entry> resolveAll(int referenceId, Predicate<Entry> filter){
        resolveReference(referenceId, filter);
        List<Entry> results = new ArrayCollection<>(this.results);
        reset();
        return results;
    }
    private void resolveReference(int referenceId, Predicate<Entry> filter){
        if(referenceId == 0 || isFinished() || this.resolvedIds.contains(referenceId)){
            return;
        }
        this.resolvedIds.add(referenceId);
        List<Entry> entryList = listNonNullEntries(referenceId);
        List<Entry> results = this.results;
        for(Entry entry:entryList){
            if(isFinished()){
                return;
            }
            if(results.contains(entry)){
                continue;
            }
            if(entry.isComplex()){
                addResult(filter, entry);
                continue;
            }
            ResValue resValue = entry.getResValue();
            if(resValue.getValueType() != ValueType.REFERENCE){
                addResult(filter, entry);
                continue;
            }
            resolveReference(resValue.getData(), filter);
        }
    }
    private void reset(){
        this.results.clear();
        this.resolvedIds.clear();
        this.limit = -1;
    }
    private boolean isFinished(){
        return this.limit >= this.results.size();
    }
    private void addResult(Predicate<Entry> filter, Entry entry){
        if(filter == null || filter.test(entry)){
            this.results.add(entry);
        }
    }
    private List<Entry> listNonNullEntries(int resourceId){
        List<Entry> results = new ArrayCollection<>();
        ResourceEntry resourceEntry = this.entryStore.getResource(resourceId);
        if(resourceEntry == null){
            return results;
        }
        Iterator<Entry> itr = resourceEntry.iterator(true);
        while (itr.hasNext()){
            results.add(itr.next());
        }
        return results;
    }

    public static class ConfigFilter implements Predicate<Entry>, Comparator<Entry>{
        private final ResConfig config;
        public ConfigFilter(ResConfig config){
            this.config = config;
        }
        @Override
        public boolean test(Entry entry) {
            ResConfig resConfig = entry.getResConfig();
            if(resConfig == null){
                return false;
            }
            return resConfig.isEqualOrMoreSpecificThan(this.config);
        }
        @Override
        public int compare(Entry entry1, Entry entry2) {
            ResConfig config = this.config;
            ResConfig config1 = entry1.getResConfig();
            ResConfig config2 = entry2.getResConfig();
            if(config.equals(config1)){
                if(config.equals(config2)) {
                    return 0;
                }
                return -1;
            }
            if(config.equals(config2) || config1.isEqualOrMoreSpecificThan(config2)){
                return 1;
            }
            if(config2.isEqualOrMoreSpecificThan(config1)) {
                return -1;
            }
            return 0;
        }
    }
}
