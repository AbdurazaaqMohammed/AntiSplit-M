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

import com.reandroid.dex.base.DexException;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;

import java.io.IOException;

public abstract class SmaliLabel extends SmaliCode implements InstructionLabel {

    private String labelName;
    private InstructionLabelType labelType;

    public SmaliLabel(InstructionLabelType labelType) {
        super();
        this.labelType = labelType;
    }

    @Override
    public String getLabelName() {
        return labelName;
    }
    public void setLabelName(String labelName) {
        if (!StringsUtil.isEmpty(labelName)) {
            char c = labelName.charAt(0);
            if (c != ':' && c != '.') {
                labelName = ":" + labelName;
            }
        }
        setLabelNameInternal(labelName);
    }
    private void setLabelNameInternal(String labelName) {
        this.labelName = labelName;
    }

    public abstract SmaliLabel getDestinationLabel();

    public int getIntegerData() {
        int address = getTargetAddress();
        if (address == -1) {
            throw new DexException("Missing target label '" + getLabelName() + "'" + buildOrigin());
        }
        return address;
    }
    @Override
    public int getTargetAddress() {
        SmaliInstruction instruction = getTargetInstruction();
        if (instruction != null) {
            return instruction.getAddress();
        }
        return -1;
    }
    @Override
    public void setTargetAddress(int address) {
        // TODO
        throw new RuntimeException("Method not implemented");
    }
    @Override
    public abstract SmaliInstruction getTargetInstruction();

    @Override
    public int getOwnerAddress() {
        SmaliInstruction owner = getOwnerInstruction();
        if (owner != null) {
            return owner.getAddress();
        }
        return -1;
    }
    @Override
    public SmaliInstruction getOwnerInstruction() {
        return null;
    }

    public boolean isDestinationLabel() {
        return false;
    }
    public boolean isSourceLabel() {
        return false;
    }
    @Override
    public InstructionLabelType getLabelType() {
        // TODO: implement properly
        InstructionLabelType labelType = this.labelType;
        if (labelType == null) {
            labelType = InstructionLabelType.DEBUG;
        }
        return labelType;
    }
    public void setLabelType(InstructionLabelType labelType) {
        this.labelType = labelType;
    }
    public void setLabelTypeIfNull(InstructionLabelType labelType) {
        if (this.labelType == null && this.getLabelType() == InstructionLabelType.DEBUG) {
            setLabelType(labelType);
        }
    }

    @Override
    public boolean isRemoved() {
        return getParent() == null;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendLabelName(getLabelName());
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespaces();
        setOrigin(reader.getCurrentOrigin());
        SmaliParseException.expect(reader, ':');
        reader.skip(-1);
        int i1 = reader.indexOfWhiteSpaceOrComment();
        int i2 = reader.indexOfBeforeLineEnd('}');
        int i;
        if (i2 >= 0 && i2 < i1) {
            i = i2;
        } else {
            i = i1;
        }
        int length = i - reader.position();
        setLabelNameInternal(reader.readString(length));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SmaliLabel)) {
            return false;
        }
        SmaliLabel other = (SmaliLabel) obj;
        return ObjectsUtil.equals(getLabelName(), other.getLabelName());
    }
    @Override
    public int hashCode() {
        return ObjectsUtil.hash(getLabelName());
    }

    public static InstructionLabelType of(Opcode<?> opcode) {
        if (opcode == null) {
            return null;
        }
        if (opcode.isGoto()) {
            return InstructionLabelType.GOTO;
        }
        if (opcode.isIfTest()) {
            return InstructionLabelType.COND;
        }
        if (opcode == Opcode.PACKED_SWITCH) {
            return InstructionLabelType.P_SWITCH;
        }
        if (opcode == Opcode.SPARSE_SWITCH) {
            return InstructionLabelType.S_SWITCH;
        }
        if (opcode == Opcode.FILL_ARRAY_DATA) {
            return InstructionLabelType.ARRAY;
        }
        if (opcode == Opcode.ARRAY_PAYLOAD) {
            return InstructionLabelType.ARRAY_DATA;
        }
        if (opcode == Opcode.PACKED_SWITCH_PAYLOAD) {
            return InstructionLabelType.P_SWITCH_DATA;
        }
        if (opcode == Opcode.SPARSE_SWITCH_PAYLOAD) {
            return InstructionLabelType.S_SWITCH_DATA;
        }
        return null;
    }
}
