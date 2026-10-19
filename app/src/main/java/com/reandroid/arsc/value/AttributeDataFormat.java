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
package com.reandroid.arsc.value;

import com.reandroid.utils.StringsUtil;

public enum AttributeDataFormat {

    REFERENCE(1<<0,
            new ValueType[]{
                    ValueType.REFERENCE,
                    ValueType.ATTRIBUTE,
                    ValueType.DYNAMIC_REFERENCE,
                    ValueType.DYNAMIC_ATTRIBUTE,
                    ValueType.NULL,
    }),
    INTEGER(1<<2, new ValueType[]{
            ValueType.DEC,
            ValueType.HEX
    }),
    BOOL(1<<3, new ValueType[]{
            ValueType.BOOLEAN
    }),
    COLOR(1<<4, new ValueType[]{
            ValueType.COLOR_ARGB8,
            ValueType.COLOR_RGB8,
            ValueType.COLOR_RGB4,
            ValueType.COLOR_ARGB4
    }),
    FLOAT(1<<5, new ValueType[]{
            ValueType.FLOAT
    }),
    DIMENSION(1<<6, new ValueType[]{
            ValueType.DIMENSION
    }),
    FRACTION(1<<7, new ValueType[]{
            ValueType.FRACTION
    }),
    ANY(0x0000FFFF, ValueType.values().clone()),

    ENUM(1<<16, new ValueType[]{
            ValueType.DEC,
            ValueType.HEX
    }),
    FLAG(1<<17, new ValueType[]{
            ValueType.HEX,
            ValueType.DEC
    }),
    STRING(1<<1, new ValueType[]{
            ValueType.STRING
    });

    private final int mask;
    private final ValueType[] valueTypes;

    AttributeDataFormat(int mask, ValueType[] valueTypes) {
        this.mask = mask;
        this.valueTypes = valueTypes;
    }

    public int getMask() {
        return mask;
    }
    public boolean matches(int value){
        int mask = this.mask;
        return (value & mask) == mask;
    }

    public ValueType[] valueTypes() {
        return valueTypes;
    }
    public ValueType[] getValueTypes() {
        return valueTypes.clone();
    }
    public boolean contains(ValueType valueType){
        ValueType[] valueTypes = this.valueTypes;
        for(int i = 0; i < valueTypes.length; i++){
            if(valueType == valueTypes[i]){
                return true;
            }
        }
        return false;
    }
    public String getName(){
        return name().toLowerCase(java.util.Locale.ROOT);
    }
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(getName());
        builder.append('{');
        ValueType[] valueTypes = this.valueTypes;
        for(int i = 0; i < valueTypes.length; i++){
            if(i != 0){
                builder.append(',');
            }
            builder.append(valueTypes[i]);
        }
        builder.append('}');
        return builder.toString();
    }

    public static String toStringValueTypes(int data){
        return toString(decodeValueTypes(data));
    }
    public static String toString(AttributeDataFormat[] typeValues){
        if(typeValues == null || typeValues.length == 0){
            return null;
        }
        StringBuilder builder = new StringBuilder();

        boolean appendOnce = false;
        int appendedTypes = 0;
        for(AttributeDataFormat typeValue : typeValues){
            if(typeValue == ENUM || typeValue == FLAG){
                continue;
            }
            if(typeValue == AttributeDataFormat.ANY){
                return AttributeDataFormat.ANY.getName();
            }
            int mask = typeValue.getMask();
            if((appendedTypes & mask) == mask){
                continue;
            }
            if(appendOnce){
                builder.append('|');
            }
            builder.append(typeValue.getName());
            appendOnce = true;
            appendedTypes = appendedTypes | mask;
        }
        return builder.toString();
    }
    public static int sum(AttributeDataFormat[] typeValues){
        if(typeValues == null){
            return 0;
        }
        int result = 0;
        for(AttributeDataFormat typeValue:typeValues){
            if(typeValue==null){
                continue;
            }
            result |= typeValue.getMask();
        }
        return result;
    }

    public static AttributeDataFormat[] decodeValueTypes(int data) {
        if ((data & 0xffff) == 0xffff) {
            return new AttributeDataFormat[]{ANY};
        }
        data &= 0xff;
        if (data == 0) {
            return null;
        }
        AttributeDataFormat[] results = new AttributeDataFormat[Integer.bitCount(data)];
        AttributeDataFormat[] valueTypes = VALUE_TYPES;
        int length = valueTypes.length - 1;
        int j = 0;
        for (int i = 0; i < length; i++) {
            if ((data & 1) != 0) {
                results[j] = valueTypes[i];
                j ++;
            }
            data = data >> 1;
        }
        return results;
    }
    public static AttributeDataFormat[] parseValueTypes(String valuesTypes){
        if(valuesTypes == null){
            return null;
        }
        valuesTypes = valuesTypes.trim();
        String[] valueNames = valuesTypes.split("\\s*\\|\\s*");
        AttributeDataFormat[] tmp = new AttributeDataFormat[VALUE_TYPES.length];
        int length = 0;
        for(String name:valueNames){
            AttributeDataFormat typeValue = fromValueTypeName(name);
            if(typeValue!=null){
                tmp[length] = typeValue;
                length++;
            }
        }
        if(length == 0){
            return null;
        }
        AttributeDataFormat[] results = new AttributeDataFormat[length];
        System.arraycopy(tmp, 0, results, 0, length);
        return results;
    }
    public static AttributeDataFormat valueOf(int mask){
        for(AttributeDataFormat typeValue : VALUE_TYPES){
            if(typeValue.getMask() == mask){
                return typeValue;
            }
        }
        return null;
    }
    public static AttributeDataFormat typeOfBag(int data){
        for(AttributeDataFormat typeValue : BAG_TYPES){
            if(typeValue.matches(data)){
                return typeValue;
            }
        }
        return null;
    }
    public static AttributeDataFormat fromValueTypeName(String name){
        if(name == null){
            return null;
        }
        name = StringsUtil.toUpperCase(name.trim());
        for(AttributeDataFormat typeValue : VALUE_TYPES){
            if(name.equals(typeValue.name())){
                return typeValue;
            }
        }
        return null;
    }
    public static AttributeDataFormat fromBagTypeName(String bagTypeName){
        if(bagTypeName == null){
            return null;
        }
        bagTypeName = StringsUtil.toUpperCase(bagTypeName.trim());
        for(AttributeDataFormat typeValue: BAG_TYPES){
            if(bagTypeName.equals(typeValue.name())){
                return typeValue;
            }
        }
        return null;
    }

    public static ValueType[] getExpectedValueTypes(String typeName){
        AttributeDataFormat dataFormat = fromValueTypeName(typeName);
        if(dataFormat != null){
            return dataFormat.valueTypes();
        }
        return null;
    }

    public static boolean contains(AttributeDataFormat[] formats, ValueType valueType){
        if(formats == null || valueType == null){
            return false;
        }
        for(AttributeDataFormat dataFormat : formats){
            if(dataFormat != null && dataFormat.contains(valueType)){
                return true;
            }
        }
        return false;
    }

    private static final AttributeDataFormat[] VALUE_TYPES = new AttributeDataFormat[]{
            REFERENCE,
            STRING,
            INTEGER,
            BOOL,
            COLOR,
            FLOAT,
            DIMENSION,
            FRACTION,
            ANY
    };

    private static final AttributeDataFormat[] BAG_TYPES = new AttributeDataFormat[]{
            ENUM,
            FLAG
    };
}
