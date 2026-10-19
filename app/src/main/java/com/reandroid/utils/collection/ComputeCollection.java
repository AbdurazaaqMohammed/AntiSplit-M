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
package com.reandroid.utils.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import com.reandroid.utils.function.Function;

public class ComputeCollection<T, E> implements Collection<T> {

    private final Collection<? extends E> source;
    private final Function<? super E, T> function;

    public ComputeCollection(Collection<? extends E> collection, Function<? super E, T> function){
        this.source = collection;
        this.function = function;
    }

    public T apply(E input){
        return function.apply(input);
    }
    public Collection<? extends E> getSource() {
        return source;
    }
    @Override
    public int size() {
        return source.size();
    }
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
    @Override
    public boolean contains(Object obj) {
        for(T item : this){
            if(Objects.equals(obj, item)){
                return true;
            }
        }
        return false;
    }
    @Override
    public Iterator<T> iterator() {
        return ComputeIterator.of(source.iterator(), function);
    }
    @Override
    public Object[] toArray() {
        int size = size();
        if(size == 0){
            return ArrayCollection.EMPTY_OBJECTS;
        }
        Object[] results = new Object[size];
        Iterator<? extends E> iterator = source.iterator();
        int i = 0;
        while (iterator.hasNext()){
            results[i] = function.apply(iterator.next());
            i++;
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T1> T1[] toArray(T1[] out) {
        if(out.length == 0){
            return out;
        }
        int size = size();
        if(size == 0){
            return out;
        }
        if(size > out.length){
            size = out.length;
        }
        Iterator<? extends E> iterator = source.iterator();
        int i = 0;
        while (i < size && iterator.hasNext()){
            out[i] = (T1) function.apply(iterator.next());
            i++;
        }
        return out;
    }

    @Override
    public boolean add(T t) {
        throw new IllegalArgumentException("Can't add on " + getClass().getSimpleName());
    }

    @Override
    public boolean remove(Object o) {
        throw new IllegalArgumentException("Can't remove on " + getClass().getSimpleName());
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if(collection.isEmpty()){
            return false;
        }
        for(Object obj : collection){
            if(!contains(obj)){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        if(collection.isEmpty()){
            return false;
        }
        throw new IllegalArgumentException("Can't add on " + getClass().getSimpleName());
    }
    @Override
    public boolean removeAll(Collection<?> collection) {
        if(collection.isEmpty()){
            return false;
        }
        throw new IllegalArgumentException("Can't remove on " + getClass().getSimpleName());
    }
    @Override
    public boolean retainAll(Collection<?> collection) {
        if(collection.isEmpty()){
            return false;
        }
        throw new IllegalArgumentException("Can't retain on " + getClass().getSimpleName());
    }
    @Override
    public void clear() {
        if(isEmpty()){
            return;
        }
        throw new IllegalArgumentException("Can't clear on " + getClass().getSimpleName() + ", use getSource().clear();");
    }
}
