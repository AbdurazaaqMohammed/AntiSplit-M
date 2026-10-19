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

import com.reandroid.dex.base.DexException;
import com.reandroid.dex.common.OperandType;
import com.reandroid.dex.common.RegisterFormat;
import com.reandroid.dex.data.FixedDexContainerWithTool;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.data.MethodDef;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.program.Instruction;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.program.ProgramType;
import com.reandroid.dex.program.ReferenceLabelSet;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliCodeSet;
import com.reandroid.dex.smali.model.SmaliInstruction;
import com.reandroid.dex.smali.model.SmaliLabelDestination;
import com.reandroid.utils.ObjectsStore;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.InstanceIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

public class Ins extends FixedDexContainerWithTool implements Instruction, SmaliFormat {

    private final Opcode<?> opcode;
    private Object referencingLabelList;
    private Instruction targetInstruction;

    Ins(int childesCount, Opcode<?> opcode) {
        super(childesCount);
        this.opcode = opcode;
    }
    Ins(Opcode<?> opcode) {
        this(1, opcode);
    }

    public MethodDef getMethodDef() {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.getMethodDef();
        }
        return null;
    }
    public MethodKey getMethodKey() {
        MethodDef methodDef = getMethodDef();
        if (methodDef != null) {
            return methodDef.getKey();
        }
        return null;
    }
    public Ins edit() {
        MethodDef methodDef = getMethodDef();
        InstructionList current = methodDef.getInstructionList();
        methodDef.edit();
        InstructionList update = methodDef.getInstructionList();
        if (current != update) {
            return update.get(getIndex());
        }
        return this;
    }
    InsBlockList getInsBlockList() {
        return getParentInstance(InsBlockList.class);
    }
    public InstructionList getInstructionList() {
        return getParentInstance(InstructionList.class);
    }

    public void updateTargetAddress() {
        if (this instanceof InstructionLabel) {
            Instruction target = getTargetInstruction();
            if (target == null) {
                throw new NullPointerException("Null target: " + this + ", " + getMethodKey());
            }
            ((InstructionLabel) this).setTargetAddress(target.getAddress());
        }
    }
    public void transferReferenceLabelsTo(Ins destination) {
        destination.referencingLabelList = ObjectsStore.addAll(
                destination.referencingLabelList, this.getReferencingLabels());

        Iterator<InstructionLabel> iterator = destination.getReferencingLabels();
        while (iterator.hasNext()) {
            InstructionLabel label = iterator.next();
            label.setTargetInstruction(null);
            label.setTargetInstruction(destination);
        }
        Instruction target = this.targetInstruction;
        if (target != null && destination instanceof InstructionLabel) {
            destination.setTargetInstruction(target);
        }
        this.clearReferenceLabels();
    }
    public void replace(Ins ins) {
        if (ins == null || ins == this) {
            return;
        }
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            return;
        }
        instructionList.replace(this, ins);
    }
    public<T1 extends Ins> T1 replace(Opcode<T1> opcode) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            throw new DexException("Missing parent "
                    + InstructionList.class.getSimpleName());
        }
        return instructionList.replace(this, opcode);
    }
    public<T1 extends Ins> T1 createNext(Opcode<T1> opcode) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            throw new DexException("Parent " + getClass().getSimpleName() + " == null");
        }
        return instructionList.createAt(getIndex() + 1, opcode);
    }
    public<T1 extends Ins> T1 createNext(boolean shiftLabels, Opcode<T1> opcode) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            throw new DexException("Parent " + getClass().getSimpleName() + " == null");
        }
        return instructionList.createAt(shiftLabels, getIndex() + 1, opcode);
    }
    public<T1 extends Ins> T1 createPrevious(boolean shiftLabels, Opcode<T1> opcode) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            throw new DexException("Parent " + getClass().getSimpleName() + " == null");
        }
        return instructionList.createAt(shiftLabels, getIndex(), opcode);
    }
    public void moveTo(int index) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            throw new DexException("Parent " + getClass().getSimpleName() + " == null");
        }
        instructionList.moveTo(this, index);
    }

    @Override
    public Opcode<?> getOpcode() {
        return opcode;
    }
    @Override
    public Key getAsKey() {
        return null;
    }

    public InstructionLabelType getLabelType() {
        return InstructionLabelType.INSTRUCTION;
    }
    public RegisterFormat getRegisterFormat() {
        return getOpcode().getRegisterFormat();
    }

    public int getCodeUnits() {
        return countBytes() / 2;
    }
    public int getOutSize() {
        return 0;
    }
    public int getAddress() {
        InsBlockList insBlockList = getInsBlockList();
        if (insBlockList != null) {
            return insBlockList.addressOf(this);
        }
        return -1;
    }
    public int getOwnerAddress() {
        return getAddress();
    }
    public Ins getOwnerInstruction() {
        return this;
    }
    void linkTargetInstruction() {
        Instruction target = this.targetInstruction;
        if (target == null) {
            setTargetInstruction(findTargetIns());
        }
        if ((this instanceof InstructionLabel) && this.targetInstruction == null) {
            throw new NullPointerException("Missing target: " + this + ", " + getMethodKey());
        }
    }
    void detachTargetInstructions() {
        Instruction target = this.targetInstruction;
        if (target != null) {
            setTargetInstruction(null);
        }
        clearReferenceLabels();
    }
    public Instruction getTargetInstruction() {
        Instruction target = ensureTargetNotRemoved();
        if (target == null) {
            target = findTargetIns();
            setTargetInstruction(target);
            target = this.targetInstruction;
        }
        return target;
    }
    public void setTargetInstruction(Instruction targetInstruction) {
        if (targetInstruction == this) {
            // TODO: throw ?
            return;
        }
        if (targetInstruction != this.targetInstruction) {
            this.targetInstruction = targetInstruction;
            if (targetInstruction != null) {
                ((InstructionLabel) this).setTargetAddress(targetInstruction.getAddress());
                targetInstruction.addReferencingLabel(this);
            }
        }
    }

    private Instruction ensureTargetNotRemoved() {
        Instruction target = this.targetInstruction;
        if (target != null && target.isRemoved()) {
            target = null;
            this.targetInstruction = null;
        }
        return target;
    }
    private Ins findTargetIns() {
        if (this instanceof InstructionLabel) {
            InsBlockList insBlockList = getInsBlockList();
            if (insBlockList != null) {
                int targetAddress = ((InstructionLabel) this).getTargetAddress();
                Ins target = insBlockList.getAtAddress(targetAddress);
                if (targetAddress != 0 || target != this) {
                    return target;
                }
            }
        }
        return null;
    }
    public void addReferencingLabel(Object label) {
        if (label != this) {
            this.referencingLabelList = ObjectsStore.add(this.referencingLabelList, label);
        }
    }
    public Iterator<InstructionLabel> getReferencingLabels() {
        ObjectsStore.sort(this.referencingLabelList, InstructionLabel.LABEL_COMPARATOR);
        return ObjectsStore.iterator(this.referencingLabelList);
    }
    public<T1> Iterator<T1> getReferencingLabels(Class<T1> instance) {
        ObjectsStore.sort(this.referencingLabelList, InstructionLabel.LABEL_COMPARATOR);
        return ObjectsStore.iterator(this.referencingLabelList, instance);
    }
    public Iterator<InstructionLabel> getForcedReferencingLabels() {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            return EmptyIterator.of();
        }
        instructionList.linkReferenceLabels();
        return getReferencingLabels();
    }
    public<T1> Iterator<T1> getForcedReferencingLabels(Class<T1> instance) {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            return EmptyIterator.of();
        }
        instructionList.linkReferenceLabels();
        return getReferencingLabels(instance);
    }
    public void clearReferenceLabels() {
        referencingLabelList = ObjectsStore.clear(referencingLabelList);
    }
    public void removeReferenceLabels(InstructionLabel label) {
        referencingLabelList = ObjectsStore.remove(referencingLabelList, label);
    }
    public boolean hasReferenceLabels() {
        return !ObjectsStore.isEmpty(referencingLabelList);
    }
    private void appendReferenceLabels(SmaliWriter writer) throws IOException {
        Iterator<InstructionLabel> iterator = getReferencingLabels();
        InstructionLabel label = null;
        boolean hasHandler = false;
        InstructionLabel previous = null;
        while (iterator.hasNext()) {
            label = iterator.next();
            if (previous != null && label.equalsLabel(previous)) {
                label.appendLabelComment(writer);
                continue;
            }
            writer.newLine();
            boolean append = label.getLabelType().isHandler();
            if (hasHandler && !append) {
                writer.newLine();
            }
            label.appendLabelName(writer);
            label.appendLabelComment(writer);
            hasHandler = append;
            previous = label;
        }
        if (hasHandler) {
            writer.newLine();
        }
    }

    @Override
    public ReferenceLabelSet getReferenceLabelSet() {
        return null;
    }

    public void replaceKeys(Key search, Key replace) {

    }
    public boolean uses(Key key) {
        return false;
    }
    public Iterator<IdItem> usedIds() {
        return EmptyIterator.of();
    }
    public boolean isRemoved() {
        return getParent() == null;
    }
    public void merge(Ins ins) {
        throw new RuntimeException("merge method not implemented, opcode = " + getOpcode());
    }
    public void fromSmali(SmaliInstruction smaliInstruction) {
        throw new RuntimeException("fromSmali method not implemented, opcode = " + getOpcode());
    }

    public SmaliInstruction toSmali() {
        return toSmali(new SmaliCodeSet());
    }
    public SmaliInstruction toSmali(SmaliCodeSet smaliCodeSet) {
        SmaliInstruction instruction = smaliCodeSet.newInstruction(getOpcode());
        toSmali(instruction);
        return instruction;
    }

    private void toSmali(SmaliInstruction instruction) {
        if (instruction.getOperandType() != OperandType.NONE) {
            toSmaliOperand(instruction);
        }
        if (instruction.getRegisterFormat() != RegisterFormat.NONE) {
            toSmaliRegisters(instruction);
        }
        if (hasReferenceLabels() && instruction.getCodeSet() != null) {
            toSmaliReferenceLabels(instruction);
        }
        toSmaliOthers(instruction);
    }
    void toSmaliOperand(SmaliInstruction instruction) {
    }
    void toSmaliRegisters(SmaliInstruction instruction) {
    }
    void toSmaliReferenceLabels(SmaliInstruction smaliInstruction) {
        SmaliCodeSet smaliCodeSet = smaliInstruction.getCodeSet();
        Iterator<InstructionLabel> iterator = InstanceIterator.of(getReferencingLabels(),
                InstructionLabel.class, label -> !(label instanceof ExceptionLabel));
        InstructionLabel label;
        InstructionLabel previous = null;
        int index = smaliCodeSet.indexOf(smaliInstruction);
        while (iterator.hasNext()) {
            label = iterator.next();
            if (label.equalsLabel(previous)) {
                continue;
            }
            SmaliLabelDestination smaliLabel = new SmaliLabelDestination(
                    SmaliLabelDestination.of(getOpcode()));
            smaliCodeSet.add(index, smaliLabel);
            smaliLabel.setLabelName(label.getLabelName());
            index ++;
            previous = label;
        }
    }
    void toSmaliOthers(SmaliInstruction instruction) {
    }
    void validateOpcode(SmaliInstruction smaliInstruction) {
        if (getOpcode() != smaliInstruction.getOpcode()) {
            throw new DexException("Mismatch opcode " + getOpcode()
                    + " vs " + smaliInstruction.getOpcode());
        }
    }
    @Override
    public final void append(SmaliWriter writer) throws IOException {
        appendReferenceLabels(writer);
        writer.newLine();
        appendCode(writer);
    }
    public void appendCode(SmaliWriter writer) throws IOException {
        writer.append(getOpcode().getName());
        writer.append(' ');
    }

    @Override
    public ProgramType programType() {
        return ProgramType.DEX;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        SmaliWriter smaliWriter = new SmaliWriter(writer);
        try {
            appendCode(smaliWriter);
            smaliWriter.close();
            return writer.toString().trim();
        } catch (Throwable exception) {
            return exception.toString();
        }
    }
    static int toSigned(int unsigned, int width) {
        int half = width / 2;
        if (unsigned <= half) {
            return unsigned;
        }
        return unsigned - width - 1;
    }
}
