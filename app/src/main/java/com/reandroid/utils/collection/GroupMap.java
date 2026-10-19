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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.reandroid.utils.function.Function;

public class GroupMap<K, V> {

    private final int initialSize;
    private Map<K, Object> itemsMap;

    public GroupMap(int initialSize){
        this.initialSize = initialSize;
        Map<K, Object> itemsMap;
        if(initialSize > 0){
            itemsMap = new HashMap<>(initialSize);
        }else {
            itemsMap = new HashMap<>();
        }
        this.itemsMap = itemsMap;
    }
    public GroupMap(){
        this(0);
    }

    public void clear(){
        Map<K, Object> itemsMap = this.itemsMap;
        if(itemsMap.size() == 0){
            return;
        }
        itemsMap.clear();
        if(initialSize > 0){
            itemsMap = new HashMap<>(initialSize);
        }else {
            itemsMap = new HashMap<>();
        }
        this.itemsMap = itemsMap;
    }
    private void initializeEmpty(int size){
        Map<K, Object> itemsMap = this.itemsMap;
        if(itemsMap.size() != 0){
            return;
        }
        if(size < initialSize){
            size = initialSize;
        }
        if(size > 0){
            itemsMap = new HashMap<>(size);
        }else {
            itemsMap = new HashMap<>();
        }
        this.itemsMap = itemsMap;
    }
    public int size(){
        return itemsMap.size();
    }
    public boolean contains(K key){
        return itemsMap.containsKey(key);
    }
    @SuppressWarnings("unchecked")
    public Iterator<V> getAll(K key){
        Map<K, Object> itemsMap = this.itemsMap;
        Object obj = itemsMap.get(key);
        if(obj == null){
            return EmptyIterator.of();
        }
        if(obj instanceof Groups){
            return ((Groups<V>) obj).iterator();
        }
        return SingleIterator.of((V) obj);
    }
    @SuppressWarnings("unchecked")
    public V get(K key){
        Map<K, Object> itemsMap = this.itemsMap;
        Object obj = itemsMap.get(key);
        if(obj == null){
            return null;
        }
        if(obj instanceof Groups){
            return ((Groups<V>) obj).getFirst();
        }
        return (V) obj;
    }

    public void putAll(Collection<? extends V> collection, Function<? super V, K> function){
        if(collection.isEmpty()){
            return;
        }
        initializeEmpty(collection.size());
        for(V value : collection){
            put(function.apply(value), value);
        }
    }
    @SuppressWarnings("unchecked")
    public void put(K key, V value){
        if(key == null || value == null){
            return;
        }
        Map<K, Object> itemsMap = this.itemsMap;
        Object obj = itemsMap.get(key);
        if(obj == null){
            itemsMap.put(key, value);
            return;
        }
        Groups<V> groups;
        if(obj instanceof Groups){
            groups = (Groups<V>) obj;
        }else {
            groups = new Groups<>();
            groups.add((V) obj);
            itemsMap.remove(key);
        }
        groups.addIfAbsent(value);
    }
    public void updateKey(K old, K key){
        if(old == null || key == null || old == key){
            return;
        }
        Map<K, Object> itemsMap = this.itemsMap;
        Object obj = itemsMap.remove(old);
        if(obj != null){
            itemsMap.put(key, obj);
        }
    }

    @Override
    public String toString() {
        return "size = " + size();
    }

    static class Groups<T> extends ArrayCollection<T>{
        Groups(){
            super();
        }
        void addIfAbsent(T item){
            if(!containsExact(item)){
                add(item);
            }
        }
    }

}
