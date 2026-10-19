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

public class IndexIterator<T> implements Iterator<T> {
    private final Predicate<? super T> mFilter;
    private final SizedSupplier<? extends T> mSupplier;
    private int mIndex;
    private T mNext;
    public IndexIterator(SizedSupplier<? extends T> supplier, Predicate<? super T> filter){
        this.mSupplier = supplier;
        this.mFilter = filter;
    }
    public IndexIterator(SizedSupplier<? extends T> supplier){
        this(supplier, null);
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

    private T getNext(){
        if(mNext == null) {
            while (mIndex < mSupplier.size()) {
                T item = mSupplier.get(mIndex);
                mIndex ++;
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
        return mFilter == null
                || mFilter.test(item);
    }

    public static<T1> Iterator<T1> of(SizedSupplier<T1> supplier){
        return of(supplier, null);
    }
    public static<T1> Iterator<T1> of(SizedSupplier<T1> supplier, Predicate<? super T1> filter){
        if(supplier == null || supplier.size() == 0){
            return EmptyIterator.of();
        }
        return new IndexIterator<T1>(supplier, filter);
    }

}
