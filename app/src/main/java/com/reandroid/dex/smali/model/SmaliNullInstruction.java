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
import com.reandroid.dex.program.InstructionOpcode;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class SmaliNullInstruction extends SmaliInstruction {

    public SmaliNullInstruction() {
        super(Opcode.NOP);
    }

    @Override
    public int getCodeUnits() {
        return 0;
    }
    @Override
    public int getIndex() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.size();
        }
        return -1;
    }
    @Override
    public boolean is(InstructionOpcode opcode) {
        return false;
    }
    @Override
    public void parse(SmaliReader reader) throws IOException {
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
    }
}
