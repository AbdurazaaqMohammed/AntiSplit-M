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
package com.reandroid.dex.program;

import com.reandroid.dex.key.AnnotationItemKey;
import com.reandroid.dex.key.AnnotationSetKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;

import com.reandroid.utils.function.Predicate;

public interface AnnotatedProgram {

    AnnotationSetKey getAnnotation();
    void setAnnotation(AnnotationSetKey annotationSet);
    void clearAnnotations();

    default void addAnnotation(AnnotationItemKey annotation) {
        setAnnotation(getAnnotation().add(annotation));
    }
    default boolean removeAnnotationIf(Predicate<? super AnnotationItemKey> predicate) {
        AnnotationSetKey key = getAnnotation();
        AnnotationSetKey update = key.removeIf(predicate);
        if (key.equals(update)) {
            return false;
        }
        setAnnotation(update);
        return true;
    }

    default boolean hasAnnotations() {
        return !getAnnotation().isEmpty();
    }
    default boolean hasAnnotation(TypeKey typeKey) {
        return getAnnotation(typeKey) != null;
    }
    default AnnotationItemKey getAnnotation(TypeKey typeKey) {
        return getAnnotation().get(typeKey);
    }
    default Key getAnnotationValue(TypeKey typeKey, String name) {
        AnnotationItemKey annotation = getAnnotation(typeKey);
        if (annotation != null) {
            return annotation.getValue(name);
        }
        return null;
    }

    default boolean removeAnnotation(TypeKey typeKey) {
        AnnotationSetKey annotation = getAnnotation();
        AnnotationSetKey update = annotation.remove(typeKey);
        if (annotation.equals(update)) {
            return false;
        }
        setAnnotation(update);
        return true;
    }
}
