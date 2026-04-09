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
package com.reandroid.apkeditor.protect;

import com.reandroid.dex.ins.InsArrayData;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.model.DexInstruction;
import com.reandroid.dex.model.DexMethod;

import java.util.Random;

public class DexArrayPayloadConfuser extends DexConfuseTask {

    private final Random mRandom;

    public DexArrayPayloadConfuser() {
        super("array-data-confuser: ");
        this.mRandom = new Random();
    }

    @Override
    public int confuseLevel() {
        return 1;
    }
    @Override
    public boolean apply(DexMethod dexMethod) {
        if (isBlankOrAlreadyConfused(dexMethod)) {
            return false;
        }
        DexInstruction instruction = dexMethod.addInstruction(Opcode.ARRAY_PAYLOAD);
        InsArrayData arrayData = (InsArrayData) instruction.getIns();
        arrayData.setWidth(getRandomWidth());
        arrayData.setSize(getRandomCount());
        int size = arrayData.size();
        for (int i = 0; i < size; i++) {
            fillRandom(arrayData.get(i).getBytes());
        }
        return true;
    }
    private boolean isBlankOrAlreadyConfused(DexMethod dexMethod) {
        int insCount = dexMethod.getInstructionsCount();
        if (insCount == 0) {
            return true;
        }
        DexInstruction instruction = dexMethod.getInstruction(insCount - 1);
        if (!instruction.is(Opcode.ARRAY_PAYLOAD)) {
            return false;
        }
        InsArrayData arrayData = (InsArrayData) instruction.getIns();
        int width = arrayData.getWidth();
        return width == 0 || width > 8 || (width % 2) == 1;
    }

    private void fillRandom(byte[] bytes) {
        int num = getRandom();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            if (num == 0) {
                num = getRandom();
            }
            bytes[i] = (byte) (num & 0xff);
            num = num >>> 8;
        }
    }

    private int getRandomWidth() {
        int i = mRandom.nextInt(16) + 3;
        if ((i % 2) == 0) {
            i = i + 1;
        }
        return i;
    }
    private int getRandomCount() {
        return mRandom.nextInt(6) + 1;
    }
    private int getRandom() {
        return mRandom.nextInt(Integer.MAX_VALUE);
    }
}
