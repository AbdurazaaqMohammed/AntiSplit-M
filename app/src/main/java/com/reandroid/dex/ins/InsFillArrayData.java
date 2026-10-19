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

import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.program.InstructionLabelType;

public class InsFillArrayData extends Ins31t{

    public InsFillArrayData() {
        super(Opcode.FILL_ARRAY_DATA);
    }

    public InsArrayData getInsArrayData(){
        InstructionList instructionList = getInstructionList();
        if(instructionList == null){
            return null;
        }
        Ins ins = instructionList.getAtAddress(getTargetAddress());
        if(ins instanceof InsArrayData){
            return (InsArrayData) ins;
        }
        return null;
    }
    Ins22c findNewArrayLazy() {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            return null;
        }
        int index = getIndex();
        Ins ins = instructionList.get(index - 1);
        if (ins.getOpcode() == Opcode.NEW_ARRAY) {
            Ins22c ins22c = (Ins22c) ins;
            if (getRegister(0) == ins22c.getRegister(0)) {
                return ins22c;
            }
        }
        return null;
    }
    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.ARRAY;
    }
}
