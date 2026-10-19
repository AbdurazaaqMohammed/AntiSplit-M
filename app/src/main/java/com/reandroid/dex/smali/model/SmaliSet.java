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
package com.reandroid.dex.smali.model;

import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.InstanceIterator;
import com.reandroid.utils.collection.Swappable;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class SmaliSet<T extends Smali> extends Smali implements Swappable {

    private final ArrayCollection<T> body;

    public SmaliSet() {
        super();
        this.body = new ArrayCollection<>();
        this.body.setMonitor(new SmaliSetMonitor<>(this));
    }

    public Iterator<T> iterator() {
        return body.iterator();
    }
    public Iterator<T> iterator(int start) {
        return body.iterator(start);
    }
    public<T2> Iterator<T2> iterator(Class<T2> instance) {
        return body.iterator(instance);
    }
    public<T2> Iterator<T2> iterator(int start, Class<T2> instance) {
        return InstanceIterator.of(body.iterator(start), instance);
    }
    public Iterator<T> reversedIterator() {
        return body.reversedIterator();
    }
    public Iterator<T> reversedIterator(int start) {
        return body.reversedIterator(start);
    }
    public<T2> Iterator<T2> reversedIterator(Class<T2> instance) {
        return InstanceIterator.of(body.reversedIterator(), instance);
    }
    public<T2> Iterator<T2> reversedIterator(int start, Class<T2> instance) {
        return InstanceIterator.of(body.reversedIterator(start), instance);
    }
    public int size() {
        return body.size();
    }
    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("all")
    public int indexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        if (obj instanceof Smali) {
            Smali smali = (Smali) obj;
            if (smali.getParent() == this) {
                int index = smali.getIndex();
                if (obj.equals(get(index))) {
                    return index;
                }
            }
        }
        return body.indexOf(obj);
    }
    public int indexOfIdentity(T smali) {
        if (smali == null) {
            return -1;
        }
        int index = smali.getIndex();
        if (smali == get(index)) {
            return index;
        }
        return body.indexOfExact(smali);
    }
    public T get(int i) {
        if (i < 0 || i >= size()) {
            return null;
        }
        return body.get(i);
    }
    public T get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(indexOf(obj));
    }
    public T getFirst() {
        return get(0);
    }
    public T getLast() {
        return get(size() - 1);
    }
    public boolean add(T smali) {
        if (smali == null) {
            return false;
        }
        Smali parent = smali.getParent();
        if (parent == null) {
            parent = this;
            smali.setParent(parent);
        }
        if (parent == this) {
            smali.setIndex(size());
        }
        return body.add(smali);
    }
    public void add(int i, T smali) {
        if (smali == null) {
            return;
        }
        Smali parent = smali.getParent();
        if (parent == null) {
            parent = this;
            smali.setParent(parent);
        }
        body.add(i, smali);
        if (parent == this) {
            smali.setIndex(i);
            updateIndexes(i + 1);
        }
    }
    public void addAll(Iterator<? extends T> iterator) {
        while (iterator.hasNext()) {
            add(iterator.next());
        }
    }
    public boolean contains(T smali) {
        return body.contains(smali);
    }
    public boolean remove(T smali) {
        return body.remove(smali);
    }
    public T remove(int i) {
        return body.remove(i);
    }
    public void moveTo(T smali, int to) {
        moveTo(indexOfIdentity(smali), to);
    }
    public void moveTo(int from, int to) {
        body.move(from, to);
        updateIndexes(NumbersUtil.min(from, to));
    }
    
    @Override
    public boolean swap(int from, int to) {
        ArrayCollection<T> body = this.body;
        if (body.swap(from, to)) {
            T item = body.get(from);
            if (item != null) {
                item.setIndex(to);
            }
            item = body.get(to);
            if (item != null) {
                item.setIndex(from);
            }
            return true;
        }
        return false;
    }
    public boolean removeIf(Predicate<? super T> filter) {
        return body.removeIf(filter);
    }
    public boolean removeInstances(Class<?> instance) {
        return body.removeIf((Predicate<T>) instance::isInstance);
    }
    public void clear() {
        for (T smali : body) {
            smali.setParent(null);
        }
        body.clear();
    }
    void onRemoved(T item) {
        if (item != null) {
            item.setParent(null);
        }
    }
    void onAdded(T item) {
        if (item != null) {
            item.setParent(this);
        }
    }

    public void updateIndexes() {
        updateIndexes(0);
    }
    protected void updateIndexes(int start) {
        int size = size();
        if (start < 0) {
            start = 0;
        }
        for (int i = start; i < size; i++) {
            get(i).setIndex(i);
        }
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendAllWithDoubleNewLine(iterator());
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        while (parseNext(reader) != null) {
            reader.skipWhitespacesOrComment();
        }
    }
    public T parseNext(SmaliReader reader) throws IOException {
        return parseAdd(size(), reader);
    }
    public T parseAdd(int index, SmaliReader reader) throws IOException {
        if (reader.finished()) {
            return null;
        }
        T item = createNext(reader);
        if (item != null) {
            add(index, item);
            item.parse(reader);
        }
        return item;
    }
    T createNext(SmaliReader reader) {
        throw new RuntimeException("Method not implemented");
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        int size = size();
        for (int i = 0; i < size; i++) {
            get(i).validate();
        }
    }

    static class SmaliSetMonitor<T extends Smali> implements ArrayCollection.Monitor<T> {

        private final SmaliSet<T> smaliSet;

        SmaliSetMonitor(SmaliSet<T> smaliSet) {
            this.smaliSet = smaliSet;
        }
        @Override
        public void onAdd(int i, T item) {
            smaliSet.onAdded(item);
        }
        @Override
        public void onRemoved(int i, T item) {
            smaliSet.onRemoved(item);
        }
    }
}
