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
package com.reandroid.arsc.base;

import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.common.ArraySupplier;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.collection.FilterIterator;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class BlockArray<T extends Block> extends BlockList<T>
        implements Creator<T>, ArraySupplier<T> {

    public BlockArray(){
        super();
        setCreator(this);
    }
    public BlockArray(Creator<? extends T> creator){
        super(creator);
    }

    @Override
    protected void onReadBytes(BlockReader reader) throws IOException {
        super.readChildes(reader);
    }

    @Override
    public void setSize(int size) {
        int max = size();
        for(int i = size; i < max; i ++) {
            onPreRemove(get(i));
        }
        super.setSize(size);
    }

    public void removeAllNull(int start){
        int lastCount = size() - start;
        if(lastCount <= 0) {
            return;
        }
        int count = NumbersUtil.min(lastCount, countFromLast(nullPredicate()));
        setSize(size() - count);
    }
    public Iterable<T> listItems(){
        return listItems(false);
    }
    public Iterable<T> listItems(boolean skipNullBlocks){
        return () -> BlockArray.this.iterator(skipNullBlocks);
    }
    public final int countNonNull(){
        return countIf(nonNullPredicate());
    }
    public int indexOf(Object block){
        int i = 0;
        Iterator<T> iterator = iterator();
        while(iterator.hasNext()){
            T item = iterator.next();
            if(block == item){
                return i;
            }
            i ++;
        }
        return -1;
    }
    public int lastIndexOf(Object block){
        int result = -1;
        int i = 0;
        Iterator<T> iterator = iterator();
        while(iterator.hasNext()){
            T item = iterator.next();
            if(block==item){
                result=i;
            }
            i++;
        }
        return result;
    }
    public Iterator<T> iterator(boolean skipNullBlock) {
        if(skipNullBlock) {
            return iterator(nonNullPredicate());
        }
        return super.iterator();
    }
    public Iterator<T> iterator(boolean skipNullBlock, int start, int size) {
        Iterator<T> iterator = super.iterator(start, size);
        if(skipNullBlock) {
            iterator = FilterIterator.of(iterator, nonNullPredicate());
        }
        return iterator;
    }
    public void clear() {
        super.clearChildes();
    }
    public void onPreRemove(T block){
        block.setParent(null);
        block.setIndex(-1);
    }
    private Predicate<? super T> nullPredicate() {
        return Block::isNull;
    }
    private Predicate<? super T> nonNullPredicate() {
        return block -> !block.isNull();
    }
}
