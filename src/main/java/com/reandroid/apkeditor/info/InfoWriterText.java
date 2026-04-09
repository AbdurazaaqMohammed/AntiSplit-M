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
import com.reandroid.arsc.chunk.xml.*;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.header.StringPoolHeader;
import com.reandroid.arsc.item.StringItem;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.arsc.value.*;
import com.reandroid.common.Namespace;
import com.reandroid.dex.model.DexFile;
import com.reandroid.dex.model.DexLayout;
import com.reandroid.dex.model.DexSectionInfo;
import com.reandroid.dex.sections.Marker;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class InfoWriterText extends InfoWriter {

    public InfoWriterText(Writer writer) {
        super(writer);
    }

    @Override
    public void writeStringPool(String source, StringPool<?> stringPool) throws IOException {
        Writer writer = getWriter();
        writer.write(source);
        writer.write("\n");
        writer.write("String pool of ");
        writer.write(Integer.toString(stringPool.size()));
        writer.write(" unique ");
        if (stringPool.isUtf8()) {
            writer.write("UTF-8 ");
        } else {
            writer.write("UTF-16 ");
        }
        StringPoolHeader header = stringPool.getHeaderBlock();
        if (!header.isSorted()) {
            writer.write("non-");
        }
        writer.write("sorted strings, ");
        writer.write(Integer.toString(stringPool.size()));
        writer.write(" entries and ");
        writer.write(Integer.toString(stringPool.countStyles()));
        writer.write(" styles using ");
        writer.write(Integer.toString(header.getChunkSize()));
        writer.write(" bytes:");
        writer.write("\n");
        int size = stringPool.size();
        for (int i = 0; i < size; i++ ) {
            StringItem item = stringPool.get(i);
            writer.write("String #");
            writer.write(Integer.toString(i));
            writer.write(": ");
            writer.write(item.get());
            writer.write("\n");
        }
    }

    @Override
    public void writeXmlDocument(String sourcePath, ResXmlDocument xmlDocument) throws IOException {
        writeNameValue("source-path", sourcePath);
        Iterator<ResXmlNode> iterator = xmlDocument.iterator();
        while (iterator.hasNext()) {
            ResXmlNode node = iterator.next();
            if (node instanceof ResXmlElement) {
                writeElement((ResXmlElement) node);
            } else if (node instanceof ResXmlTextNode) {
                writeTextNode(0, (ResXmlTextNode) node);
            }
        }
        Writer writer = getWriter();
        writer.flush();
    }
    private void writeElement(ResXmlElement element) throws IOException {
        int indent = element.getDepth() * 2;

        int count = element.getNamespaceCount();
        for (int i = 0; i < count; i++) {
            writeNamespace(indent, element.getNamespaceAt(i));
        }
        indent = indent + 2;
        Writer writer = getWriter();
        writeIndent(indent);
        writer.write("E: ");
        writer.write(element.getName(true));
        writer.write(" (line=");
        writer.write(Integer.toString(element.getLineNumber()));
        writer.write(")");
        writer.write("\n");

        Iterator<ResXmlAttribute> attributes = element.getAttributes();
        while (attributes.hasNext()) {
            writeAttribute(indent, attributes.next());
        }
        flush();
        Iterator<ResXmlNode> iterator = element.iterator();
        while (iterator.hasNext()) {
            ResXmlNode node = iterator.next();
            if (node instanceof ResXmlElement) {
                writeElement((ResXmlElement) node);
            } else if (node instanceof ResXmlTextNode) {
                writeTextNode(indent, (ResXmlTextNode) node);
            }
        }
    }
    private void writeTextNode(int indent, ResXmlTextNode textNode) throws IOException {
        Writer writer = getWriter();
        writeIndent(indent + 2);
        writer.write("T: \"");
        writer.write(textNode.getText());
        writer.write("\"");
        writer.write("\n");
    }
    private void writeNamespace(int indent, ResXmlNamespace namespace) throws IOException {
        Writer writer = getWriter();
        writeIndent(indent);
        writer.write("N: ");
        writer.write(namespace.getPrefix());
        writer.write("=");
        writer.write(namespace.getUri());
        writer.write("\n");
    }
    private void writeAttribute(int indent, ResXmlAttribute attribute) throws IOException {
        Writer writer = getWriter();
        writeIndent(indent + 2);
        writer.write("A: ");
        Namespace namespace = attribute.getNamespace();
        if (namespace != null) {
            writer.write(namespace.getPrefix());
            writer.append(':');
        }
        writer.write(attribute.getName());
        int id = attribute.getNameId();
        if (id != 0) {
            writer.append('(');
            writer.write(HexUtil.toHex8(id));
            writer.append(')');
        }
        writer.append('=');
        ValueType valueType = attribute.getValueType();
        if (valueType == ValueType.STRING) {
            writer.append('"');
            writer.write(attribute.getDataAsPoolString().getXml());
            writer.append('"');
            writer.write(" (Raw: \"");
            writer.write(attribute.getValueString());
            writer.write("\")");
        } else if (valueType == ValueType.BOOLEAN) {
            writer.append('"');
            writer.write(attribute.getValueAsBoolean() ? "true" : "false");
            writer.append('"');
        } else {
            writer.write("(type ");
            writer.write(HexUtil.toHex(valueType.getByte() & 0xff, 1));
            writer.write(")");
            writer.write(HexUtil.toHex(attribute.getData(), 1));
        }
        writer.write("\n");
    }

    @Override
    public void writeCertificates(List<CertificateBlock> certificateList, boolean base64) throws IOException {
        Writer writer = getWriter();
        writer.write("\n");
        writeNameValue("Certificates", certificateList.size());
        for(CertificateBlock certificateBlock : certificateList) {
            writeWithTab(writer, ARRAY_TAB, certificateBlock.printCertificate());
            if(base64) {
                writer.write(ARRAY_TAB);
                writer.write("Base64: ");
                writer.write(toBase64(certificateBlock.getCertificateBytes()));
                writer.write("\n");
            }
        }
        flush();
    }
    @Override
    public void writeDexInfo(DexFile dexFile, boolean writeSectionInfo) throws IOException {
        Writer writer = getWriter();
        writer.write("\n");
        writeNameValue("Name", dexFile.getFileName());
        if (dexFile.isMultiLayout()) {
            int size = dexFile.size();
            for (int i = 0; i < size; i++) {
                writeLayout(dexFile.getLayout(i));
            }
            writeNameValue("TotalClasses", dexFile.getDexClassesCountForDebug());
        } else if(dexFile.size() != 0) {
            writeLayout(dexFile.getFirst());
        } else {
            writer.write("EMPTY DEX");
            writer.write("\n");
        }
        writer.flush();
    }
    private void writeLayout(DexLayout layout) throws IOException {
        Writer writer = getWriter();
        if (layout.isMultiLayoutEntry()) {
            writer.write(layout.getName());
            writer.write(", ");
        }
        writeNameValue("Version", layout.getVersion());
        writeMarkers(layout.getMarkers());
        writer.write("Sections:");
        Iterator<DexSectionInfo> iterator = layout.getSectionInfo();
        while (iterator.hasNext()){
            DexSectionInfo sectionInfo = iterator.next();
            writer.write("\n");
            writer.write(ARRAY_TAB);
            writer.write(sectionInfo.print(false));
        }
        writer.write("\n");
    }
    private void writeMarkers(Iterator<Marker> iterator) throws IOException {
        Writer writer = getWriter();
        List<Marker> markersList = CollectionUtil.toList(iterator);
        if(markersList.size() != 0){
            writer.write("Markers:");
            for(Marker marker : markersList){
                writer.write("\n");
                writer.write(ARRAY_TAB);
                writer.write(marker.toString());
            }
            writer.write("\n");
        }
    }
    @Override
    public void writeResources(PackageBlock packageBlock, List<String> typeFilters, boolean writeEntries) throws IOException {
        Writer writer = getWriter();
        writer.write("Package name=");
        writer.write(packageBlock.getName());
        writer.write(" id=");
        writer.write(HexUtil.toHex2((byte) packageBlock.getId()));
        writer.write("\n");
        packageBlock.sortTypes();

        for(SpecTypePair specTypePair : packageBlock.listSpecTypePairs()){
            if(!contains(specTypePair, typeFilters)){
                continue;
            }
            writeResources(specTypePair, writeEntries);
        }
    }
    public void writeResources(SpecTypePair specTypePair, boolean writeEntries) throws IOException {
        Writer writer = getWriter();
        writer.write("  type ");
        writer.write(specTypePair.getTypeName());
        writer.write(" id=");
        writer.write(Integer.toString(specTypePair.getId()));
        writer.write(" entryCount=");
        writer.write(Integer.toString(specTypePair.getHighestEntryCount()));
        writer.write("\n");

        Iterator<ResourceEntry> iterator = specTypePair.getResources();
        while (iterator.hasNext()){

            ResourceEntry resourceEntry = iterator.next();
            writeResources(resourceEntry, writeEntries);
        }
    }
    @Override
    public void writeResources(ResourceEntry resourceEntry, boolean writeEntries) throws IOException {
        if(resourceEntry.isEmpty()){
            return;
        }
        Writer writer = getWriter();
        writer.write("    ");
        writer.write(NAME_RESOURCE);
        writer.write(" ");
        writer.write(HexUtil.toHex8(resourceEntry.getResourceId()));
        writer.write(" ");
        writer.write(resourceEntry.getType());
        writer.write("/");
        writer.write(resourceEntry.getName());
        writer.write("\n");
        if(writeEntries){
            writeEntries(resourceEntry);
        }
        writer.flush();
    }

    public void writeEntries(ResourceEntry entryList) throws IOException {
        for(Entry entry : entryList){
            writeEntry(entry);
        }
    }
    public void writeEntry(Entry entry) throws IOException {
        Writer writer = getWriter();
        writer.write("      (");
        writer.write(entry.getResConfig().getQualifiers());
        writer.write(") ");
        //write file
        if(entry.isComplex()){
            writeBagEntry(entry);
        }else {
            writeResEntry(entry);
        }
        writer.flush();
    }
    private void writeResEntry(Entry entry) throws IOException {
        ResValue resValue = entry.getResValue();
        if(resValue == null){
            return;
        }
        Writer writer = getWriter();
        writer.write(getValueAsString(resValue));
        writer.write("\n");
    }
    private void writeBagEntry(Entry entry) throws IOException {
        Writer writer = getWriter();
        ResValueMapArray mapArray = entry.getResValueMapArray();
        writer.write(" size=");
        writer.write(Integer.toString(mapArray.getCount()));
        writer.write(" parent=");
        writer.write(HexUtil.toHex8(((ResTableMapEntry)entry.getTableEntry()).getParentId()));
        writer.write("\n");
        for(ResValueMap resValueMap : mapArray.getChildes()){
            writeValueMap(resValueMap);
        }
    }
    private void writeValueMap(ResValueMap resValueMap) throws IOException {
        Writer writer = getWriter();
        writer.write("        ");
        String name = resValueMap.decodeName();
        if(name != null){
            writer.write(name);
            writer.write("(");
        }
        writer.write(HexUtil.toHex8(resValueMap.getNameId()));
        if(name != null){
            writer.write(")");
        }
        writer.write("=");
        writer.write(getValueAsString(resValueMap));
        writer.write("\n");
    }
    @Override
    public void writePackageNames(Collection<PackageBlock> packageBlocks) throws IOException {
        if(packageBlocks == null || packageBlocks.size() == 0){
            return;
        }
        Writer writer = getWriter();
        writer.write(TAG_RES_PACKAGES);
        writer.write("  [ count ");
        writer.write(Integer.toString(packageBlocks.size()));
        writer.write("]");
        writer.write("\n");
        for(PackageBlock packageBlock : packageBlocks){
            writer.write(ARRAY_TAB);
            writer.write(HexUtil.toHex2((byte) packageBlock.getId()));
            writer.write("  \"");
            writer.write(packageBlock.getName());
            writer.write("\"");
            writer.write("\n");
        }
    }
    @Override
    public void writeEntries(String name, List<Entry> entryList) throws IOException {
        if(entryList == null || entryList.size() == 0){
            return;
        }
        Entry first = entryList.get(0);
        Writer writer = getWriter();
        writer.write(name);
        writer.write("  [ configs = ");
        writer.write(Integer.toString(entryList.size()));
        writer.write(", id=");
        writer.write(HexUtil.toHex8(first.getResourceId()));
        writer.write(", type=");
        writer.write(first.getTypeName());
        writer.write(", name=");
        writer.write(first.getName());
        writer.write(" ]");
        writer.write("\n");
        int index = 0;
        for(Entry entry : entryList){
            index++;
            writer.write(ARRAY_TAB);
            writeEntry(entry);
            writer.write("\n");
            if((index % 3) == 0){
                writer.flush();
            }
        }
    }

    @Override
    public void writeArray(String name, Object[] values) throws IOException {
        if(values == null){
            return;
        }
        Writer writer = getWriter();
        writer.write(name);
        writer.write("  [ count ");
        writer.write(Integer.toString(values.length));
        writer.write("]");
        writer.write("\n");
        String format = "%0" + getDecimalPlaces(values.length) + "d) ";
        int index = 0;
        for(Object value : values){
            index ++;
            writer.write(ARRAY_TAB);
            writer.write(String.format(format, index));
            String text = toString(value);
            if(text == null){
                text = "null";
            }
            writer.write(text);
            writer.write("\n");
            if((index % 3) == 0){
                writer.flush();
            }
        }
    }
    @Override
    public void writeNameValue(String name, Object value) throws IOException {
        String text = toString(value);
        if(text == null){
            return;
        }
        Writer writer = getWriter();
        writer.write(name);
        writer.write("=\"");
        writer.write(text);
        writer.write("\"");
        writer.write("\n");
        writer.flush();
    }

    @Override
    public void flush() throws IOException {
        Writer writer = getWriter();
        writer.flush();
    }


    private void writeIndent(int amount) throws IOException {
        writeSpaces(getWriter(), amount);
    }
    private void writeWithTab(Writer writer, String tab, String value) throws IOException {
        String[] splits = StringsUtil.split(value, '\n');
        for(String line : splits){
            writer.write(tab);
            writer.write(line.trim());
            writer.write("\n");
        }
    }
    private static int getDecimalPlaces(int max){
        int i = 0;
        while (max != 0){
            i++;
            max = max / 10;
        }
        return i;
    }

    private static final String ARRAY_TAB = "     ";
}
