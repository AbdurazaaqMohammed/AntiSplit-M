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
package com.reandroid.apkeditor.info;

import com.reandroid.archive.block.CertificateBlock;
import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResTableMapEntry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.dex.model.DexFile;
import com.reandroid.dex.sections.Marker;
import com.reandroid.json.JSONObject;
import com.reandroid.json.JSONWriter;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class InfoWriterJson extends InfoWriter{
    private final JSONWriter mJsonWriter;
    private final JSONObject mJsonObject;

    public InfoWriterJson(Writer writer) {
        super(writer);
        JSONWriter jsonWriter = new JSONWriter(writer);
        jsonWriter = jsonWriter.array();
        JSONObject jsonObject = new JSONObject();
        this.mJsonWriter = jsonWriter;
        this.mJsonObject = jsonObject;
    }

    @Override
    public void writeStringPool(String source, StringPool<?> stringPool) throws IOException {
        JSONObject stringPoolObject = new JSONObject();
        stringPoolObject.put("source", source);
        stringPoolObject.put("count", stringPool.size());
        stringPoolObject.put("styles", stringPool.countStyles());
        stringPoolObject.put("sorted", stringPool.getHeaderBlock().isSorted());
        stringPoolObject.put("utf8", stringPool.isUtf8());
        stringPoolObject.put("bytes", stringPool.getHeaderBlock().getChunkSize());
        stringPoolObject.put("strings", stringPool.toJson());
        mJsonObject.put("string_pool", stringPoolObject);
    }
    @Override
    public void writeXmlDocument(String sourcePath, ResXmlDocument xmlDocument) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object();
        jsonWriter.key("source_path").value(sourcePath);
        jsonWriter.key("document").value(xmlDocument.toJson());
        jsonWriter.endObject();
    }

    @Override
    public void writeCertificates(List<CertificateBlock> certificateList, boolean base64) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("certificates").array();
        for(CertificateBlock certificateBlock : certificateList){
            JSONObject jsonObject = certificateBlock.toJson();
            if(base64){
                jsonObject.put("base64", toBase64(certificateBlock.getCertificateBytes()));
            }
            jsonWriter.value(jsonObject);
        }
        jsonWriter.endArray().endObject();
    }

    @Override
    public void writeDexInfo(DexFile dexFile, boolean writeSectionInfo) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("name").value(dexFile.getFileName())
                .key("version").value(dexFile.getVersion())
                .key("markers").array();
        List<Marker> markersList = CollectionUtil.toList(dexFile.getMarkers());
        for(Marker marker : markersList){
            jsonWriter.value(marker.getJsonObject());
        }
        jsonWriter.endArray().endObject();
    }
    @Override
    public void writeResources(PackageBlock packageBlock, List<String> typeFilters, boolean writeEntries) throws IOException {
        packageBlock.sortTypes();
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(packageBlock.getId())
                .key("package").value(packageBlock.getName())
                .key("types").array();

        for(SpecTypePair specTypePair : packageBlock.listSpecTypePairs()){
            if(!contains(specTypePair, typeFilters)){
                continue;
            }
            writeResources(specTypePair, writeEntries);
        }
        jsonWriter.endArray()
                .endObject();
    }
    public void writeResources(SpecTypePair specTypePair, boolean writeEntries) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(specTypePair.getId())
                .key("type").value(specTypePair.getTypeName())
                .key("entries").array();

        Iterator<ResourceEntry> iterator = specTypePair.getResources();
        while (iterator.hasNext()){
            ResourceEntry resourceEntry = iterator.next();
            writeResources(resourceEntry, writeEntries);
        }
        jsonWriter.endArray().endObject();
    }
    @Override
    public void writeResources(ResourceEntry resourceEntry, boolean writeEntries) throws IOException {
        if(resourceEntry.isEmpty()){
            return;
        }
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(resourceEntry.getResourceId())
                .key("type").value(resourceEntry.getType())
                .key("name").value(resourceEntry.getName());
        if(writeEntries){
            jsonWriter.key("configs");
            writeEntries(resourceEntry);
        }
        jsonWriter.endObject();
    }

    public void writeEntries(ResourceEntry entryList) throws IOException {
        JSONWriter jsonWriter = mJsonWriter.array();
        for(Entry entry : entryList){
            writeEntry(entry);
        }
        jsonWriter.endArray();
    }
    public void writeEntry(Entry entry) throws IOException {
        if(entry.isComplex()){
            writeBagEntry(entry);
        }else {
            writeResEntry(entry);
        }
    }
    private void writeResEntry(Entry entry) throws IOException {
        ResValue resValue = entry.getResValue();
        if(resValue == null){
            return;
        }
        mJsonWriter.object()
                .key(NAME_QUALIFIERS).value(entry.getResConfig().getQualifiers())
                .key("value").value(getValueAsString(resValue))
                .endObject();
    }
    private void writeBagEntry(Entry entry) {
        ResValueMapArray mapArray = entry.getResValueMapArray();
        JSONWriter jsonWriter = mJsonWriter.object()
                .key(NAME_QUALIFIERS).value(entry.getResConfig().getQualifiers())
                .key("size").value(mapArray.size())
                .key("parent").value(((ResTableMapEntry)entry.getTableEntry()).getParentId())
                .key(TAG_BAG).array();
        for(ResValueMap resValueMap : mapArray.getChildes()){
            writeValueMap(resValueMap);
        }
        jsonWriter.endArray()
                .endObject();
    }
    private void writeValueMap(ResValueMap resValueMap){
        mJsonWriter.object()
                .key("name").value(resValueMap.decodeName())
                .key("id").value(resValueMap.getNameId())
                .key("value").value(getValueAsString(resValueMap))
                .endObject();
    }
    @Override
    public void writePackageNames(Collection<PackageBlock> packageBlocks) throws IOException {
        if(packageBlocks == null || packageBlocks.size() == 0){
            return;
        }
        JSONWriter jsonWriter = mJsonWriter.object()
                .key(TAG_RES_PACKAGES).array();

        for(PackageBlock packageBlock : packageBlocks){
            jsonWriter.object()
                    .key("id").value(packageBlock.getId())
                    .key("name").value(packageBlock.getName())
                    .endObject();
        }
        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeEntries(String name, List<Entry> entryList) throws IOException {
        if(entryList == null || entryList.size() == 0){
            return;
        }
        Entry first = entryList.get(0);
        JSONWriter jsonWriter = mJsonWriter.object()
                .key("id").value(first.getResourceId())
                .key("type").value(first.getTypeName())
                .key("name").value(first.getName())
                .key("entries")
                .array();

        for(Entry entry : entryList){
            writeEntry(entry);
        }
        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeArray(String name, Object[] values) throws IOException {

        JSONWriter jsonWriter = mJsonWriter.object()
                .key(name)
                .array();

        for(Object value:values){
            jsonWriter.value(value);
        }

        jsonWriter.endArray()
                .endObject();
    }
    @Override
    public void writeNameValue(String name, Object value) throws IOException {
        mJsonObject.put(name, value);
        getWriter().flush();
    }

    @Override
    public void flush() throws IOException {
        Writer writer = getWriter();
        if (!mJsonObject.isEmpty()) {
            mJsonWriter.value(mJsonObject);
        }
        mJsonWriter.endArray();
        writer.write("\n");
        writer.flush();
    }
}
