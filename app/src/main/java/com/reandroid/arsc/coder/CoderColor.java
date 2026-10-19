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

public abstract class CoderColor extends Coder {
    private final int decodedStringLength;
    CoderColor(int decodedStringLength){
        this.decodedStringLength = decodedStringLength;
    }

    @Override
    public EncodeResult encode(String text){
        if(text.length() != this.decodedStringLength){
            return null;
        }
        int[] hexValues = hexToIntegers(text);
        if(hexValues == null){
            return null;
        }
        return new EncodeResult(
                getValueType(),
                encode(hexValues));
    }
    private int encode(int[] hexValues){
        int start = hexValues.length - 1;
        boolean compact = start < 6;
        boolean has_alpha = (start % 4 == 0);
        int result;
        if(has_alpha){
            result = 0;
        }else {
            result = 0xff000000;
        }
        int shift = 0;
        for(int i = start; i >= 0; i--){
            int value = hexValues[i];
            result |= value << shift;
            shift += 4;
            if(compact){
                result |= value << shift;
                shift += 4;
            }
        }
        return result;
    }
    @Override
    public String decode(int data){
        StringBuilder builder = new StringBuilder();
        builder.append('#');
        int count = this.decodedStringLength - 2;
        int shiftStep;
        if(count < 5){
            shiftStep = 8;
        }else {
            shiftStep = 4;
        }
        int shift = shiftStep * count;
        while (shift >= 0){
            builder.append(byteToHex(data >> shift));
            shift = shift - shiftStep;
        }
        return builder.toString();
    }
    @Override
    boolean canStartWith(char first) {
        return first == '#';
    }

    private static int[] hexToIntegers(String hexColor){
        hexColor = hexColor.toUpperCase(java.util.Locale.ROOT);
        char[] chars = hexColor.toCharArray();
        if(chars[0] != '#'){
            return null;
        }
        int length = chars.length;
        int[] result = new int[length];
        for(int i = 1; i < length; i++){
            int ch = chars[i];
            int value;
            if(ch >= '0' && ch <= '9'){
                value = ch - '0';
            }else if(ch >= 'A' && ch <= 'F'){
                value = 0xA + (ch - 'A');
            }else {
                return null;
            }
            result[i] = value;
        }
        return result;
    }
    private static char byteToHex(int i){
        i = i & 0xf;
        if(i < 0xa){
            return (char) ('0' + i);
        }
        i = i - 0xa;
        return (char) ('a' + i);
    }
}
