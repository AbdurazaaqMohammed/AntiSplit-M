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
import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.base.DirectStreamReader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;

import java.io.IOException;
import java.io.InputStream;
import com.reandroid.utils.function.Predicate;

public class CountedBlockList<T extends Block> extends BlockList<T> implements DirectStreamReader {

    private final IntegerReference countReference;

    public CountedBlockList(Creator<? extends T> creator, IntegerReference countReference) {
        super(creator);
        this.countReference = countReference;
    }

    public IntegerReference getCountReference() {
        return countReference;
    }

    @Override
    protected void onRefreshed() {
        updateCountReference();
        super.onRefreshed();
    }

    protected void updateCountReference() {
        getCountReference().set(size());
    }

    @Override
    protected void onReadBytes(BlockReader reader) throws IOException {
        setSize(getCountReference().get());
        readChildes(reader);
    }

    @Override
    public int readBytes(InputStream inputStream) throws IOException, ClassCastException {
        int size = getCountReference().get();
        setSize(size);
        int result = 0;
        for (int i = 0; i < size; i++) {
            result += ((DirectStreamReader)get(i)).readBytes(inputStream);
        }
        return result;
    }
}
