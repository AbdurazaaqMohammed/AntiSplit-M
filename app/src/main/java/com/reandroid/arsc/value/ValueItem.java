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
package com.reandroid.arsc.value;

import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.MainChunk;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.ParentChunk;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.coder.CoderUnknownStringRef;
import com.reandroid.arsc.coder.EncodeResult;
import com.reandroid.arsc.coder.ValueCoder;
import com.reandroid.arsc.coder.XmlSanitizer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.BlockItem;
import com.reandroid.arsc.item.ReferenceItem;
import com.reandroid.arsc.item.StringItem;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.refactor.ResourceMergeOption;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.xml.StyleDocument;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public abstract class ValueItem extends BlockItem implements Value,
        JSONConvert<JSONObject> {

    private ReferenceItem mStringReference;
    private final int sizeOffset;

    public ValueItem(int bytesLength, int sizeOffset) {
        super(bytesLength);
        this.sizeOffset = sizeOffset;
        writeSize();
    }

    public boolean isUndefined() {
        return getValueType() == ValueType.NULL && getData() == 0;
    }
    public ResourceEntry resolve(int resourceId) {
        PackageBlock context = getPackageBlock();
        if (context == null) {
            return null;
        }
        TableBlock tableBlock = context.getTableBlock();
        if (tableBlock == null) {
            return null;
        }
        return tableBlock.getResource(context, resourceId);
    }
    public PackageBlock getPackageBlock() {
        ParentChunk parentChunk = getParentChunk();
        if (parentChunk != null) {
            return parentChunk.getPackageBlock();
        }
        return null;
    }

    void linkTableStrings(TableStringPool tableStringPool) {
        if (getValueType() == ValueType.STRING) {
            linkStringReference(tableStringPool);
        }
    }
    public void onRemoved() {
        unLinkStringReference();
    }
    protected void onDataChanged() {
    }
    public void refresh() {
        updateSize();
    }

    @SuppressWarnings("unused")
    byte getRes0() {
        return getBytesInternal()[this.sizeOffset + OFFSET_RES0];
    }
    void setRes0(byte b) {
        getBytesInternal()[this.sizeOffset + OFFSET_RES0] = b;
    }
    public byte getType() {
        return getBytesInternal()[this.sizeOffset + OFFSET_TYPE];
    }
    public void setType(byte type) {
        if (type == getType()) {
            return;
        }
        byte[] bts = getBytesInternal();
        int offset = this.sizeOffset + OFFSET_TYPE;
        byte old = bts[offset];
        bts[offset] = type;
        onTypeChanged(old, type);
        onDataChanged();
    }
    public int getSize() {
        return 0xffff & getShort(getBytesInternal(), this.sizeOffset + OFFSET_SIZE);
    }
    public void setSize(int size) {
        size = this.sizeOffset + size;
        setBytesLength(size, false);
        writeSize();
    }
    void updateSize() {
        writeSize();
    }
    private void writeSize() {
        int offset = this.sizeOffset;
        int size = countBytes() - offset;
        putShort(getBytesInternal(), offset + OFFSET_SIZE, (short) size);
    }
    protected void onDataLoaded() {
        if (getValueType() == ValueType.STRING) {
            linkStringReference();
        }else {
            unLinkStringReference();
        }
    }
    @Override
    public ValueType getValueType() {
        return ValueType.valueOf(getType());
    }
    @Override
    public void setValueType(ValueType valueType) {
        byte type = 0;
        if (valueType!=null) {
            type = valueType.getByte();
        }
        setType(type);
    }
    @Override
    public int getData() {
        return getInteger(getBytesInternal(), this.sizeOffset + OFFSET_DATA);
    }
    @Override
    public void setData(int data) {
        int old = getData();
        if (old == data) {
            return;
        }
        unLinkStringReference();
        writeData(data);
        if (ValueType.STRING==getValueType()) {
            linkStringReference();
        }
        onDataChanged();
    }
    void writeData(int data) {
        putInteger(getBytesInternal(), this.sizeOffset + OFFSET_DATA, data);
    }

    public StringItem getDataAsPoolString() {
        if (getValueType() != ValueType.STRING) {
            return null;
        }
        StringPool<?> stringPool = getStringPool();
        if (stringPool == null) {
            return null;
        }
        return stringPool.get(getData());
    }
    private void onTypeChanged(byte old, byte type) {
        byte typeString = ValueType.STRING.getByte();
        if (old == typeString) {
            unLinkStringReference();
        }else if (type == typeString) {
            linkStringReference();
        }
    }
    private void linkStringReference() {
        StringPool<?> stringPool = getStringPool();
        if (stringPool == null || stringPool.isStringLinkLocked()) {
            return;
        }
        linkStringReference(stringPool);
    }
    private void linkStringReference(StringPool<?> stringPool) {
        StringItem tableString = stringPool.get(getData());
        if (tableString == null) {
            unLinkStringReference();
            return;
        }
        ReferenceItem stringReference = mStringReference;
        if (stringReference!=null) {
            unLinkStringReference();
        }
        stringReference = new ValueStringReference(this);
        mStringReference = stringReference;
        tableString.addReference(stringReference);
    }
    private void unLinkStringReference() {
        ReferenceItem stringReference = mStringReference;
        if (stringReference==null) {
            return;
        }
        mStringReference = null;
        onUnlinkDataString(stringReference);
    }
    protected void onUnlinkDataString(ReferenceItem referenceItem) {
        StringPool<?> stringPool = getStringPool();
        if (stringPool == null) {
            return;
        }
        stringPool.removeReference(referenceItem);
    }
    public StringPool<?> getStringPool() {
        Block parent = getParent();
        while (parent!=null) {
            if (parent instanceof MainChunk) {
                return ((MainChunk) parent).getStringPool();
            }
            parent=parent.getParent();
        }
        return null;
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        int readSize = initializeBytes(reader);
        super.onReadBytes(reader);
        if (readSize<8) {
            setBytesLength(this.sizeOffset + 8, false);
            writeSize();
        }
    }
    private int initializeBytes(BlockReader reader) throws IOException {
        int position = reader.getPosition();
        int offset = this.sizeOffset;
        reader.offset(offset);
        int readSize = reader.readUnsignedShort();
        int size = readSize;
        if (size<8) {
            if (reader.available()>=8) {
                size = 8;
            }
        }
        reader.seek(position);
        setBytesLength(offset + size, false);
        return readSize;
    }
    @Override
    public String getValueAsString() {
        StringItem stringItem = getDataAsPoolString();
        if (stringItem!=null) {
            String value = stringItem.getXml();
            if (value == null) {
                value = "";
            }
            return value;
        }
        return null;
    }
    public Object getValueAsEncoded() {
        ValueType valueType = getValueType();
        if (valueType == ValueType.STRING) {
            return getValueAsString();
        }
        if (valueType == ValueType.BOOLEAN) {
            return getValueAsBoolean();
        }
        if (valueType == ValueType.DEC || valueType == ValueType.HEX) {
            return getData();
        }
        if (valueType == ValueType.FLOAT) {
            return getValueAsFloat();
        }
        return null;
    }
    public StyleDocument getValueAsStyleDocument() {
        StringItem stringItem = getDataAsPoolString();
        if (stringItem != null) {
            return stringItem.getStyleDocument();
        }
        return null;
    }
    public void setValueAsString(StyleDocument styledString) {
        if (styledString == null) {
            setValueAsString("");
            return;
        }
        StringPool<?> stringPool = getStringPool();
        if (!styledString.hasElements()) {
            setValueAsString(XmlSanitizer.unEscapeUnQuote(styledString.getXml(false)));
            return;
        }
        StringItem stringItem = stringPool.getOrCreate(styledString);
        setData(stringItem.getIndex());
        setValueType(ValueType.STRING);
    }
    @Override
    public void setValueAsString(String str) {
        if (getValueType() == ValueType.STRING
                && Objects.equals(str, getValueAsString())) {
            return;
        }
        if (str == null) {
            str = "";
        }
        StringItem stringItem = getStringPool().getOrCreate(str);
        setData(stringItem.getIndex());
        setValueType(ValueType.STRING);
    }
    public void serializeText(XmlSerializer serializer) throws IOException {
        serializeText(serializer, false);
    }
    public void serializeText(XmlSerializer serializer, boolean escapeValues) throws IOException {
        if (getValueType() == ValueType.STRING) {
            StringItem stringItem = getDataAsPoolString();
            if (stringItem != null) {
                stringItem.serializeText(serializer, escapeValues);
            }else {
                serializer.text(CoderUnknownStringRef.INS.decode(getData()));
            }
            return;
        }
        String value = decodeValue();
        if (value == null) {
            // TODO: could not happen ?
            value = "";
        }
        serializer.text(value);
    }
    public void serializeAttribute(XmlSerializer serializer, String name, boolean ignore_empty) throws IOException {
        serializeAttribute(serializer, null, name, ignore_empty);
    }
    public void serializeAttribute(XmlSerializer serializer, String namespace, String name, boolean ignore_empty) throws IOException {
        if (getValueType() == ValueType.STRING) {
            StringItem stringItem = getDataAsPoolString();
            if (stringItem != null) {
                stringItem.serializeAttribute(serializer, namespace, name);
            }else {
                // TODO: should throw ?
                serializer.attribute(namespace, name, CoderUnknownStringRef.INS.decode(getData()));
            }
            return;
        }
        String value = decodeValue();
        if (ignore_empty && StringsUtil.isEmpty(value)) {
            return;
        }
        if (value == null) {
            value = "";
        }
        serializer.attribute(namespace, name, value);
    }
    public boolean getValueAsBoolean() {
        return getData() != 0;
    }
    public void setValueAsBoolean(boolean value) {
        setValueType(ValueType.BOOLEAN);
        setData(value ? 0xffffffff : 0);
    }
    @Override
    public void setValue(EncodeResult encodeResult) {
        if (encodeResult == null) {
            throw new NullPointerException();
        }
        if (encodeResult.isError()) {
            throw new IllegalArgumentException("Can not set error value: "
                    + encodeResult.getError());
        }
        setTypeAndData(encodeResult.valueType, encodeResult.value);
    }
    public void merge(ValueItem valueItem) {
        if (valueItem == null || valueItem == this) {
            return;
        }
        int size = valueItem.getSize();
        if (size != 0) {
            setSize(valueItem.getSize());
        }
        ValueType coming = valueItem.getValueType();
        if (coming == ValueType.STRING) {
            StringItem stringItem = valueItem.getDataAsPoolString();
            if (stringItem != null) {
                StyleDocument document = stringItem.getStyleDocument();
                if (document != null) {
                    setValueAsString(document);
                }else {
                    setValueAsString(stringItem.get());
                }
            }
        }else {
            setTypeAndData(coming, valueItem.getData());
        }
    }
    public void mergeWithName(ResourceMergeOption mergeOption, ValueItem valueItem) {
        if (valueItem == null || valueItem == this) {
            return;
        }
        int size = valueItem.getSize();
        if (size != 0) {
            setSize(valueItem.getSize());
        }
        ValueType coming = valueItem.getValueType();
        if (coming == ValueType.STRING) {
            StyleDocument styleDocument = valueItem.getValueAsStyleDocument();
            if (styleDocument != null) {
                setValueAsString(styleDocument);
            }else {
                ApkFile apk1 = getPackageBlock().getTableBlock().getApkFile();
                ApkFile apk2 = valueItem.getPackageBlock().getTableBlock().getApkFile();
                String value = valueItem.getValueAsString();
                setValueAsString(value);
                if (apk1 != null && apk2 != null) {
                    apk1.mergeWithName(mergeOption, apk2, value);
                }
            }
        }else if (coming.isReference()) {
            int id = 0;
            ResourceEntry comingResourceEntry = valueItem.getValueAsReference();
            if (comingResourceEntry == null) {
                id = valueItem.getData();
            }else if (comingResourceEntry.isContext(valueItem.getPackageBlock())) {
                ResourceEntry mergedReference;
                if (comingResourceEntry.isDeclared()) {
                    mergedReference = getPackageBlock().mergeWithName(mergeOption, comingResourceEntry);
                }else {
                    mergedReference = mergeOption.resolveUndeclared(getPackageBlock(), comingResourceEntry);
                }
                if (mergedReference != null) {
                    id = mergedReference.getResourceId();
                }
            }else {
                id = valueItem.getData();
            }
            setTypeAndData(coming, id);
        }else {
            setTypeAndData(coming, valueItem.getData());
        }
    }
    public String decodeValue() {
        return decodeValue(true);
    }
    public String decodeValue(boolean validatePackage) {
        ValueType valueType = getValueType();
        if (valueType == null) {
            return null;
        }
        if (valueType.isReference()) {
            return decodeAsReferenceString(valueType, validatePackage);
        }
        if (valueType == ValueType.STRING) {
            return getValueAsString();
        }
        return ValueCoder.decode(valueType, getData());
    }
    private String decodeAsReferenceString(ValueType valueType, boolean validatePackage) {
        int data = getData();
        if (data == 0) {
            return ValueCoder.decodeReference(null, valueType, data);
        }
        ResourceEntry resourceEntry = getValueAsReference();
        if (validatePackage && resourceEntry == null && getPackageBlock() == null) {
            throw new NullPointerException("Parent package block is null");
        }
        if (resourceEntry == null || !resourceEntry.isDeclared()) {
            return ValueCoder.decodeUnknownResourceId(valueType, data);
        }
        return resourceEntry.buildReference(getPackageBlock(), valueType);
    }
    @Override
    public JSONObject toJson() {
        if (isNull()) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        ValueType valueType = getValueType();
        jsonObject.put(NAME_value_type, valueType.name());
        if (valueType == ValueType.STRING) {
            StringItem stringItem = getDataAsPoolString();
            if (stringItem.hasStyle()) {
                jsonObject.put(NAME_data, getDataAsPoolString().toJson());
            }else {
                jsonObject.put(NAME_data, stringItem.get());
            }
        }else if (valueType == ValueType.BOOLEAN) {
            jsonObject.put(NAME_data, getValueAsBoolean());
        }else {
            jsonObject.put(NAME_data, getData());
        }
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        if (json != null) {
            ValueType valueType = ValueType.fromName(json.getString(NAME_value_type));
            if (valueType == ValueType.STRING) {
                JSONObject jsonObject = json.optJSONObject(NAME_data);
                StringPool<?> stringPool = getStringPool();
                StringItem stringItem;
                if (jsonObject != null) {
                    stringItem = stringPool.getOrCreate(jsonObject);
                }else {
                    stringItem = stringPool.getOrCreate(json.getString(NAME_data));
                }
                setTypeAndData(valueType, stringItem.getIndex());
            }else if (valueType == ValueType.BOOLEAN) {
                setValueAsBoolean(json.getBoolean(NAME_data));
            }else {
                setValueType(valueType);
                setData(json.getInt(NAME_data));
            }
        }
    }

    @Override
    public String toString() {
        if (getPackageBlock() != null) {
            return getValueType() + ":" + HexUtil.toHex8(getData()) + " " + decodeValue();
        }
        StringBuilder builder = new StringBuilder();
        int size = getSize();
        if (size!=8) {
            builder.append("size=").append(getSize());
            builder.append(", ");
        }
        builder.append("type=");
        ValueType valueType=getValueType();
        if (valueType!=null) {
            builder.append(valueType);
        }else {
            builder.append(HexUtil.toHex2(getType()));
        }
        builder.append(", data=");
        int data = getData();
        if (valueType==ValueType.STRING) {
            StringItem tableString = getDataAsPoolString();
            if (tableString!=null) {
                builder.append(tableString.getHtml());
            }else {
                builder.append(HexUtil.toHex8(data));
            }
        }else {
            builder.append(HexUtil.toHex8(data));
        }
        return builder.toString();
    }

    static class ValueStringReference implements ReferenceItem {

        private final ValueItem valueItem;

        ValueStringReference(ValueItem valueItem) {
            this.valueItem = valueItem;
        }
        @Override
        public int get() {
            return valueItem.getData();
        }
        @Override
        public void set(int value) {
            valueItem.writeData(value);
        }
        @SuppressWarnings("unchecked")
        @Override
        public <T1 extends Block> T1 getReferredParent(Class<T1> parentClass) {
            ValueItem block = this.valueItem;
            if (parentClass.isInstance(block)) {
                return (T1) block;
            }
            return block.getParentInstance(parentClass);
        }
    }
    private static final int OFFSET_SIZE = 0;
    private static final int OFFSET_RES0 = 2;
    private static final int OFFSET_TYPE = 3;
    private static final int OFFSET_DATA = 4;


    public static final String NAME_data = "data";
    public static final String NAME_value_type = "value_type";
}
