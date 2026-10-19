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
package com.reandroid.utils;

import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.InstanceIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;


/**
 * A utility class to hold few non-null objects, the main purpose is to minimize new HashSet class creation.
 *    <br />If no entries, the container will be null.
 *    <br />If one entry, the container will become entry itself.
 *    <br />If more than one entry, HashSet class will be created and becomes the container.
 * */

public class HashSetStore {

    public static boolean isEmpty(Object container) {
        if (container == null) {
            return true;
        }
        if (container.getClass() == ObjectsSet.class) {
            return ((ObjectsSet) container).isEmpty();
        }
        return false;
    }
    public static int size(Object container) {
        if (container == null) {
            return 0;
        }
        if (container.getClass() == ObjectsSet.class) {
            return ((ObjectsSet) container).size();
        }
        return 1;
    }
    public static boolean contains(Object container, Object item) {
        if (container == null) {
            return false;
        }
        if (container.getClass() == ObjectsSet.class) {
            return ((ObjectsSet) container).contains(item);
        }
        return container.equals(item);
    }
    public static<T> boolean containsIf(Object container, Predicate<T> predicate) {
        return iteratorIf(container, predicate).hasNext();
    }
    public static Object clear(Object container) {
        if (container != null && container.getClass() == ObjectsSet.class) {
            ((ObjectsSet) container).clear();
        }
        return null;
    }
    public static<T> Iterator<T> iterator(Object container) throws ClassCastException {
        Iterator<?> iterator;
        if (container == null) {
            iterator = EmptyIterator.of();
        } else if (container.getClass() == ObjectsSet.class) {
            iterator = ((ObjectsSet) container).iterator();
        } else {
            iterator = SingleIterator.of(container);
        }
        return ObjectsUtil.cast(iterator);
    }
    public static<T> Iterator<T> iterator(Object container, Class<T> instance) throws ClassCastException {
        Iterator<?> iterator;
        if (container == null) {
            iterator = EmptyIterator.of();
        } else if (container.getClass() == ObjectsSet.class) {
            iterator = ((ObjectsSet) container).iterator(instance);
        } else if (instance.isInstance(container)) {
            iterator = SingleIterator.of(container);
        } else {
            iterator = EmptyIterator.of();
        }
        return ObjectsUtil.cast(iterator);
    }
    public static<T> Iterator<T> clonedIterator(Object container) throws ClassCastException {
        Iterator<?> iterator;
        if (container == null) {
            iterator = EmptyIterator.of();
        } else if (container.getClass() == ObjectsSet.class) {
            iterator = ((ObjectsSet) container).clonedIterator();
        } else {
            iterator = SingleIterator.of(container);
        }
        return ObjectsUtil.cast(iterator);
    }
    public static<T> Iterator<T> iteratorIf(Object container, Predicate<T> predicate) throws ClassCastException {
        return FilterIterator.of(iterator(container), predicate);
    }
    public static Object remove(Object container, Object item) {
        if (item == null || container == null || item == container) {
            return null;
        }
        if (container.getClass() != ObjectsSet.class) {
            if (container.equals(item)) {
                container = null;
            }
            return container;
        }
        ObjectsSet set = (ObjectsSet) container;
        set.remove(item);
        int size = set.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return set.iterator().next();
        }
        return set;
    }
    public static Object add(Object container, Object item) {
        if (item == null || item == container) {
            return container;
        }
        if (container == null) {
            return item;
        }
        ObjectsSet set;
        if (container.getClass() == ObjectsSet.class) {
            set = (ObjectsSet) container;
        } else {
            set = new ObjectsSet();
            set.add(container);
        }
        set.add(item);
        return set;
    }
    public static Object addAll(Object container, Iterator<?> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return container;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return add(container, first);
        }
        ObjectsSet set;
        if (container != null && container.getClass() == ObjectsSet.class) {
            set = (ObjectsSet) container;
        } else {
            set = new ObjectsSet();
            if (container != null) {
                set.add(container);
            }
        }
        set.add(first);
        set.add(iterator.next());
        set.addAll(iterator);
        int size = set.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return set.iterator().next();
        }
        return set;
    }
    public static Object addAll(Object container, Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return container;
        }
        ObjectsSet set;
        if (container != null && container.getClass() == ObjectsSet.class) {
            set = (ObjectsSet) container;
            set.addAll(collection);
        } else {
            if (container == null) {
                set = new ObjectsSet(collection);
            } else {
                set = new ObjectsSet();
                set.add(container);
                set.addAll(collection);
            }
        }
        int size = set.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return set.getFirst();
        }
        return set;
    }
    public static Object addAll(Object container, Object[] itemsArray) {
        if (itemsArray == null || itemsArray.length == 0) {
            return container;
        }
        ObjectsSet set;
        if (container != null && container.getClass() == ObjectsSet.class) {
            set = (ObjectsSet) container;
            set.addAll(itemsArray);
        } else {
            if (container == null) {
                set = new ObjectsSet(itemsArray);
            } else {
                set = new ObjectsSet();
                set.add(container);
                set.addAll(itemsArray);
            }
        }
        int size = set.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return set.getFirst();
        }
        return set;
    }
    public static void collect(Object container, Object[] array) {
        if (container == null || array == null || array.length == 0) {
            return;
        }
        if (container.getClass() == ObjectsSet.class) {
            ObjectsSet set = (ObjectsSet) container;
            set.toArrayFill(array);
        } else {
            array[0] = container;
        }
    }
    public static Object create(Object[] array) {
        if (array == null) {
            return null;
        }
        int length = array.length;
        if (length == 0) {
            return null;
        }
        if (length == 1) {
            return array[0];
        }
        return new ObjectsSet(array);
    }
    public static Object create(Iterator<?> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first;
        }
        ObjectsSet set = new ObjectsSet();
        set.add(first);
        set.addAll(iterator);
        return set;
    }

    static final class ObjectsSet extends HashSet<Object> {

        ObjectsSet() {
            super();
        }
        ObjectsSet(Object[] elements) {
            super(elements.length);
            for (Object obj : elements) {
                add(obj);
            }
        }
        ObjectsSet(Collection<?> collection) {
            super(collection);
        }

        public<T> Iterator<T> iterator(Class<? extends T> instance) {
            return ObjectsUtil.cast(InstanceIterator.of(iterator(), instance));
        }

        public Object getFirst() {
            if (!isEmpty()) {
                return iterator().next();
            }
            return null;
        }

        public Iterator<Object> clonedIterator() {
            return ArrayIterator.of(toArray());
        }

        public void addAll(Iterator<?> iterator) {
            while (iterator.hasNext()) {
                add(iterator.next());
            }
        }
        public void addAll(Object[] elements) {
            if (elements != null) {
                for (Object obj : elements) {
                    add(obj);
                }
            }
        }
        public void toArrayFill(Object[] out) {
            Object[] elements = toArray();
            int length = NumbersUtil.min(out.length, elements.length);
            System.arraycopy(elements, 0, out, 0, length);
        }
    }
}
