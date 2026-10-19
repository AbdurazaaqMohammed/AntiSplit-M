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

import com.reandroid.common.ArraySupplier;
import com.reandroid.utils.NumbersUtil;

import java.util.*;
import com.reandroid.utils.function.Predicate;

@SuppressWarnings("unchecked")
public class ArrayCollection<T> implements ArraySupplier<T>, List<T>, Set<T>, Swappable {

    private Object[] mElements;
    private Initializer<T> mInitializer;
    private int size;
    private int mLastGrow;

    private int mHashCode;
    private boolean mLocked;

    private Monitor<T> mMonitor;

    public ArrayCollection(int initialCapacity){
        Object[] elements;
        if(initialCapacity == 0){
            elements = EMPTY_OBJECTS;
        }else {
            elements = new Object[initialCapacity];
        }
        this.mElements = elements;
        this.size = 0;
    }
    public ArrayCollection(Object[] elements){
        if(elements == null || elements.length == 0){
            elements = EMPTY_OBJECTS;
        }
        this.mElements = elements;
        this.size = elements.length;
    }
    public ArrayCollection(Collection<? extends T> collection){
        Object[] elements;
        int size = collection.size();
        if(size == 0){
            elements = EMPTY_OBJECTS;
        }else {
            elements = collection.toArray();
            if(collection.getClass() != ArrayCollection.class){
                elements = elements.clone();
            }
        }
        this.mElements = elements;
        this.size = size;
    }
    public ArrayCollection(){
        this(0);
    }

    public Monitor<T> getMonitor() {
        return mMonitor;
    }
    public void setMonitor(Monitor<T> monitor) {
        this.mMonitor = monitor;
    }

    public Initializer<T> getInitializer() {
        return mInitializer;
    }
    public void setInitializer(Initializer<T> initializer) {
        this.mInitializer = initializer;
    }

    public ArrayCollection<T> copy(){
        return new ArrayCollection<>(toArray().clone());
    }
    public ArrayCollection<T> filter(Predicate<? super T> filter){
        int count = count(filter);
        if(count == size()){
            return this;
        }
        ArrayCollection<T> collection = new ArrayCollection<>(count);
        collection.addAll(this.iterator(filter));
        return collection;
    }
    public<T1 extends T> ArrayCollection<T1> filter(Class<T1> instance){
        int count = count(instance);
        if(count == size()){
            return (ArrayCollection<T1>) this;
        }
        ArrayCollection<T1> collection = new ArrayCollection<>(count);
        collection.addAll(this.iterator(instance));
        return collection;
    }
    public int count(Predicate<? super T> filter){
        return count(filter, size());
    }
    public int count(Predicate<? super T> filter, int limit){
        int result = 0;
        int size = size();
        for(int i = 0; i < size; i++){
            if(result >= limit){
                break;
            }
            if(filter.test(get(i))){
                result ++;
            }
        }
        return result;
    }
    public int count(Class<?> instance){
        return count(instance, size());
    }
    public int count(Class<?> instance, int limit){
        int result = 0;
        int size = size();
        Object[] elements = this.mElements;
        for(int i = 0; i < size; i++){
            if(result >= limit){
                break;
            }
            Object obj = elements[i];
            if(instance.isInstance(obj)){
                result ++;
            }
        }
        return result;
    }
    public int countFromLast(Predicate<? super T> predicate){
        return countFromLast(0, predicate);
    }
    public int countFromLast(int end, Predicate<? super T> predicate) {
        if (end < 0) {
            end = 0;
        }
        int result = 0;
        int i = this.size() - 1;
        while (i >= end && predicate.test(get(i))) {
            result ++;
            i --;
        }
        return result;
    }
    @Override
    public void sort(Comparator<? super T> comparator){
        if(mLocked){
            return;
        }
        int size = size();
        if(size < 2){
            return;
        }
        ArraySort.ObjectSort sort = new ArraySort.ObjectSort(mElements, 0, size, comparator);
        if(sort.sort()){
            onChanged();
        }
    }
    public boolean sortItems(Comparator<? super T> comparator){
        if(mLocked){
            return false;
        }
        int size = size();
        if(size < 2){
            return false;
        }
        ArraySort.ObjectSort sort = new ArraySort.ObjectSort(mElements, 0, size, comparator);
        if(sort.sort()){
            onChanged();
            return true;
        }
        return false;
    }

