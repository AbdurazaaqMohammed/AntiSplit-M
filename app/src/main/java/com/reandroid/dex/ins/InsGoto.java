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

import com.reandroid.arsc.item.ByteArray;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.HexUtil;

import java.io.IOException;

public class InsGoto extends SizeXIns implements InstructionLabel {

    private Opcode<?> opcode;

    public InsGoto(Opcode<?> opcode) {
        super(opcode);
        this.opcode = opcode;
    }
    @Override
    public int getData() {
        int size = getOpcode().size();
        if(size == 2){
            return getByteSigned();
        }
        if(size == 4){
            return getShortSigned();
        }
        return getInteger();
    }
    @Override
    public void setData(int data) {
        int size = getOpcode().size();
        if (size == 2) {
            setByte(1, data);
        } else if(size == 4) {
            setShort(2, data);
        } else {
            setInteger(data);
        }
    }
    @Override
    public int getTargetAddress() {
        return getAddress() + getData();
    }
    @Override
    public void setTargetAddress(int targetAddress) {
        int data = targetAddress - getAddress();
        if(!canFitOpcode(data)) {
            Ins targetIns = null;
            InsBlockList insBlockList = getInsBlockList();
            if(insBlockList != null) {
                targetIns = insBlockList.getAtAddress(targetAddress);
            }
            ensureFittingOpcode(data);
            if(targetIns != null) {
                targetAddress = targetIns.getAddress();
            }
            data = targetAddress - getAddress();
        }
        setData(data);
    }
    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.GOTO;
    }
    @Override
    public void appendCode(SmaliWriter writer) throws IOException {
        writer.append(getOpcode().getName());
        writer.append(' ');
        writer.appendLabelName(getLabelName());
    }

    @Override
    public int getCodeUnits() {
        return getOpcode().size() / 2;
    }

    @Override
    public Opcode<?> getOpcode() {
        return this.opcode;
    }
    private boolean canFitOpcode(int data) {
        int size = getOpcode().size();
        if (size == 2) {
            return data == (byte) data;
        } else if(size == 4) {
            return data == (short) data;
        }
        return true;
    }
    private void ensureFittingOpcode(int data) {
        Opcode<InsGoto> opcode = null;
        int size = getOpcode().size();
        if (size == 2 && data != (byte) data) {
            if(data == (short) data) {
                opcode = Opcode.GOTO_16;
            } else {
                opcode = Opcode.GOTO_32;
            }
        } else if(size == 4 && data != (short) data) {
            opcode = Opcode.GOTO_32;
        }
        if(opcode != null) {
            setOpcode(opcode);
        }
    }
    public void setOpcode(Opcode<InsGoto> opcode) {
        if(opcode == this.opcode) {
            return;
        }
        int data = getData();
        InsBlockList insBlockList = getInsBlockList();
        if(insBlockList != null) {
            insBlockList.link();
        }
        this.opcode = opcode;
        ByteArray byteArray = getValueBytes();
        byteArray.setSize(0);
        byteArray.setSize(opcode.size());
        byteArray.putShort(0, opcode.getValue());
        setData(data);
        if(insBlockList != null) {
            insBlockList.unlink();
        }
    }
}