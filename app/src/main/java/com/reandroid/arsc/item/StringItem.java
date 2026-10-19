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
package com.reandroid.arsc.item;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.coder.ThreeByteCharsetDecoder;
import com.reandroid.arsc.coder.XmlSanitizer;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.list.StringItemList;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.HashSetStore;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.xml.StyleDocument;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class StringItem extends StringBlock implements JSONConvert<JSONObject>, Comparable<StringItem> {

    private boolean mUtf8;
    private Object mReferencedSet;
    private StyleItem mStyleItem;

    public StringItem(boolean utf8) {
        super();
        this.mUtf8 = utf8;
    }

    public StyleDocument getStyleDocument() {
        if (hasStyle()) {
            return getStyle().build(get());
        }
        return null;
    }

    public<T extends Block> Iterator<T> getUsers(Class<T> parentClass) {
        return getUsers(parentClass, null);
    }
    public<T extends Block> Iterator<T> getUsers(Class<T> parentClass,
                                                 Predicate<T> resultFilter) {
        return ComputeIterator.of(getReferences(), referenceItem -> {
            T result = referenceItem.getReferredParent(parentClass);
            if (result == null || resultFilter != null && !resultFilter.test(result)) {
                result = null;
            }
            return result;
        });

    }

    public void removeReference(ReferenceItem reference) {
        mReferencedSet = HashSetStore.remove(mReferencedSet, reference);
    }
    public void clearReferences() {
        mReferencedSet = HashSetStore.clear(mReferencedSet);
    }
    public boolean hasReference() {
        ensureStringLinkUnlocked();
        return HashSetStore.containsIf(mReferencedSet, NON_STYLE_INDEX);
    }
    public int getReferencesSize() {
        return HashSetStore.size(mReferencedSet);
    }
    public Iterator<ReferenceItem> getReferences() {
        ensureStringLinkUnlocked();
        return HashSetStore.iterator(mReferencedSet);
    }
    void ensureStringLinkUnlocked() {
        StringPool<?> stringPool = getParentInstance(StringPool.class);
        if (stringPool != null) {
            stringPool.ensureStringLinkUnlockedInternal();
        }
    }
    public void addReference(ReferenceItem reference) {
        if (reference != null) {
            mReferencedSet = HashSetStore.add(mReferencedSet, reference);
            int index = this.getIndex();
            if (reference.get() != index) {
                reference.set(index);
            }
        }
    }
    private void reUpdateReferences(int newIndex) {
        Iterator<ReferenceItem> iterator = HashSetStore.clonedIterator(mReferencedSet);
        while (iterator.hasNext()) {
            ReferenceItem reference = iterator.next();
            reference.set(newIndex);
        }
    }
    public void onRemoved() {
        clearStyle();
        setParent(null);
    }
    @Override
    public void onIndexChanged(int oldIndex, int newIndex) {
        reUpdateReferences(newIndex);
    }
    @SuppressWarnings("unchecked")
    @Override
    protected void onStringChanged(String old, String text) {
        super.onStringChanged(old, text);
        StringItemList<StringItem> stringPool = getParentInstance(StringItemList.class);
        if (stringPool != null) {
            stringPool.onStringChanged(old, this);
        }
    }

    public void serializeText(XmlSerializer serializer) throws IOException {
        serializeText(serializer, false);
    }
    public void serializeText(XmlSerializer serializer, boolean escapeValues) throws IOException {
        String text = get();
        if (text == null) {
            return;
        }
        if (escapeValues) {
            text = XmlSanitizer.escapeDecodedValue(text);
        } else {
            text = XmlSanitizer.escapeSpecialCharacter(text);
        }
        serializer.text(text);
    }
    public void serializeAttribute(XmlSerializer serializer, String namespace, String name) throws IOException {
        String text = get();
        if (text == null) {
            text = "";
        }
        serializer.attribute(namespace, name, XmlSanitizer.escapeSpecialCharacter(text));
    }
    public String getHtml() {
        String text = get();
        if (text == null) {
            return null;
        }
        StyleItem styleItem = getStyle();
        if (styleItem == null) {
            return text;
        }
        return styleItem.applyStyle(text, false, false);
    }
    public String getXml() {
        return getXml(false);
    }
    public String getXml(boolean escapeXmlText) {
        String text = get();
        if (text == null) {
            return null;
        }
        StyleItem styleItem = getStyle();
        if (styleItem == null) {
            return text;
        }
        return styleItem.applyStyle(text, true, escapeXmlText);
    }
    @Override
    public void set(String str) {
        boolean is_null = str == null;
        setNull(is_null);
        if (is_null) {
            StyleItem style = getStyle();
            if (style != null) {
                style.clearStyle();
            }
        }
        super.set(str);
    }
    public void set(StyleDocument document) {
        String old = getXml();
        if (countBytes() == 0) {
            old = null;
        }
        clearStyle();
        this.set(document.getStyledString(), false);
        if (document.hasElements()) {
            StyleItem styleItem = getOrCreateStyle();
            styleItem.parse(document);
        }
        String update = getXml();
        onStringChanged(old, update);
    }
    public void set(JSONObject jsonObject) {
        String old = getXml();
        if (countBytes() == 0) {
            old = null;
        }
        clearStyle();
        this.set(jsonObject.getString(NAME_string), false);
        JSONObject style = jsonObject.optJSONObject(NAME_style);
        if (style != null) {
            StyleItem styleItem = getOrCreateStyle();
            styleItem.fromJson(style);
        }
        String update = getXml();
        onStringChanged(old, update);
    }

    public boolean isUtf8() {
        return mUtf8;
    }
    public void setUtf8(boolean utf8) {
        if (utf8 != mUtf8) {
            mUtf8 = utf8;
            if (countBytes() != 0) {
                writeStringBytes(get());
            }
        }
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        if (reader.available() < 4) {
            return;
        }
        setBytesLength(calculateReadLength(reader), false);
        reader.readFully(getBytesInternal());
        onBytesChanged();
    }
    int calculateReadLength(BlockReader reader) throws IOException {
        if (reader.available() < 4) {
            return reader.available();
        }
        byte[] bytes = new byte[4];
        reader.readFully(bytes);
        reader.offset(-4);
        int[] lengthResult;
        if (isUtf8()) {
            lengthResult = decodeUtf8StringByteLength(bytes);
        } else {
            lengthResult = decodeUtf16StringByteLength(bytes);
        }
        int add = isUtf8() ? 1:2;
        return lengthResult[0] + lengthResult[1] + add;
    }
    @Override
    protected String decodeString(byte[] bytes) {
        return decodeString(bytes, mUtf8);
    }
    @Override
    protected byte[] encodeString(String str) {
        if (mUtf8) {
            return encodeUtf8ToBytes(str);
        } else {
            return encodeUtf16ToBytes(str);
        }
    }
    private String decodeString(byte[] encodedBytes, boolean isUtf8) {
        if (isNullBytes(encodedBytes)) {
            if (encodedBytes == null || encodedBytes.length == 0) {
                return null;
            }
            return "";
        }
        int[] offLen;
        if (isUtf8) {
            offLen = decodeUtf8StringByteLength(encodedBytes);
        } else {
            offLen = decodeUtf16StringByteLength(encodedBytes);
        }
        CharsetDecoder charsetDecoder;
        if (isUtf8) {
            charsetDecoder = UTF8_DECODER;
        } else {
            charsetDecoder = UTF16LE_DECODER;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encodedBytes, offLen[0], offLen[1]);
            return charsetDecoder.decode(buffer).toString();
        } catch (CharacterCodingException ex) {
            if (isUtf8) {
                return tryThreeByteDecoder(encodedBytes, offLen[0], offLen[1]);
            }
            return new String(encodedBytes, offLen[0], offLen[1], StandardCharsets.UTF_16LE);
        }
    }
    private String tryThreeByteDecoder(byte[] bytes, int offset, int length) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
            CharBuffer charBuffer = DECODER_3B.decode(byteBuffer);
            return charBuffer.toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, offset, length, StandardCharsets.UTF_8);
        }
    }
    public boolean hasStyle() {
        StyleItem styleItem = getStyle();
        if (styleItem != null) {
            return styleItem.hasSpans();
        }
        return false;
    }
    public StyleItem getStyle() {
        return mStyleItem;
    }
    public StyleItem getOrCreateStyle() {
        StyleItem styleItem = getStyle();
        if (styleItem == null) {
            styleItem = getParentInstance(StringPool.class).getStyleArray().createNext();
            linkStyleItemInternal(styleItem);
            styleItem = getStyle();
        }
        return styleItem;
    }
    public void linkStyleItemInternal(StyleItem styleItem) {
        if (styleItem == null) {
            throw new NullPointerException("Can not link null style item");
        }
        if (this.mStyleItem == styleItem) {
            return;
        }
        if (this.mStyleItem != null) {
            throw new IllegalStateException("Style item is already linked");
        }
        this.mStyleItem = styleItem;
        styleItem.setStringItemInternal(this);
    }
    public void unlinkStyleItemInternal(StyleItem styleItem) {
        if (this.mStyleItem == null) {
            return;
        }
        if (styleItem != this.mStyleItem) {
            throw new IllegalStateException("Wrong style item");
        }
        this.mStyleItem = null;
        styleItem.setStringItemInternal(null);
    }
    private void clearStyle() {
        StyleItem styleItem = getStyle();
        if (styleItem != null) {
            styleItem.clearStyle();
        }
    }
    public void transferReferences(StringItem source) {
        if (source == this || source == null || getParent() != source.getParent()) {
            return;
        }
        if (getIndex() < 0 || source.getIndex() < 0) {
            return;
        }
        Iterator<ReferenceItem> iterator = HashSetStore.clonedIterator(source.mReferencedSet);
        while (iterator.hasNext()) {
            ReferenceItem reference = iterator.next();
            if (isTransferable(reference)) {
                source.removeReference(reference);
                addReference(reference);
            }
        }
    }
    private boolean isTransferable(ReferenceItem referenceItem) {
        return !((referenceItem instanceof WeakStringReference));
    }
    public boolean merge(StringItem other) {
        if (!canMerge(other)) {
            return false;
        }
        clearStyle();
        set(other.get(), false);
        StyleItem otherStyle = other.getStyle();
        if (otherStyle != null && otherStyle.hasSpans()) {
            getOrCreateStyle().merge(otherStyle);
        }
        onStringChanged(null, getXml());
        return true;
    }
    boolean canMerge(StringItem stringItem) {
        if (stringItem == null || stringItem == this) {
            return false;
        }
        Block array1 = this.getParentInstance(StringItemList.class);
        Block array2 = stringItem.getParentInstance(StringItemList.class);
        return array1 != null && array2 != null && array1 != array2;
    }
    public boolean equalsValue(StyleDocument styled) {
        if (styled == null) {
            return isNull();
        }
        if (this.hasStyle()) {
            return styled.equals(getStyleDocument());
        } else if (!styled.hasElements()) {
            return ObjectsUtil.equals(getXml(), styled.getXml());
        }
        return false;
    }
    public boolean equalsValue(String value) {
        if (value == null) {
            return isNull();
        }
        return value.equals(getXml());
    }
    @Override
    public int compareTo(StringItem stringItem) {
        if (stringItem == null) {
            return -1;
        }
        if (stringItem == this) {
            return 0;
        }
        int i = compareStringValue(stringItem);
        if (i != 0) {
            return i;
        }
        return compareReferences(stringItem);
    }
    public int compareStringValue(StringItem stringItem) {
        int i = -1 * CompareUtil.compare(hasStyle(), stringItem.hasStyle());
        if (i != 0) {
            return i;
        }
        return CompareUtil.compare(get(), stringItem.get());
    }
    public int compareReferences(StringItem stringItem) {
        return CompareUtil.compare(stringItem.getReferencesSize(), this.getReferencesSize());
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(NAME_string, get());
        if (hasStyle()) {
            jsonObject.put(NAME_style, getStyle().toJson());
        }
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        set(json);
    }
    @Override
    public String toString() {
        String xml = getXml();
        if (xml == null) {
            return getIndex() + ": NULL";
        }
        StringPool<?> stringPool = getParentInstance(StringPool.class);
        if (stringPool != null && !stringPool.isStringLinkLocked()) {
            return getIndex() + ": USED BY=" + getReferencesSize() + "{" + xml + "}";
        }
        return getIndex() + ":" + xml;
    }

    private static int[] decodeUtf8StringByteLength(byte[] lengthBytes) {
        int offset = 0;
        int val = lengthBytes[offset];
        int length;
        if ((val & 0x80) != 0) {
            offset += 2;
        } else {
            offset += 1;
        }
        val = lengthBytes[offset];
        offset += 1;
        if ((val & 0x80) != 0) {
            int low = (lengthBytes[offset] & 0xFF);
            length = val & 0x7F;
            length = length << 8;
            length = length + low;
            offset += 1;
        } else {
            length = val;
        }
        return new int[] { offset, length};
    }
    private static int[] decodeUtf16StringByteLength(byte[] lengthBytes) {
        int val = ((lengthBytes[1] & 0xFF) << 8 | lengthBytes[0] & 0xFF);
        if ((val & 0x8000) != 0) {
            int high = (lengthBytes[3] & 0xFF) << 8;
            int low = (lengthBytes[2] & 0xFF);
            int len_value =  ((val & 0x7FFF) << 16) + (high + low);
            return new int[] {4, len_value * 2};

        }
        return new int[] {2, val * 2};
    }
    static boolean isNullBytes(byte[] bytes) {
        if (bytes == null) {
            return true;
        }
        int length = bytes.length;
        if (length < 2) {
            return true;
        }
        for (int i = 2; i < length; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }


    private static byte[] encodeUtf8ToBytes(String str) {
        byte[] bytes;
        byte[] lenBytes = new byte[2];
        if (str != null) {
            bytes = str.getBytes(StandardCharsets.UTF_8);
            int strLen = bytes.length;
            if ((strLen & 0xff80) != 0) {
                lenBytes = new byte[4];
                int l2 = strLen & 0xff;
                int l1 = (strLen-l2) >> 8;
                lenBytes[3] = (byte) (l2);
                lenBytes[2] = (byte) (l1 | 0x80);
                strLen = str.length();
                l2 = strLen & 0xff;
                l1 = (strLen - l2) >> 8;
                lenBytes[1] = (byte) (l2);
                lenBytes[0] = (byte) (l1 | 0x80);
            } else {
                lenBytes = new ShortItem((short) strLen).getBytesInternal();
                lenBytes[1] = lenBytes[0];
                lenBytes[0] = (byte) str.length();
            }
        } else {
            bytes = new byte[0];
        }
        return addBytes(lenBytes, bytes, new byte[1]);
    }
    private static byte[] encodeUtf16ToBytes(String str) {
        if (str == null) {
            return null;
        }
        byte[] lenBytes;
        byte[] bytes = getUtf16Bytes(str);
        int strLen = bytes.length;
        strLen = strLen / 2;
        if ((strLen & 0xffff8000) != 0) {
            lenBytes = new byte[4];
            int low = strLen & 0xff;
            int high = (strLen - low) & 0xff00;
            int rem = strLen - low - high;
            lenBytes[3] = (byte) (high >> 8);
            lenBytes[2] = (byte) low;
            low = rem & 0xff;
            high = (rem & 0xff00) >> 8;
            lenBytes[1] = (byte) (high | 0x80);
            lenBytes[0] = (byte) low;
        } else {
            lenBytes = new ShortItem((short) strLen).getBytesInternal();
        }
        return addBytes(lenBytes, bytes, new byte[2]);
    }
    static byte[] getUtf16Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_16LE);
    }

    private static byte[] addBytes(byte[] bytes1, byte[] bytes2, byte[] bytes3) {
        if (bytes1 == null && bytes2 == null && bytes3 == null) {
            return null;
        }
        int length = 0;
        if (bytes1 != null) {
            length = bytes1.length;
        }
        if (bytes2 != null) {
            length += bytes2.length;
        }
        if (bytes3 != null) {
            length += bytes3.length;
        }
        byte[] result = new byte[length];
        int start = 0;
        if (bytes1 != null) {
            start = bytes1.length;
            System.arraycopy(bytes1, 0, result, 0, start);
        }
        if (bytes2 != null) {
            System.arraycopy(bytes2, 0, result, start, bytes2.length);
            start += bytes2.length;
        }
        if (bytes3 != null) {
            System.arraycopy(bytes3, 0, result, start, bytes3.length);
        }
        return result;
    }

    private static final CharsetDecoder UTF16LE_DECODER = StandardCharsets.UTF_16LE.newDecoder();
    private static final CharsetDecoder DECODER_3B = ThreeByteCharsetDecoder.INSTANCE;

    public static final String NAME_string = ObjectsUtil.of("string");
    public static final String NAME_style = ObjectsUtil.of("style");

    private static final Predicate<Object> NON_STYLE_INDEX = item ->
            !(item instanceof StyleItem.StyleIndexReference);
}
