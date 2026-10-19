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

import java.util.*;
import com.reandroid.utils.function.Consumer;
import com.reandroid.utils.function.Function;
import com.reandroid.utils.function.Predicate;

@SuppressWarnings("unchecked")
public class MultiMap<K, V> {

    private final Object mLock = new Object();
    private Map<K, Object> map;
    private int initialSize;
    private Comparator<? super V> favouriteObjectsSorter;

    public MultiMap() {
    }

    public void findDuplicates(Comparator<? super V> comparator, Consumer<List<V>> consumer) {
        synchronized (mLock) {
            Map<K, Object> map = this.map;
            if(map == null || map.isEmpty()) {
                return;
            }
            Set<Map.Entry<K, Object>> entrySet = map.entrySet();
            for(Map.Entry<K, Object> entry : entrySet) {
                Object obj = entry.getValue();
                if(obj == null) {
                    continue;
                }
                if(obj.getClass() == EntryList.class) {
                    EntryList<V> entryList = (EntryList<V>) obj;
                    if(entryList.size() == 1) {
                        entry.setValue(entryList.get(0));
                    }else {
                        processDuplicateValues(comparator, consumer, entryList);
                    }
                }
            }
        }
    }
    private void processDuplicateValues(Comparator<? super V> comparator, Consumer<List<V>> consumer, EntryList<V> entryList) {
        int size = entryList.size();
        if(size < 2) {
            return;
        }

        ArrayCollection<V> sortedList = new ArrayCollection<>(entryList);
        size = sortedList.size();
        if(!sortFavourites((List<Object>) sortedList)) {
            sortedList.sort(comparator);
        }

        V previous = sortedList.get(0);
        if(comparator.compare(previous, sortedList.get(size - 1)) == 0) {
            consumer.accept(sortedList);
            return;
        }

        ArrayCollection<V> result = new ArrayCollection<>(size);
        result.add(previous);
        for(int i = 1; i < size; i++) {
            V value = sortedList.get(i);
            int compare = comparator.compare(previous, value);
            if(compare != 0) {
                if(result.size() > 1) {
                    consumer.accept(result);
                }
                result.clearTemporarily();
            }
            result.add(value);
            previous = value;
        }
        if(result.size() > 1) {
            consumer.accept(result);
        }
    }
    public void putAll(Function<? super V, K> function, Iterator<? extends V> iterator) {
        synchronized (mLock) {
            while (iterator.hasNext()) {
                V value = iterator.next();
                putUnlocked(function.apply(value), value);
            }
        }
    }
    public Set<K> keySet() {
        synchronized (mLock) {
            return map.keySet();
        }
    }
    public void put(K key, V value) {
        synchronized (mLock) {
            putUnlocked(key, value);
        }
    }
    private void putUnlocked(K key, V value) {
        if(key == null || value == null) {
            return;
        }
        Map<K, Object> map = this.getInitializedMap();
        Object obj = map.get(key);
        if(obj == null) {
            map.put(key, value);
            return;
        }
        if(obj.getClass() == EntryList.class) {
            EntryList<Object> entryList = (EntryList<Object>) obj;
            if(!entryList.containsExact(value)) {
                entryList.add(value);
                sortFavourites(entryList);
            }
        }else {
            obj = map.remove(key);
            obj = combine(obj, value);
            map.put(key, obj);
        }
    }
    @SuppressWarnings("all")
    public V remove(Object key, Object value) {
        synchronized (mLock) {
            if(key == null) {
                return null;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return null;
            }
            Object obj = map.get(key);
            if(obj == null) {
                return null;
            }
            if(obj.getClass() == EntryList.class) {
                EntryList<?> entryList = (EntryList<?>) obj;
                int i = entryList.indexOfExact(value);
                if(i < 0) {
                    return null;
                }
                obj = entryList.remove(i);
                if(entryList.isEmpty()) {
                    map.remove(key);
                }else if(entryList.size() == 1) {
                    obj = entryList.get(0);
                    map.remove(key);
                    map.put((K) key, obj);
                }
                return (V) obj;
            }
            if(obj == value) {
                return (V) map.remove(key);
            }
            return null;
        }
    }
    @SuppressWarnings("all")
    public V remove(Object key) {
        synchronized (mLock) {
            if(key == null) {
                return null;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return null;
            }
            return (V) map.remove(key);
        }
    }
    @SuppressWarnings("all")
    public V removeIf(Object key, Predicate<? super V> predicate) {
        synchronized (mLock) {
            if(key == null) {
                return null;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return null;
            }
            Object obj = map.get(key);
            if(obj == null) {
                return null;
            }
            if(obj.getClass() == EntryList.class) {
                EntryList<V> entryList = (EntryList<V>) obj;
                int i = entryList.indexOfIf(predicate);
                if(i < 0) {
                    return null;
                }
                obj = entryList.remove(i);
                if(entryList.isEmpty()) {
                    map.remove(key);
                }else if(entryList.size() == 1) {
                    obj = entryList.get(0);
                    map.remove(key);
                    map.put((K) key, obj);
                }
                return (V) obj;
            }
            if(predicate.test((V) obj)) {
                return (V) map.remove(key);
            }
            return null;
        }
    }
    @SuppressWarnings("all")
    public boolean containsKey(Object key) {
        synchronized (mLock) {
            if(key == null) {
                return false;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return false;
            }
            return map.containsKey(key);
        }
    }
    @SuppressWarnings("all")
    public boolean containsValue(Object key, Predicate<? super V> predicate) {
        synchronized (mLock) {
            if(key == null) {
                return false;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return false;
            }
            Object obj = map.get(key);
            if(obj == null) {
                return false;
            }
            if(obj.getClass() == EntryList.class) {
                EntryList<V> entryList = (EntryList<V>) obj;
                return entryList.containsIf(predicate);
            }
            return predicate.test((V) obj);
        }
    }

