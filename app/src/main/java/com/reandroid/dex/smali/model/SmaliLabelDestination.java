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

import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliValidateException;

import java.io.IOException;

public class SmaliLabelDestination extends SmaliLabel {

    public SmaliLabelDestination(InstructionLabelType labelType) {
        super(labelType);
    }
    public SmaliLabelDestination() {
        this(null);
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
    public SmaliInstruction getTargetInstruction() {
        SmaliCodeSet codeSet = getParent();
        if (codeSet != null) {
            return codeSet.getNextInstruction(getIndex());
        }
        return null;
    }
    @Override
    public SmaliInstruction getOwnerInstruction() {
        return null;
    }

    @Override
    public SmaliLabel getDestinationLabel() {
        return this;
    }
    @Override
    public boolean isSourceLabel() {
        return false;
    }
    @Override
    public boolean isDestinationLabel() {
        return true;
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        super.parse(reader);
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            SmaliLabel duplicate = codeSet.validateUniqueLabel(this);
            if (duplicate != null) {
                throw new SmaliValidateException("There is already a label with name '"
                        + getLabelName() + "'.", this);
            }
        }
    }

    @Override
    public SmaliCodeSet getParent() {
        return (SmaliCodeSet) super.getParent();
    }
    @Override
    void setParent(Smali parent) {
        if (parent != null && !(parent instanceof SmaliCodeSet)) {
            throw new IllegalStateException("Wrong parent class: " + parent.getClass());
        }
        super.setParent(parent);
    }
}
