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
package com.reandroid.arsc.coder;

import com.reandroid.arsc.value.ValueType;

public class CoderNull extends Coder {

    public CoderNull() {
        super();
    }

    @Override
    public EncodeResult encode(String text) {
        if (text.equals("@null")) {
            return new EncodeResult(ValueType.REFERENCE, 0);
        }
        if (text.equals("@@null")) {
            return new EncodeResult(ValueType.DYNAMIC_REFERENCE, 0);
        }
        if (text.equals("@empty")) {
            return new EncodeResult(ValueType.NULL, 1);
        }
        return null;
    }

    @Override
    public String decode(int data) {
        return data == 0 ? "@null" : "@empty";
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NULL;
    }

    @Override
    boolean canStartWith(char first) {
        return first == '@';
    }

    public static final CoderNull INS = new CoderNull();
}
