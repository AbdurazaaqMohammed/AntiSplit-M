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
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlNode;
import com.reandroid.arsc.item.StringItem;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.dex.model.DexFile;
import com.reandroid.dex.model.DexLayout;
import com.reandroid.dex.model.DexSectionInfo;
import com.reandroid.dex.sections.Marker;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeList;
import com.reandroid.xml.XMLUtil;
import com.reandroid.xml.kxml2.KXmlSerializer;
import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.utils.HexUtil;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResTableMapEntry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ResValueMap;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class InfoWriterXml extends InfoWriter{

    private KXmlSerializer mSerializer;
    private int mIndent;

    public InfoWriterXml(Writer writer) {
        super(writer);
    }

    @Override
    public void writeStringPool(String source, StringPool<?> stringPool) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        indent = mIndent + 2;
        mIndent = indent;
        serializer.startTag(null, "string-pool");
        serializer.attribute(null, "source", source);
        serializer.attribute(null, "count", Integer.toString(stringPool.size()));
        serializer.attribute(null, "styles", Integer.toString(stringPool.countStyles()));
        serializer.attribute(null, "sorted", String.valueOf(stringPool.getHeaderBlock().isSorted()));
        serializer.attribute(null, "utf8", String.valueOf(stringPool.isUtf8()));
        serializer.attribute(null, "bytes", String.valueOf(stringPool.getHeaderBlock().getChunkSize()));

        int size = stringPool.size();
        for (int i = 0; i < size; i++ ) {
            StringItem item = stringPool.get(i);
            writeIndent(serializer, indent);
            serializer.startTag(null, "string");
            serializer.attribute(null, "id", Integer.toString(item.getIndex()));
            item.serializeText(serializer);
            serializer.endTag(null, "string");
        }

        indent = indent - 2;
        writeIndent(serializer, indent);
        serializer.endTag(null, "string-pool");
        serializer.flush();
    }
    @Override
    public void writeXmlDocument(String sourcePath, ResXmlDocument xmlDocument) throws IOException {
        KXmlSerializer serializer = getSerializer();
        serializer.flush();
        boolean decode = false;
        if (xmlDocument.getPackageBlock() != null) {
            decode = true;
        }
        Writer writer = getWriter();
        writer.write("\n");
        String name = "document";
        serializer.startTag(null, name);
        serializer.attribute(null, "source-path", sourcePath);
        XmlSerializer documentSerializer = newSerializer();
        Iterator<ResXmlNode> iterator = xmlDocument.iterator();
        while (iterator.hasNext()) {
            ResXmlNode xmlNode = iterator.next();
            xmlNode.serialize(documentSerializer, decode);
        }
        documentSerializer.flush();
        writer.write("\n");
        serializer.endTag(null, name);
    }

    @Override
    public void writeCertificates(List<CertificateBlock> certificateList, boolean base64) throws IOException {
        List<String> infoList = new ComputeList<>(certificateList, certificateBlock -> {
            String value = certificateBlock.printCertificate();
            if(base64) {
                value = value + "\n" + toBase64(certificateBlock.getCertificateBytes());
            }
            return value;
        });
        writeArray("certificates", infoList.toArray());
    }
    @Override
    public void writeDexInfo(DexFile dexFile, boolean writeSectionInfo) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, "dex");

        serializer.attribute(null, "name", dexFile.getFileName());
        serializer.attribute(null, "version", Integer.toString(dexFile.getVersion()));
        if (dexFile.isMultiLayout()) {
            int size = dexFile.size();
            serializer.attribute(null, "layouts", Integer.toString(size));
            for (int i = 0; i < size; i++) {
                writeDexLayout(serializer, dexFile.getLayout(i));
            }
        } else if (dexFile.size() != 0) {
            writeDexLayout(serializer, dexFile.getFirst());
        } else {
            serializer.text("EMPTY DEX");
        }
        writeIndent(serializer, indent);
        serializer.endTag(null, "dex");
        indent = mIndent - 2;
        mIndent = indent;
    }
    private void writeDexLayout(KXmlSerializer serializer, DexLayout layout) throws IOException {
        int indent = mIndent + 2;
        mIndent = indent;
        boolean tagOpened = false;
        if (layout.isMultiLayoutEntry()) {
            writeIndent(serializer, indent);
            serializer.startTag(null, "layout");
            indent = mIndent + 2;
            mIndent = indent;
            tagOpened = true;
            serializer.attribute(null, "name", layout.getName());
            serializer.attribute(null, "version", Integer.toString(layout.getVersion()));
        }
        List<Marker> markersList = CollectionUtil.toList(layout.getMarkers());
        if (markersList.size() != 0) {
            writeArray("markers", markersList.toArray());
        }
        writeSectionInfo(serializer, layout);
        indent = indent - 2;
        this.mIndent = indent;
        if (tagOpened) {
            writeIndent(serializer, indent);
            serializer.endTag(null, "layout");
            indent = indent - 2;
            this.mIndent = indent;
        }
    }
    private void writeSectionInfo(KXmlSerializer serializer, DexLayout layout) throws IOException {
        Iterator<DexSectionInfo> iterator = layout.getSectionInfo();
        if (!iterator.hasNext()) {
            return;
        }
        int indent = this.mIndent;
        writeIndent(serializer, indent);
        serializer.startTag(null, "dex-sections");
        while (iterator.hasNext()) {
            DexSectionInfo sectionInfo = iterator.next();
            writeIndent(serializer, indent + 2);
            serializer.startTag(null, "section");
            serializer.attribute(null, "name", sectionInfo.getSectionType().getName());
            serializer.attribute(null, "count", Integer.toString(sectionInfo.getCount()));
            serializer.attribute(null, "offset", Integer.toString(sectionInfo.getOffset()));
            serializer.endTag(null, "section");
        }
        this.mIndent = indent;
        writeIndent(serializer, indent);
        serializer.endTag(null, "dex-sections");
    }
    @Override
    public void writeResources(PackageBlock packageBlock, List<String> typeFilters, boolean writeEntries) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, "package");
        serializer.attribute(null, "id", HexUtil.toHex2((byte) packageBlock.getId()));
        serializer.attribute(null, "name", packageBlock.getName());
        packageBlock.sortTypes();

        for(SpecTypePair specTypePair : packageBlock.listSpecTypePairs()){
            if(!contains(specTypePair, typeFilters)){
                continue;
            }
            writeResources(specTypePair, writeEntries);
        }

        writeIndent(serializer, indent);
        indent = indent - 2;
        mIndent = indent;
        serializer.endTag(null, "package");
    }
    public void writeResources(SpecTypePair specTypePair, boolean writeEntries) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, "type");

        serializer.attribute(null, "name",
                specTypePair.getTypeName());
        serializer.attribute(null, "id",
                Integer.toString(specTypePair.getId()));
        serializer.attribute(null, "entryCount",
                Integer.toString(specTypePair.getHighestEntryCount()));

        Iterator<ResourceEntry> iterator = specTypePair.getResources();
        while (iterator.hasNext()){
            ResourceEntry resourceEntry = iterator.next();
            writeResources(resourceEntry, writeEntries);
        }

        writeIndent(serializer, indent);
        indent = indent - 2;
        mIndent = indent;
        serializer.endTag(null, "type");
        serializer.flush();
    }
    @Override
    public void writeResources(ResourceEntry resourceEntry, boolean writeEntries) throws IOException {
        if(resourceEntry.isEmpty()){
            return;
        }
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, NAME_RESOURCE);
        serializer.attribute(null, "id", HexUtil.toHex8(resourceEntry.getResourceId()));
        serializer.attribute(null, "type", resourceEntry.getType());
        serializer.attribute(null, "name", resourceEntry.getName());

        if(writeEntries){

            writeEntries(resourceEntry);

            writeIndent(serializer, indent);
        }

        indent = indent - 2;
        mIndent = indent;

        serializer.endTag(null, NAME_RESOURCE);
        serializer.flush();
    }
    public void writeEntries(ResourceEntry entryList) throws IOException {
        for(Entry entry : entryList){
            writeEntry(entry);
        }
    }
    public void writeEntry(Entry entry) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, TAG_CONFIG);
        serializer.attribute(null, NAME_QUALIFIERS,
                entry.getResConfig().getQualifiers());
        if(entry.isComplex()){
            writeBagEntry(entry);
        }else {
            writeResEntry(entry);
        }
        writeIndent(serializer, indent);
        serializer.endTag(null, TAG_CONFIG);
        indent = indent - 2;
        mIndent = indent;
    }
    private void writeResEntry(Entry entry) throws IOException {
        ResValue resValue = entry.getResValue();
        if(resValue == null){
            return;
        }
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, TAG_VALUE);
        serializer.attribute(null, "type", resValue.getValueType().name());
        serializer.text(getValueAsString(resValue));
        serializer.endTag(null, TAG_VALUE);
        indent = indent - 2;
        mIndent = indent;
    }
    private void writeBagEntry(Entry entry) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, TAG_BAG);
        serializer.attribute(null, "parent",
                HexUtil.toHex8(((ResTableMapEntry)entry.getTableEntry()).getParentId()));
        ResValueMapArray mapArray = entry.getResValueMapArray();
        for(ResValueMap resValueMap : mapArray.getChildes()){
            writeValueMap(resValueMap);
        }
        writeIndent(serializer, indent);
        serializer.endTag(null, TAG_BAG);
        indent = indent - 2;
        mIndent = indent;
    }
    private void writeValueMap(ResValueMap resValueMap) throws IOException {
        KXmlSerializer serializer = getSerializer();
        int indent = mIndent + 2;
        mIndent = indent;
        writeIndent(serializer, indent);
        serializer.startTag(null, TAG_VALUE);
        serializer.attribute(null, "name",
                HexUtil.toHex8(resValueMap.getNameId()));
        serializer.attribute(null, "type", resValueMap.getValueType().name());
        serializer.text(getValueAsString(resValueMap));
        serializer.endTag(null, TAG_VALUE);
        indent = indent - 2;
        mIndent = indent;
    }
    @Override
    public void writePackageNames(Collection<PackageBlock> packageBlocks) throws IOException {
        if(packageBlocks == null || packageBlocks.size() == 0){
            return;
        }
        int level = INDENT;
        KXmlSerializer serializer = getSerializer();
        writeIndent(serializer, level);
        serializer.startTag(null, TAG_RES_PACKAGES);
        serializer.attribute(null, "count", Integer.toString(packageBlocks.size()));
        level = level + 2;
        for(PackageBlock packageBlock : packageBlocks){
            writeIndent(serializer, level);
            serializer.startTag(null, "package");
            serializer.attribute(null, "id",
                    HexUtil.toHex2((byte) packageBlock.getId()));
            serializer.attribute(null, "name",
                    packageBlock.getName());
            serializer.endTag(null, "package");
        }
        level = level - 2;
        writeIndent(serializer, level);
        serializer.endTag(null, TAG_RES_PACKAGES);
        serializer.flush();
    }
    @Override
    public void writeEntries(String name, List<Entry> entryList) throws IOException {
        if(entryList == null || entryList.size() == 0){
            return;
        }
        Entry first = entryList.get(0);
        int level = INDENT;
        KXmlSerializer serializer = getSerializer();
        writeIndent(serializer, level);
        serializer.startTag(null, name);
        serializer.attribute(null, "count", Integer.toString(entryList.size()));
        serializer.attribute(null, "id", HexUtil.toHex8(first.getResourceId()));
        serializer.attribute(null, "type", first.getTypeName());
        serializer.attribute(null, "name", first.getName());
        level = level + 2;
        for(Entry entry : entryList){
            writeEntry(entry);
        }
        level = level - 2;
        writeIndent(serializer, level);
        serializer.endTag(null, name);
        serializer.flush();
    }

    @Override
    public void writeArray(String name, Object[] values) throws IOException {
        if(values == null){
            return;
        }
        int level = mIndent;
        if(level == 0){
            level = INDENT;
        }
        KXmlSerializer serializer = getSerializer();
        writeIndent(serializer, level);
        serializer.startTag(null, name);
        serializer.attribute(null, "count", Integer.toString(values.length));
        level = level + 2;
        for(Object value : values){
            String text = toString(value);
            if(text == null){
                text =  "";
            }
            writeIndent(serializer, level);
            serializer.startTag(null, "item");
            serializer.text(text);
            serializer.endTag(null, "item");
        }
        level = level - 2;
        writeIndent(serializer, level);
        serializer.endTag(null, name);
        serializer.flush();
    }

    @Override
    public void writeNameValue(String name, Object value) throws IOException {
        String text = toString(value);
        if(text == null){
            return;
        }
        KXmlSerializer serializer = getSerializer();
        writeIndent(serializer, INDENT);
        serializer.startTag(null, name);
        serializer.text(text);
        serializer.endTag(null, name);
        serializer.flush();
    }

    @Override
    public void flush() throws IOException {
        KXmlSerializer serializer = this.mSerializer;
        if(serializer != null){
            writeIndent(serializer, 0);
            serializer.endTag(null, TAG_INFO);
            serializer.endDocument();
            writeIndent(serializer, 0);
            serializer.flush();
        }
    }
    private void writeIndent(KXmlSerializer serializer, int level) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append('\n');
        for(int i = 0; i < level; i++){
            builder.append(' ');
        }
        serializer.text(builder.toString());
    }
    private KXmlSerializer getSerializer() throws IOException {
        KXmlSerializer serializer = this.mSerializer;
        if(serializer != null){
            return serializer;
        }
        serializer = new KXmlSerializer();
        serializer.setOutput(getWriter());
        serializer.startDocument("utf-8", null);
        writeIndent(serializer, 0);
        serializer.startTag(null, TAG_INFO);
        mSerializer = serializer;
        return serializer;
    }
    private XmlSerializer newSerializer() throws IOException {
        XmlSerializer current = this.mSerializer;
        if (current != null) {
            current.flush();
        }
        Writer writer = getWriter();
        writer.flush();
        XmlSerializer serializer = new KXmlSerializer();
        serializer.setOutput(writer);
        XMLUtil.setFeatureSafe(serializer, XMLUtil.FEATURE_INDENT_OUTPUT, true);
        return serializer;
    }

    private static final int INDENT = 3;
    private static final String TAG_INFO = "info";
}
