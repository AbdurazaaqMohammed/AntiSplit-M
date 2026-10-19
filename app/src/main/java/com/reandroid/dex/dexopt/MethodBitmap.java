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
package com.reandroid.dex.dexopt;

import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.common.ArraySupplier;
import com.reandroid.dex.model.DexFile;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.collection.ArraySupplierIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;


public class MethodBitmap extends FixedBlockContainer
        implements LinkableProfileItem, JSONConvert<JSONArray> {

    private final IntegerReference countReference;
    private final BooleanList startupList;
    private final BooleanList postStartupList;

    public MethodBitmap(IntegerReference countReference) {
        super(2);
        this.countReference = countReference;
        this.startupList = new BooleanList(countReference);
        this.postStartupList = new BooleanList(countReference);

        addChild(0, startupList);
        addChild(1, postStartupList);
    }

    public int size() {
        return startupList.size();
    }
    public void setSize(int size) {
        startupList.setSize(size);
        postStartupList.setSize(size);
        countReference.set(size);
    }
    public void ensureSize(int size) {
        if (size > size()) {
            setSize(size);
        }
    }
    public MethodBitmapElement get(int i) {
        int size = size();
        if (i < 0 || i >= size) {
            return null;
        }
        return new MethodBitmapElement(startupList.get(i), postStartupList.get(i));
    }
    public MethodBitmapElement getOrCreate(int id) {
        ensureSize(id + 1);
        return get(id);
    }
    public Iterator<MethodBitmapElement> iterator() {
        return ArraySupplierIterator.of(new ArraySupplier<MethodBitmapElement>() {
            @Override
            public MethodBitmapElement get(int i) {
                return MethodBitmap.this.get(i);
            }
            @Override
            public int getCount() {
                return MethodBitmap.this.size();
            }
        });
    }
    public void moveTo(MethodBitmapElement element, int position) {
        ensureSize(position + 1);
        startupList.moveTo(element.startup(), position);
        postStartupList.moveTo(element.postStartup(), position);
    }
    public boolean remove(int i) {
        if (startupList.remove(i) != null) {
            postStartupList.remove(i);
            updateCountReference();
            return true;
        }
        return false;
    }
    public boolean remove(MethodBitmapElement element) {
        if (startupList.remove(element.startup())) {
            postStartupList.remove(element.postStartup());
            updateCountReference();
            return true;
        }
        return false;
    }
    public void removeIf(Predicate<? super MethodBitmapElement> predicate) {
        int size = size();
        for (int i = 0; i < size; i++) {
            if (predicate.test(get(i)) && remove(i)) {
                i --;
                size = size();
            }
        }
    }
    private void updateCountReference() {
        countReference.set(size());
    }

    @Override
    public void link(DexFile dexFile) {
        LinkableProfileItem.linkAll(dexFile, iterator());
    }
    @Override
    public void update(DexFile dexFile) {
        update(dexFile, false);
    }
    public void update(DexFile dexFile, boolean initialize) {
        if (initialize) {
            setSize(dexFile.getCount(SectionType.METHOD_ID));
            link(dexFile);
        } else {
            LinkableProfileItem.updateAll(dexFile, iterator());
        }
    }

    @Override
    public int countBytes() {
        return BitItem.bitsToBytes(size() * 2);
    }

    @Override
    public byte[] getBytes() {
        int size = size();
        if (size == 0) {
            return new byte[0];
        }
        BitItem pool = new BitItem(size * 2);
        int offset = 0;
        offset = startupList.writeTo(pool, offset);
        postStartupList.writeTo(pool, offset);
        return pool.getBytes();
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        BooleanList startupList = this.startupList;
        BooleanList postStartupList = this.postStartupList;
        int itemsCount = this.countReference.get();
        startupList.setSize(itemsCount);
        postStartupList.setSize(itemsCount);
        BitItem pool = new BitItem(itemsCount * 2);
        pool.onReadBytes(reader);
        int offset = 0;
        offset = startupList.readFrom(pool, offset);
        postStartupList.readFrom(pool, offset);
    }
    @Override
    public int onWriteBytes(OutputStream stream) throws IOException {
        byte[] bytes = getBytes();
        int length = bytes.length;
        stream.write(bytes, 0, length);
        return length;
    }
    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        updateCountReference();
    }

    @Override
    public JSONArray toJson() {
        int size = size();
        JSONArray jsonArray = new JSONArray(size);
        for (int i = 0; i < size; i++) {
            MethodBitmapElement element = get(i);
            if (element.getFlags() != 0) {
                jsonArray.put(element.toJson());
            }
        }
        return jsonArray;
    }

    @Override
    public void fromJson(JSONArray json) {
        int size = json == null ? 0 : json.length();
        ensureSize(size);
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = json.getJSONObject(i);
            int id = jsonObject.getInt("id");
            getOrCreate(id).fromJson(jsonObject);
        }
    }
    @Override
    public String toString() {
        return "size=" + size();
    }

}
