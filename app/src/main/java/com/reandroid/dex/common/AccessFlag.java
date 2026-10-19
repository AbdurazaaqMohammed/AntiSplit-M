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
package com.reandroid.dex.common;

import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import com.reandroid.utils.function.Predicate;

public class AccessFlag extends Modifier {

    public static final AccessFlag PUBLIC;
    public static final AccessFlag PRIVATE;
    public static final AccessFlag PROTECTED;
    public static final AccessFlag STATIC;
    public static final AccessFlag FINAL;
    public static final AccessFlag SYNCHRONIZED;
    public static final AccessFlag VOLATILE;
    public static final AccessFlag BRIDGE;
    public static final AccessFlag TRANSIENT;
    public static final AccessFlag VARARGS;
    public static final AccessFlag NATIVE;
    public static final AccessFlag INTERFACE;
    public static final AccessFlag ABSTRACT;
    public static final AccessFlag STRICTFP;
    public static final AccessFlag SYNTHETIC;
    public static final AccessFlag ANNOTATION;
    public static final AccessFlag ENUM;

    // for flags of dalvik.annotation.MethodParameters
    public static final AccessFlag MANDATED;

    public static final AccessFlag CONSTRUCTOR;
    public static final AccessFlag DECLARED_SYNCHRONIZED;

    private static final AccessFlag[] VALUES;
    private static final AccessFlag[] METHOD_VALUES;
    private static final int MASK;

    private static final HashMap<String, AccessFlag> accessFlagsByName;

    static {

        AccessFlag[] values = new AccessFlag[18];
        VALUES = values;

        PUBLIC = values[0] = new AccessFlag(0x1, "public", true, true, true);
        PRIVATE = values[1] = new AccessFlag(0x1 << 1, "private", true, true, true);
        PROTECTED = values[2] = new AccessFlag(0x1 << 2, "protected", true, true, true);
        STATIC = values[3] = new AccessFlag(0x1 << 3, "static", true, true, true);
        FINAL = values[4] = new AccessFlag(0x1 << 4, "final", true, true, true);
        SYNCHRONIZED = values[5] = new AccessFlag(0x1 << 5, "synchronized", false, true, false);

        VOLATILE = values[6] = new AccessFlag(0x1 << 6, "volatile", false, false, true);
        BRIDGE = new AccessFlag(0x1 << 6, "bridge", false, true, false);

        TRANSIENT = values[7] = new AccessFlag(0x1 << 7, "transient", false, false, true);
        VARARGS = new AccessFlag(0x1 << 7, "varargs", false, true, false);

        NATIVE = values[8] = new AccessFlag(0x1 << 8, "native", false, true, false);
        INTERFACE = values[9] = new AccessFlag(0x1 << 9, "interface", true, false, false);
        ABSTRACT = values[10] = new AccessFlag(0x1 << 10, "abstract", true, true, false);
        STRICTFP = values[11] = new AccessFlag(0x1 << 11, "strictfp", false, true, false);
        SYNTHETIC = values[12] = new AccessFlag(0x1 << 12, "synthetic", true, true, true);
        ANNOTATION = values[13] = new AccessFlag(0x1 << 13, "annotation", true, false, false);
        ENUM = values[14] = new AccessFlag(0x1 << 14, "enum", true, false, true);
        MANDATED = values[15] = new AccessFlag(0x1 << 15, "mandated", false, false, false);
        CONSTRUCTOR = values[16] = new AccessFlag(0x1 << 16, "constructor", false, true, false);
        DECLARED_SYNCHRONIZED = values[17] = new AccessFlag(0x1 << 17, "declared-synchronized", false, true, false);

        MASK = (0x1 << values.length) - 1;

        AccessFlag[] methodValues = values.clone();
        methodValues[6] = BRIDGE;
        methodValues[7] = VARARGS;
        METHOD_VALUES = methodValues;

        HashMap<String, AccessFlag> map = new HashMap<>(20);
        accessFlagsByName = map;

        for (AccessFlag accessFlag : values) {
            map.put(accessFlag.getName(), accessFlag);
        }
        map.put(BRIDGE.getName(), BRIDGE);
        map.put(VARARGS.getName(), VARARGS);
    }

    private final boolean validForClass;
    private final boolean validForMethod;
    private final boolean validForField;

    private AccessFlag(int value, String name, boolean validForClass, boolean validForMethod,
                       boolean validForField) {
        super(value, name);
        this.validForClass = validForClass;
        this.validForMethod = validForMethod;
        this.validForField = validForField;
    }

