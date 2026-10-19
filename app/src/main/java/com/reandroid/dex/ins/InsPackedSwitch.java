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

import com.reandroid.dex.program.InstructionLabelType;

public class InsPackedSwitch extends InsSwitch {

    private InsSparseSwitch mReplacement;

    public InsPackedSwitch() {
        super(Opcode.PACKED_SWITCH);
    }

    @Override
    public InsPackedSwitchData getPayload() {
        return (InsPackedSwitchData) super.getPayload();
    }
    @Override
    public Opcode<InsPackedSwitchData> getPayloadOpcode() {
        return Opcode.PACKED_SWITCH_PAYLOAD;
    }

    InsSparseSwitch getSparseSwitchReplacement() {
        InsSparseSwitch sparseSwitch = this.mReplacement;
        if(sparseSwitch == null) {
            int reg = getRegister();
            int target = getTargetAddress();
            sparseSwitch = replace(Opcode.SPARSE_SWITCH);
            this.mReplacement = sparseSwitch;
            sparseSwitch.setRegister(reg);
            sparseSwitch.clearReferenceLabels();
            sparseSwitch.setTargetAddress(target);
        }
        return sparseSwitch;
    }

    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.P_SWITCH_DATA;
    }
}
