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

public class InstanceIterator<T> implements Iterator<T> {
    private final Iterator<?> iterator;
    private final Class<T> instance;
    private final Predicate<? super T> filter;
    private T mCurrent;

    public InstanceIterator(Iterator<?> iterator, Class<T> instance, Predicate<? super T> filter){
        this.iterator = iterator;
        this.instance = instance;
        this.filter = filter;
    }

    public InstanceIterator(Iterator<?> iterator, Class<T> instance){
        this(iterator, instance, null);
    }

    @Override
    public boolean hasNext() {
        return getCurrent() != null;
    }
    @Override
    public T next() {
        T current = getCurrent();
        if (current == null) {
            throw new NoSuchElementException();
        }
        this.mCurrent = null;
        return current;
    }

    @SuppressWarnings("unchecked")
    private T getCurrent() {
        T current = mCurrent;
        if(current != null){
            return current;
        }
        Iterator<?> iterator = this.iterator;
        Class<T> instance = this.instance;
        Predicate<? super T> filter = this.filter;
        while (iterator.hasNext()){
            Object obj = iterator.next();
            if(obj == null){
                continue;
            }
            if(instance.isInstance(obj)){
                current = (T)obj;
                if(filter == null || filter.test(current)){
                    mCurrent = current;
                    return current;
                }
            }
        }
        return null;
    }

    public static<T1> Iterator<T1> of(Iterator<?> iterator, Class<T1> instance){
        if(!iterator.hasNext()){
            return EmptyIterator.of();
        }
        return new InstanceIterator<>(iterator, instance);
    }
    public static<T1> Iterator<T1> of(Iterator<?> iterator, Class<T1> instance, Predicate<? super T1> filter){
        if(!iterator.hasNext()){
            return EmptyIterator.of();
        }
        return new InstanceIterator<>(iterator, instance, filter);
    }
}
