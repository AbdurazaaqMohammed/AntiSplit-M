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

import com.reandroid.dex.ins.InsSparseSwitch;
import com.reandroid.dex.ins.InsSparseSwitchData;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.smali.*;

public class SmaliPayloadSparseSwitch extends SmaliSwitchPayload<SmaliSparseSwitchEntry> {

    public SmaliPayloadSparseSwitch() {
        super(Opcode.SPARSE_SWITCH_PAYLOAD);
    }

    @Override
    public Opcode<InsSparseSwitch> getSwitchOpcode() {
        return Opcode.SPARSE_SWITCH;
    }

    @Override
    public int getCodeUnits() {
        int count = getCount();
        int size = 2 // opcode bytes
                + 2  // count short reference
                + 8 * count; // element = 4, key = 4
        return size / 2;
    }

    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.SPARSE_SWITCH;
    }
    @Override
    public Opcode<InsSparseSwitchData> getOpcode() {
        return Opcode.SPARSE_SWITCH_PAYLOAD;
    }
    @Override
    SmaliSparseSwitchEntry createEntry() {
        return new SmaliSparseSwitchEntry();
    }

}
