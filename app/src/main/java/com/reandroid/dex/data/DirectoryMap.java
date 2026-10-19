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
package com.reandroid.dex.data;

import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.container.CountedBlockList;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.common.DefIndex;
import com.reandroid.dex.key.AnnotationsKey;
import com.reandroid.dex.key.Key;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class DirectoryMap<DEFINITION extends DefIndex, VALUE extends AnnotationsList<?>>
        extends CountedBlockList<DirectoryEntry<DEFINITION, VALUE>>
        implements Iterable<DirectoryEntry<DEFINITION, VALUE>> {

    public DirectoryMap(IntegerReference itemCount, Creator<DirectoryEntry<DEFINITION, VALUE>> creator) {
        super(creator, itemCount);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean sort() {
        return super.sort(CompareUtil.getComparableComparator());
    }
    public void put(DEFINITION definition, Key value) {
        if (value == null) {
            remove(definition);
        } else {
            getOrCreateSingleEntry(definition).setValue(value);
        }
    }
    public void put(Key definitionKey, Key value) {
        if (value == null) {
            removeWithDefinitionKey(definitionKey);
        } else {
            getOrCreateSingleEntry(definitionKey).setValue(value);
        }
    }
    public void add(DEFINITION definition, Key value) {
        if (!contains(definition, value)) {
            DirectoryEntry<DEFINITION, VALUE> entry = createNext();
            entry.set(definition, value);
        }
    }
    public void add(DEFINITION definition, VALUE value) {
        if (!contains(definition, value)) {
            DirectoryEntry<DEFINITION, VALUE> entry = createNext();
            entry.set(definition, value);
        }
    }
    public boolean contains(DEFINITION definition) {
        return getEntries(definition).hasNext();
    }
    public boolean contains(DEFINITION definition, VALUE value) {
        Iterator<DirectoryEntry<DEFINITION, VALUE>> iterator = getEntries(definition);
        while (iterator.hasNext()) {
            if (iterator.next().equalsValue(value)) {
                return true;
            }
        }
        return false;
    }
    public boolean contains(DEFINITION definition, Key valueKey) {
        Iterator<DirectoryEntry<DEFINITION, VALUE>> iterator = getEntries(definition);
        while (iterator.hasNext()) {
            if (iterator.next().matchesValue(valueKey)) {
                return true;
            }
        }
        return false;
    }
    public boolean contains(Key definitionKey, Key valueKey) {
        Iterator<DirectoryEntry<DEFINITION, VALUE>> iterator = getEntries(definitionKey);
        while (iterator.hasNext()) {
            if (iterator.next().matchesValue(valueKey)) {
                return true;
            }
        }
        return false;
    }
    public void removeWithDefinitionKey(Key definitionKey) {
        super.removeIf(entry -> entry.matchesDefinition(definitionKey));
    }
    public void remove(DEFINITION definition) {
        super.removeIf(entry -> entry.equalsDefIndex(definition));
    }
    public void remove(DEFINITION definition, Predicate<VALUE> filter) {
        super.removeIf(entry -> entry.equalsDefIndex(definition) && filter.test(entry.getValue()));
    }
    public void link(DEFINITION definition) {
        for (DirectoryEntry<DEFINITION, VALUE> entry : this) {
            entry.link(definition);
        }
    }
    public Iterator<VALUE> getValues(int definitionIndex) {
        return ComputeIterator.of(getEntries(definitionIndex), DirectoryEntry::getValue);
    }
    public Iterator<DirectoryEntry<DEFINITION, VALUE>> getEntries(int definitionIndex) {
        return FilterIterator.of(iterator(), entry -> entry.equalsDefIndex(definitionIndex));
    }
    public Iterator<VALUE> getValues(DEFINITION definition) {
        return ComputeIterator.of(getEntries(definition), DirectoryEntry::getValue);
    }
    public Iterator<AnnotationsKey<?>> getValueKeys(DEFINITION definition) {
        return ComputeIterator.of(getEntries(definition), DirectoryEntry::getValueKey);
    }
    public Iterator<VALUE> getValues() {
        return ComputeIterator.of(iterator(), DirectoryEntry::getValue);
    }
    public Iterator<DirectoryEntry<DEFINITION, VALUE>> getEntries(DEFINITION definition) {
        return FilterIterator.of(iterator(), entry -> entry.equalsDefIndex(definition));
    }
    public Iterator<DirectoryEntry<DEFINITION, VALUE>> getEntries(Key definitionKey) {
        return FilterIterator.of(iterator(), entry -> entry.matchesDefinition(definitionKey));
    }
    private DirectoryEntry<DEFINITION, VALUE> getOrCreateSingleEntry(DEFINITION definition) {
        DirectoryEntry<DEFINITION, VALUE> result;
        Iterator<DirectoryEntry<DEFINITION, VALUE>> iterator = getEntries(definition);
        if (!iterator.hasNext()) {
            result = createNext();
            result.setDefinition(definition);
        } else {
            result = iterator.next();
            while (iterator.hasNext()) {
                remove(iterator.next());
            }
        }
        return result;
    }
    private DirectoryEntry<DEFINITION, VALUE> getOrCreateSingleEntry(Key definitionKey) {
        DirectoryEntry<DEFINITION, VALUE> result;
        Iterator<DirectoryEntry<DEFINITION, VALUE>> iterator = getEntries(definitionKey);
        if (!iterator.hasNext()) {
            result = createNext();
            result.setDefinitionKey(definitionKey);
        } else {
            result = iterator.next();
            while (iterator.hasNext()) {
                remove(iterator.next());
            }
        }
        return result;
    }

    @Override
    public int countBytes() {
        return getCount() * DirectoryEntry.SIZE;
    }

    public void merge(DirectoryMap<DEFINITION, VALUE> directoryMap) {
        if (directoryMap == this) {
            return;
        }
        int size = directoryMap.size();
        ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            DirectoryEntry<DEFINITION, VALUE> coming = directoryMap.get(i);
            DirectoryEntry<DEFINITION, VALUE> item = createNext();
            item.merge(coming);
        }
    }

    public void editInternal() {
        int size = size();
        for (int i = 0; i < size; i++) {
            get(i).editInternal();
        }
    }
}