    public boolean updateKey(K old, K key, V value) {
        synchronized (mLock) {
            if(old == null && key == null) {
                return false;
            }
            Map<K, Object> map = getInitializedMap();
            Object obj = combine(map.remove(old), map.remove(key));
            obj = combine(obj, value);
            map.put(key, obj);
            return true;
        }
    }
    private Object combine(Object obj1, Object obj2) {
        if(obj1 == obj2) {
            return obj1;
        }
        if(obj1 == null) {
            return obj2;
        }
        if(obj2 == null) {
            return obj1;
        }
        boolean list1 = obj1.getClass() == EntryList.class;
        boolean list2 = obj2.getClass() == EntryList.class;
        if(!list1 && !list2) {
            EntryList<Object> entryList = new EntryList<>(obj1, obj2);
            sortFavourites(entryList);
            return entryList;
        }
        if(list1 && !list2) {
            EntryList<Object> entryList = (EntryList<Object>) obj1;
            if(!entryList.containsExact(obj2)) {
                entryList.add(obj2);
                sortFavourites(entryList);
            }
            return entryList;
        }
        if(!list1) {
            EntryList<Object> entryList = (EntryList<Object>) obj2;
            if(!entryList.containsExact(obj1)) {
                entryList.add(obj1);
                sortFavourites(entryList);
            }
            return entryList;
        }
        EntryList<Object> entryList1 = (EntryList<Object>) obj1;
        EntryList<Object> entryList2 = (EntryList<Object>) obj2;
        boolean added = false;
        for(Object obj : entryList2) {
            if(!entryList1.containsExact(obj)){
                entryList1.add(obj);
                added = true;
            }
        }
        if(added) {
            sortFavourites(entryList1);
        }
        return entryList1;
    }
    public Iterator<V> getAll(K key) {
        synchronized (mLock) {
            if(key == null) {
                return EmptyIterator.of();
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return EmptyIterator.of();
            }
            Object obj = map.get(key);
            if(obj == null) {
                return EmptyIterator.of();
            }
            if(obj.getClass() == EntryList.class) {
                return ((EntryList<V>) obj).iterator();
            }
            return SingleIterator.of((V) obj);
        }
    }
    public V get(K key) {
        return get(key, null);
    }
    public V get(K key, Predicate<? super V> predicate) {
        synchronized (mLock) {
            if(key == null) {
                return null;
            }
            Map<K, Object> map = this.map;
            if(map == null) {
                return null;
            }
            Object obj = map.get(key);
            if(obj == null) {
                return null;
            }
            if(obj.getClass() == EntryList.class) {
                return getFromEntryList(key, (EntryList<?>) obj, predicate);
            }
            V value = (V) obj;
            if(predicate != null && !predicate.test(value)) {
                value = null;
            }
            return value;
        }
    }
    public void clear() {
        synchronized (mLock) {
            Map<K, Object> map = this.map;
            if(map != null) {
                if(this.initialSize == 0) {
                    initialSize = map.size();
                }
                this.map = null;
                map.clear();
            }
        }
    }
    public int size() {
        synchronized (mLock) {
            Map<K, Object> map = this.map;
            if(map == null) {
                return 0;
            }
            return map.size();
        }
    }
    private Map<K, Object> getInitializedMap() {
        Map<K, Object> map = this.map;
        if(map == null) {
            int size = this.initialSize;
            if(size == 0) {
                map = new HashMap<>();
            }else {
                map = new HashMap<>(size);
            }
            this.map = map;
        }
        return map;
    }

    private boolean sortFavourites(List<Object> entryList) {
        Comparator<?> comparator = this.favouriteObjectsSorter;
        if(comparator == null) {
            return false;
        }
        if(entryList == null || entryList.size() < 2) {
            return false;
        }
        java.util.Collections.sort(entryList, (Comparator<? super Object>) comparator);
        return true;
    }
    public void setFavouriteObjectsSorter(Comparator<? super V> favouriteObjectsSorter) {
        synchronized (mLock) {
            this.favouriteObjectsSorter = favouriteObjectsSorter;
        }
    }

    public void setInitialSize(int size) {
        synchronized (mLock) {
            this.initialSize = size;
            Map<K, Object> map = this.map;
            if(map != null && map.isEmpty()) {
                this.map = null;
            }
        }
    }
    private V getFromEntryList(K key, EntryList<?> entryList, Predicate<? super V> predicate) {
        if(entryList.isEmpty()) {
            this.map.remove(key);
            return null;
        }
        EntryList<V> list = (EntryList<V>) entryList;
        if(predicate == null) {
            return list.getFirst();
        }
        return CollectionUtil.getFirst(list.iterator(predicate));
    }

    @Override
    public String toString() {
        return "size = " + size();
    }
    static class EntryList<T> extends ArrayCollection<T> {
        public EntryList(Object item1, Object item2) {
            super(new Object[]{item1, item2});
        }
    }
}
