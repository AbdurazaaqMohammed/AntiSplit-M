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
import com.reandroid.utils.function.Predicate;

public class ArrayIterator<T> implements Iterator<T>, SizedItem, SizedIterator{

    private final Object[] elements;
    private final int mStart;
    private final int mLength;
    private final Predicate<? super T> mFilter;
    private int index;
    private T mNext;

    public ArrayIterator(Object[] elements, int start, int length, Predicate<? super T> filter){
        this.elements = elements;
        this.mStart = start;
        this.mLength = length;
        this.mFilter = filter;
    }
    public ArrayIterator(Object[] elements, int start, int length){
        this(elements, start, length, null);
    }
    public ArrayIterator(Object[] elements, Predicate<? super T> filter){
        this(elements, 0, elements.length, filter);
    }
    public ArrayIterator(Object[] elements){
        this(elements, null);
    }


    @Override
    public int getRemainingSize() {
        return mLength - index;
    }
    @Override
    public int size(){
        return mLength;
    }
    @Override
    public boolean hasNext() {
        return getNext() != null;
    }
    @Override
    public T next() {
        T item = getNext();
        if(item == null){
            throw new NoSuchElementException();
        }
        mNext = null;
        return item;
    }
    @SuppressWarnings("unchecked")
    private T getNext(){
        Object[] elements = this.elements;
        if(mNext == null && elements != null) {
            while (index < mLength) {
                T item = (T)elements[mStart + index];
                index ++;
                if (testAll(item)) {
                    mNext = item;
                    break;
                }
            }
        }
        return mNext;
    }
    private boolean testAll(T item){
        if(item == null){
            return false;
        }
        return mFilter == null || mFilter.test(item);
    }
    public static<T1> Iterator<T1> of(Object[] elements){
        if(isEmpty(elements)){
            return EmptyIterator.of();
        }
        return new ArrayIterator<>(elements);
    }
    public static<T1> Iterator<T1> of(Object[] elements, Predicate<? super T1> filter){
        if(isEmpty(elements)){
            return EmptyIterator.of();
        }
        return new ArrayIterator<>(elements, filter);
    }
    public static<T1> Iterator<T1> of(Object[] elements, int start, int length){
        if(isEmpty(elements)){
            return EmptyIterator.of();
        }
        return new ArrayIterator<>(elements, start, length);
    }
    private static boolean isEmpty(Object[] elements){
        if(elements == null || elements.length == 0){
            return true;
        }
        for(Object element : elements){
            if(element != null){
                return false;
            }
        }
        return true;
    }
}
