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
import com.reandroid.dex.program.Instruction;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliSparseSwitchEntry;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.ObjectsUtil;

import java.io.IOException;

public class SparseSwitchEntry implements SwitchEntry {

    private final InsSparseSwitchData payload;
    private final IntegerReference element;
    private final SparseSwitchEntryKey entryKey;

    public SparseSwitchEntry(InsSparseSwitchData payload,
                             IntegerReference element,
                             SparseSwitchEntryKey entryKey) {
        this.payload = payload;
        this.element = element;
        this.entryKey = entryKey;
    }

    @Override
    public InsSparseSwitchData getOwnerInstruction() {
        return payload;
    }

    @Override
    public int get() {
        return element.get();
    }
    @Override
    public void set(int value) {
        if (value != element.get()) {
            element.set(value);
            this.payload.mSortRequired = true;
        }
    }
    @Override
    public int getIndex() {
        return entryKey.getIndex();
    }

    @Override
    public Instruction getTargetInstruction() {
        Instruction target = this.entryKey.getTargetInstruction();
        if (target == null) {
            setTargetInstruction(findTargetIns());
            target = this.entryKey.getTargetInstruction();
        }
        return target;
    }

    @Override
    public void setTargetInstruction(Instruction target) {
        Instruction instruction = entryKey.getTargetInstruction();
        if (target != instruction) {
            entryKey.setTargetInstruction(target);
            if (target != null) {
                target.addReferencingLabel(this);
            }
        }
    }

    SparseSwitchEntryKey getEntryKey() {
        return entryKey;
    }
    IntegerReference getElement() {
        return element;
    }

    public int getKey() {
        return entryKey.get();
    }

    public void setKey(int value) {
        entryKey.set(value);
    }

    @Override
    public int getOwnerAddress() {
        return payload.getAddress();
    }

    @Override
    public int getTargetAddress() {
        return getKey() + payload.getBaseAddress();
    }

    @Override
    public void setTargetAddress(int targetAddress) {
        setKey(targetAddress - payload.getBaseAddress());
    }

    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.S_SWITCH;
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

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.newLine();
        int value = get();
        writer.appendHex(value);
        writer.append(" -> ");
        writer.appendLabelName(getLabelName());
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

    public void removeSelf() {
        payload.remove(this);
    }

    public void fromPackedSwitch(PackedSwitchEntry packedSwitchEntry) {
        this.set(packedSwitchEntry.get());
        Instruction ins = packedSwitchEntry.getTargetInstruction();
        this.setTargetAddress(ins.getAddress());
        this.setTargetInstruction(ins);
        ins.addReferencingLabel(this);
    }

    public void merge(SparseSwitchEntry data) {
        set(data.get());
        setKey(data.getKey());
    }

    public void fromSmali(SmaliSparseSwitchEntry smaliEntry) {
        set(smaliEntry.getValue());
        setKey(smaliEntry.getRelativeOffset());
    }

    public SmaliSparseSwitchEntry toSmali() {
        SmaliSparseSwitchEntry entry = new SmaliSparseSwitchEntry();
        entry.setLabelName(getLabelName());
        entry.setValue(get());
        return entry;
    }

    @Override
    public int hashCode() {
        return ObjectsUtil.hash(payload, element);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SparseSwitchEntry data = (SparseSwitchEntry) obj;
        return element == data.element && payload == data.payload;
    }

    @Override
    public String toString() {
        return HexUtil.toHex8(get()) + " -> " + getKey();
    }
}
