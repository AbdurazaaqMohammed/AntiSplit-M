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

import java.util.*;
import com.reandroid.utils.function.Function;

public class ComputeList<T, E> extends ComputeCollection<T, E> implements List<T>{

    public ComputeList(List<? extends E> list, Function<? super E, T> function) {
        super(list, function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends E> getSource() {
        return (List<? extends E>) super.getSource();
    }
    @Override
    public T get(int i) {
        return apply(getSource().get(i));
    }

    @Override
    public T set(int i, T t) {
        throw new IllegalArgumentException("Can't set on " + getClass().getSimpleName());
    }
    @Override
    public void add(int i, T t) {
        throw new IllegalArgumentException("Can't add on " + getClass().getSimpleName());
    }
    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        if(collection.isEmpty()){
            return false;
        }
        throw new IllegalArgumentException("Can't add on " + getClass().getSimpleName());
    }
    @Override
    public T remove(int i) {
        throw new IllegalArgumentException("Can't remove on " + getClass().getSimpleName());
    }
    @Override
    public int indexOf(Object obj) {
        int size = size();
        for(int i = 0; i < size; i++){
            if(Objects.equals(obj, get(i))){
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object obj) {
        int result = -1;
        int size = size();
        for(int i = 0; i < size; i++){
            if(Objects.equals(obj, get(i))){
                result = i;
            }
        }
        return result;
    }

    @Override
    public ListIterator<T> listIterator() {
        return ListItr.of(this);
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return ListItr.of(this);
    }

    @Override
    public List<T> subList(int i, int i1) {
        return null;
    }
}
