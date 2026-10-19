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

import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class KeyList<T extends Key> implements Key, Iterable<T> {

    static final Key[] EMPTY_ARRAY = new Key[0];

    private final Key[] elements;
    private final Key[] sortedElements;
    private int mHash;

    public KeyList(Key[] elements, boolean sorted) {
        this.elements = elements;
        this.sortedElements = sorted ? sortElements(elements) : null;
    }
    public KeyList(Key[] elements) {
        this(elements, false);
    }

    @SuppressWarnings("unchecked")
    public T get(int i) {
        Key[] elements = this.elements;
        if (i >= 0 && i < elements.length) {
            return (T) elements[i];
        }
        return null;
    }
    public int size() {
        return elements.length;
    }
    public boolean isEmpty() {
        return size() == 0;
    }
    @Override
    public Iterator<T> iterator() {
        return ArrayIterator.of(this.elements);
    }
    public Iterator<T> iterator(Predicate<? super T> predicate) {
        return ArrayIterator.of(this.elements, predicate);
    }
    public<T1 extends Key> Iterator<T1> iterator(Class<T1> instance) {
        return InstanceIterator.of(iterator(), instance);
    }

    public int indexOf(Object key) {
        Key[] elements = this.elements;
        int length = elements.length;
        for (int i = 0; i < length; i++) {
            if (ObjectsUtil.equals(elements[i], key)) {
                return i;
            }
        }
        return -1;
    }
    public boolean contains(Object item) {
        Key[] elements = this.elements;
        int length = elements.length;
        for (int i = 0; i < length; i++) {
            if (ObjectsUtil.equals(elements[i], item)) {
                return true;
            }
        }
        return false;
    }
    public KeyList<T> setSize(int size, Key item) {
        if (size == size() || item == null) {
            return this;
        }
        Key[] elements = this.elements;
        int length = NumbersUtil.min(size, elements.length);
        Key[] result = newArray(size);
        for (int i = 0; i < length; i++) {
            result[i] = elements[i];
        }
        for (int i = length; i < size; i++) {
            result[i] = item;
        }
        return newInstance(result);
    }
    public KeyList<T> set(int i, T item) {
        if (ObjectsUtil.equals(item, get(i))) {
            return this;
        }
        Key[] elements = this.elements.clone();
        elements[i] = item;
        return newInstance(elements);
    }
    public KeyList<T> add(T item) {
        Key[] elements = this.elements;
        int length = elements.length;
        Key[] result = newArray(length + 1);
        for (int i = 0; i < length; i++) {
            result[i] = elements[i];
        }
        result[length] = item;
        return newInstance(result);
    }
    public KeyList<T> add(int index, T item) {
        if (item == null) {
            return this;
        }
        Key[] elements = this.elements;
        int length = elements.length;
        if (index > length) {
            index = length;
        }
        length = length + 1;
        Key[] result = newArray(length);
        if (index < 0) {
            index = 0;
        }
        for (int i = 0; i < index; i++) {
            result[i] = elements[i];
        }
        result[index] = item;
        index = index + 1;
        for (int i = index; i < length; i++) {
            result[i] = elements[i - 1];
        }
        return newInstance(result);
    }
    public KeyList<T> remove(T itemKey) {
        return remove(indexOf(itemKey));
    }
    public KeyList<T> remove(int index) {
        if (index < 0) {
            return this;
        }
        Key[] elements = this.elements;
        int length = elements.length;
        if (index >= length) {
            return this;
        }
        if (length == 1) {
            return newInstance(newArray(0));
        }
        Key[] result = newArray(length - 1);
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (i != index) {
                result[j] = elements[i];
                j ++;
            }
        }
        return newInstance(result);
    }
    @SuppressWarnings("unchecked")
    public KeyList<T> removeIf(Predicate<? super T> predicate) {
        Key[] elements = this.elements;
        int length = elements.length;
        if (length == 0) {
            return this;
        }
        Key[] filtered = null;
        int j = 0;
        for (int i = 0; i < length; i++) {
            T item = (T) elements[i];
            if (predicate.test(item)) {
                if (filtered == null) {
                    filtered = elements.clone();
                    j = i;
                }
            } else if (filtered != null) {
                filtered[j] = item;
                j ++;
            }
        }
        if (filtered == null) {
            return this;
        }
        Key[] result = newArray(j);
        length = result.length;
        for (int i = 0; i < length; i++) {
            result[i] = filtered[i];
        }
        return newInstance(result);
    }

    @SuppressWarnings("unchecked")
    public T getDuplicate() {
        Key[] elements = getSortedElements();
        int length = elements.length;
        if (length < 2) {
            return null;
        }
        Key last = elements[0];
        for (int i = 1; i < length; i++) {
            Key element = elements[i];
            if (ObjectsUtil.equals(last, element)) {
                return (T) element;
            }
            last = element;
        }
        return null;
    }
    public KeyList<T> clearDuplicates() {
        return clearDuplicates(CompareUtil.getComparableComparator());
    }
    public KeyList<T> clearDuplicates(Comparator<? super T> comparator) {
        Key[] elements = getSortedElements();
        if (elements.length < 2) {
            return this;
        }
        elements = sortElements(elements, comparator);
        if (elements == null) {
            elements = getSortedElements();
        }
        Comparator<Key> c = ObjectsUtil.cast(comparator);
        int length = elements.length;
        Key previous = elements[0];
        Key[] results = null;
        for (int i = 1; i < length; i++) {
            Key element = elements[i];
            if (c.compare(previous, element) == 0) {
                if (results == null) {
                    results = elements.clone();
                }
                results[i] = null;
            }
            previous = element;
        }
        if (results == null) {
            return this;
        }
        results = removeNulls(results);
        return newInstance(results);
    }
    public KeyList<T> sorted() {
        Key[] elements = this.sortedElements;
        if (elements == null) {
            return this;
        }
        return newInstance(elements);
    }
    @SuppressWarnings("unchecked")
    public KeyList<T> sort(Comparator<? super T> comparator) {
        Key[] elements = this.elements;
        int length = elements.length;
        if (length < 2) {
            return this;
        }
        boolean sortRequired = false;
        T previous = (T) elements[0];
        for (int i = 1; i < length; i++) {
            T item = (T) elements[i];
            if (comparator.compare(previous, item) > 0) {
                sortRequired = true;
                break;
            }
            previous = item;
        }
        if (!sortRequired) {
            return this;
        }
        elements = elements.clone();
        ArraySort.sort(elements, comparator);
        return newInstance(elements);
    }
    private Key[] getSortedElements() {
        Key[] elements = this.sortedElements;
        if (elements == null) {
            elements = this.elements;
        }
        return elements;
    }

    Key[] getElements() {
        return elements;
    }
    abstract KeyList<T> newInstance(Key[] elements);
    private Key[] newArray(int length) {
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        return new Key[length];
    }

    @Override
    public Object asObject() {
        int size = size();
        Object[] results = new Object[size];
        for (int i = 0; i < size; i++) {
            results[i] = get(i).asObject();
        }
        return results;
    }

    @Override
    public KeyList<T> replaceKey(Key search, Key replace) {
        if (this.equals(search)) {
            return ObjectsUtil.cast(replace);
        }
        return replaceElements(search, replace);
    }
    KeyList<T> replaceElements(Key search, Key replace) {
        KeyList<T> result = this;
        int size = result.size();
        for (int i = 0; i < size; i++) {
            T item = get(i);
            if (item == null) {
                continue;
            }
            item = ObjectsUtil.cast(item.replaceKey(search, replace));
            result = result.set(i, item);
        }
        return result;
    }
    @Override
    public Iterator<? extends Key> contents() {
        return CombiningIterator.singleOne(
                this,
                new IterableIterator<T, Key>(iterator()) {
                    @Override
                    public Iterator<Key> iterator(T element) {
                        return ObjectsUtil.cast(element.contents());
                    }
                });
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendAll(iterator());
    }


    int compareElements(KeyList<?> keyList) {
        return CompareUtil.compare(getSortedElements(), keyList.getSortedElements());
    }
    boolean equalsElements(KeyList<?> keyList) {
        return ObjectsUtil.equalsArray(this.getSortedElements(), keyList.getSortedElements());
    }
    int getHashCode() {
        int hash = this.mHash;
        if (hash == 0) {
            hash = computeHash();
            this.mHash = hash;
        }
        return hash;
    }
    int computeHash() {
        return ObjectsUtil.hashElements(this.getSortedElements());
    }

    @Override
    public int hashCode() {
        return getHashCode();
    }
    @Override
    public String toString() {
        return SmaliWriter.toStringSafe(this);
    }

    public static<T1 extends Key> boolean equalsIgnoreEmpty(KeyList<T1> key1, KeyList<T1> key2) {
        if (key1 == key2) {
            return true;
        }
        if (key1 == null || key1.isEmpty()) {
            return key2 == null || key2.isEmpty();
        }
        if (key2 == null || key2.isEmpty()) {
            return key1.isEmpty();
        }
        return key1.equalsElements(key2);
    }

    private static<T extends Key> T[] sortElements(T[] elements) {
        return sortElements(elements, CompareUtil.getComparableComparator());
    }

    private static<T extends Key, E extends Key> T[] sortElements(T[] elements, Comparator<? super E> comparator) {
        if (elements == null || elements.length < 2) {
            return null;
        }
        boolean needsSort = false;
        int length = elements.length;
        Key previous  = elements[0];
        Comparator<Key> c = ObjectsUtil.cast(comparator);
        for (int i = 1; i < length; i ++) {
            Key next = elements[i];
            if (c.compare(previous, next) > 0) {
                needsSort = true;
                break;
            }
            previous = next;
        }
        if (!needsSort) {
            return null;
        }
        elements = elements.clone();
        ArraySort.sort(elements, c);
        return elements;
    }

    static Key[] removeNulls(Key[] elements) {
        if (elements == null || elements.length == 0) {
            return EMPTY_ARRAY;
        }
        int length = elements.length;
        int size = 0;
        for (int i = 0; i < length; i ++) {
            Key key = elements[i];
            if (key != null) {
                size ++;
            }
        }
        if (size == length) {
            return elements;
        }
        if (size == 0) {
            return EMPTY_ARRAY;
        }
        Key[] results = new Key[size];
        int j = 0;
        for (int i = 0; i < length; i ++) {
            Key key = elements[i];
            if (key != null) {
                results[j] = key;
                j ++;
            }
        }
        return results;
    }
}
