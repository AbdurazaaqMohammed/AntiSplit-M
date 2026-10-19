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
package com.reandroid.dex.key;

import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.collection.ComputeIterator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class ArrayValueKey extends ArrayKey<Key> {

    private static final ArrayValueKey EMPTY;

    static {
        EMPTY = new ArrayValueKey(EMPTY_ARRAY);
    }

    private ArrayValueKey(Key[] elements) {
        super(elements);
    }

    public Iterator<String> stringValuesIterator() {
        return ComputeIterator.of(iterator(StringKey.class), StringKey::getString);
    }

    public boolean isStrings() {
        return ArrayKeyHelper.isAllTypeOf(StringKey.class, this);
    }
    public boolean isBytes() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.ByteKey.class, this);
    }
    public boolean isShorts() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.ShortKey.class, this);
    }
    public boolean isIntegers() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.IntegerKey.class, this);
    }
    public boolean isLongs() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.LongKey.class, this);
    }
    public boolean isFloats() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.FloatKey.class, this);
    }
    public boolean isDoubles() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.DoubleKey.class, this);
    }
    public boolean isNumbers() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.NumberKey.class, this);
    }
    public boolean isBooleans() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.BooleanKey.class, this);
    }
    public boolean isChars() {
        return ArrayKeyHelper.isAllTypeOf(PrimitiveKey.CharKey.class, this);
    }
    public boolean isValuesTypeOfKey(Class<? extends Key> keyClass) {
        return ArrayKeyHelper.isAllTypeOf(keyClass, this);
    }


    public String[] getStringValues() {
        return ArrayKeyHelper.toStringValues(this);
    }
    public byte[] getByteValues() {
        return ArrayKeyHelper.toByteValues(this);
    }
    public short[] getShortValues() {
        return ArrayKeyHelper.toShortValues(this);
    }
    public int[] getIntegerValues() {
        return ArrayKeyHelper.toIntValues(this);
    }
    public long[] getLongValues() {
        return ArrayKeyHelper.toLongValues(this);
    }
    public float[] getFloatValues() {
        return ArrayKeyHelper.toFloatValues(this);
    }
    public double[] getDoubleValues() {
        return ArrayKeyHelper.toDoubleValues(this);
    }
    public char[] getCharValues() {
        return ArrayKeyHelper.toCharValues(this);
    }
    public boolean[] getBooleanValues() {
        return ArrayKeyHelper.toBooleanValues(this);
    }
    public long[] getNumberValues() {
        return ArrayKeyHelper.toNumberValues(this);
    }

    @Override
    public ArrayValueKey add(Key item) {
        return (ArrayValueKey) super.add(item);
    }
    @Override
    public ArrayValueKey remove(Key itemKey) {
        return (ArrayValueKey) super.remove(itemKey);
    }
    @Override
    public ArrayValueKey remove(int index) {
        return (ArrayValueKey) super.remove(index);
    }
    @Override
    public ArrayValueKey removeIf(Predicate<? super Key> predicate) {
        return (ArrayValueKey) super.removeIf(predicate);
    }
    @Override
    public ArrayValueKey set(int i, Key item) {
        return (ArrayValueKey) super.set(i, item);
    }
    @Override
    public ArrayValueKey setSize(int i, Key item) {
        return (ArrayValueKey) super.setSize(i, item);
    }
    @Override
    public ArrayValueKey sort(Comparator<? super Key> comparator) {
        return (ArrayValueKey) super.sort(comparator);
    }
    @Override
    public ArrayValueKey clearDuplicates() {
        return (ArrayValueKey) super.clearDuplicates();
    }
    @Override
    public ArrayValueKey clearDuplicates(Comparator<? super Key> comparator) {
        return (ArrayValueKey) super.clearDuplicates(comparator);
    }

    @Override
    public ArrayValueKey replaceKey(Key search, Key replace) {
        return (ArrayValueKey) super.replaceKey(search, replace);
    }

    @Override
    protected ArrayValueKey newInstance(Key[] elements) {
        return of(elements);
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        int size = this.size();
        writer.append('{');
        writer.indentPlus();
        for (int i = 0; i < size; i++) {
            Key key = get(i);
            if (i != 0) {
                writer.append(',');
            }
            writer.newLine();
            key.append(writer);
        }
        writer.indentMinus();
        if (size != 0) {
            writer.newLine();
        }
        writer.append('}');
    }

    @Override
    public String toString() {
        return SmaliWriter.toStringSafe(this);
    }


    public static ArrayValueKey empty() {
        return EMPTY;
    }
    public static ArrayValueKey of(Key ... elements) {
        if (elements == null || elements.length == 0) {
            return empty();
        }
        return new ArrayValueKey(elements);
    }
    public static ArrayValueKey create(ArrayKey<?> arrayKey) {
        if (arrayKey instanceof ArrayValueKey) {
            return (ArrayValueKey) arrayKey;
        }
        if (arrayKey.isEmpty()) {
            return empty();
        }
        return of(arrayKey.getElements());
    }

    public static ArrayValueKey read(SmaliReader reader) throws IOException {
        SmaliParseException.expect(reader, '{');
        return of(readElements(reader, '}'));
    }

    public static ArrayValueKey parse(String text) {
        //FIXME
        throw new RuntimeException("ArrayValueKey.parse not implemented");
    }

    public static ArrayValueKey of(String[] values) {
        return of(ArrayKeyHelper.toStringKeys(values));
    }
    public static ArrayValueKey of(byte[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(short[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(int[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(long[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(float[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(double[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(char[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
    public static ArrayValueKey of(boolean[] values) {
        return of(ArrayKeyHelper.toPrimitiveKeys(values));
    }
}
