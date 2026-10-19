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
package com.reandroid.xml.base;

import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.InstanceIterator;

import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public interface NodeTree<T extends Node> extends Node {

    boolean add(T node);
    void add(int i, T node);
    boolean remove(T node);
    T remove(int i);
    boolean removeIf(Predicate<? super T> predicate);
    T get(int i);
    int size();
    Iterator<? extends T> iterator();
    boolean sort(Comparator<? super T> comparator);
    void clear();

    default <T1 extends Node> boolean containsNodeWithType(Class<T1> instance) {
        return iterator(instance).hasNext();
    }
    default <T1 extends Node> int countNodeWithType(Class<T1> instance) {
        return CollectionUtil.count(iterator(instance));
    }
    default <T1 extends Node> Iterator<T1> iterator(Class<T1> instance) {
        return iterator(instance, null);
    }
    default <T1 extends Node> Iterator<T1> iterator(Class<T1> instance, Predicate<? super T1> filter) {
        return new InstanceIterator<>(iterator(), instance, filter);
    }
}
