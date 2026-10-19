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
import com.reandroid.dex.smali.*;

import java.io.IOException;
import java.util.Iterator;

public abstract class SmaliInstructionPayload<T extends Smali> extends SmaliInstruction
        implements SmaliRegion {

    private final SmaliSet<T> entries;

    public SmaliInstructionPayload(Opcode<?> opcode) {
        super(opcode);
        this.entries = new SmaliSet<>();
        this.entries.setParent(this);
    }

    @Override
    public abstract int getCodeUnits();
    @Override
    public Opcode<?> getOpcode() {
        return super.getOpcode();
    }
    abstract T createEntry();
    public void addEntry(T entry) {
        getEntries().add(entry);
    }
    public T getEntry(int i) {
        return getEntries().get(i);
    }
    public int getCount() {
        return getEntries().size();
    }
    public Iterator<T> entries() {
        return getEntries().iterator();
    }
    public SmaliSet<T> getEntries() {
        return entries;
    }
    public T newEntry() {
        T entry = createEntry();
        addEntry(entry);
        return entry;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        getSmaliDirective().append(writer);
        getOperand().append(writer);
        writer.indentPlus();
        getEntries().append(writer);
        writer.indentMinus();
        getSmaliDirective().appendEnd(writer);
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        setOrigin(reader.getCurrentOrigin());
        SmaliDirective directive = getSmaliDirective();
        SmaliParseException.expect(reader, directive);
        reader.skipSpaces();
        parseOperand(getOpcode(), reader);
        reader.skipWhitespacesOrComment();
        while (!directive.isEnd(reader)) {
            newEntry().parse(reader);
            reader.skipWhitespacesOrComment();
        }
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, getSmaliDirective(), true);
    }
    void parseOperand(Opcode<?> opcode, SmaliReader reader) throws IOException {
        getOperand().parse(opcode, reader);
    }
    @Override
    public void validate() throws IOException {
        super.validate();
        validateEntries();
    }
    public void validateEntries() throws IOException {
        getEntries().validate();
    }
}