    public boolean sort(Comparator<? super T> comparator, Swappable swappable){
        if(swappable == null) {
            throw new NullPointerException("swappable == null");
        }
        if(swappable == this) {
            throw new IllegalArgumentException("swappable == this");
        }
        return sort(comparator, SwapListener.redirectTo(swappable));
    }
    public boolean sort(Comparator<? super T> comparator, SwapListener swapListener){
        if(mLocked){
            return false;
        }
        int size = size();
        if(size < 2){
            return false;
        }
        if(swapListener == null) {
            throw new NullPointerException("swapListener == null");
        }
        ArraySort.ObjectSort sort = new ArraySort.ObjectSort(mElements, 0, size, comparator){
            @Override
            public void onSwap(int i, int j) {
                super.onSwap(i, j);
                swapListener.onSwap(i, j);
            }
        };
        if(sort.sort()){
            onChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Object obj) {
        if(obj == null){
            return false;
        }
        return containsExact(obj) || containsEquals(obj);
    }
    public boolean containsIf(Predicate<? super T> predicate) {
        return containsIf(0, predicate);
    }
    public boolean containsIf(int start, Predicate<? super T> predicate) {
        if(start < 0) {
            start = 0;
        }
        int size = this.size;
        if(size == 0) {
            return false;
        }
        Object[] elements = this.mElements;
        for(int i = start; i < size; i++){
            if(predicate.test((T)elements[i])){
                return true;
            }
        }
        return false;
    }
    public boolean containsInstance(Class<?> instance) {
        return containsInstance(0, instance);
    }
    public boolean containsInstance(int start, Class<?> instance) {
        if(start < 0) {
            start = 0;
        }
        int size = this.size;
        if(size == 0) {
            return false;
        }
        Object[] elements = this.mElements;
        for (int i = start; i < size; i++) {
            if(instance.isInstance(elements[i])){
                return true;
            }
        }
        return false;
    }
    public boolean containsEquals(Object obj) {
        return indexOf(obj) >= 0;
    }
    @Deprecated
    public boolean containsFast(Object item){
        return containsExact(item);
    }
    public boolean containsExact(Object item) {
        return indexOfExact(item) >= 0;
    }
    @Override
    public boolean isEmpty(){
        return size() == 0;
    }
    public boolean isImmutableEmpty(){
        return false;
    }

    public T getFirst(){
        if(size() == 0){
            return null;
        }
        return get(0);
    }
    public T getLast(){
        int size = size();
        if(size == 0){
            return null;
        }
        return get(size - 1);
    }
    public T getElement(Object obj){
        int i = indexOf(obj);
        if(i >= 0){
            return get(i);
        }
        return null;
    }
    @Override
    public T get(int i){
        return (T)mElements[i];
    }
    @Override
    public int size(){
        return size;
    }
    public void ensureSize(int size){
        if(size > size()){
            setSize(size);
        }
    }
    public void setSize(int size){
        setSize(size, false);
    }
    public void setSize(int size, boolean notify) {
        int start = this.size;
        if (size == start) {
            return;
        }
        if (size < start) {
            if (notify) {
                shrinkWithNotify(size);
            } else {
                this.size = size;
                onChanged();
            }
            return;
        }
        boolean locked = this.mLocked;
        this.mLocked = true;
        int length = size - start;
        ensureCapacity(length);
        this.size = size;
        fillElements(this.mElements, start, length);
        this.mLocked = locked;
        onChanged();
    }
    private void shrinkWithNotify(int targetSize) {
        boolean locked = this.mLocked;
        this.mLocked = true;
        int i = this.size - 1;
        int end = targetSize - 1;
        Object[] elements = this.mElements;
        while (i > end) {
            T item = (T) elements[i];
            this.size --;
            elements[i] = null;
            notifyRemoved(i, item);
            i--;
        }
        this.mLocked = locked;
        onChanged();
    }
    private void fillElements(Object[] elements, int start, int length){
        Initializer<T> initializer = getInitializer();
        if(initializer == null){
            return;
        }
        length = start + length;
        for(int i = start; i < length; i++){
            T item = initializer.createNewItem(i);
            elements[i] = item;
            notifyAdd(i, item);
        }
    }
    @Override
    public int getCount() {
        return size();
    }

    public<T1> Iterator<T1> iterator(Class<T1> instance){
        return InstanceIterator.of(iterator(), instance);
    }
    public Iterator<T> iterator(Predicate<? super T> filter){
        return FilterIterator.of(iterator(), filter);
    }
    @Override
    public Iterator<T> iterator() {
        return ArraySupplierIterator.of(this);
    }
    public Iterator<T> iterator(int start) {
        return iterator(start, size() - start);
    }
    public Iterator<T> iterator(int start, int length) {
        return ArraySupplierIterator.of(this, start, length);
    }
    public Iterator<T> arrayIterator() {
        return ArrayIterator.of(this.mElements, 0, size());
    }
    public Iterator<T> clonedIterator() {
        return clonedIterator(0, size());
    }
    public Iterator<T> clonedIterator(int start, int length) {
        if(isEmpty()){
            return EmptyIterator.of();
        }
        return ArrayIterator.of(this.mElements.clone(), start, length);
    }

    public Iterator<T> reversedIterator() {
        return ReversedIterator.of(this);
    }
    public Iterator<T> reversedIterator(int start) {
        return ReversedIterator.of(this, start);
    }
    public Iterator<T> reversedIterator(int start, int length) {
        return ReversedIterator.of(this, start, length);
    }

    public Iterator<T> reversedClonedIterator() {
        return ReversedIterator.of(this.mElements.clone());
    }
    public Iterator<T> reversedClonedIterator(int start) {
        return ReversedIterator.of(this.mElements.clone(), start);
    }
    public Iterator<T> reversedClonedIterator(int start, int length) {
        return ReversedIterator.of(this.mElements.clone(), start, length);
    }

    @Override
    public Object[] toArray() {
        Object[] results = trimToSize(getElements(), size());
        if (results == this.mElements) {
            results = results.clone();
        }
        return results;
    }
    public Object[] getElements() {
        return this.mElements;
    }
    public void setElements(Object[] elements) {
        setElements(elements, elements.length);
    }
    public void setElements(Object[] elements, int size) {
        this.mElements = elements;
        this.size = size;
        onChanged();
    }

    @Override
    public <T1> T1[] toArray(T1[] out) {
        int size = this.size();
        if(size == 0){
            return out;
        }
        Object[] elements = this.mElements;
        int length = out.length;
        if (length != 0 && length <= size) {
            arrayCopy(elements, out, length);
            return out;
        }
        return (T1[]) Arrays.copyOf(elements, size, out.getClass());
    }
    public <T1> T1[] toArrayFill(T1[] out) {
        Object[] elements = this.mElements;
        int length = NumbersUtil.min(out.length, this.size());
        arrayCopy(elements, out, length);
        return out;
    }

    public T removeItem(Object item){
        int i = indexOf(item);
        if (i < 0){
            return null;
        }
        T result = get(i);
        remove(i);
        onChanged();
        return result;
    }

    public ArrayCollection<T> subListIf(Predicate<? super T> predicate) {
        ArrayCollection<T> results = new ArrayCollection<>();
        results.addAll(this.iterator(predicate));
        return results;
    }
    @Override
    public ArrayCollection<T> subList(int start, int length) {
        int end = start + length;
        int size = size();
        if(end > size){
            end = size;
        }
        if(start == 0 && end == size){
            return this;
        }
        Object[] result = getNewArray(length);
        Object[] elements = this.mElements;
        for(int i = start; i < end; i ++){
            result[i] = elements[i];
        }
        return new ArrayCollection<>(result);
    }
    public ArrayCollection<T> reversedCopy() {
        Object[] elements = toArray();
        if(elements == this.mElements && elements.length != 0) {
            elements = elements.clone();
        }
        ArrayUtil.reverse(elements);
        return new ArrayCollection<>(elements);
    }

    @Override
    public int indexOf(Object item){
        return indexOf(item, 0, false);
    }


    @Deprecated
    public int indexOfFast(Object item){
        return indexOfExact(item);
    }
    public int indexOfExact(Object item){
        return indexOf(item, 0, true);
    }
    public int indexOf(Object item, int start){
        return indexOf(item, start, false);
    }
    @Deprecated
    public int indexOfFast(Object item, int start){
        return indexOf(item, start, true);
    }
    public int indexOfExact(Object item, int start){
        return indexOf(item, start, true);
    }
    public int lastIndexOf(Object item){
        return lastIndexOf(item, false);
    }

    @Override
    public Spliterator<T> spliterator() {
        throw new IllegalArgumentException("Not implemented");
    }
    @Override
    public ListIterator<T> listIterator() {
        return ListItr.of(this);
    }
    @Override
    public ListIterator<T> listIterator(int start) {
        return ListItr.of(this, start);
    }

    @Deprecated
    public int lastIndexOfFast(Object item){
        return lastIndexOfExact(item);
    }
    public int lastIndexOfExact(Object item){
        return lastIndexOf(item, true);
    }

    private int indexOf(Object item, int start, boolean fast){
        if(item == null){
            return -1;
        }
        if(start < 0){
            start = 0;
        }
        int size = this.size;
        if(size == 0){
            return -1;
        }
        Object[] elements = this.mElements;
        for(int i = start; i < size; i++){
            if(matches(item, elements[i], true)){
                return i;
            }
        }
        if(fast){
            return -1;
        }
        for(int i = start; i < size; i++){
            if(matches(item, elements[i], false)){
                return i;
            }
        }
        return -1;
    }
    private boolean matches(Object item, Object obj, boolean fast){
        if(obj == null){
            return false;
        }
        if(item == obj){
            return true;
        }
        if(fast){
            return false;
        }
        return item.equals(obj);
    }
    private int lastIndexOf(Object item, boolean exact){
        if(item == null){
            return -1;
        }
        int result = -1;
        Object[] elements = this.mElements;
        int length = this.size;
        for(int i = 0; i < length; i++){
            Object obj = elements[i];
            if(obj == null){
                continue;
            }
            if(exact){
                if(item == obj){
                    result = i;
                }
            }else if(item.equals(obj)){
                result = i;
            }
        }
        return result;
    }

    public int indexOfIf(Predicate<? super T> predicate) {
        return indexOfIf(0, predicate);
    }
    public int indexOfIf(int start, Predicate<? super T> predicate) {
        if(start < 0) {
            start = 0;
        }
        int size = this.size;
        if(size == 0) {
            return -1;
        }
        Object[] elements = this.mElements;
        for(int i = start; i < size; i++){
            if(predicate.test((T)elements[i])){
                return i;
            }
        }
        return -1;
    }
    @Override
    public boolean containsAll(Collection<?> collection) {
        for(Object obj : collection){
            if(!contains(obj)){
                return false;
            }
        }
        return !collection.isEmpty();
    }

    public void addAll(int index, T[] items){
        if(items == null){
            return;
        }
        if(size() == 0){
            setElements(items);
            return;
        }
        int size = items.length;
        ensureCapacity(size);
        for(int i = 0; i < size; i++){
            add(index + i, items[i]);
        }
    }
    public void addAll(T[] items){
        if(items == null){
            return;
        }
        addAll(size(), items);
    }
    public void addAll(Iterator<? extends T> iterator){
        if(iterator == null){
            return;
        }
        if(iterator instanceof SizedIterator){
            int size = ((SizedIterator) iterator).getRemainingSize();
            ensureCapacity(size);
        }
        while (iterator.hasNext()){
            add(iterator.next());
        }
    }
    public void addIterable(Iterable<? extends T> iterable){
        if(iterable == null){
            return;
        }
        if(iterable instanceof Collection){
            addAll((Collection<? extends T>) iterable);
            return;
        }
        addAll(iterable.iterator());
    }
    @Override
    public boolean addAll(Collection<? extends T> collection) {
        if(collection == null){
            return false;
        }
        int size = this.size();
        if(size == 0){
            Object[] elements = getNewArray(collection.toArray(), collection.size());
            setElements(elements);
            return true;
        }
        size = collection.size();
        boolean result = false;
        mLocked = true;
        for (T item : collection) {
            if (availableCapacity() == 0) {
                ensureCapacity(size);
            }
            boolean added = add(item);
            if (added) {
                result = true;
            }
        }
        mLocked = false;
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Object[] elements = this.mElements;
        if(elements == null){
            return false;
        }
        int length = this.size;
        if(length == 0){
            return false;
        }
        int result = 0;
        for(int i = 0; i < length; i++){
            Object item = elements[i];
            for(Object obj : collection) {
                if(item == obj){
                    elements[i] = null;
                    notifyRemoved(i, (T)item);
                    result ++;
                    break;
                }
            }
        }
        if(result == 0){
            return false;
        }
        if(mElements != elements) {
            throw new ConcurrentModificationException();
        }
        this.size -= result;
        if(this.size == 0){
            this.mElements = EMPTY_OBJECTS;
            return true;
        }
        Object[] update = getNewArray(this.size);
        int count = 0;
        for(int i = 0; i < length; i++){
            Object obj = elements[i];
            if(obj == null){
                continue;
            }
            update[count] = obj;
            count++;
        }
        this.size = count;
        this.mElements = update;
        return true;
    }
    public boolean removeIf(Predicate<? super T> filter){
        Object[] elements = this.mElements;
        if(elements == null){
            return false;
        }
        int length = this.size;
        if(length == 0){
            return false;
        }
        int result = 0;
        for(int i = 0; i < length; i++){
            T item = (T)elements[i];
            if(filter.test(item)) {
                elements[i] = null;
                notifyRemoved(i, item);
                result ++;
            }
        }
        if(result == 0){
            return false;
        }
        if(mElements != elements) {
            throw new ConcurrentModificationException();
        }
        this.size -= result;
        if(this.size == 0){
            this.mElements = EMPTY_OBJECTS;
            return true;
        }
        Object[] update = getNewArray(this.size);
        int count = 0;
        for(int i = 0; i < length; i++){
            Object obj = elements[i];
            if(obj == null){
                continue;
            }
            update[count] = obj;
            count++;
        }
        this.size = count;
        this.mElements = update;
        return true;
    }
    public boolean removeAllIndexes(int[] indexes) {
        Object[] elements = this.mElements;
        if(elements == null || indexes.length == 0){
            return false;
        }
        int length = this.size;
        if(length == 0){
            return false;
        }
        int result = 0;
        int indexesLength = indexes.length;
        for(int i = 0; i < indexesLength; i++){
            int j = indexes[i];
            if(j < 0 || j >= length) {
                continue;
            }
            Object item = elements[j];
            elements[j] = null;
            notifyRemoved(i, (T)item);
            result ++;
        }
        if(result == 0){
            return false;
        }
        this.size -= result;
        if(this.size == 0){
            this.mElements = EMPTY_OBJECTS;
            return true;
        }
        Object[] update = getNewArray(this.size);
        int count = 0;
        for(int i = 0; i < length; i++){
            Object obj = elements[i];
            if(obj == null){
                continue;
            }
            update[count] = obj;
            count++;
        }
        this.size = count;
        this.mElements = update;
        return true;
    }
    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new RuntimeException("Method not implemented");
    }

