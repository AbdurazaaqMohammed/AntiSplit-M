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
import com.reandroid.dex.smali.SmaliValidateException;

import java.io.IOException;

public class SmaliLabelSource extends SmaliLabel {

    private SmaliLabel mDestination;

    public SmaliLabelSource(InstructionLabelType labelType) {
        super(labelType);
    }
    public SmaliLabelSource() {
        this(null);
    }

    @Override
    public SmaliInstruction getTargetInstruction() {
        SmaliLabel label = getDestinationLabel();
        if (label != null) {
            return label.getTargetInstruction();
        }
        return null;
    }

    @Override
    public SmaliLabel getDestinationLabel() {
        String name = getLabelName();
        if (name == null) {
            this.mDestination = null;
            return null;
        }
        SmaliLabel label = this.mDestination;
        if (label != null && !label.isRemoved()
                && name.equals(label.getLabelName())) {
            return label;
        }
        label = null;
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            label = codeSet.getDestinationLabel(this);
        }
        this.mDestination = label;
        if (label != null) {
            label.setLabelTypeIfNull(getLabelType());
        }
        return label;
    }
    @Override
    public SmaliInstruction getOwnerInstruction() {
        return getParentInstance(SmaliInstruction.class);
    }

    @Override
    public boolean isSourceLabel() {
        return true;
    }
    @Override
    public boolean isDestinationLabel() {
        return false;
    }

    @Override
    public boolean isRemoved() {
        return getCodeSet() == null;
    }

    @Override
    void setParent(Smali parent) {
        if (parent instanceof SmaliCodeSet) {
            throw new IllegalStateException("Wrong parent class: " + parent.getClass());
        }
        super.setParent(parent);
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        if (getDestinationLabel() == null) {
            throw new SmaliValidateException("Cannot get the location of a label '"
                    + getLabelName() + "'.", this);
        }
    }
}
