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

import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.common.Register;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.program.Instruction;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelSet;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliRegion;
import com.reandroid.utils.ObjectsUtil;

import java.util.Iterator;
import java.util.List;

public abstract class InsSwitchPayload<T extends SwitchEntry> extends PayloadData<T>
        implements InstructionLabelSet, SmaliRegion, SmaliFormat {

    private InsSwitch insSwitch;

    public InsSwitchPayload(int childesCount, Opcode<?> opcode) {
        super(childesCount, opcode);
    }

    @Override
    public void updateTargetAddress() {
        super.updateTargetAddress();
        InsSwitch insSwitch = getSwitch();
        if (insSwitch != null) {
            insSwitch.setTargetInstruction(this);
            insSwitch.updateTargetAddress();
            for (SwitchEntry switchEntry : this) {
                switchEntry.updateTargetAddress();
            }
        }
    }

    @Override
    void linkTargetInstruction() {
        super.linkTargetInstruction();
        for(SwitchEntry switchEntry : this) {
            switchEntry.getTargetInstruction();
        }
    }

    @Override
    void detachTargetInstructions() {
        super.detachTargetInstructions();
        for(SwitchEntry switchEntry : this) {
            switchEntry.setTargetInstruction(null);
        }
    }

    @Override
    public Iterator<IntegerReference> getReferences() {
        return ObjectsUtil.cast(iterator());
    }
    public InsSwitch getSwitch() {
        InsSwitch insSwitch = this.insSwitch;
        if (insSwitch == null) {
            insSwitch = findOnReferencingLabels();
            if (insSwitch == null) {
                insSwitch = findByAddress();
            }
            this.insSwitch = insSwitch;
            if (insSwitch != null) {
                insSwitch.setPayload(this);
            }
        }
        return insSwitch;
    }
    public void setSwitch(InsSwitch insSwitch) {
        if (insSwitch == null) {
            this.insSwitch = null;
        } else if (getSwitchOpcode() != insSwitch.getOpcode()) {
            throw new ClassCastException("Incompatible switch opcode: '" + getSwitchOpcode()
                    + "' vs '" + insSwitch.getOpcode() + "'");
        } else {
            this.insSwitch = insSwitch;
            addReferencingLabel(insSwitch);
            insSwitch.setPayload(this);
        }
    }
    public abstract Opcode<? extends InsSwitch> getSwitchOpcode();

    private InsSwitch findOnReferencingLabels() {
        Iterator<InstructionLabel> iterator = getReferencingLabels();
        while (iterator.hasNext()) {
            Instruction instruction = iterator.next().getOwnerInstruction();
            if (instruction instanceof InsSwitch) {
                InsSwitch insSwitch = (InsSwitch) instruction;
                if (insSwitch.getOpcode() == getSwitchOpcode()) {
                    return insSwitch;
                }
            }
        }
        return null;
    }
    private InsSwitch findByAddress() {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            Iterator<? extends InsSwitch> iterator = instructionList
                    .iterator(getSwitchOpcode());
            int address = getAddress();
            while (iterator.hasNext()) {
                InsSwitch sparseSwitch = iterator.next();
                if (sparseSwitch.getTargetAddress() == address) {
                    return sparseSwitch;
                }
            }
        }
        return null;
    }

    public boolean replaceByIfEq() {
        InstructionList instructionList = getInstructionList();
        RegistersTable registersTable = instructionList.getRegistersTable();
        int local = registersTable.getLocalRegistersCount();
        int constRegister = local;
        local = local + 1;
        InsSwitch insSwitch = getSwitch();
        if (!registersTable.ensureLocalRegistersCount(local)) {
            List<Register> freeRegisters = instructionList.getLocalFreeRegisters(insSwitch.getIndex());
            if (freeRegisters.isEmpty()) {
                return false;
            }
            // FIXME: consider using InstructionList#getLocalFreeRegisters
            constRegister = freeRegisters.get(0).getValue();
        }
        if (constRegister > 0xf) {
            // FIXME
            return false;
        }
        replaceByIfEq(constRegister);
        return true;
    }
    public void replaceByIfEq(int constRegister) {
        InsSwitch packedSwitch = getSwitch();
        InsBlockList insBlockList = getInsBlockList();
        Object lock = insBlockList.link(new Object());
        linkTargetInstruction();
        for (SwitchEntry switchEntry : this) {
            switchEntry.addEquivalentIfEq(constRegister);
        }
        InstructionList instructionList = getInstructionList();
        instructionList.remove(packedSwitch);
        instructionList.remove(this);
        insBlockList.unlinkLocked(lock);
    }
}
