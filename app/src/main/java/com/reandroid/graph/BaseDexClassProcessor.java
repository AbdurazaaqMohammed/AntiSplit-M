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
package com.reandroid.graph;

import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.utils.collection.FilterIterator;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class BaseDexClassProcessor extends GraphTask {

    private final DexClassRepository classRepository;

    public BaseDexClassProcessor(DexClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    public Iterator<DexClass> getDexClasses(Predicate<? super DexClass> filter) {
        return FilterIterator.of(getClassRepository().getDexClasses(), filter);
    }
    public DexClassRepository getClassRepository() {
        return classRepository;
    }
}