    /**
     * Keeps the current array for latter use
     * */
    public void clearTemporarily() {
        int size = this.size;
        this.size = 0;
        Object[] elements = this.mElements;
        this.mLocked = false;
        for(int i = 0; i < size; i++){
            elements[i] = null;
        }
        onChanged();
    }
    @Override
    public void clear() {
        int size = this.size;
        this.size = 0;
        Object[] elements = this.mElements;
        this.mElements = EMPTY_OBJECTS;
        this.mLastGrow = 0;
        this.mLocked = false;
        for(int i = 0; i < size; i++){
            elements[i] = null;
        }
        onChanged();
    }

    @Override
    public boolean remove(Object obj) {
        return removeItem(obj) != null;
    }
    @Override
    public boolean add(T item){
        if (item == null ){
            return false;
        }
        boolean locked = mLocked;
        mLocked = true;
        ensureCapacity();
        int index = this.size;
        this.mElements[index] = item;
        this.size ++;
        mLocked = locked;
        onChanged();
        notifyAdd(index, item);
        return true;
    }
    @Override
    public void add(int i, T item){
        if(item == null){
            return;
        }
        boolean locked = mLocked;
        this.mLocked = true;
        if (availableCapacity() == 0) {
            ensureCapacity(calculateGrow());
        }
        slideRight(i, 1);
        this.mElements[i] = item;
        notifyAdd(i, item);
        this.mLocked = locked;
        onChanged();
    }
    @Override
    public boolean swap(int i1, int i2){
        if(i1 == i2){
            return false;
        }
        Object[] elements = this.mElements;
        Object item = elements[i1];
        elements[i1] = elements[i2];
        elements[i2] = item;
        onChanged();
        return true;
    }
    public void move(Object obj, int to) {
        int i = indexOf(obj);
        if(i >= 0) {
            move(i, to);
        }
    }
    public void move(int from, int to){
        if(from == to || to < 0){
            return;
        }
        ensureSize(to + 1);
        boolean locked = mLocked;
        this.mLocked = true;
        Object[] elements = this.mElements;
        Object item = elements[from];
        if(from > to){
            for(int i = from; i > to; i --){
                elements[i] = elements[i - 1];
            }
        }else {
            for(int i = from; i < to; i ++){
                elements[i] = elements[i + 1];
            }
        }
        elements[to] = item;
        this.mElements = elements;
        this.mLocked = locked;
        onChanged();
    }
    @Override
    public T set(int i, T item){
        if(item == null || i < 0){
            return null;
        }
        ensureSize(i + 1);
        T existing = (T)this.mElements[i];
        this.mElements[i] = item;
        if(item != existing){
            onChanged();
        }
        return existing;
    }
    @Override
    public boolean addAll(int index, Collection<? extends T> collection){
        if(collection == null){
            return false;
        }
        int length = collection.size();
        if(length == 0){
            return false;
        }
        boolean locked = mLocked;
        this.mLocked = true;
        ensureCapacity(length);
        slideRight(index, length);
        Object[] elements = this.mElements;
        int i = index;
        for(T item : collection){
            if(item == null || containsExact(item)){
                continue;
            }
            elements[i] = item;
            notifyAdd(i, item);
            i ++;
        }
        int duplicates = length - (i - index);
        slideLeft(i, duplicates);
        this.mLocked = locked;
        return duplicates < length;
    }
    @Override
    public T remove(int index){
        T result = get(index);
        slideLeft(index, 1);
        notifyRemoved(index, result);
        return result;
    }
    public T removeSilent(int index){
        T result = get(index);
        slideLeft(index, 1);
        return result;
    }
    private void slideLeft(int position, int amount){
        if(amount == 0 || position < 0){
            return;
        }
        boolean locked = mLocked;
        this.mLocked = true;
        Object[] elements = this.mElements;
        int size = this.size;
        int length = size - amount;
        for(int i = position; i < length; i++){
            elements[i] = elements[i + amount];
        }
        for(int i = length; i < size; i++){
            elements[i] = null;
        }
        this.size = length;
        this.mLocked = locked;
        onChanged();
    }
    private void slideRight(int position, int amount){
        boolean locked = mLocked;
        this.mLocked = true;
        ensureCapacity(amount);
        Object[] elements = this.mElements;
        int size = this.size;
        int i = size - 1;
        while (i >= position){
            elements[i + amount] = elements[i];
            i--;
        }
        this.size = size + amount;
        amount = position + amount;
        for(i = position; i < amount; i++){
            elements[i] = null;
        }
        this.mLocked = locked;
        onChanged();
    }
    public void trimToSize(){
        if(mLocked || availableCapacity() == 0){
            if(mLastGrow == 0){
                mLastGrow = size() / 3;
            }
            return;
        }
        this.mElements = trimToSize(this.mElements, this.size);
        this.mLastGrow = this.size / 4;
    }

