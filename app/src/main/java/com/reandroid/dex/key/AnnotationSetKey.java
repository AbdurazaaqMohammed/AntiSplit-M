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
package com.reandroid.dex.key;

import com.reandroid.dex.common.AnnotationVisibility;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.ComputeIterator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class AnnotationSetKey extends AnnotationsKey<AnnotationItemKey> {

    private static final AnnotationSetKey EMPTY = new AnnotationSetKey(EMPTY_ARRAY);

    private AnnotationSetKey(Key[] elements) {
        super(elements, true);
    }

    public Iterator<TypeKey> getTypes() {
        return ComputeIterator.of(iterator(), AnnotationItemKey::getType);
    }
    public AnnotationSetKey removeElementIf(TypeKey typeKey, Predicate<? super AnnotationElementKey> predicate) {
        AnnotationSetKey result = this;
        AnnotationItemKey itemKey = result.get(typeKey);
        if (itemKey == null) {
            return result;
        }
        int i = result.indexOf(itemKey);
        itemKey = itemKey.removeIf(predicate);
        return result.set(i, itemKey);
    }
    public AnnotationSetKey removeElement(TypeKey typeKey, String name) {
        AnnotationSetKey result = this;
        AnnotationItemKey itemKey = result.get(typeKey);
        if (itemKey == null) {
            return result;
        }
        int i = result.indexOf(itemKey);
        itemKey = itemKey.remove(name);
        return result.set(i, itemKey);
    }
    public AnnotationSetKey changeType(TypeKey typeKey, TypeKey newType) {
        AnnotationSetKey result = this;
        AnnotationItemKey itemKey = result.get(typeKey);
        if (itemKey == null) {
            return result;
        }
        int i = result.indexOf(itemKey);
        itemKey = itemKey.changeType(newType);
        return result.set(i, itemKey).sorted();
    }
    public AnnotationSetKey renameElement(TypeKey typeKey, String name, String newName) {
        AnnotationSetKey result = this;
        AnnotationItemKey itemKey = result.get(typeKey);
        if (itemKey == null) {
            return result;
        }
        int i = result.indexOf(itemKey);
        itemKey = itemKey.rename(name, newName);
        return result.set(i, itemKey);
    }
    public AnnotationSetKey setAnnotation(TypeKey typeKey, String name, Key value) {
        AnnotationSetKey result = this;
        result = result.getOrCreate(typeKey);
        AnnotationItemKey itemKey = result.get(typeKey);
        int i = result.indexOf(itemKey);
        itemKey = itemKey.setValue(name, value);
        return result.set(i, itemKey);
    }
    public AnnotationSetKey getOrCreate(TypeKey typeKey, String name) {
        AnnotationSetKey result = this;
        result = result.getOrCreate(typeKey);
        AnnotationItemKey itemKey = result.get(typeKey);
        int i = result.indexOf(itemKey);
        itemKey = itemKey.getOrCreate(name);
        return result.set(i, itemKey);
    }
    public AnnotationSetKey setVisibility(TypeKey typeKey, AnnotationVisibility visibility) {
        AnnotationItemKey itemKey = get(typeKey);
        if (itemKey == null) {
            return this;
        }
        int i = indexOf(itemKey);
        itemKey = itemKey.changeVisibility(visibility);
        return set(i, itemKey);
    }
    public AnnotationSetKey getOrCreate(TypeKey typeKey) {
        AnnotationItemKey itemKey = get(typeKey);
        if (itemKey != null) {
            return this;
        }
        itemKey = AnnotationItemKey.create(AnnotationVisibility.BUILD, typeKey);
        return this.add(itemKey).sorted();
    }
    public Key getAnnotationValue(TypeKey typeKey, String name) {
        AnnotationItemKey itemKey = get(typeKey);
        if (itemKey != null) {
            return itemKey.get(name);
        }
        return null;
    }
    public boolean containsElement(TypeKey typeKey, String name) {
        AnnotationItemKey itemKey = get(typeKey);
        if (itemKey != null) {
            return itemKey.containsElement(name);
        }
        return false;
    }
    public boolean contains(TypeKey typeKey) {
        int length = size();
        for (int i = 0; i < length; i++) {
            AnnotationItemKey itemKey = get(i);
            if (itemKey != null && ObjectsUtil.equals(typeKey, itemKey.getType())) {
                return true;
            }
        }
        return false;
    }
    public AnnotationItemKey get(TypeKey typeKey) {
        int length = size();
        for (int i = 0; i < length; i++) {
            AnnotationItemKey itemKey = get(i);
            if (itemKey != null && ObjectsUtil.equals(typeKey, itemKey.getType())) {
                return itemKey;
            }
        }
        return null;
    }

    @Override
    public AnnotationSetKey merge(AnnotationsKey<? extends AnnotationItemKey> item) {
        if (item == null || item == this || item.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return (AnnotationSetKey) item;
        }
        AnnotationSetKey result = this;
        int size = item.size();
        for (int i = 0; i < size; i++) {
            result = result.add(item.get(i));
        }
        return result;
    }
    @Override
    public AnnotationSetKey merge(int i, AnnotationItemKey item) {
        return add(item);
    }
    public AnnotationSetKey addAll(Iterator<? extends AnnotationItemKey> iterator) {
        AnnotationSetKey result = this;
        while (iterator.hasNext()) {
            result = result.add(iterator.next());
        }
        return result;
    }
    @Override
    public AnnotationSetKey add(AnnotationItemKey item) {
        if (item == null) {
            return this;
        }
        return this.remove(item.getType())
                .addUnchecked(item)
                .sorted();
    }
    private AnnotationSetKey addUnchecked(AnnotationItemKey item) {
        return (AnnotationSetKey) super.add(item);
    }
    @Override
    public AnnotationSetKey remove(AnnotationItemKey itemKey) {
        return (AnnotationSetKey) super.remove(itemKey);
    }
    @Override
    public AnnotationSetKey remove(int index) {
        return (AnnotationSetKey) super.remove(index);
    }
    @Override
    public AnnotationSetKey removeIf(Predicate<? super AnnotationItemKey> predicate) {
        return (AnnotationSetKey) super.removeIf(predicate);
    }
    public AnnotationSetKey remove(TypeKey typeKey) {
        return removeIf(item -> item.equalsType(typeKey));
    }
    @Override
    public AnnotationSetKey set(int i, AnnotationItemKey item) {
        return (AnnotationSetKey) super.set(i, item);
    }
    @Override
    public AnnotationSetKey sorted() {
        return (AnnotationSetKey) super.sorted();
    }
    @Override
    public AnnotationSetKey clearDuplicates() {
        return (AnnotationSetKey) super.clearDuplicates();
    }
    @Override
    public AnnotationSetKey clearDuplicates(Comparator<? super AnnotationItemKey> comparator) {
        return (AnnotationSetKey) super.clearDuplicates(comparator);
    }

    @Override
    AnnotationSetKey newInstance(Key[] elements) {
        return createKey(elements);
    }

    @Override
    public AnnotationSetKey replaceKey(Key search, Key replace) {
        return (AnnotationSetKey) super.replaceKey(search, replace);
    }

    public void appendClass(SmaliWriter writer) throws IOException {
        int size = size();
        if (size == 0) {
            return;
        }
        writer.newLine();
        writer.newLine();
        writer.appendCommentNewLine("annotations");
        for (int i = 0; i < size; i ++) {
            if (i != 0) {
                writer.newLine();
            }
            writer.newLine();
            get(i).append(writer);
        }
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        int size = size();
        for (int i = 0; i < size; i ++) {
            writer.newLine();
            get(i).append(writer);
        }
    }

    public static AnnotationSetKey empty() {
        return EMPTY;
    }
    public static AnnotationSetKey of(AnnotationItemKey ... elements) {
        return createKey(elements);
    }
    private static AnnotationSetKey createKey(Key[]  elements) {
        if (elements == null || elements.length == 0) {
            return empty();
        }
        elements = removeNulls(elements);
        if (elements.length == 0) {
            return empty();
        }
        return new AnnotationSetKey(elements);
    }
    public static AnnotationSetKey create(Iterator<AnnotationItemKey> iterator) {
        ArrayCollection<AnnotationItemKey> elements = null;
        while (iterator.hasNext()) {
            AnnotationItemKey key = iterator.next();
            if (elements == null) {
                elements = new ArrayCollection<>();
            }
            elements.add(key);
        }
        if (elements == null) {
            return empty();
        }
        return createKey(elements.toArrayFill(new Key[elements.size()]));
    }
    public static AnnotationSetKey combine(Iterator<AnnotationSetKey> iterator) {
        AnnotationSetKey result = empty();
        while (iterator.hasNext()) {
            result = result.merge(iterator.next());
        }
        return result;
    }
}
