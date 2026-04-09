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

import com.reandroid.archive.block.ApkSignatureBlock;
import com.reandroid.archive.block.CertificateBlock;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.coder.ValueCoder;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.dex.model.DexDirectory;
import com.reandroid.dex.model.DexFile;
import com.reandroid.utils.HexUtil;
import com.reandroid.arsc.value.*;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public abstract class InfoWriter implements Closeable {
    private final Writer writer;
    public InfoWriter(Writer writer){
        this.writer = writer;
    }

    public void writeSignatureInfo(ApkSignatureBlock signatureBlock, boolean base64) throws IOException {
        if(signatureBlock == null){
            writeNameValue("certificates", "null");
        }else {
            writeCertificates(CollectionUtil.toList(signatureBlock.getCertificates()), base64);
        }
    }
    public void writeResources(PackageBlock packageBlock, List<String> typeFilters, boolean writeEntries) throws IOException {
        Iterator<ResourceEntry> itr = packageBlock.getResources();
        while (itr.hasNext()){
            ResourceEntry resourceEntry = itr.next();
            writeResources(resourceEntry, writeEntries);
        }
    }
    public void writeDexInfo(DexDirectory dexDirectory) throws IOException {
        for(DexFile dexFile : dexDirectory){
            writeDexInfo(dexFile, true);
        }
    }

    public abstract void writeStringPool(String source, StringPool<?> stringPool) throws IOException;
    public abstract void writeXmlDocument(String sourcePath, ResXmlDocument xmlDocument) throws IOException;
    public abstract void writeCertificates(List<CertificateBlock> certificateList, boolean base64) throws IOException;
    public abstract void writeDexInfo(DexFile dexFile, boolean writeSectionInfo) throws IOException;
    public abstract void writeResources(ResourceEntry resourceEntry, boolean writeEntries) throws IOException;
    public abstract void writePackageNames(Collection<PackageBlock> packageBlocks) throws IOException;
    public abstract void writeEntries(String name, List<Entry> entryList) throws IOException;
    public abstract void writeArray(String name, Object[] values) throws IOException;
    public abstract void writeNameValue(String name, Object value) throws IOException;
    public abstract void flush() throws IOException;
    boolean contains(SpecTypePair specTypePair, List<String> filterList){
        if(filterList.size() == 0){
            return true;
        }
        return filterList.contains(specTypePair.getTypeName());
    }
    public Writer getWriter() {
        return writer;
    }
    @Override
    public void close() throws IOException{
        this.writer.close();
    }

    static String toString(Object obj){
        if(obj != null){
            return obj.toString();
        }
        return null;
    }
    static String getValueAsString(Value value){
        ValueType valueType = value.getValueType();
        if(valueType == ValueType.STRING){
            return value.getValueAsString();
        }
        String decoded = ValueCoder.decode(valueType, value.getData());
        if(decoded != null){
            return decoded;
        }
        if(valueType == ValueType.ATTRIBUTE){
            return HexUtil.toHex8("?0x", value.getData());
        }
        if(valueType == ValueType.REFERENCE){
            return HexUtil.toHex8("@0x", value.getData());
        }
        return HexUtil.toHex8("0x", value.getData());
    }
    static String toBase64(byte[] bytes) {
        return android.Base64.encodeToString(bytes, 0);
    }

    static void writeSpaces(Writer writer, int amount) throws IOException {
        for (int i = 0; i < amount; i ++) {
            writer.append(' ');
        }
    }

    static final String TAG_RES_PACKAGES = "resource-packages";
    static final String TAG_PUBLIC = "public";
    static final String TAG_RESOURCES = "resources";
    static final String NAME_RESOURCE = "resource";
    static final String TAG_CONFIG = "config";
    static final String TAG_VALUE = "value";
    static final String TAG_BAG = "bag";
    static final String TAG_ITEM = "item";
    static final String NAME_QUALIFIERS = "qualifiers";

}
