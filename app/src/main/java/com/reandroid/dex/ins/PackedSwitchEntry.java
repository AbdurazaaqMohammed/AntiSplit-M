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

import com.reandroid.arsc.item.IntegerItem;
import com.reandroid.dex.program.Instruction;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliPackedSwitchEntry;
import com.reandroid.utils.HexUtil;

import java.io.IOException;

public class PackedSwitchEntry extends IntegerItem implements SwitchEntry {

    private Instruction targetIns;

    public PackedSwitchEntry() {
        super();
    }

    @Override
    public Instruction getTargetInstruction() {
        Instruction targetIns = this.targetIns;
        if (targetIns == null) {
            setTargetInstruction(findTargetIns());
            targetIns = this.targetIns;
        }
        return targetIns;
    }

    @Override
    public void setTargetInstruction(Instruction targetIns) {
        if (targetIns != this.targetIns) {
            this.targetIns = targetIns;
            if (targetIns != null) {
                targetIns.addReferencingLabel(this);
            }
        }
    }

    @Override
    public InsPackedSwitchData getOwnerInstruction() {
        return getParentDataList().getSwitchData();
    }

    @Override
    public int get() {
        return getParentDataList().getFirstKey() + getIndex();
    }

    @Override
    public void set(int value) {
        if (value != this.get()) {
            getParentDataList().onDataChange(getIndex(), value);
        }
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.newLine();
        writer.appendLabelName(getLabelName());
        int value = this.get();
        writer.appendComment(HexUtil.toSignedHex(value));
        writer.appendResourceIdComment(value);
    }

    @Override
    public void appendLabelName(SmaliWriter writer) throws IOException {
        writer.appendLabelName(getLabelName());
    }
    @Override
    public void appendLabelComment(SmaliWriter writer) throws IOException {
        writer.appendComment(HexUtil.toSignedHex(get()));
    }

    @Override
    public int getOwnerAddress() {
        return super.get();
    }

    public void setAddress(int address) {
        super.set(address);
    }

    @Override
    public int getTargetAddress() {
        return getParentDataList().getBaseAddress() + getOwnerAddress();
    }

    @Override
    public void setTargetAddress(int targetAddress) {
        setAddress(targetAddress - getParentDataList().getBaseAddress());
    }

    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.P_SWITCH;
    }

    @Override
    public boolean equalsLabel(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        SwitchEntry entry = (SwitchEntry) obj;
        return getTargetAddress() == entry.getTargetAddress();
    }

    private PackedSwitchDataList getParentDataList() {
        return (PackedSwitchDataList) getParent();
    }

    public void merge(PackedSwitchEntry data) {
        setAddress(data.getOwnerAddress());
    }

    public void fromSmali(SmaliPackedSwitchEntry smaliEntry) {
        setAddress(smaliEntry.getRelativeOffset());
    }

    public SmaliPackedSwitchEntry toSmali() {
        SmaliPackedSwitchEntry entry = new SmaliPackedSwitchEntry();
        entry.setLabelName(getLabelName());
        return entry;
    }

    @Override
    public int hashCode() {
        return getIndex();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PackedSwitchEntry data = (PackedSwitchEntry) obj;
        return getIndex() == data.getIndex() && getParent() == data.getParent();
    }

    @Override
    public String toString() {
        return getLabelName();
    }
}