    private Object[] trimToSize(Object[] elements, int size){
        if(size >= elements.length){
            return elements;
        }
        if(size == 0){
            return getNewArray(0);
        }
        Object[] update = getNewArray(size);
        arrayCopy(elements, update, size);
        return update;
    }
    private void arrayCopy(Object[] source, Object[] destination, int length){
        for(int i = 0; i < length; i++){
            destination[i] = source[i];
        }
    }
    private Object[] getNewArray(Object[] source, int length){
        Object[] result = getNewArray(length);
        arrayCopy(source, result, length);
        return result;
    }
    private Object[] getNewArray(int length){
        if (length == 0) {
            return EMPTY_OBJECTS;
        }
        return new Object[length];
    }
    public void ensureMinCapacity(int capacity) {
        if (capacity > availableCapacity()) {
            ensureCapacity(calculateGrow(capacity));
        }
    }
    private void ensureCapacity(){
        if(availableCapacity() > 0){
            return;
        }
        ensureCapacity(calculateGrow());
    }
    public void ensureCapacity(int capacity) {
        if(capacity <= 0){
            return;
        }
        if(capacity - availableCapacity() <= 0){
            return;
        }
        int size = this.size;
        int length = size + capacity;
        Object[] update = getNewArray(length);
        Object[] elements = this.mElements;
        if(elements.length == 0 || size == 0){
            this.mElements = update;
            return;
        }
        arrayCopy(elements, update, size);
        this.mElements = update;
    }
    public int availableCapacity(){
        return this.mElements.length - size;
    }
    private int calculateGrow() {
        return calculateGrow(0);
    }
    private int calculateGrow(int min) {
        if (this.size == 0) {
            return NumbersUtil.max(min, 1);
        }
        int amount = this.mLastGrow;
        if(amount >= GROW_LIMIT){
            return NumbersUtil.max(min, amount);
        }
        if(amount == 0){
            amount = 1;
        }
        amount = amount << 1;
        if(amount > 32){
            amount = amount << 1;
        }
        if(amount > 32 && amount < 256){
            amount = amount << 1;
        }
        if(amount > GROW_LIMIT){
            amount = GROW_LIMIT;
        }
        if (min > amount) {
            return min;
        }
        this.mLastGrow = amount;
        if(this.size < 4){
            amount = 1;
        }
        return amount;
    }

