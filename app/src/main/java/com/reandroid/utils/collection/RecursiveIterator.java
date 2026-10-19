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

import java.util.Iterator;
import java.util.NoSuchElementException;
import com.reandroid.utils.function.Function;
import com.reandroid.utils.function.Predicate;

public class RecursiveIterator<T> implements Iterator<T> {

    private T item;
    private final Function<T, Iterator<? extends T>> function;
    private final Predicate<? super T> filter;

    private boolean firstProcessed;
    private T current;
    private Iterator<? extends T> currentIterator;
    private RecursiveIterator<T> currentRecursiveIterator;

    public RecursiveIterator(T item, Function<T, Iterator<? extends T>> function, Predicate<? super T> filter){
        this.item = item;
        this.function = function;
        this.filter = filter;
    }
    public RecursiveIterator(T item, Function<T, Iterator<? extends T>> function){
        this(item, function, null);
    }

    @Override
    public boolean hasNext() {
        return getCurrent() != null;
    }
    @Override
    public T next() {
        T current = getCurrent();
        if(current == null){
            throw new NoSuchElementException();
        }
        this.current = null;
        return current;
    }

    private T getCurrent() {
        T current = this.current;
        if(current == null){
            current = computeCurrent();
            this.current = current;
        }
        return current;
    }
    private T computeCurrent() {
        if(!firstProcessed){
            firstProcessed = true;
            T element = this.item;
            if(matches(element)){
                return element;
            }
            this.item = null;
            return null;
        }
        if(this.item != null){
            initCurrentIterator();
        }
        Iterator<? extends T> iterator = getCurrentIterator();
        if(iterator != null && iterator.hasNext()){
            return iterator.next();
        }
        return null;
    }

    private Iterator<? extends T> getCurrentIterator() {
        Iterator<? extends T> iterator = currentRecursiveIterator;
        if(iterator != null && iterator.hasNext()){
            return iterator;
        }else {
            this.currentRecursiveIterator = null;
        }
        iterator = this.currentIterator;
        if(iterator != null && iterator.hasNext()){
            this.currentRecursiveIterator = new RecursiveIterator<>(iterator.next(), this.function, this.filter);
            iterator = this.currentRecursiveIterator;
        }else {
            iterator = null;
            this.currentIterator = null;
        }
        if(iterator != null && !iterator.hasNext()){
            return getCurrentIterator();
        }
        return iterator;
    }
    private void initCurrentIterator(){
        T element = this.item;
        this.item = null;
        Iterator<? extends T> iterator;
        if(element == null){
            iterator = null;
        }else {
            iterator = function.apply(element);
        }
        this.currentIterator = iterator;
    }

    private boolean matches(T element){
        if(element == null){
            return false;
        }
        Predicate<? super T> filter = this.filter;
        return filter == null || filter.test(element);
    }

    public static<T1> Iterator<T1> of(T1 item, Function<T1, Iterator<? extends T1>> function, Predicate<? super T1> filter){
        return new RecursiveIterator<>(item, function, filter);
    }
    public static<T1> Iterator<T1> of(T1 item, Function<T1, Iterator<? extends T1>> function){
        return new RecursiveIterator<>(item, function);
    }

    public static<T1, E> Iterator<E> compute(T1 item, Function<T1, Iterator<? extends T1>> function, Function<T1, Iterator<? extends E>> computer){
        return compute(item, function, null, computer);
    }
    @SuppressWarnings("unchecked")
    public static<T1, E> Iterator<E> compute(T1 item, Function<T1, Iterator<? extends T1>> function, Predicate<? super T1> filter, Function<T1, Iterator<? extends E>> computer){
        return new IterableIterator<T1, E>(new RecursiveIterator<>(item, function, filter)) {
            @Override
            public Iterator<E> iterator(T1 element) {
                return (Iterator<E>) computer.apply(element);
            }
        };
    }
}
