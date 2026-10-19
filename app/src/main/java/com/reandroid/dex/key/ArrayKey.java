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

import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import com.reandroid.utils.function.Predicate;

public class ArrayKey<T extends Key> extends KeyList<T> {

    private static final ArrayKey<?> EMPTY = new ArrayKey<>(EMPTY_ARRAY);

    protected ArrayKey(Key[] elements) {
        super(elements);
    }

    public int getInt(int i) {
        return ArrayKeyHelper.getInt(this, i);
    }
    public String getString(int i) {
        return ArrayKeyHelper.getString(this, i);
    }
    @Override
    public ArrayKey<T> add(T item) {
        return (ArrayKey<T>) super.add(item);
    }
    @Override
    public ArrayKey<T> remove(T itemKey) {
        return (ArrayKey<T>) super.remove(itemKey);
    }
    @Override
    public ArrayKey<T> remove(int index) {
        return (ArrayKey<T>) super.remove(index);
    }
    @Override
    public ArrayKey<T> removeIf(Predicate<? super T> predicate) {
        return (ArrayKey<T>) super.removeIf(predicate);
    }
    @Override
    public ArrayKey<T> set(int i, T item) {
        return (ArrayKey<T>) super.set(i, item);
    }
    @Override
    public ArrayKey<T> setSize(int i, Key item) {
        return (ArrayKey<T>) super.setSize(i, item);
    }
    @Override
    public ArrayKey<T> sort(Comparator<? super T> comparator) {
        return (ArrayKey<T>) super.sort(comparator);
    }
    @Override
    public ArrayKey<T> clearDuplicates() {
        return (ArrayKey<T>) super.clearDuplicates();
    }
    @Override
    public ArrayKey<T> clearDuplicates(Comparator<? super T> comparator) {
        return (ArrayKey<T>) super.clearDuplicates(comparator);
    }

    @Override
    protected ArrayKey<T> newInstance(Key[] elements) {
        if (getClass() != ArrayKey.class) {
            throw new RuntimeException("Method not implemented");
        }
        return create(elements);
    }

    @Override
    public Object[] asObject() {
        return (Object[]) super.asObject();
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        append(writer, ", ");
    }

    public void append(SmaliWriter writer, String separator) throws IOException {
        boolean appendOnce = false;
        int size = this.size();
        for (int i = 0; i < size; i++) {
            Key key = get(i);
            if (appendOnce) {
                writer.append(separator);
            }
            if (key == null) {
                writer.append("# null");
            } else {
                key.append(writer);
            }
            appendOnce = true;
        }
    }

    @Override
    public String toString() {
        return toString(", ");
    }

    public String toString(String separator) {
        StringWriter stringWriter = new StringWriter();
        SmaliWriter writer = new SmaliWriter(stringWriter);
        try {
            this.append(writer, separator);
            writer.close();
            return stringWriter.toString();
        } catch (IOException exception) {
            return "# " + exception.toString();
        }
    }

    @Override
    public ArrayKey<T> replaceKey(Key search, Key replace) {
        return (ArrayKey<T>) super.replaceKey(search, replace);
    }

    @Override
    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (!(obj instanceof ArrayKey)) {
            return StringsUtil.compareToString(this, obj);
        }
        return compareElements((ArrayKey<?>) obj);
    }

    @Override
    public int hashCode() {
        return getHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ArrayKey)) {
            return false;
        }
        return equalsElements((ArrayKey<?>) obj);
    }

    @SuppressWarnings("unchecked")
    public static<E extends Key> ArrayKey<E> empty() {
        return (ArrayKey<E>) EMPTY;
    }
    public static<E extends Key> ArrayKey<E> create(Key ... elements) {
        if (elements == null || elements.length == 0) {
            return empty();
        }
        return new ArrayKey<>(elements);
    }
    public static<E extends Key> ArrayKey<E> read(SmaliReader reader, char end) throws IOException {
        return create(readElements(reader, end));
    }
    public static Key[] readElements(SmaliReader reader, char end) throws IOException {
        reader.skipWhitespacesOrComment();
        if (reader.getASCII(reader.position()) == end) {
            reader.readASCII();
            return EMPTY_ARRAY;
        }
        ArrayCollection<Key> results = new ArrayCollection<>();
        while (true) {
            Key key = KeyUtil.readKey(reader);
            results.add(key);
            reader.skipWhitespacesOrComment();
            if (reader.getASCII(reader.position()) == end) {
                break;
            }
            SmaliParseException.expect(reader, ',');
        }
        SmaliParseException.expect(reader, end);
        return results.toArrayFill(new Key[results.size()]);
    }

    public static<E extends Key> ArrayKey<E> parse(String text) {
        //FIXME
        throw new RuntimeException("ArrayKey.parse not implemented");
    }
}
