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
package com.reandroid.dex.program;

import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;

import java.io.IOException;
import java.util.Comparator;

public interface InstructionLabel extends InstructionStatement {

    int getTargetAddress();
    void setTargetAddress(int address);
    Instruction getTargetInstruction();
    default void setTargetInstruction(Instruction targetInstruction) {
    }
    default Instruction getOwnerInstruction() {
        if (this instanceof Instruction) {
            return (Instruction) this;
        }
        return null;
    }
    default int getOwnerAddress() {
        Instruction instruction = getOwnerInstruction();
        if (instruction != null) {
            return instruction.getAddress();
        }
        return -1;
    }
    default boolean equalsLabel(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        InstructionLabel label = (InstructionLabel) obj;
        return ObjectsUtil.equals(getLabelName(), label.getLabelName());
    }
    default boolean isRemoved() {
        return false;
    }
    default void updateTarget() {
        Instruction target = getTargetInstruction();
        if (target != null && !target.isRemoved()) {
            setTargetAddress(target.getAddress());
        }
    }
    default String getLabelName() {
        return getLabelType().buildLabelName(getTargetAddress());
    }
    InstructionLabelType getLabelType();

    @Override
    default ProgramType programType() {
        return ProgramType.UNKNOWN;
    }
    default void appendLabelName(SmaliWriter writer) throws IOException {
        writer.appendLabelName(getLabelName());
    }
    default void appendLabelComment(SmaliWriter writer) throws IOException {
    }

    default int compareLabel(InstructionLabel label) {
        return compareLabels(this, label);
    }
    static int compareLabels(InstructionLabel label, InstructionLabel other) {
        if (label == other) {
            return 0;
        }
        int i = CompareUtil.compare(label.getLabelType(), other.getLabelType());
        if (i == 0) {
            i = CompareUtil.compare(label.getTargetAddress(), other.getTargetAddress());
        }
        return i;
    }

    Comparator<InstructionLabel> LABEL_COMPARATOR = InstructionLabel::compareLabel;

    class LabelWrapper implements InstructionLabel {

        private final InstructionLabel baseLabel;

        public LabelWrapper(InstructionLabel baseLabel) {
            this.baseLabel = baseLabel;
        }

        @Override
        public int getTargetAddress() {
            return getBaseLabel().getTargetAddress();
        }
        @Override
        public void setTargetAddress(int address) {
            getBaseLabel().setTargetAddress(address);
        }

        @Override
        public Instruction getTargetInstruction() {
            return baseLabel.getTargetInstruction();
        }
        @Override
        public void setTargetInstruction(Instruction targetInstruction) {
            baseLabel.setTargetInstruction(targetInstruction);
        }
        @Override
        public int getOwnerAddress() {
            return baseLabel.getOwnerAddress();
        }

        @Override
        public Instruction getOwnerInstruction() {
            return baseLabel.getOwnerInstruction();
        }

        @Override
        public String getLabelName() {
            return getBaseLabel().getLabelName();
        }
        @Override
        public InstructionLabelType getLabelType() {
            return getBaseLabel().getLabelType();
        }
        @Override
        public ProgramType programType() {
            return getBaseLabel().programType();
        }
        public InstructionLabel getBaseLabel() {
            return baseLabel;
        }

        @Override
        public int compareLabel(InstructionLabel label) {
            if (label == this) {
                return 0;
            }
            if (label instanceof LabelWrapper) {
                label = ((LabelWrapper) label).getBaseLabel();
            }
            return getBaseLabel().compareLabel(label);
        }

        @Override
        public boolean equalsLabel(Object obj) {
            return false;
        }

        @Override
        public void appendLabelName(SmaliWriter writer) throws IOException {
            getBaseLabel().appendLabelName(writer);
        }
        @Override
        public void appendLabelComment(SmaliWriter writer) throws IOException {
            getBaseLabel().appendLabelComment(writer);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof InstructionLabel)) {
                return false;
            }
            if (obj instanceof LabelWrapper) {
                if (getBaseLabel() == ((LabelWrapper) obj).getBaseLabel()) {
                    return true;
                }
            }
            InstructionLabel label = (InstructionLabel) obj;
            return this.getTargetAddress() == label.getTargetAddress() &&
                    ObjectsUtil.equals(this.getLabelType(), label.getLabelType()) &&
                    ObjectsUtil.equals(this.getLabelName(), label.getLabelName());
        }
        @Override
        public int hashCode() {
            return 31 + getTargetAddress() + 31 * ObjectsUtil.hash(this.getLabelType());
        }

        @Override
        public String toString() {
            return getLabelName();
        }
    }
}
