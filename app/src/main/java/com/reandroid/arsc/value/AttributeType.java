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

public enum AttributeType {

    FORMATS(0x01000000),
    MIN(0x01000001),
    MAX(0x01000002),
    L10N(0x01000003),

    OTHER(0x01000004),
    ZERO(0x01000005),
    ONE(0x01000006),
    TWO(0x01000007),
    FEW(0x01000008),
    MANY(0x01000009);

    private final int id;
    AttributeType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isPlural(){
        int i = id & 0xffff;
        return i>=4 && i<=9;
    }

    public String getName(){
        return StringsUtil.toLowercase(name());
    }
    @Override
    public String toString(){
        return getName();
    }

    public static AttributeType valueOf(int value){
        if(value == 0){
            return null;
        }
        for(AttributeType type:VALUES){
            if(type.getId() == value){
                return type;
            }
        }
        return null;
    }

    public static AttributeType fromName(String name){
        if(name == null){
            return null;
        }
        name = name.toUpperCase(java.util.Locale.ROOT);
        if("FORMAT".equals(name)){
            return FORMATS;
        }
        for(AttributeType type:VALUES){
            if(name.equals(type.name())){
                return type;
            }
        }
        return null;
    }

    private static final AttributeType[] VALUES = values();
}
