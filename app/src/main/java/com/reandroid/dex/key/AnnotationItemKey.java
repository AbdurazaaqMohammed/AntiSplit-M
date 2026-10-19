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
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsStore;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import com.reandroid.utils.function.Predicate;

public class AnnotationItemKey extends KeyList<AnnotationElementKey> implements Key, Iterable<AnnotationElementKey> {

    private final AnnotationVisibility visibility;
    private final TypeKey type;

    private AnnotationItemKey(AnnotationVisibility visibility, TypeKey type, Key[] elements) {
        super(elements, true);
        this.visibility = visibility;
        this.type = type;
    }

    public boolean hasVisibility() {
        return getVisibility() != null;
    }
    public AnnotationVisibility getVisibility() {
        return visibility;
    }
    public TypeKey getType() {
        return type;
    }
    public AnnotationItemKey changeType(TypeKey typeKey) {
        if (typeKey.equals(getType())) {
            return this;
        }
        return createKey(getVisibility(), typeKey, getElements());
    }
    public AnnotationItemKey remove(String name) {
        return removeIf(elementKey -> ObjectsUtil.equals(elementKey.getName(), name));
    }
    public AnnotationItemKey rename(String name, String newName) {
        AnnotationItemKey result = this;
        AnnotationElementKey elementKey = result.get(name);
        if (elementKey == null) {
            return result;
        }
        int i = result.indexOf(elementKey);
        elementKey = elementKey.changeName(newName);
        return result.set(i, elementKey).sorted();
    }
    public AnnotationItemKey setValue(String name, Key value) {
        AnnotationItemKey result = this;
        result = result.getOrCreate(name);
        AnnotationElementKey elementKey = result.get(name);
        int i = result.indexOf(elementKey);
        elementKey = elementKey.changeValue(value);
        return result.set(i, elementKey);
    }
    public AnnotationItemKey getOrCreate(String name) {
        AnnotationElementKey elementKey = get(name);
        if (elementKey != null) {
            return this;
        }
        return add(AnnotationElementKey.create(name, null)).sorted();
    }
    public AnnotationItemKey changeVisibility(AnnotationVisibility visibility) {
        if (ObjectsUtil.equals(getVisibility(), visibility)) {
            return this;
        }
        return createKey(visibility, getType(), getElements());
    }
    public AnnotationElementKey get(String name) {
        int size = size();
        for (int i = 0; i < size; i++) {
            AnnotationElementKey elementKey = get(i);
            if (elementKey != null && ObjectsUtil.equals(name, elementKey.getName())) {
                return elementKey;
            }
        }
        return null;
    }
    public Key getValue(String name) {
        AnnotationElementKey element = get(name);
        if (element != null) {
            return element.getValue();
        }
        return null;
    }
    public boolean containsElement(String name) {
        int size = size();
        for (int i = 0; i < size; i++) {
            AnnotationElementKey elementKey = get(i);
            if (elementKey != null && ObjectsUtil.equals(name, elementKey.getName())) {
                return true;
            }
        }
        return false;
    }

    public AnnotationItemKey add(String name, Key value) {
        return add(AnnotationElementKey.create(name, value));
    }
    @Override
    public AnnotationItemKey add(AnnotationElementKey item) {
        if (item == null) {
            return this;
        }
        return this.remove(item.getName())
                .addUnchecked(item)
                .sorted();
    }
    private AnnotationItemKey addUnchecked(AnnotationElementKey item) {
        return (AnnotationItemKey) super.add(item);
    }
    @Override
    public AnnotationItemKey remove(AnnotationElementKey itemKey) {
        return (AnnotationItemKey) super.remove(itemKey);
    }
    @Override
    public AnnotationItemKey remove(int index) {
        return (AnnotationItemKey) super.remove(index);
    }
    @Override
    public AnnotationItemKey removeIf(Predicate<? super AnnotationElementKey> predicate) {
        return (AnnotationItemKey) super.removeIf(predicate);
    }
    @Override
    public AnnotationItemKey set(int i, AnnotationElementKey item) {
        return (AnnotationItemKey) super.set(i, item);
    }
    @Override
    public AnnotationItemKey sorted() {
        return (AnnotationItemKey) super.sorted();
    }
    @Override
    public AnnotationItemKey clearDuplicates() {
        return (AnnotationItemKey) super.clearDuplicates();
    }
    @Override
    public AnnotationItemKey clearDuplicates(Comparator<? super AnnotationElementKey> comparator) {
        return (AnnotationItemKey) super.clearDuplicates(comparator);
    }

    @Override
    AnnotationItemKey newInstance(Key[] elements) {
        return createKey(getVisibility(), getType(), elements);
    }

    public SmaliDirective getSmaliDirective() {
        if (hasVisibility()) {
            return SmaliDirective.ANNOTATION;
        }
        return SmaliDirective.SUB_ANNOTATION;
    }

    public Iterator<MethodKey> getMethods() {
        TypeKey typeKey = getType();
        return ComputeIterator.of(iterator(), elementKey -> elementKey.toMethod(typeKey));
    }

