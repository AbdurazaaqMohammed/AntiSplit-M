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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class UniqueIterator<T> extends FilterIterator<T> {

    private Set<T> excludeSet;

    public UniqueIterator(Iterator<? extends T> iterator, Predicate<? super T> filter){
        super(iterator, filter);
    }
    public UniqueIterator(Iterator<? extends T> iterator){
        super(iterator);
    }

    public UniqueIterator<T> exclude(T item) {
        addExclude(item);
        return this;
    }
    private T addExclude(T item) {
        if(item == null){
            return null;
        }
        Set<T> excludeSet = this.excludeSet;
        if(excludeSet == null){
            excludeSet = new HashSet<>();
            this.excludeSet = excludeSet;
        }
        excludeSet.add(item);
        return item;
    }

    @Override
    public T next() {
        return addExclude(super.next());
    }

    @Override
    public boolean test(T item) {
        if(item == null){
            return false;
        }
        Set<T> excludeSet = this.excludeSet;
        return excludeSet == null || !excludeSet.contains(item);
    }

    @Override
    public void onFinished() {
        Set<T> excludeSet = this.excludeSet;
        if(excludeSet != null) {
            excludeSet.clear();
            this.excludeSet = null;
        }
    }
    public static<T1> Iterator<T1> of(Iterator<? extends T1> iterator){
        if (iterator == null || !iterator.hasNext()) {
            return EmptyIterator.of();
        }
        if (iterator instanceof UniqueIterator) {
            return ObjectsUtil.cast(iterator);
        }
        return new UniqueIterator<>(iterator);
    }
    public static<T1> Iterator<T1> of(Iterator<? extends T1> iterator,  Predicate<? super T1> filter){
        if (iterator == null || !iterator.hasNext()) {
            return EmptyIterator.of();
        }
        if (filter == null) {
            if (iterator instanceof UniqueIterator) {
                return ObjectsUtil.cast(iterator);
            }
        }
        return new UniqueIterator<>(iterator, filter);
    }
}
