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

import com.reandroid.dex.common.OperandType;
import com.reandroid.dex.common.Register;
import com.reandroid.dex.common.RegisterFormat;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.program.Instruction;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class SmaliInstruction extends SmaliCode implements Instruction {

    private final Opcode<?> opcode;
    private final SmaliRegisterSet registerSet;
    private final SmaliInstructionOperand operand;

    private int address;

    public SmaliInstruction(Opcode<?> opcode) {
        super();
        if (opcode == null) {
            throw new NullPointerException();
        }

        this.opcode = opcode;
        this.registerSet = SmaliRegisterSet.registerSetFor(opcode);
        this.operand = SmaliInstructionOperand.operandFor(opcode);

        registerSet.setParent(this);
        operand.setParent(this);
    }

    public SmaliInstruction getPrevious() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.getPreviousInstruction(getIndex() - 1);
        }
        return null;
    }
    public SmaliInstruction getNext() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.getNextInstruction(getIndex() + 1);
        }
        return null;
    }
    @Override
    public Key getAsKey() {
        return getOperand().getAsKey();
    }
    public SmaliLabel getAsSourceLabel() {
        return getOperand().getAsLabel();
    }
    public Key getKey() {
        SmaliInstructionOperand operand = getOperand();
        if (operand instanceof SmaliInstructionOperand.SmaliKeyOperand) {
            return ((SmaliInstructionOperand.SmaliKeyOperand) operand).getKey();
        }
        return null;
    }
    public Key getKey2() {
        SmaliInstructionOperand operand = getOperand();
        if (operand instanceof SmaliInstructionOperand.SmaliDualKeyOperand) {
            return ((SmaliInstructionOperand.SmaliDualKeyOperand) operand).getKey2();
        }
        return null;
    }
    public boolean hasNumberData() {
        SmaliInstructionOperand operand = getOperand();
        return operand instanceof SmaliInstructionOperand.SmaliHexOperand ||
                operand instanceof SmaliInstructionOperand.SmaliLabelOperand;
    }
    public long getDataAsLong() {
        SmaliInstructionOperand operand = getOperand();
        if (operand instanceof SmaliInstructionOperand.SmaliHexOperand) {
            return operand.getValueAsLong();
        }
        if (operand instanceof SmaliInstructionOperand.SmaliLabelOperand) {
            return ((int)operand.getValueAsLong()) - getAddress();
        }
        return 0;
    }
    @Override
    public int getAddress() {
        return address;
    }
    public void setAddress(int address) {
        this.address = address;
    }

    @Override
    public Opcode<?> getOpcode() {
        return opcode;
    }
    @Override
    public int getCodeUnits() {
        return getOpcode().size() / 2;
    }
    @Override
    public void addReferencingLabel(Object label) {
        // TODO
    }
    @Override
    public boolean isRemoved() {
        return getParent() == null;
    }
    public SmaliLabelSet getSmaliLabelSet() {
        return new SmaliLabelSet(this);
    }

    public Register getRegister() {
        return getRegister(0);
    }
    public Register getRegister(int i) {
        return getRegisterSet().getRegister(i);
    }
    public int getRegistersCount() {
        return getRegisterSet().size();
    }
    public RegistersTable getRegistersTable() {
        return getRegisterSet().getRegistersTable();
    }
    public void setRegistersTable(RegistersTable registersTable) {
        getRegisterSet().setRegistersTable(registersTable);
    }
    public SmaliRegisterSet getRegisterSet() {
        return registerSet;
    }
    public RegisterFormat getRegisterFormat() {
        return getOpcode().getRegisterFormat();
    }
    public SmaliInstructionOperand getOperand() {
        return operand;
    }
    public OperandType getOperandType() {
        return getOperand().getOperandType();
    }
    public boolean hasLabelOperand(InstructionLabel label) {
        return getOperand().isSourceLabel(label);
    }
    public SmaliMethod getParentMethod() {
        return getParentInstance(SmaliMethod.class);
    }
    public SmaliClass getParentClass() {
        return getParentInstance(SmaliClass.class);
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        Opcode<?> opcode = getOpcode();
        if (opcode == null) {
            return;
        }
        writer.newLine();
        opcode.append(writer);
        getRegisterSet().append(writer);
        if (opcode.getRegisterFormat() != RegisterFormat.NONE &&
                opcode.getOperandType() != OperandType.NONE) {
            writer.append(", ");
        }
        getOperand().append(writer);
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        setOrigin(reader.getCurrentOrigin());
        Opcode<?> opcode = parseOpcode(reader);
        getRegisterSet().parse(reader);

        if (opcode.getRegisterFormat() != RegisterFormat.NONE &&
                opcode.getOperandType() != OperandType.NONE) {
            reader.skipWhitespacesOrComment();
            SmaliParseException.expect(reader, ',');
            reader.skipWhitespacesOrComment();
        }
        getOperand().parse(opcode, reader);
    }
    private Opcode<?> parseOpcode(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        Opcode<?> opcode = Opcode.parseSmali(reader, true);
        if (opcode != this.getOpcode()) {
            throw new SmaliParseException("Expecting opcode: " + getOpcode()
                    + ", but found: " + opcode, reader);
        }
        return opcode;
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        getRegisterSet().validate();
        getOperand().validate();
    }

    public static SmaliInstruction createInstruction(Opcode<?> opcode) {
        SmaliInstruction instruction;
        if (opcode == Opcode.ARRAY_PAYLOAD) {
            instruction = new SmaliPayloadArray();
        } else if (opcode == Opcode.PACKED_SWITCH_PAYLOAD) {
            instruction = new SmaliPayloadPackedSwitch();
        } else if (opcode == Opcode.SPARSE_SWITCH_PAYLOAD) {
            instruction = new SmaliPayloadSparseSwitch();
        } else {
            instruction = new SmaliInstruction(opcode);
        }
        return instruction;
    }
}
