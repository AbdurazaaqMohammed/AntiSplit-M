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
package com.reandroid.arsc.container;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.base.BlockCounter;
import com.reandroid.arsc.base.BlockLocator;
import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.Swappable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class BlockList<T extends Block> extends Block implements BlockRefresh, Swappable {
    
    private ArrayCollection<T> mItems;
    private Creator<? extends T> mCreator;

    public BlockList(Creator<? extends T> creator) {
        super();
        mItems = ArrayCollection.empty();
        this.mCreator = creator;
    }
    public BlockList() {
        this(null);
    }

    public Creator<? extends T> getCreator() {
        return mCreator;
    }
    public void setCreator(Creator<? extends T> creator) {
        this.mCreator = creator;
        if (mItems.isImmutableEmpty()) {
            return;
        }
        updateCreator();
    }
    public void ensureSize(int size) {
        if (size > this.size()) {
            unlockList();
            mItems.ensureMinCapacity(size);
            setSize(size);
        }
    }
    public void setSize(int size) {
        setSize(size, false);
    }
    public void setSize(int size, boolean notify) {
        if (size == 0 && !notify) {
            lockList();
        } else if (mCreator != null || size < size()) {
            unlockList();
            mItems.setSize(size, notify);
            if (size == 0) {
                lockList();
            }
        }
    }
    public void setElements(T[] elements) {
        if (elements == null || elements.length == 0) {
            lockList();
            return;
        }
        unlockList();
        Creator<? extends T> creator = getCreator();
        int length = elements.length;
        for (int i = 0; i < length; i++) {
            T item = elements[i];
            if (item == null && creator != null) {
                item = creator.newInstanceAt(i);
                elements[i] = item;
            }
            onItemCreated(i, item);
        }
        mItems.setElements(elements);
        onChanged();
    }
    void onItemCreated(int index, T item) {
        if (item == null) {
            return;
        }
        item.setIndex(index);
        item.setParent(this);
    }
    public T createAt(int index) {
        Creator<? extends T> creator = getCreator();
        ensureSize(index);
        T item = creator.newInstanceAt(index);
        add(index, item);
        return item;
    }
    public T createNext() {
        Creator<? extends T> creator = getCreator();
        T item = creator.newInstanceAt(size());
        add(item);
        return item;
    }
    public T getFirst() {
        int size = size();
        for (int i = 0; i < size; i++) {
            T item = get(i);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
    public T getLast() {
        int size = size() - 1;
        for (int i = size; i >= 0; i--) {
            T item = get(i);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    public Iterator<T> clonedIterator() {
        return mItems.clonedIterator();
    }
    public Iterator<T> clonedIterator(int start) {
        return clonedIterator(start, size() - start);
    }
    public Iterator<T> clonedIterator(int start, int length) {
        return mItems.clonedIterator(start, length);
    }
    public Iterator<T> arrayIterator() {
        return mItems.arrayIterator();
    }
    public Iterator<T> iterator() {
        return mItems.iterator();
    }
    public Iterator<T> iterator(int start, int length) {
        return mItems.iterator(start, length);
    }
    public Iterator<T> iterator(Predicate<? super T> filter) {
        return mItems.iterator(filter);
    }
    public<T1> Iterator<T1> iterator(Class<T1> instance) {
        return mItems.iterator(instance);
    }

    public Iterator<T> reversedIterator() {
        return mItems.reversedIterator();
    }
    public int lastIndexOf(T item) {
        return mItems.lastIndexOf(item);
    }

    public int countIf(Predicate<? super T> predicate) {
        return mItems.count(predicate);
    }
    public int countFromLast(Predicate<? super T> predicate) {
        return mItems.countFromLast(predicate);
    }
    public ArrayCollection<T> subListIf(Predicate<? super T> predicate) {
        return mItems.subListIf(predicate);
    }
    public void clearChildes() {
        if (mItems.isEmpty()) {
            return;
        }
        Object lock = onRemoveRequestStarted();
        int size = size();
        for (int i = 0; i < size; i++) {
            remove(size() - 1, false);
        }
        lockList();
        onChanged();
        onRemoveRequestCompleted(lock);
    }
    public void destroy() {
        mItems.clear();
        lockList();
        onChanged();
    }
    public boolean sort(Comparator<? super T> comparator) {
        if (size() < 2) {
            return false;
        }
        boolean sorted = mItems.sort(comparator, (i, j) -> {
            T item1 = get(i);
            T item2 = get(j);
            if (item1 != null) {
                item1.setIndex(i);
            }
            if (item2 != null) {
                item2.setIndex(j);
            }
        });
        if (sorted) {
            updateIndex();
        }
        return sorted;
    }
    public boolean sort(Comparator<? super T> comparator, Swappable swappable) {
        if (size() < 2) {
            return false;
        }
        if (mItems.sort(comparator, swappable)) {
            return updateIndex();
        }
        return false;
    }
    public boolean needsSort(Comparator<? super T> comparator) {
        if (comparator == null) {
            return false;
        }
        int length = size();
        if (length < 2) {
            return false;
        }
        T previous = get(0);
        for (int i = 1; i < length; i++) {
            T item = get(i);
            if (comparator.compare(previous, item) > 0) {
                return true;
            }
            previous = item;
        }
        return false;
    }
    public boolean removeAll(Collection<?> collection) {
        return removeAllIndexes(toIndexArray(collection));
    }
    private int[] toIndexArray(Collection<?> collection) {
        int[] results = new int[collection.size()];
        int i = 0;
        List<T> items = this.mItems;
        int size = items.size();
        for (Object obj : collection) {
            if (obj == null) {
                continue;
            }
            Block block = (Block) obj;
            int index = block.getIndex();
            if (index < 0 || index >= size || block != items.get(index)) {
                continue;
            }
            results[i] = index;
            i ++;
        }
        int length = results.length;
        while (i < length) {
            results[i] = -1;
            i ++;
        }
        return results;
    }
    public boolean removeAllIndexes(int[] indexes) {
        Object lock = onRemoveRequestStarted();
        mItems.removeAllIndexes(indexes);
        updateIndex();
        onRemoveRequestCompleted(lock);
        return true;
    }
    public boolean removeIf(Predicate<? super T> filter) {
        Object lock = onRemoveRequestStarted();
        boolean removed = mItems.removeIf(filter);
        if (removed) {
            updateIndex();
        }
        onRemoveRequestCompleted(lock);
        return removed;
    }
    public void trimLastIf(Predicate<? super T> predicate) {
        trimLastIf(0, predicate);
    }
    public void trimLastIf(int start, Predicate<? super T> predicate) {
        int size = size();
        int i = size - 1;
        int end = i - mItems.countFromLast(start, predicate);
        while (i > end) {
            remove(i);
            i --;
        }
    }
    public T remove(int index) {
        Object lock = onRemoveRequestStarted();
        T item = remove(index, true);
        onRemoveRequestCompleted(lock);
        return item;
    }
    private T remove(int index, boolean updateIndex) {
        T item = mItems.remove(index);
        if (item == null) {
            return null;
        }
        item.setParent(null);
        item.setIndex(-1);
        if (updateIndex) {
            updateIndex(index);
        }
        onChanged();
        return item;
    }
    public boolean remove(T item) {
        if (item == null) {
            return false;
        }
        int index = mItems.indexOfExact(item, item.getIndex());
        if (index < 0) {
            index = mItems.indexOfExact(item);
        }
        if (index < 0) {
            return false;
        }
        Object lock = onRemoveRequestStarted();
        boolean removed = mItems.remove(index) != null;
        if (removed) {
            updateIndex(index);
            item.setIndex(-1);
            item.setParent(null);
        }
        onChanged();
        onRemoveRequestCompleted(lock);
        return removed;
    }
    public int indexOf(T item) {
        if (item == null) {
            return -1;
        }
        int index = mItems.indexOfExact(item, item.getIndex());
        if (index < 0) {
            index = mItems.indexOfExact(item);
        }
        return index;
    }
    protected void notifyPreRemove(T item) {
        if (item != null && item.getParent() == this) {
            onPreRemove(item);
            item.setIndex(-1);
            item.setParent(null);
        }
    }
    public void onPreRemove(T item) {

    }
    protected Object onRemoveRequestStarted() {
        return null;
    }
    protected void onRemoveRequestCompleted(Object lock) {
    }
    public boolean swap(int i, int j) {
        if (i == j) {
            return false;
        }
        return swap(get(i), get(j));
    }
    public boolean swap(T item1, T item2) {
        if (item1 == item2 || item1 == null || item2 == null) {
            return false;
        }
        int i1 = item1.getIndex();
        int i2 = item2.getIndex();
        mItems.swap(i1, i2);
        item1.setIndex(i2);
        item2.setIndex(i1);
        return true;
    }
    public void moveTo(T item, int index) {
        if (index < 0) {
            index = 0;
        }
        int i = mItems.indexOfExact(item, item.getIndex());
        Object lock = onRemoveRequestStarted();
        mItems.move(item, index);
        updateIndex(i, index);
        onRemoveRequestCompleted(lock);
    }
    public boolean transferTo(T item, BlockList<? super T> destination) {
        if (item == null || destination == null || destination == this) {
            return false;
        }
        int i = mItems.indexOfExact(item, item.getIndex());
        if (i < 0) {
            return false;
        }
        Object lock = onRemoveRequestStarted();
        mItems.removeSilent(i);
        boolean moved = destination.add(item);
        onRemoveRequestCompleted(lock);
        return moved;
    }
    public boolean transferTo(int index, BlockList<? super T> destination) {
        if (index < 0 || destination == null || destination == this) {
            return false;
        }
        Object lock = onRemoveRequestStarted();
        T item = mItems.removeSilent(index);
        boolean moved = destination.add(item);
        onRemoveRequestCompleted(lock);
        return moved;
    }
    public void set(int index, T item) {
        if (item == null) {
            return;
        }
        unlockList();
        item.setIndex(index);
        item.setParent(this);
        mItems.set(index, item);
        onChanged();
    }
    public void addAll(int index, T[] items) {
        if (items == null) {
            return;
        }
        int length = items.length;
        if (length == 0) {
            return;
        }
        unlockList();
        mItems.addAll(index, items);
        for (int i = 0; i < length; i++) {
            T item = items[i];
            if (item == null) {
                continue;
            }
            item.setIndex(index);
            item.setParent(this);
            index ++;
        }
        updateIndex(index);
        onChanged();
    }
    public void add(int index, T item) {
        if (item == null) {
            return;
        }
        unlockList();
        item.setIndex(index);
        item.setParent(this);
        mItems.add(index, item);
        updateIndex(index);
        onChanged();
    }
    private boolean updateIndex() {
        return updateIndex(0);
    }
    private boolean updateIndex(int start) {
        return updateIndex(start, size());
    }
    private boolean updateIndex(int start, int end) {
        if (start < 0) {
            start = 0;
        }
        if (start > end) {
            int i = start;
            start = end;
            end = i;
        }
        end = end + 1;
        boolean changed = false;
        int count = size();
        if (end > count) {
            end = count;
        }
        List<T> items = this.getChildes();
        for (int i = start; i < end; i++) {
            T item = items.get(i);
            if (item.getIndex() != i) {
                item.setIndex(i);
                changed = true;
            }
        }
        return changed;
    }
    public boolean add(T item) {
        if (item == null) {
            return false;
        }
        unlockList();
        int index = size();
        item.setIndex(index);
        item.setParent(this);
        boolean result = mItems.add(item);
        onChanged();
        return result;
    }
    public T get(int i) {
        if (i >= mItems.size() || i < 0) {
            return null;
        }
        return mItems.get(i);
    }
    public int getCount() {
        return size();
    }
    public int size() {
        return mItems.size();
    }
    public void ensureCapacity(int capacity) {
        unlockList();
        mItems.ensureCapacity(capacity);
    }
    public void trimToSize() {
        mItems.trimToSize();
        if (mItems.size() == 0) {
            lockList();
        }
    }
    public boolean contains(Object obj) {
        return mItems.contains(obj);
    }
    public boolean containsExact(Object obj) {
        return mItems.containsExact(obj);
    }
    public Object[] toArray() {
        return mItems.toArray();
    }
    public <T1> T1[] toArray(T1[] ts) {
        return mItems.toArray(ts);
    }
    public List<T> getChildes() {
        return mItems;
    }
    private void lockList() {
        if (mItems.isImmutableEmpty()) {
            return;
        }
        mItems = ArrayCollection.empty();
    }
    private void unlockList() {
        if (!mItems.isImmutableEmpty()) {
            return;
        }
        mItems = new ArrayCollection<>();
        updateCreator();
        mItems.setMonitor(getMonitor());
    }
    protected ArrayCollection.Monitor<T> getMonitor() {
        return new ArrayCollection.Monitor<T>() {
            @Override
            public void onAdd(int i, T item) {
            }
            @Override
            public void onRemoved(int i, T item) {
                notifyPreRemove(item);
            }
        };
    }
    private void updateCreator() {
        Creator<? extends T> creator = getCreator();
        if (creator == null) {
            mItems.setInitializer(null);
            return;
        }
        ArrayCollection.Initializer<T> initializer = index -> {
            T item = creator.newInstanceAt(index);
            onItemCreated(index, item);
            return item;
        };
        mItems.setInitializer(initializer);
    }
    @Override
    public final void refresh() {
        if (isNull()) {
            return;
        }
        trimToSize();
        onPreRefresh();
        refreshChildes();
        onRefreshed();
        onChanged();
    }
    protected void onPreRefresh() {
    }
    protected void onRefreshed() {
        onChanged();
    }
    public void onChanged() {
        mItems.onChanged();
    }
    protected void refreshChildes() {
        Iterator<?> iterator = iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (item instanceof BlockRefresh) {
                ((BlockRefresh) item).refresh();
            }
        }
    }
    @Override
    public byte[] getBytes() {
        byte[] results = null;
        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            results = addBytes(results, iterator.next().getBytes());
        }
        return results;
    }
    @Override
    public int countBytes() {
        int result = 0;
        int size = size();
        if (size != 0) {
            if (hasSimilarEntries()) {
                result = size * get(0).countBytes();
            } else {
                for (int i = 0; i < size; i++) {
                    result += get(i).countBytes();
                }
            }
        }
        return result;
    }

    @Override
    public void onCountUpTo(BlockCounter counter) {
        if (counter.FOUND) {
            return;
        }
        counter.setCurrent(this);
        if (counter.END == this) {
            counter.FOUND = true;
            return;
        }
        if (skipIndividualCounting(counter)) {
            counter.addCount(countBytes());
            return;
        }
        int size = size();
        for (int i = 0; i < size && !counter.FOUND; i++) {
            T item = get(i);
            if (item == null) {
                continue;
            }
            item.onCountUpTo(counter);
        }
    }
    private boolean skipIndividualCounting(BlockCounter counter) {
        if (size() == 0) {
            return true;
        }
        if (counter instanceof BlockLocator) {
            return false;
        }
        Block end = counter.END;
        if (end == null) {
            return true;
        }
        if (!hasSimilarEntries()) {
            return false;
        }
        return get(0).getClass() != end.getClass();
    }
    protected boolean hasSimilarEntries() {
        return false;
    }
    public void readChildes(BlockReader reader) throws IOException{
        int size = this.size();
        for (int i = 0; i < size; i++) {
            T item = get(i);
            item.readBytes(reader);
        }
        onChanged();
    }
    @Override
    protected int onWriteBytes(OutputStream stream) throws IOException {
        int result = 0;
        int size = size();
        for (int i = 0; i < size; i++) {
            T item = get(i);
            if (item == null) {
                continue;
            }
            result += item.writeBytes(stream);
        }
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BlockList<?> blockList = (BlockList<?>) obj;
        return mItems.equals(blockList.mItems);
    }
    @Override
    public int hashCode() {
        return mItems.hashCode();
    }

    @Override
    public String toString() {
        return "size=" + size();
    }

    public static boolean isImmutableEmpty(Object blockList) {
        return empty_list == blockList;
    }
    @SuppressWarnings("unchecked")
    public static<T1 extends Block> BlockList<T1> empty() {
        return (BlockList<T1>) empty_list;
    }

    public static JSONArray toJsonArray(BlockList<? extends JSONConvert<?>> blockList) {
        int size = blockList.size();
        if (size == 0) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(size);
        for (int i = 0; i < size; i++) {
            jsonArray.put(i, blockList.get(i).toJson());
        }
        return jsonArray;
    }
    public static void fromJsonArray(BlockList<? extends JSONConvert<?>> blockList, JSONArray jsonArray) {
        int size = jsonArray == null ? 0 : jsonArray.length();
        blockList.setSize(size);
        for (int i = 0; i < size; i++) {
            blockList.get(i).fromJson(ObjectsUtil.cast(jsonArray.get(i)));
        }
    }

    private static final BlockList<?> empty_list = new BlockList<Block>() {
        @Override
        public boolean add(Block item) {
            throw new IllegalArgumentException("Empty BlockList");
        }
        @Override
        public void add(int index, Block item) {
            throw new IllegalArgumentException("Empty BlockList");
        }
        @Override
        public void ensureCapacity(int capacity) {
            if (capacity != 0) {
                throw new IllegalArgumentException("Empty BlockList");
            }
        }
        @Override
        public void setSize(int size, boolean notify) {
            if (size != 0) {
                throw new IllegalArgumentException("Empty BlockList");
            }
        }
        @Override
        public int size() {
            return 0;
        }
    };
}