    public void onChanged(){
        mHashCode = 0;
    }
    private void notifyAdd(int i, T item){
        Monitor<T> monitor = getMonitor();
        if(monitor != null){
            monitor.onAdd(i, item);
        }
    }
    private void notifyRemoved(int i, T item){
        Monitor<T> monitor = getMonitor();
        if(monitor != null){
            monitor.onRemoved(i, item);
        }
    }
    @Override
    public int hashCode(){
        if(mHashCode != 0){
            return mHashCode;
        }
        this.mHashCode = computeHashCode();
        return mHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ArrayCollection<?> collection = (ArrayCollection<?>) obj;
        int size = this.size();
        if(size != collection.size() || hashCode() != collection.hashCode()){
            return false;
        }
        for(int i = 0; i < size; i++){
            if(!Objects.equals(get(i), collection.get(i))){
                return false;
            }
        }
        return true;
    }

    public int computeHashCode(){
        int size = size();
        if(size == 0){
            return 0;
        }
        int hashSum = 1;
        Object[] elements = this.mElements;
        for(int i = 0; i < size; i++){
            Object obj = elements[i];
            int hash = obj == null ? 0 : obj.hashCode();
            hashSum = 31 * hashSum + hash;
        }
        this.mHashCode = hashSum;
        return hashSum;
    }
    @Override
    public String toString() {
        if(size() == 0){
            return "EMPTY";
        }
        return size() + "{" + get(0) + "}";
    }
    public static<T> ArrayCollection<T> of(Iterable<? extends T> iterable){
        if(iterable == null){
            return empty();
        }
        ArrayCollection<T> collection = new ArrayCollection<>();
        collection.addIterable(iterable);
        collection.trimToSize();
        return collection;
    }
    public static<T> ArrayCollection<T> of(Iterator<? extends T> iterator){
        ArrayCollection<T> collection = new ArrayCollection<>();
        collection.addAll(iterator);
        collection.trimToSize();
        return collection;
    }
    public static<T> ArrayCollection<T> empty(){
        return (ArrayCollection<T>) EMPTY;
    }

