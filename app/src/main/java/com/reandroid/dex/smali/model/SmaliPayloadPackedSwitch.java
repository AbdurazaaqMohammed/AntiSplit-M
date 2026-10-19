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

import com.reandroid.dex.ins.InsPackedSwitch;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliRegion;

public class SmaliPayloadPackedSwitch extends SmaliSwitchPayload<SmaliPackedSwitchEntry>
        implements SmaliRegion {

    public SmaliPayloadPackedSwitch() {
        super(Opcode.PACKED_SWITCH_PAYLOAD);
    }

    public int getFirstKey() {
        return (int) getOperand().getValueAsLong();
    }
    public void setFirstKey(int firstKey) {
        getOperand().setNumber(firstKey);
    }

    @Override
    public Opcode<InsPackedSwitch> getSwitchOpcode() {
        return Opcode.PACKED_SWITCH;
    }
    @Override
    public int getCodeUnits() {
        int count = getEntries().size();
        int size = 2 // opcode bytes
                + 4  // first key integer reference
                + 2  // count short reference
                + 4 * count; // element = 4
        return size / 2;
    }

    @Override
    public SmaliInstructionOperand.SmaliHexOperand getOperand() {
        return (SmaliInstructionOperand.SmaliHexOperand) super.getOperand();
    }

    @Override
    SmaliPackedSwitchEntry createEntry() {
        return new SmaliPackedSwitchEntry();
    }

    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.PACKED_SWITCH;
    }

}
