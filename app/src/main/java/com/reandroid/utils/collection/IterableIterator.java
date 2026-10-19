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

public abstract class IterableIterator<E, T> implements Iterator<T> {
    
    private final Iterator<? extends E> iterator;
    private Iterator<? extends T> mCurrent;
    private int mCount;
    private boolean mStop;
    
    public IterableIterator(Iterator<? extends E> iterator) {
        this.iterator = iterator;
    }
    public int getCountValue() {
        return mCount;
    }
    public void stop() {
        mStop = true;
    }
    public abstract Iterator<? extends T> iterator(E element);

    @Override
    public boolean hasNext() {
        Iterator<? extends T> current = getCurrent();
        return current != null && !mStop && current.hasNext();
    }

    @Override
    public T next() {
        Iterator<? extends T> current = getCurrent();
        if(current == null) {
            throw new NoSuchElementException();
        }
        T item = current.next();
        mCount ++;
        return item;
    }

    private Iterator<? extends T> getCurrent() {
        if(mCurrent == null || !mCurrent.hasNext()) {
            mCurrent = null;
            while (iterator.hasNext()) {
                Iterator<? extends T> item = iterator(iterator.next());
                if (item != null && item.hasNext()) {
                    mCurrent = item;
                    break;
                }
            }
        }
        return mCurrent;
    }

    public static<T, E> Iterator<T> of(Iterator<? extends E> iterator,
                                       Function<E, Iterator<? extends T> > function) {
        if (iterator == null || !iterator.hasNext()) {
            return EmptyIterator.of();
        }
        return new IterableIterator<E, T>(iterator) {
            @Override
            public Iterator<? extends T> iterator(E element) {
                if (element != null) {
                    return function.apply(element);
                }
                return null;
            }
        };
    }
}
