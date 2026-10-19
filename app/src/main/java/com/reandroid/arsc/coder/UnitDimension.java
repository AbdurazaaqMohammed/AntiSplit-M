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

public class UnitDimension extends ComplexUnit{

    public static final UnitDimension PX = new UnitDimension(0, "px");
    public static final UnitDimension DP = new UnitDimension(1, "dp");
    public static final UnitDimension DIP = new UnitDimension(1, "dip");
    public static final UnitDimension SP = new UnitDimension(2, "sp");
    public static final UnitDimension PT = new UnitDimension(3, "pt");
    public static final UnitDimension IN = new UnitDimension(4, "in");
    public static final UnitDimension MM = new UnitDimension(5, "mm");

    private static final UnitDimension[] VALUES = new UnitDimension[]{
            PX,
            DP,
            DIP,
            SP,
            PT,
            IN,
            MM
    };

    private UnitDimension(int flag, String symbol) {
        super(flag, symbol);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DIMENSION;
    }

    public static UnitDimension[] values(){
        return VALUES.clone();
    }

    public static UnitDimension valueOf(int flag) {
        for (UnitDimension unit : VALUES) {
            if (flag == unit.getFlag()) {
                return unit;
            }
        }
        return null;
    }
    public static UnitDimension valueOf(String symbol) {
        if(symbol == null){
            return null;
        }
        symbol = symbol.toLowerCase(java.util.Locale.ROOT);
        for (UnitDimension unit : VALUES) {
            if (symbol.equals(unit.getSymbol())) {
                return unit;
            }
        }
        return null;
    }
    public static UnitDimension fromPostfix(String text) {
        for (UnitDimension unit : VALUES) {
            if (unit.hasPostfix(text)) {
                return unit;
            }
        }
        return null;
    }
}
