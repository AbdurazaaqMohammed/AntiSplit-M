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
import com.reandroid.dex.program.InstructionLabelSet;

import java.util.Iterator;

public abstract class SmaliSwitchPayload<T extends SmaliSwitchEntry> extends SmaliInstructionPayload<T>
        implements InstructionLabelSet {

    private SmaliInstruction switchInstruction;

    public SmaliSwitchPayload(Opcode<?> opcode) {
        super(opcode);
    }

    @Override
    public Iterator<T> getLabels() {
        return entries();
    }

    public abstract Opcode<?> getSwitchOpcode();

    public SmaliInstruction getSwitch() {
        SmaliInstruction switchInstruction = this.switchInstruction;
        if (switchInstruction == null) {
            switchInstruction = findSwitch();
            this.switchInstruction = switchInstruction;
        }
        return switchInstruction;
    }
    public void setSwitch(SmaliInstruction switchInstruction) {
        this.switchInstruction = switchInstruction;
    }
    private SmaliInstruction findSwitch() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            Opcode<?> switchOpcode = getSwitchOpcode();
            Iterator<SmaliLabelDestination> iterator = getSmaliLabelSet()
                    .iterator(SmaliLabelDestination.class);
            while (iterator.hasNext()) {
                Iterator<SmaliInstruction> instructions = codeSet.getSourcingInstructions(
                        iterator.next());
                while (instructions.hasNext()) {
                    SmaliInstruction instruction = instructions.next();
                    if (switchOpcode == instruction.getOpcode()) {
                        return instruction;
                    }
                }
            }
        }
        return null;
    }
}
