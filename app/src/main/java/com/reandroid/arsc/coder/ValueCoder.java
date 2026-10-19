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

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Value;
import com.reandroid.arsc.value.attribute.AttributeBag;
import com.reandroid.arsc.value.AttributeDataFormat;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.StringsUtil;

import java.util.HashMap;
import java.util.Map;

public class ValueCoder {

    public static EncodeResult encodeAttributeValue(boolean validate, Value output, ResourceEntry name, String value) {
        PackageBlock context = output.getPackageBlock();
        EncodeResult encodeResult = encodeReference(context, value);
        if (encodeResult != null) {
            if (encodeResult.isError()) {
                return encodeResult;
            }
            output.setValue(encodeResult);
            return encodeResult;
        }
        if (name != null) {
            name = name.resolveReference();
            AttributeBag attributeBag = AttributeBag.create(name.get());
            if (attributeBag != null) {
                encodeResult = attributeBag.encode(value);
                if (encodeResult != null) {
                    if (encodeResult.valueType == ValueType.STRING) {
                        output.setValueAsString(XmlSanitizer.unEscapeSpecialCharacter(value));
                        return new EncodeResult(ValueType.STRING, output.getData());
                    }
                    if (encodeResult.isError()) {
                        if (validate) {
                            return encodeResult;
                        }
                        encodeResult = null;
                    }
                }
            }
        }
        if (encodeResult == null) {
            encodeResult = ValueCoder.encode(value);
        }
        if (encodeResult != null) {
            output.setValue(encodeResult);
            return encodeResult;
        }
        output.setValueAsString(XmlSanitizer.unEscapeSpecialCharacter(value));
        return new EncodeResult(ValueType.STRING, output.getData());
    }
    public static String decodeReference(PackageBlock packageBlock, ValueType referenceType, int resourceId) {
        if (resourceId == 0) {
            if (referenceType == ValueType.REFERENCE) {
                return CoderNullReference.INS.decode(resourceId);
            }
            if (referenceType == ValueType.ATTRIBUTE) {
                return CoderNullAttribute.INS.decode(resourceId);
            }
            if (referenceType == ValueType.DYNAMIC_REFERENCE) {
                return CoderNullDynamicReference.INS.decode(resourceId);
            }
            if (referenceType == ValueType.DYNAMIC_ATTRIBUTE) {
                return CoderNullDynamicAttribute.INS.decode(resourceId);
            }
            return CoderNullAttribute.INS.decode(resourceId);
        }
        TableBlock tableBlock = packageBlock.getTableBlock();
        ResourceEntry resourceEntry = tableBlock.getResource(packageBlock, resourceId);
        if (resourceEntry != null && resourceEntry.isDefined()) {
            return resourceEntry.buildReference(packageBlock, referenceType);
        }
        return decodeUnknownResourceId(referenceType, resourceId);
    }
    public static EncodeResult encodeReference(PackageBlock packageBlock, String value) {
        if (value == null || value.length() < 3) {
            return null;
        }
        EncodeResult encodeResult = encodeUnknownResourceId(value);
        if (encodeResult != null) {
            return encodeResult;
        }
        ReferenceString referenceString = ReferenceString.parseReference(value);
        if (referenceString != null) {
            encodeResult = referenceString.encode(packageBlock, EncodeResult.RESOURCE_NOT_FOUND);
            if (encodeResult.isError()) {
                encodeResult = new EncodeResult(
                        buildResourceNotFoundMessage(packageBlock, value));
            }
            return encodeResult;
        }
        return null;
    }
    public static EncodeResult encodeReference(TableBlock tableBlock, String value) {
        if (value == null || value.length() < 3) {
            return null;
        }
        EncodeResult encodeResult = encodeUnknownResourceId(value);
        if (encodeResult != null) {
            return encodeResult;
        }
        ReferenceString referenceString = ReferenceString.parseReference(value);
        if (referenceString != null) {
            encodeResult = referenceString.encode(tableBlock, EncodeResult.RESOURCE_NOT_FOUND);
            if (encodeResult.isError()) {
                encodeResult = new EncodeResult(
                        buildResourceNotFoundMessage(tableBlock, value));
            }
            return encodeResult;
        }
        return null;
    }
    private static String buildResourceNotFoundMessage(PackageBlock packageBlock, String value) {
        TableBlock tableBlock = packageBlock.getTableBlock();
        if (tableBlock == null) {
            return "Resource not found for: '" + value +
                    "', package " + packageBlock + ", parent table = null";
        }
        return buildResourceNotFoundMessage(tableBlock, value);
    }
    private static String buildResourceNotFoundMessage(TableBlock tableBlock, String value) {
        return "Resource not found for: '" + value +
                "', frameworks " +
                StringsUtil.toString(tableBlock.getFrameWorks());
    }
    public static String decodeUnknownNameId(int referenceId) {
        return CoderUnknownNameId.INS.decode(referenceId);
    }
    public static String decodeUnknownResourceId(ValueType valueType, int referenceId) {
        if (valueType == ValueType.REFERENCE) {
            return CoderUnknownReferenceId.INS.decode(referenceId);
        }
        if (valueType == ValueType.ATTRIBUTE) {
            return CoderUnknownAttributeId.INS.decode(referenceId);
        }
        if (valueType == ValueType.DYNAMIC_REFERENCE) {
            return CoderUnknownDynamicReferenceId.INS.decode(referenceId);
        }
        if (valueType == ValueType.DYNAMIC_ATTRIBUTE) {
            return CoderUnknownDynamicAttributeId.INS.decode(referenceId);
        }
        return null;
    }
    public static EncodeResult encodeUnknownNameId(String text) {
        return CoderUnknownNameId.INS.encode(text);
    }
    public static EncodeResult encodeUnknownResourceId(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        EncodeResult encodeResult = encodeNull(text);
        if (encodeResult != null) {
            return encodeResult;
        }
        return encodeUnknown(text);
    }