    static final Object[] EMPTY_OBJECTS = new Object[0];

    private static final int GROW_LIMIT = 8192;

    private static final ArrayCollection<?> EMPTY = new ArrayCollection<Object>(){
        @Override
        public Object[] toArray() {
            return EMPTY_OBJECTS;
        }
        @Override
        public void ensureCapacity(int capacity) {
        }
        @Override
        public void trimToSize() {
        }
        @Override
        public void addAll(Iterator<?> iterator) {
            throw new IllegalArgumentException("Empty ArrayCollection!");
        }
        @Override
        public boolean contains(Object obj) {
            return false;
        }
        @Override
        public boolean containsAll(Collection<?> collection) {
            return false;
        }
        @Override
        public void clearTemporarily(){
        }
        @Override
        public void clear() {
        }
        @Override
        public boolean addAll(Collection<?> collection) {
            throw new IllegalArgumentException("Empty ArrayCollection!");
        }
        @Override
        public boolean add(Object item) {
            throw new IllegalArgumentException("Empty ArrayCollection!");
        }
        @Override
        public Object set(int i, Object item) {
            throw new IllegalArgumentException("Empty ArrayCollection!");
        }
        @Override
        public void add(int i, Object item) {
            throw new IllegalArgumentException("Empty ArrayCollection!");
        }
        @Override
        public Iterator<Object> iterator() {
            return EmptyIterator.of();
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
        public boolean isImmutableEmpty(){
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return false;
        }
        @Override
        public Object remove(int index) {
            return null;
        }
        @Override
        public boolean remove(Object obj) {
            return false;
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public void sort(Comparator<? super Object> comparator) {
        }
        @Override
        public void setSize(int size, boolean notify) {
        }
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public boolean equals(Object obj) {
            if(obj == this){
                return true;
            }
            if(obj instanceof Collection){
                return ((Collection<?>) obj).size() == 0;
            }
            return false;
        }
    };

    public interface Initializer<T1> {
        T1 createNewItem(int index);
    }
    public interface Monitor<T> {
        void onAdd(int i, T item);
        void onRemoved(int i, T item);
    }
}
