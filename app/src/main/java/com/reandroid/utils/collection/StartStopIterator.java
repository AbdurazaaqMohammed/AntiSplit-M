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

public class StartStopIterator<T> implements Iterator<T> {

    private final boolean excludeStop;
    private final Iterator<? extends T> iterator;
    private final Predicate<? super T> startPredicate;
    private final Predicate<? super T> stopPredicate;
    
    private T mNext;
    private boolean mStarted;
    private boolean mStopped;

    public StartStopIterator(boolean excludeStop,
                             Iterator<? extends T> iterator,
                             Predicate<? super T> startPredicate,
                             Predicate<? super T> stopPredicate) {

        this.excludeStop = excludeStop;
        this.iterator = iterator;
        this.startPredicate = startPredicate;
        this.stopPredicate = stopPredicate;

        if (startPredicate == null) {
            this.mStarted = true;
        }
    }

    @Override
    public boolean hasNext() {
        return getNext() != null;
    }
    @Override
    public T next() {
        T item = getNext();
        if (item == null) {
            throw new NoSuchElementException();
        }
        mNext = null;
        return item;
    }

    private T getNext() {
        T next = this.mNext;
        if (next != null || mStopped) {
            return next;
        }
        Iterator<? extends T> iterator = this.iterator;
        Predicate<? super T> predicate = this.startPredicate;
        boolean started = this.mStarted;
        while (iterator.hasNext()) {
            next = iterator.next();
            if (next == null) {
                continue;
            }
            if (!started) {
                if (predicate.test(next)) {
                    mStarted = true;
                } else {
                    continue;
                }
            }
            break;
        }
        predicate = this.stopPredicate;
        if (next == null || (predicate != null && predicate.test(next))) {
            mStopped = true;
            if (excludeStop) {
                next = null;
            }
        }
        this.mNext = next;
        return next;
    }

    public static<T1> Iterator<T1> of(boolean excludeStop,
                                      Iterator<? extends T1> iterator,
                                      Predicate<? super T1> startPredicate,
                                      Predicate<? super T1> stopPredicate) {
        if (iterator == null || !iterator.hasNext()) {
            return EmptyIterator.of();
        }
        if (startPredicate == null && stopPredicate == null) {
            return ObjectsUtil.cast(iterator);
        }
        return new StartStopIterator<>(excludeStop, iterator, startPredicate, stopPredicate);
    }
    public static<T1> Iterator<T1> of(Iterator<? extends T1> iterator,
                                      Predicate<? super T1> startPredicate,
                                      Predicate<? super T1> stopPredicate) {
        return of(false, iterator, startPredicate, stopPredicate);
    }
    public static<T1> Iterator<T1> start(Iterator<? extends T1> iterator,
                                         Predicate<? super T1> startPredicate) {
        return of(false, iterator, startPredicate, null);
    }
    public static<T1> Iterator<T1> stop(Iterator<? extends T1> iterator,
                                        Predicate<? super T1> stopPredicate) {
        return of(false, iterator, null, stopPredicate);
    }
    public static<T1> Iterator<T1> stop(Iterator<? extends T1> iterator,
                                        Predicate<? super T1> stopPredicate,
                                        boolean excludeStop) {
        return of(excludeStop, iterator, null, stopPredicate);
    }
    public static<T1> Iterator<T1> stopExclude(Iterator<? extends T1> iterator, Predicate<? super T1> stopPredicate) {
        return of(true, iterator, null, stopPredicate);
    }
}
