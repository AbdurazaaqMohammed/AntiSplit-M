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

import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.model.DexFile;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ComputeIterator;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class ProfileBody extends DeflatedBlockContainer
        implements LinkableProfileItem, JSONConvert<JSONArray> {

    private final ProfileVersion version;

    public ProfileBody(int childesCount,
                       ProfileVersion version,
                       IntegerReference sizeUncompressed,
                       IntegerReference sizeCompressed) {
        super(childesCount, version.isDeflatedBody(), sizeUncompressed, sizeCompressed);
        this.version = version;
    }

    public ProfileVersion version() {
        return version;
    }
    public abstract int size();
    public abstract void setSize(int size);
    public abstract ProfileData get(int i);
    public ProfileData get(String name) {
        int size = size();
        for (int i = 0; i < size; i++) {
            ProfileData data = get(i);
            if (ObjectsUtil.equals(data.getName(), name)) {
                return data;
            }
        }
        return null;
    }
    public abstract Iterator<? extends ProfileData> iterator();
    public abstract boolean removeIfName(Predicate<String> predicate);
    public abstract boolean removeData(int i);
    public boolean removeData(String name) {
        return removeIfName(s -> ObjectsUtil.equals(s, name));
    }
    public ProfileData createNew() {
        int size = size();
        setSize(size + 1);
        return get(size);
    }

    @Override
    public void link(DexFile dexFile) {
        ProfileData data = get(dexFile.getSimpleName());
        if (data != null) {
            data.link(dexFile);
        }
    }
    @Override
    public void update(DexFile dexFile) {
        ProfileData data = get(dexFile.getSimpleName());
        if (data != null) {
            data.update(dexFile);
        }
    }
    @Override
    public JSONArray toJson() {
        int size = size();
        JSONArray jsonArray = new JSONArray(size);
        for (int i = 0; i < size; i++) {
            jsonArray.put(get(i).toJson());
        }
        return jsonArray;
    }

    @Override
    public void fromJson(JSONArray json) {
        int size = json == null ? 0 : json.length();
        setSize(size);
        for (int i = 0; i < size; i++) {
            get(i).fromJson(json.getJSONObject(i));
        }
    }

    @Override
    public String toString() {
        return "size = " + size() + " [" + StringsUtil.join(
                ComputeIterator.of(iterator(), ProfileData::getName), ", ") + "]";
    }
}
