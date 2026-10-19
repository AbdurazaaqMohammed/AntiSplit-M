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

public class InsSparseSwitch extends InsSwitch {

    public InsSparseSwitch() {
        super(Opcode.SPARSE_SWITCH);
    }

    @Override
    public InsSparseSwitchData getPayload() {
        return (InsSparseSwitchData) super.getPayload();
    }
    @Override
    public Opcode<InsSparseSwitchData> getPayloadOpcode() {
        return Opcode.SPARSE_SWITCH_PAYLOAD;
    }

    @Override
    public void setTargetAddress(int targetAddress){
        setData(targetAddress - getAddress());
    }
    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.S_SWITCH_DATA;
    }
}