    public static EncodeResult encodeHexOrInteger(String text) {
        if (text == null) {
            return null;
        }
        EncodeResult encodeResult = CoderHex.INS.encode(text);
        if (encodeResult == null) {
            encodeResult = CoderInteger.INS.encode(text);
        }
        return encodeResult;
    }
    public static EncodeResult encode(String text, AttributeDataFormat... expectedDataFormats) {
        if (isEmpty(expectedDataFormats)) {
            return encodeAny(text);
        }
        if (text == null || text.length() == 0) {
            return null;
        }
        EncodeResult encodeResult = encodeUnknown(text);
        if (encodeResult != null) {
            return encodeResult;
        }
        return encodeWithin(text, expectedDataFormats);
    }
    public static EncodeResult encode(String text, ValueType... expectedTypes) {
        if (isEmpty(expectedTypes)) {
            return encodeAny(text);
        }
        if (text == null || text.length() == 0) {
            return null;
        }
        EncodeResult encodeResult = encodeUnknown(text);
        if (encodeResult != null) {
            return encodeResult;
        }
        return encodeWithin(text, expectedTypes);
    }
    public static EncodeResult encode(String text) {
        return encodeAny(text);
    }
    private static boolean isEmpty(Object[] objects) {
        if (objects == null || objects.length == 0) {
            return true;
        }
        for(Object obj : objects) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }
    private static EncodeResult encodeWithin(String text, AttributeDataFormat... expectedDataFormats) {
        if (text == null || text.length() == 0) {
            return null;
        }
        for(AttributeDataFormat dataFormat : expectedDataFormats) {
            EncodeResult encodeResult = encodeWithin(text, dataFormat.valueTypes());
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        return null;
    }
    private static EncodeResult encodeWithin(String text, ValueType... expectedTypes) {
        if (text == null || text.length() == 0) {
            return null;
        }
        EncodeResult encodeResult;
        char first = text.charAt(0);
        for(ValueType valueType : expectedTypes) {
            Coder coder = getCoder(valueType);
            if (coder == null) {
                continue;
            }
            if (!coder.canStartWith(first)) {
                continue;
            }
            encodeResult = coder.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        return null;
    }
    private static EncodeResult encodeAny(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        EncodeResult encodeResult = encodeUnknown(text);
        if (encodeResult != null) {
            return encodeResult;
        }
        char first = text.charAt(0);
        for(Coder coder : CODERS) {
            if (!coder.canStartWith(first)) {
                continue;
            }
            encodeResult = coder.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        return null;
    }
    private static EncodeResult encodeUnknown(String text) {
        char first = text.charAt(0);
        Coder unknown = CoderUnknownReferenceId.INS;
        EncodeResult encodeResult;
        if (unknown.canStartWith(first)) {
            encodeResult = unknown.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        unknown = CoderUnknownAttributeId.INS;
        if (unknown.canStartWith(first)) {
            encodeResult = unknown.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        unknown = CoderUnknownDynamicReferenceId.INS;
        if (unknown.canStartWith(first)) {
            encodeResult = unknown.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        unknown = CoderUnknownDynamicAttributeId.INS;
        if (unknown.canStartWith(first)) {
            encodeResult = unknown.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        unknown = CoderUnknownStringRef.INS;
        if (unknown.canStartWith(first)) {
            return unknown.encode(text);
        }
        return null;
    }
    public static String decode(ValueType valueType, int data) {
        String decoded = decodeNull(valueType, data);
        if (decoded != null) {
            return decoded;
        }
        Coder coder = CODER_MAP.get(valueType);
        if (coder == null) {
            return null;
        }
        return coder.decode(data);
    }
    private static EncodeResult encodeNull(String text) {
        if (text == null) {
            return null;
        }
        int length = text.length();
        if (length < 5 || length > 8) {
            return null;
        }
        char first = text.charAt(0);
        for (Coder coder : CODERS_NULL) {
            if (!coder.canStartWith(first)) {
                continue;
            }
            EncodeResult encodeResult = coder.encode(text);
            if (encodeResult != null) {
                return encodeResult;
            }
        }
        return null;
    }
    private static String decodeNull(ValueType valueType, int data) {
        if (data != 0 && data != 1) {
            return null;
        }
        if (valueType == ValueType.NULL) {
            return CoderNull.INS.decode(data);
        }
        if (data != 0) {
            return null;
        }
        if (valueType == ValueType.ATTRIBUTE) {
            return CoderNullAttribute.INS.decode(data);
        }
        if (valueType == ValueType.DYNAMIC_ATTRIBUTE) {
            return CoderNullDynamicAttribute.INS.decode(data);
        }
        if (valueType == ValueType.REFERENCE) {
            return CoderNullReference.INS.decode(data);
        }
        if (valueType == ValueType.DYNAMIC_REFERENCE) {
            return CoderNullDynamicReference.INS.decode(data);
        }
        return null;
    }
    public static Coder getCoder(ValueType valueType) {
        return CODER_MAP.get(valueType);
    }



    public static final Coder[] CODERS;
    private static final Map<ValueType, Coder> CODER_MAP;
    private static final Coder[] CODERS_NULL;

    static {

        CODERS = new Coder[]{
                CoderNull.INS,
                CoderBoolean.INS,
                CoderDimension.INS,
                CoderFraction.INS,
                CoderColorARGB4.INS,
                CoderColorRGB4.INS,
                CoderColorRGB8.INS,
                CoderColorARGB8.INS,
                CoderFloat.INS,
                CoderHex.INS,
                CoderInteger.INS
        };
        Map<ValueType, Coder> map = new HashMap<>();
        map.put(CoderNull.INS.getValueType(), CoderNull.INS);
        map.put(CoderBoolean.INS.getValueType(), CoderBoolean.INS);
        map.put(CoderDimension.INS.getValueType(), CoderDimension.INS);
        map.put(CoderFraction.INS.getValueType(), CoderFraction.INS);
        map.put(CoderColorRGB4.INS.getValueType(), CoderColorRGB4.INS);
        map.put(CoderColorARGB4.INS.getValueType(), CoderColorARGB4.INS);
        map.put(CoderColorRGB8.INS.getValueType(), CoderColorRGB8.INS);
        map.put(CoderColorARGB8.INS.getValueType(), CoderColorARGB8.INS);
        map.put(CoderFloat.INS.getValueType(), CoderFloat.INS);
        map.put(CoderHex.INS.getValueType(), CoderHex.INS);
        map.put(CoderInteger.INS.getValueType(), CoderInteger.INS);
        CODER_MAP = map;

        CODERS_NULL = new Coder[]{
                CoderNullReference.INS,
                CoderNullAttribute.INS,
                CoderNullDynamicReference.INS,
                CoderNullDynamicAttribute.INS,
                CoderNull.INS,
        };
    }
}
