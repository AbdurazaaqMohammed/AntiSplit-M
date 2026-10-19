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

import com.reandroid.dex.common.Register;
import com.reandroid.dex.common.RegisterFormat;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.util.Iterator;

public class SmaliRegisterSet extends SmaliSet<SmaliRegister> implements
        Iterable<SmaliRegister>{

    private final RegisterFormat format;
    private RegistersTable registersTable;

    public SmaliRegisterSet(RegisterFormat format){
        super();
        this.format = format;
    }
    public SmaliRegisterSet() {
        this(RegisterFormat.READ16);
    }

    public RegistersTable getRegistersTable() {
        RegistersTable registersTable = this.registersTable;
        if(registersTable == null){
            return getParentInstance(SmaliMethod.class);
        }
        return registersTable;
    }
    public void setRegistersTable(RegistersTable registersTable) {
        this.registersTable = registersTable;
    }
    public Register getRegister(int i){
        SmaliRegister smaliRegister = get(i);
        if(smaliRegister != null){
            return smaliRegister.toRegister(getRegistersTable());
        }
        return null;
    }

    public RegisterFormat getFormat() {
        return format;
    }

    public void addRegister(boolean parameter, int number) {
        SmaliRegister register = new SmaliRegister();
        add(register);
        register.setParameter(parameter);
        register.setNumber(number);
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        boolean appendOnce = false;
        RegisterFormat format = getFormat();
        String separator = format.isRange() ? " .. ": ", ";
        if(format.isOut()){
            writer.append('{');
        }
        for(SmaliRegister register : this){
            if(appendOnce){
                writer.append(separator);
            }
            register.append(writer);
            appendOnce = true;
        }
        if(format.isOut()){
            writer.append('}');
        }
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        RegisterFormat format = getFormat();
        if(format.isRange()){
            parseOutRangeRegisters(reader);
        }else if(format.isOut()){
            parseOutRegisters(reader);
        }else {
            parseRegisters(reader);
        }
    }

    @Override
    public SmaliRegister parseAdd(int index, SmaliReader reader) throws IOException {
        SmaliRegister register = new SmaliRegister();
        add(index, register);
        register.parse(reader);
        reader.skipWhitespacesOrComment();
        return register;
    }

    private void parseRegisters(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        int size = getFormat().size();
        for(int i = 0; i < size; i++){
            if(i != 0){
                SmaliParseException.expect(reader, ',');
                reader.skipWhitespacesOrComment();
            }
            parseNext(reader);
        }
    }
    private void parseOutRegisters(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, '{');
        reader.skipWhitespacesOrComment();
        boolean parsedOnce = false;
        while (!reader.finished() && reader.get() != '}'){
            if(parsedOnce){
                SmaliParseException.expect(reader, ',');
                reader.skipWhitespacesOrComment();
            }
            parseNext(reader);
            parsedOnce = true;
        }
        SmaliParseException.expect(reader, '}');
    }
    private void parseOutRangeRegisters(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, '{');
        reader.skipWhitespacesOrComment();

        if (reader.get() != '}') {
            // first register
            parseNext(reader);

            SmaliParseException.expect(reader, '.');
            reader.skipWhitespaces();
            SmaliParseException.expect(reader, '.');
            reader.skipWhitespacesOrComment();

            // second register
            parseNext(reader);
        }

        SmaliParseException.expect(reader, '}');
    }

    public static SmaliRegisterSet registerSetFor(Opcode<?> opcode) {
        RegisterFormat format = opcode.getRegisterFormat();
        if (format == RegisterFormat.NONE) {
            return SmaliRegisterSet.NO_REGISTER_SET;
        } else {
            return new SmaliRegisterSet(format);
        }
    }

    public static final SmaliRegisterSet NO_REGISTER_SET = new SmaliRegisterSet(RegisterFormat.NONE){
        @Override
        public boolean add(SmaliRegister smali) {
            throw new RuntimeException("NO_REGISTER_SET");
        }
        @Override
        public Iterator<SmaliRegister> iterator() {
            return EmptyIterator.of();
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public void setRegistersTable(RegistersTable registersTable) {
        }
        @Override
        public void append(SmaliWriter writer) {
        }
        @Override
        public void parse(SmaliReader reader) {
        }
        @Override
        public SmaliRegister parseAdd(int index, SmaliReader reader) {
            return null;
        }

        @Override
        public String toString() {
            return "";
        }
    };
}
