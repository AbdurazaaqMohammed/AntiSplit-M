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

import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.dex.model.DexFile;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class ProfileFile extends FixedBlockContainer
        implements LinkableProfileItem, JSONConvert<JSONObject> {

    public ProfileFile(int childesCount) {
        super(childesCount);
    }

    public abstract ProfileMagic magic();
    public abstract ProfileVersion version();
    public abstract ProfileBody body();

    public ProfileData get(String name) {
        return body().get(name);
    }
    public ProfileData getOrCreate(String name) {
        ProfileData data = get(name);
        if (data == null) {
            data = body().createNew();
            data.setName(name);
        }
        return data;
    }
    public Iterator<? extends ProfileData> iterator() {
        return body().iterator();
    }
    public boolean removeIfName(Predicate<String> predicate) {
        return body().removeIfName(predicate);
    }
    public boolean removeData(String name) {
        return body().removeData(name);
    }

    public void syncApk(ZipEntryMap zipEntryMap) {
        removeIfName(name -> !zipEntryMap.contains(name));
        Iterator<InputSource> iterator = zipEntryMap.iteratorWithPath(
                path -> DexFile.getDexFileNumber(path) >= 0);
        while (iterator.hasNext()) {
            InputSource inputSource = iterator.next();
            getOrCreate(inputSource.getAlias());
        }
    }
    public void ensureDex(DexFile dexFile) {
        String name = dexFile.getSimpleName();
        if (StringsUtil.isEmpty(name)) {
            throw new IllegalArgumentException("Unnamed dex: " + dexFile);
        }
        ProfileData data = get(name);
        if (data != null) {
            return;
        }
        data = getOrCreate(name);
        data.update(dexFile);
    }
    @Override
    public void link(DexFile dexFile) {
        body().link(dexFile);
    }
    @Override
    public void update(DexFile dexFile) {
        body().update(dexFile);
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        body().refresh();
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("magic", magic().get());
        jsonObject.put("version", version().name());
        jsonObject.put("body", body().toJson());
        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject json) {
        magic().set(json.getInt("magic"));
        version().name(json.getString("version"));
        body().fromJson(json.getJSONArray("body"));
    }

    @Override
    public byte[] getBytes() {
        BytesOutputStream outputStream = new BytesOutputStream();
        try {
            writeBytes(outputStream);
            outputStream.close();
        } catch (IOException ignored) {
        }
        return outputStream.toByteArray();
    }

    public void write(File file) throws IOException {
        OutputStream outputStream = FileUtil.outputStream(file);
        writeBytes(outputStream);
        outputStream.close();
    }

    @Override
    public String toString() {
        return "magic=" + magic() +
                ", version=" + version() +
                ", body=" + body() ;
    }

    public static final String DECODE_DIR_NAME = ObjectsUtil.of("dexopt");

    public static final String NAME_PROF = ObjectsUtil.of("baseline.prof");
    public static final String NAME_PROFM = ObjectsUtil.of("baseline.profm");

    public static final String JSON_NAME_PROF = ObjectsUtil.of("baseline.prof.json");
    public static final String JSON_NAME_PROFM = ObjectsUtil.of("baseline.profm.json");

    public static final String PATH_PROF = ObjectsUtil.of("assets/dexopt/baseline.prof");
    public static final String PATH_PROFM = ObjectsUtil.of("assets/dexopt/baseline.profm");
}
