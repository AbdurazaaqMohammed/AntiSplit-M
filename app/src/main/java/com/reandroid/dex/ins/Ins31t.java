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
package com.reandroid.dex.ins;

import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class Ins31t extends Size6Ins implements RegistersSet, InstructionLabel {

    public Ins31t(Opcode<?> opcode) {
        super(opcode);
    }

    @Override
    public int getRegistersCount() {
        return 1;
    }
    @Override
    public final void setRegistersCount(int count) {
    }
    @Override
    public int getRegister(int index) {
        return getByteUnsigned(1);
    }
    @Override
    public void setRegister(int index, int value) {
        setByte(1, value);
    }

    @Override
    public int getData(){
        return getInteger();
    }
    public void setData(int data){
        setInteger(data);
    }

    @Override
    public int getTargetAddress() {
        return getAddress() + getData();
    }
    @Override
    public void setTargetAddress(int targetAddress){
        setData(targetAddress - getAddress());
    }
    @Override
    public void appendCode(SmaliWriter writer) throws IOException {
        Opcode<?> opcode = getOpcode();
        writer.append(opcode.getName());
        writer.append(' ');
        getRegistersIterator().append(writer);
        writer.append(", ");
        writer.appendLabelName(getLabelName());
    }
}