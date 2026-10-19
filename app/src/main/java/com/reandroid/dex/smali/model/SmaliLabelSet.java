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

import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelSet;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.program.ProgramType;
import com.reandroid.dex.program.ProgramTypeDescriptor;
import com.reandroid.dex.program.ReferenceLabelSet;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.IndexIterator;
import com.reandroid.utils.collection.InstanceIterator;
import com.reandroid.utils.collection.SizedSupplier;

import java.io.IOException;
import java.util.Iterator;

public class SmaliLabelSet implements SizedSupplier<InstructionLabel>,
        ProgramTypeDescriptor, InstructionLabelSet, ReferenceLabelSet {

    private final SmaliInstruction instruction;

    public SmaliLabelSet(SmaliInstruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public Iterator<? extends InstructionLabel> getLabels() {
        return iterator();
    }

    @Override
    public void addReferenceLabel(InstructionLabel label) {
        SmaliCodeSet codeSet = codeSet();
        if (codeSet == null) {
            return;
        }
        SmaliCode smaliCode;
        if (label instanceof SmaliCode) {
            smaliCode = (SmaliCode) label;
        } else {
            smaliCode = createLabelSmaliCode(label);
        }
        if (codeSet.contains(smaliCode)) {
            return;
        }
        codeSet.add(instruction().getIndex(), smaliCode);
    }
    private SmaliCode createLabelSmaliCode(InstructionLabel label) {
        InstructionLabelType type = label.getLabelType();
        String labelName = label.getLabelName();
        if (type.isLocation()) {
            SmaliLabelDestination destinationLabel = new SmaliLabelDestination();
            destinationLabel.setLabelName(labelName);
            destinationLabel.setLabelType(type);
            return destinationLabel;
        }
        try {
            SmaliReader reader = SmaliReader.of(labelName);
            SmaliCode smaliCode = SmaliCode.createCode(reader);
            smaliCode.parse(reader);
            return smaliCode;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    @Override
    public void removeReferenceLabel(InstructionLabel label) {
        if (label == null) {
            return;
        }
        SmaliCodeSet codeSet = codeSet();
        if (codeSet == null) {
            return;
        }
        int size = size();
        for (int i = 0; i < size; i++) {
            SmaliCode code = (SmaliCode) get(i);
            if (code.equals(label)) {
                codeSet.remove(code.getIndex());
                i --;
                size = size();
            }
        }
    }
    @Override
    public void clearReferenceLabels() {
        SmaliCodeSet codeSet = codeSet();
        if (codeSet == null) {
            return;
        }
        int size = size();
        for (int i = 0; i < size; i++) {
            SmaliCode code = (SmaliCode) get(0);
            codeSet.remove(code.getIndex());
        }
    }
    @Override
    public boolean hasReferenceLabels() {
        return size() > 0;
    }
    @Override
    public Iterator<? extends InstructionLabel> getReferenceLabels() {
        return iterator();
    }

    public Iterator<InstructionLabel> iterator() {
        return IndexIterator.of(this);
    }
    public<T> Iterator<T> iterator(Class<T> instance) {
        return InstanceIterator.of(iterator(), instance);
    }

    public boolean isEmpty() {
        return size() <= 0;
    }
    @Override
    public int size() {
        int i = instruction().getIndex();
        if (i < 0) {
            return 0;
        }
        return i - indexOffset();
    }
    @Override
    public InstructionLabel get(int i) {
        if (i < 0) {
            return null;
        }
        SmaliCodeSet codeSet = codeSet();
        if (codeSet == null) {
            return null;
        }
        i = i + indexOffset();
        int end = instruction().getIndex();
        if (i >= end) {
            return null;
        }
        return (InstructionLabel) codeSet.get(i);
    }
    public SmaliCodeSet codeSet() {
        return instruction().getCodeSet();
    }

    public SmaliInstruction instruction() {
        return instruction;
    }

    public int indexOffset() {
        SmaliInstruction previous = instruction().getPrevious();
        if (previous != null) {
            int i = previous.getIndex();
            if (i >= 0) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public ProgramType programType() {
        return ProgramType.SMALI;
    }
    @Override
    public String toString() {
        return StringsUtil.join(iterator(), "\n");
    }
}