    @Override
    public Map<String, Object> asObject() {
        int size = size();
        Map<String, Object> results = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            AnnotationElementKey element = get(i);
            results.put(element.getName(), element.asObject());
        }
        return results;
    }

    @Override
    public AnnotationItemKey replaceKey(Key search, Key replace) {
        if (this.equals(search)) {
            return (AnnotationItemKey) replace;
        }
        AnnotationItemKey result = this;
        if (search instanceof MethodKey) {
            result = result.replaceMethodKeys((MethodKey) search, (MethodKey) replace);
        } else if (search.equals(getType())) {
            result = result.changeType((TypeKey) replace);
        }
        return (AnnotationItemKey) result.replaceElements(search, replace);
    }
    private AnnotationItemKey replaceMethodKeys(MethodKey search, MethodKey replace) {
        AnnotationItemKey result = this;
        TypeKey declaring = result.getType();
        if (!declaring.equals(search.getDeclaring())) {
            return result;
        }
        TypeKey replaceType = replace.getDeclaring();
        if (!declaring.equals(replaceType)) {
            result = result.changeType(replaceType);
        }
        String name = search.getName();
        String replaceName = replace.getName();
        if (!name.equals(replaceName) && !containsElement(replaceName)) {
            AnnotationElementKey element = result.get(name);
            if (element != null) {
                result = result.set(
                        indexOf(element), element.changeName(replaceName));
            }
        }
        return result;
    }

    @Override
    public Iterator<? extends Key> contents() {
        return CombiningIterator.singleTwo(
                getType(),
                super.contents(),
                getMethods());
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        getSmaliDirective().append(writer);
        writer.appendOptional(getVisibility());
        getType().append(writer);
        writer.indentPlus();
        writer.appendAllWithDoubleNewLine(iterator());
        writer.indentMinus();
        getSmaliDirective().appendEnd(writer);
    }

    @Override
    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (!(obj instanceof AnnotationItemKey)) {
            return -1;
        }
        AnnotationItemKey itemKey = (AnnotationItemKey) obj;
        return CompareUtil.compare(getType(), itemKey.getType());
    }

    @Override
    int computeHash() {
        return ObjectsUtil.hash(getVisibility(), getType()) * 31 + super.computeHash();
    }

    public boolean equalsType(TypeKey typeKey) {
        return ObjectsUtil.equals(getType(), typeKey);
    }

    public boolean equalsMethod(MethodKey methodKey) {
        return methodKey != null &&
                ObjectsUtil.equals(getType(), methodKey.getDeclaring()) &&
                containsElement(methodKey.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AnnotationItemKey)) {
            return false;
        }
        AnnotationItemKey other = (AnnotationItemKey) obj;
        return this.hashCode() == other.hashCode() &&
                ObjectsUtil.equals(this.getVisibility(), other.getVisibility()) &&
                ObjectsUtil.equals(this.getType(), other.getType()) &&
                equalsElements(other);
    }

    @Override
    public int hashCode() {
        return getHashCode();
    }

    public static AnnotationItemKey create(AnnotationVisibility visibility, TypeKey typeKey, AnnotationElementKey ... elements) {
        return createKey(visibility, typeKey, elements);
    }
    private static AnnotationItemKey createKey(AnnotationVisibility visibility, TypeKey typeKey, Key[] elements) {
        if (typeKey == null) {
            return null;
        }
        return new AnnotationItemKey(visibility, typeKey, removeNulls(elements));
    }

    public static AnnotationItemKey read(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliDirective directive = SmaliDirective.parse(reader);
        if (directive != SmaliDirective.ANNOTATION && directive != SmaliDirective.SUB_ANNOTATION) {
            throw new SmaliParseException("Expecting annotation directive", reader);
        }
        AnnotationVisibility visibility = null;
        if (directive == SmaliDirective.ANNOTATION) {
            visibility = AnnotationVisibility.parse(reader);
            if (visibility == null) {
                throw new SmaliParseException("Unrecognized annotation visibility", reader);
            }
        }
        reader.skipWhitespacesOrComment();
        TypeKey typeKey = TypeKey.read(reader);
        reader.skipWhitespacesOrComment();
        Object elementList = null;
        while (!directive.isEnd(reader)) {
            int position = reader.position();
            AnnotationElementKey element = AnnotationElementKey.read(reader);
            if (containsElementName(elementList, element.getName())) {
                // TODO: move this validator to SmaliReader
                reader.position(position);
                throw new SmaliParseException("Duplicate element name: " + element.getName(),
                        reader);
            }
            elementList = ObjectsStore.add(elementList, element);
            reader.skipWhitespacesOrComment();
        }
        SmaliParseException.expect(reader, directive, true);
        Key[] elements;
        if (elementList != null) {
            elements = new Key[ObjectsStore.size(elementList)];
            ObjectsStore.collect(elementList, elements);
        } else {
            elements = null;
        }
        return createKey(visibility, typeKey, elements);
    }
    private static boolean containsElementName(Object elementList, String name) {
        int size = ObjectsStore.size(elementList);
        for (int i = 0; i < size; i++) {
            AnnotationElementKey elementKey = ObjectsStore.get(elementList, i);
            if (name.equals(elementKey.getName())) {
                return true;
            }
        }
        return false;
    }
    public static AnnotationItemKey parse(String annotationString) {
        if (annotationString != null) {
            try {
                return read(SmaliReader.of(annotationString));
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
