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
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelSet;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.InstanceIterator;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SmaliCodeSet extends SmaliSet<SmaliCode> {

    private int addressOffset;
    private SmaliNullInstruction nullInstruction;
    private Map<String, SmaliLabel> labelMap;

    public SmaliCodeSet() {
        super();
    }

    public int getAddressOffset() {
        return addressOffset;
    }
    public void setAddressOffset(int addressOffset) {
        this.addressOffset = addressOffset;
    }

    public void updateAddresses() {
        int address = getAddressOffset();
        int size = size();
        for (int i = 0; i < size; i++) {
            SmaliCode code = getSuper(i);
            code.setIndex(i);
            if (code instanceof SmaliInstruction) {
                SmaliInstruction ins = (SmaliInstruction) code;
                ins.setAddress(address);
                address += ins.getCodeUnits();
            }
        }
        SmaliInstruction instruction = getNullInstruction();
        if (instruction != null) {
            instruction.setAddress(address);
        }
    }
    public Iterator<SmaliInstruction> getSourcingInstructions(InstructionLabel label) {
        if (label != null) {
            return InstanceIterator.of(iterator(), SmaliInstruction.class,
                    instruction -> instruction.hasLabelOperand(label));
        }
        return EmptyIterator.of();
    }
    public Iterator<SmaliInstruction> getInstructions() {
        return iterator(SmaliInstruction.class);
    }
    public Iterator<SmaliTryItem> getTryItems() {
        return iterator(SmaliTryItem.class);
    }
    public Iterator<SmaliDebug> getDebugs() {
        return iterator(SmaliDebug.class);
    }
    public Iterator<SmaliDebugElement> getDebugElements() {
        return iterator(SmaliDebugElement.class);
    }
    public Iterator<SmaliMethodParameter> getMethodParameters() {
        return iterator(SmaliMethodParameter.class);
    }
    public void clearInstructions() {
        removeInstances(SmaliDebug.class);
    }
    public void clearDebugs() {
        removeInstances(SmaliDebug.class);
    }

    public Iterator<InstructionLabel> getSourceLabels() {
        return  new IterableIterator<SmaliCode, InstructionLabel>(iterator()) {
            @Override
            public Iterator<InstructionLabel> iterator(SmaliCode code) {
                if (code instanceof SmaliLabel &&
                        ((SmaliLabel) code).isDestinationLabel()) {
                    return EmptyIterator.of();
                }
                Iterator<InstructionLabel> iterator = null;
                if (code instanceof SmaliInstruction) {
                    SmaliInstructionOperand operand = ((SmaliInstruction) code).getOperand();
                    if (operand instanceof InstructionLabel) {
                        iterator = SingleIterator.of((InstructionLabel) operand);
                    }
                } else if (code instanceof InstructionLabel) {
                    iterator = SingleIterator.of((InstructionLabel) code);
                }
                if (iterator == null) {
                    iterator = EmptyIterator.of();
                }
                if (code instanceof InstructionLabelSet) {
                    iterator = CombiningIterator.two(iterator,
                            ObjectsUtil.cast(((InstructionLabelSet) code).getLabels()));
                }
                return iterator;
            }
        };
    }
    public SmaliInstruction getNullInstruction() {
        SmaliNullInstruction nullInstruction = this.nullInstruction;
        if (needsNullInstruction()) {
            if (nullInstruction == null) {
                nullInstruction = new SmaliNullInstruction();
                nullInstruction.setParent(this);
                this.nullInstruction = nullInstruction;
            }
        } else {
            if (nullInstruction != null) {
                nullInstruction.setParent(null);
                nullInstruction = null;
                this.nullInstruction = null;
            }
        }
        return nullInstruction;
    }
    private boolean needsNullInstruction() {
        int size = size();
        if (size != 0) {
            return !(getSuper(size - 1) instanceof SmaliInstruction);
        }
        return false;
    }

    public SmaliInstruction newInstruction(Opcode<?> opcode) {
        return newInstruction(size(), opcode);
    }
    public SmaliInstruction newInstruction(int index, Opcode<?> opcode) {
        SmaliInstruction instruction = SmaliInstruction.createInstruction(opcode);
        add(index, instruction);
        return instruction;
    }
    public SmaliInstruction getNextInstruction(int index) {
        if (index < 0) {
            return null;
        }
        int size = size();
        for (int i = index; i < size; i++) {
            SmaliCode code = get(i);
            if (code instanceof SmaliInstruction) {
                return (SmaliInstruction) code;
            }
        }
        if (index <= size) {
            return getNullInstruction();
        }
        return null;
    }
    public SmaliInstruction getPreviousInstruction(int index) {
        if (index < 0 || index > size()) {
            return null;
        }
        for (int i = index; i >= 0; i--) {
            SmaliCode code = get(i);
            if (code instanceof SmaliInstruction) {
                return (SmaliInstruction) code;
            }
        }
        return null;
    }
    public SmaliLabel newLabelAtIndex(int index, InstructionLabelType type) {
        SmaliLabel label = newLabelAtIndex(index, type.prefix());
        label.setLabelType(type);
        return label;
    }
    public SmaliLabel newLabelAtIndex(int index, String prefix) {
        SmaliLabelDestination label = new SmaliLabelDestination();
        label.setLabelName(generateUniqueLabelName(prefix));
        add(index, label);
        return label;
    }
    public String generateUniqueLabelName(String prefix) {
        if (prefix.charAt(0) != ':') {
            prefix = ":" + prefix;
        }
        Set<String> uniqueSet = CollectionUtil.toHashSet(
                ComputeIterator.of(iterator(SmaliLabel.class), SmaliLabel::getLabelName));
        int i = 0;
        while (i < Integer.MAX_VALUE) {
            String name = HexUtil.toHex(prefix, i, 1);
            if (!uniqueSet.contains(name)) {
                return name;
            }
            i ++;
        }
        throw new IllegalStateException("Can not generate unique name, tried: " + i);
    }
    @Override
    public SmaliCode get(int i) {
        if (i == size()) {
            return this.getNullInstruction();
        }
        return getSuper(i);
    }
    private SmaliCode getSuper(int i) {
        return super.get(i);
    }

    public SmaliLabel getDestinationLabel(InstructionLabel label) {
        String labelName = label.getLabelName();
        if (labelName == null) {
            return (SmaliLabel) get(label);
        }
        SmaliLabel smaliLabel = null;
        Map<String, SmaliLabel> labelMap = this.labelMap;
        if (labelMap != null) {
            smaliLabel = labelMap.get(labelName);
        }
        if (smaliLabel != null) {
            if (labelName.equals(smaliLabel.getLabelName())) {
                return smaliLabel;
            }
            labelMap.remove(labelName);
        }
        smaliLabel = (SmaliLabel) get(label);
        return smaliLabel;
    }
    public SmaliLabel validateUniqueLabel(SmaliLabelDestination label) {
        Map<String, SmaliLabel> labelMap = this.labelMap;
        if (labelMap == null) {
            labelMap = new HashMap<>();
            this.labelMap = labelMap;
        }
        String name = label.getLabelName();
        SmaliLabel exist = labelMap.get(name);
        if (exist == label) {
            return null;
        }
        if (exist == null) {
            labelMap.put(name, label);
        }
        return exist;
    }
    private void clearDestinationLabels() {
        Map<String, SmaliLabel> labelMap = this.labelMap;
        if (labelMap != null) {
            labelMap.clear();
            this.labelMap = null;
        }
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        if (isEmpty()) {
            return;
        }
        writer.buildLabels(getSourceLabels());
        writer.newLine();
        writer.appendAll(iterator());
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        super.parse(reader);
        updateAddresses();
    }
    @Override
    SmaliCode createNext(SmaliReader reader) {
        return SmaliCode.createCode(reader);
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        clearDestinationLabels();
    }
}
