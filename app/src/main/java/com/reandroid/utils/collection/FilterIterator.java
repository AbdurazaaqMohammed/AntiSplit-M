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

import com.reandroid.utils.ObjectsUtil;

import java.util.Iterator;
import java.util.NoSuchElementException;
import com.reandroid.utils.function.Predicate;

public class FilterIterator<T> implements Iterator<T>, Predicate<T> {

    private final Iterator<? extends T> iterator;
    private T mNext;
    private final Predicate<? super T> mFilter;
    private boolean mFinished;

    public FilterIterator(Iterator<? extends T> iterator, Predicate<? super T> filter){
        this.iterator = iterator;
        this.mFilter = filter;
    }
    public FilterIterator(Iterator<? extends T> iterator){
        this(iterator, null);
    }

    @Override
    public boolean test(T item){
        return item != null;
    }

    @Override
    public boolean hasNext() {
        if(mFinished) {
            return false;
        }
        if(getNext() != null){
            return true;
        }
        mFinished = true;
        onFinished();
        return false;
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
    public void onFinished() {
    }
    private T getNext(){
        if(mNext == null) {
            while (iterator.hasNext()) {
                T item = iterator.next();
                if (testAll(item)) {
                    mNext = item;
                    break;
                }
            }
        }
        return mNext;
    }
    private boolean testAll(T item){
        if(item == null || !test(item)){
            return false;
        }
        return mFilter == null
                || mFilter.test(item);
    }

    public static final class Except<T1> extends FilterIterator<T1>{
        private final T1 excludeItem;
        private final boolean useEquals;

        public Except(Iterator<? extends T1> iterator, T1 excludeItem, boolean useEquals) {
            super(iterator);
            this.excludeItem = excludeItem;
            this.useEquals = useEquals;
        }
        public Except(Iterator<? extends T1> iterator, T1 excludeItem) {
            this(iterator, excludeItem, false);
        }

        @Override
        public boolean test(T1 item){
            if(item == null || item == excludeItem){
                return false;
            }
            if(!useEquals){
                return true;
            }
            return item.equals(excludeItem);
        }
    }
    public static<T1> Iterator<T1> of(Iterator<? extends T1> iterator,  Predicate<? super T1> filter){
        if (filter == null) {
            if (iterator == null) {
                return EmptyIterator.of();
            }
            return ObjectsUtil.cast(iterator);
        }
        if(iterator == null || !iterator.hasNext()){
            return EmptyIterator.of();
        }
        return new FilterIterator<>(iterator, filter);
    }
}
