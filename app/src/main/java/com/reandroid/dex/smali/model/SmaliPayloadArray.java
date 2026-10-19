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
package com.reandroid.dex.smali.model;

import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.key.PrimitiveKey;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;

import java.io.IOException;
import java.util.Iterator;

public class SmaliPayloadArray extends SmaliInstructionPayload<SmaliValueX> {

    public SmaliPayloadArray() {
        super(Opcode.ARRAY_PAYLOAD);
    }

    public void addEntryKeys(Iterator<PrimitiveKey> iterator) {
        while (iterator.hasNext()) {
            addEntry(iterator.next());
        }
    }
    public void addEntry(PrimitiveKey key) {
        newEntry().setKey(key);
    }
    public long[] getValuesAsLong() {
        SmaliSet<SmaliValueX> entries = getEntries();
        int size = entries.size();
        long[] result = new long[size];
        for(int i = 0; i < size; i++) {
            result[i] = entries.get(i).getValueAsLong();
        }
        return result;
    }
    @Override
    public SmaliInstructionOperand.SmaliDecimalOperand getOperand() {
        return (SmaliInstructionOperand.SmaliDecimalOperand) super.getOperand();
    }

    public int getWidth() {
        return getOperand().getNumber();
    }
    public void setWidth(int width) {
        getOperand().setNumber(width);
        SmaliSet<SmaliValueX> entries = getEntries();
        int size = entries.size();
        for (int i = 0; i < size; i++) {
            entries.get(i).setWidth(width);
        }
    }

    @Override
    public int getCodeUnits() {
        int count = getCount();
        int width = getWidth();

        int size = 2 // opcode bytes
                + 2  // width short reference
                + 4  // count integer reference
                + width * count;

        int align = (2 - (size % 2)) % 2;

        size += align;
        return size / 2;
    }
    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.ARRAY_DATA;
    }
    @Override
    SmaliValueX createEntry() {
        SmaliValueX value =  new SmaliValueX();
        value.setWidth(getWidth());
        return value;
    }

    @Override
    void parseOperand(Opcode<?> opcode, SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        int position = reader.position();
        SmaliInstructionOperand.SmaliDecimalOperand operand = getOperand();
        operand.parse(opcode, reader);
        int width = operand.getNumber();

        if(width < 0 || width > 8) {
            reader.position(position);
            throw new SmaliParseException("Array width out of range (0 .. 8) : '" + width + "'", reader);
        }
    }
    @Override
    public void validateEntries() {
    }
}
