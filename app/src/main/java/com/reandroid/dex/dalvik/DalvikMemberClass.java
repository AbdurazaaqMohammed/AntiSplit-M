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
package com.reandroid.dex.dalvik;

import com.reandroid.dex.common.AnnotationVisibility;
import com.reandroid.dex.key.*;
import com.reandroid.dex.program.ClassProgram;
import com.reandroid.dex.program.AnnotatedProgram;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CollectionUtil;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class DalvikMemberClass extends DalvikAnnotation {

    private DalvikMemberClass(AnnotatedProgram annotatedProgram) {
        super(annotatedProgram, TypeKey.DALVIK_MemberClass);
    }

    public Iterator<TypeKey> getMembers() {
        return getArray().iterator(TypeKey.class);
    }
    public int size() {
        return getArray().size();
    }
    public void add(TypeKey typeKey) {
        ArrayValueKey array = getArray()
                .remove(typeKey)
                .add(typeKey)
                .sort(CompareUtil.getComparableComparator());
        setArray(array);
    }
    public void remove(TypeKey typeKey) {
        ArrayValueKey array = getArray()
                .remove(typeKey)
                .add(typeKey);
        setArray(array);
    }
    public void removeIf(Predicate<? super TypeKey> predicate) {
        ArrayValueKey array = getArray()
                .removeIf(ObjectsUtil.cast(predicate));
        setArray(array);
    }
    public boolean contains(TypeKey typeKey) {
        return getArray().contains(typeKey);
    }
    public boolean isEmpty() {
        return getArray().isEmpty();
    }
    public TypeKey findEnclosing() {
        TypeKey enclosing;
        AnnotatedProgram annotatedProgram = getAnnotatedProgram();
        if (annotatedProgram instanceof ClassProgram) {
            enclosing = ((ClassProgram) getAnnotatedProgram()).getKey();
        } else {
            enclosing = CollectionUtil.getFirst(getMembers());
            if (enclosing != null) {
                enclosing = enclosing.getEnclosingClass();
            }
        }
        return enclosing;
    }
    public void addSimpleName(String simpleName) {
        addSimpleName(findEnclosing(), simpleName);
    }
    public void addSimpleName(TypeKey enclosing, String simpleName) {
        if (enclosing == null) {
            throw new NullPointerException("Null enclosing type");
        }
        enclosing = enclosing.createInnerClass(simpleName);
        add(enclosing);
    }
    public ArrayValueKey getArray() {
        ArrayValueKey arrayValueKey = (ArrayValueKey) readValue(Key.DALVIK_value);
        if (arrayValueKey == null) {
            arrayValueKey = ArrayValueKey.empty();
        }
        return arrayValueKey;
    }
    public void setArray(ArrayValueKey array) {
        if (array == null) {
            array = ArrayValueKey.empty();
        }
        writeValue(Key.DALVIK_value, array);
    }

    @Override
    public String toString() {
        return StringsUtil.join(getMembers(), "\n");
    }

    public static DalvikMemberClass of(AnnotatedProgram annotatedProgram) {
        if (annotatedProgram.hasAnnotation(TypeKey.DALVIK_MemberClass)) {
            return new DalvikMemberClass(annotatedProgram);
        }
        return null;
    }
    public static DalvikMemberClass getOrCreate(AnnotatedProgram annotatedProgram) {
        if (!annotatedProgram.hasAnnotation(TypeKey.DALVIK_MemberClass)) {
            annotatedProgram.addAnnotation(AnnotationItemKey.create(
                    AnnotationVisibility.SYSTEM,
                    TypeKey.DALVIK_MemberClass,
                    AnnotationElementKey.create(Key.DALVIK_value, ArrayValueKey.empty())
                    )
            );
        }
        return of(annotatedProgram);
    }
}
