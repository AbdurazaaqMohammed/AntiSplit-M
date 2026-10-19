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

import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

/**
 * A utility class to hold few non-null objects, the main purpose is to minimize new List class creation.
 *    <br />If no entries, the container will be null.
 *    <br />If one entry, the container will become entry itself.
 *    <br />If more than one entry, List class will be created and becomes the container.
 * */
public class ObjectsStore {

    public static boolean isEmpty(Object container) {
        if (container == null) {
            return true;
        }
        if (container.getClass() == ObjectsList.class) {
            return ((ObjectsList) container).isEmpty();
        }
        return false;
    }
    public static int size(Object container) {
        if (container == null) {
            return 0;
        }
        if (container.getClass() == ObjectsList.class) {
            return ((ObjectsList) container).size();
        }
        return 1;
    }
    public static boolean contains(Object container, Object item) {
        if (container == null) {
            return false;
        }
        if (container.getClass() == ObjectsList.class) {
            return ((ObjectsList) container).contains(item);
        }
        return container.equals(item);
    }
    public static<T> boolean containsIf(Object container, Predicate<T> predicate) {
        return iteratorIf(container, predicate).hasNext();
    }
    public static Object clear(Object container) {
        if (container != null && container.getClass() == ObjectsList.class) {
            ((ObjectsList) container).clear();
        }
        return null;
    }
    public static void sort(Object container, Comparator<?> comparator) throws ClassCastException {
        if (container != null && container.getClass() == ObjectsList.class) {
            ((ObjectsList) container).sort(ObjectsUtil.cast(comparator));
        }
    }
    public static<T> Iterator<T> iterator(Object container) throws ClassCastException {
        Iterator<?> iterator;
        if (container == null) {
            iterator = EmptyIterator.of();
        } else if (container.getClass() == ObjectsList.class) {
            iterator = ((ObjectsList) container).iterator();
        } else {
            iterator = SingleIterator.of(container);
        }
        return ObjectsUtil.cast(iterator);
    }
    public static<T> Iterator<T> iterator(Object container, Class<T> instance) throws ClassCastException {
        Iterator<?> iterator;
        if (container == null) {
            iterator = EmptyIterator.of();
        } else if (container.getClass() == ObjectsList.class) {
            iterator = ((ObjectsList) container).iterator(instance);
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
        } else if (container.getClass() == ObjectsList.class) {
            iterator = ((ObjectsList) container).clonedIterator();
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
        if (container.getClass() != ObjectsList.class) {
            if (container.equals(item)) {
                container = null;
            }
            return container;
        }
        ObjectsList list = (ObjectsList) container;
        list.remove(item);
        int size = list.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return list.get(0);
        }
        return list;
    }
    public static Object add(Object container, Object item) {
        if (item == null || item == container) {
            return container;
        }
        if (container == null) {
            return item;
        }
        ObjectsList list;
        if (container.getClass() == ObjectsList.class) {
            list = (ObjectsList) container;
        } else {
            list = new ObjectsList();
            list.add(container);
        }
        list.add(item);
        return list;
    }
    public static Object addAll(Object container, Iterator<?> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return container;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return add(container, first);
        }
        ObjectsList list;
        if (container != null && container.getClass() == ObjectsList.class) {
            list = (ObjectsList) container;
        } else {
            list = new ObjectsList();
            if (container != null) {
                list.add(container);
            }
        }
        list.add(first);
        list.add(iterator.next());
        list.addAll(iterator);
        int size = list.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return list.get(0);
        }
        return list;
    }
    public static Object addAll(Object container, Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return container;
        }
        ObjectsList list;
        if (container != null && container.getClass() == ObjectsList.class) {
            list = (ObjectsList) container;
            list.addAll(collection);
        } else {
            if (container == null) {
                list = new ObjectsList(collection.toArray());
            } else {
                list = new ObjectsList();
                list.add(container);
                list.addAll(collection);
            }
        }
        int size = list.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return list.get(0);
        }
        return list;
    }
    public static Object addAll(Object container, Object[] itemsArray) {
        if (itemsArray == null || itemsArray.length == 0) {
            return container;
        }
        ObjectsList list;
        if (container != null && container.getClass() == ObjectsList.class) {
            list = (ObjectsList) container;
            list.addAll(itemsArray);
        } else {
            if (container == null) {
                list = new ObjectsList(itemsArray);
            } else {
                list = new ObjectsList();
                list.add(container);
                list.addAll(itemsArray);
            }
        }
        int size = list.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return list.get(0);
        }
        return list;
    }
    public static<T> T get(Object container, int i) throws ClassCastException {
        Object item = null;
        if (container != null) {
            if (container.getClass() == ObjectsList.class) {
                item = ((ObjectsList) container).get(i);
            } else if(i == 0) {
                item = container;
            }
        }
        return ObjectsUtil.cast(item);
    }
    public static void collect(Object container, Object[] array) {
        if (container == null || array == null || array.length == 0) {
            return;
        }
        if (container.getClass() == ObjectsList.class) {
            ObjectsList list = (ObjectsList) container;
            list.toArrayFill(array);
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
        return new ObjectsList(array);
    }
    public static Object create(Iterator<?> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first;
        }
        ObjectsList list = new ObjectsList();
        list.add(first);
        list.addAll(iterator);
        return list;
    }

    static final class ObjectsList extends ArrayCollection<Object> {

        private boolean sorted;

        ObjectsList() {
            super(10);
        }
        ObjectsList(Object[] elements) {
            super(elements);
        }


        @Override
        public boolean remove(Object obj) {
            boolean removed = super.remove(obj);
            if (removed) {
                sorted = false;
            }
            return removed;
        }

        @Override
        public boolean add(Object item) {
            if (containsExact(item) || item == null) {
                return false;
            }
            sorted = false;
            return super.add(item);
        }

        @Override
        public void addAll(Iterator<?> iterator) {
            while (iterator.hasNext()) {
                this.add(iterator.next());
            }
        }

        @Override
        public boolean addAll(Collection<?> collection) {
            int size = size();
            this.add(collection.iterator());
            return size != size();
        }

        @Override
        public void sort(Comparator<? super Object> comparator) {
            boolean sorted = this.sorted;
            if (!sorted) {
                if (size() < 2) {
                    sorted = true;
                }
                this.sorted = true;
            }
            if (!sorted) {
                super.sort(comparator);
            }
        }
    }
}
