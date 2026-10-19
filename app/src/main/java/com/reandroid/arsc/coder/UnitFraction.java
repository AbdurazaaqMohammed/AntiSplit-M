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

public class UnitFraction extends ComplexUnit{
    public static final UnitFraction FRACTION = new UnitFraction(0, "%");
    public static final UnitFraction FRACTION_PARENT = new UnitFraction(1, "%p");

    private static final UnitFraction[] VALUES = new UnitFraction[]{
            FRACTION,
            FRACTION_PARENT
    };

    UnitFraction(int flag, String symbol) {
        super(flag, symbol);
    }

    @Override
    public boolean isFraction(){
        return true;
    }
    @Override
    public ValueType getValueType() {
        return ValueType.FRACTION;
    }

    public static UnitFraction[] values(){
        return VALUES.clone();
    }

    public static UnitFraction valueOf(int flag) {
        for (UnitFraction unit : VALUES) {
            if (flag == unit.getFlag()) {
                return unit;
            }
        }
        return null;
    }
    public static UnitFraction valueOf(String symbol) {
        if(symbol == null){
            return null;
        }
        symbol = symbol.toLowerCase(java.util.Locale.ROOT);
        for (UnitFraction unit : VALUES) {
            if (symbol.equals(unit.getSymbol())) {
                return unit;
            }
        }
        return null;
    }
    public static UnitFraction fromPostfix(String text) {
        for (UnitFraction unit : VALUES) {
            if (unit.hasPostfix(text)) {
                return unit;
            }
        }
        return null;
    }
}