    public boolean isValidForClass() {
        return validForClass;
    }
    public boolean isValidForField() {
        return validForField;
    }
    public boolean isValidForMethod() {
        return validForMethod;
    }

    @Override
    public boolean isSet(int accessFlags) {
        return (getValue() & accessFlags) != 0;
    }

    public static Iterator<AccessFlag> valuesOf(ElementType elementType, int value) {
        return valuesOf(elementType == ElementType.METHOD, value);
    }
    public static Iterator<AccessFlag> valuesOf(int value) {
        value = value & MASK;
        if (value == 0) {
            return EmptyIterator.of();
        }
        return new FlagsIterator(VALUES, value);
    }
    public static Iterator<AccessFlag> valuesOf(boolean method, int value) {
        value = value & MASK;
        if (value == 0) {
            return EmptyIterator.of();
        }
        AccessFlag[] values;
        if (method) {
            values = METHOD_VALUES;
        } else {
            values = VALUES;
        }
        return new FlagsIterator(values, value);
    }
    public static AccessFlag valueOf(String name) {
        return accessFlagsByName.get(name);
    }
    public static Iterator<AccessFlag> getValues() {
        return new ArrayIterator<>(VALUES);
    }
    public static Iterator<AccessFlag> getValues(Predicate<AccessFlag> filter) {
        return new ArrayIterator<>(VALUES, filter);
    }
    public static String toString(int flags) {
        return toString(false, flags);
    }
    public static String toString(boolean method, int flags) {
        flags = flags & MASK;
        if (flags == 0) {
            return StringsUtil.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        try {
            append(method, flags, builder);
        } catch (IOException e) {
            builder.append(" # Unexpected AccessFlags.append: ");
            builder.append(e.getMessage());
        }
        return builder.toString();
    }
    public static void append(int flags, Appendable appendable) throws IOException {
        append(false, flags, appendable);
    }
    public static void append(boolean method, int flags, Appendable appendable) throws IOException {
        flags = flags & MASK;
        if (flags == 0) {
            return;
        }
        AccessFlag[] values;
        if (method) {
            values = METHOD_VALUES;
        } else {
            values = VALUES;
        }
        int i = 0;
        while (flags != 0) {
            if ((flags & 0x1) != 0) {
                appendable.append(values[i].getName());
                appendable.append(' ');
            }
            flags = flags >>> 1;
            i ++;
        }
    }
    public static AccessFlag[] parse(SmaliReader reader) {
        List<AccessFlag> accessFlags = null;
        AccessFlag flag;
        while ((flag = parseNext(reader)) != null) {
            if (accessFlags == null) {
                accessFlags = new ArrayCollection<>();
            }
            accessFlags.add(flag);
        }
        if (accessFlags == null) {
            return null;
        }
        int size = accessFlags.size();
        if (size == 0) {
            return null;
        }
        reader.skipWhitespaces();
        return accessFlags.toArray(new AccessFlag[size]);
    }
    private static AccessFlag parseNext(SmaliReader reader) {
        reader.skipWhitespaces();
        int i = reader.indexOf(' ');
        if (i < 0) {
            return null;
        }
        int position = reader.position();
        AccessFlag accessFlag = valueOf(reader.readString(i - reader.position()));
        if (accessFlag == null) {
            reader.position(position);
        }
        return accessFlag;
    }

    public static int combineAccessFlags(Iterator<? extends Modifier> iterator) {
        int result = 0;
        while (iterator.hasNext()) {
            Modifier modifier = iterator.next();
            if (modifier instanceof AccessFlag) {
                result |= modifier.getValue();
            }
        }
        return result;
    }
    static class FlagsIterator implements Iterator<AccessFlag> {

        private final AccessFlag[] values;
        private int flags;
        private int index;

        public FlagsIterator(AccessFlag[] values, int flags) {
            this.values = values;
            this.flags = flags;
        }
        @Override
        public boolean hasNext() {
            return flags != 0;
        }
        @Override
        public AccessFlag next() {
            int flags = this.flags;
            if (flags == 0) {
                throw new NoSuchElementException();
            }
            int index = this.index;
            while ((flags & 0x1) == 0) {
                flags = flags >>> 1;
                index ++;
            }
            AccessFlag accessFlag = values[index];
            this.flags = flags >>> 1;
            this.index = index + 1;
            return accessFlag;
        }
    }
}
