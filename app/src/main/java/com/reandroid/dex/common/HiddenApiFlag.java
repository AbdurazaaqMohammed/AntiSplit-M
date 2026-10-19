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

import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.utils.ObjectsStore;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.reandroid.utils.function.Predicate;

public class HiddenApiFlag extends Modifier implements SmaliFormat {

    public static final HiddenApiFlag WHITELIST;
    public static final HiddenApiFlag GREYLIST;
    public static final HiddenApiFlag BLACKLIST;
    public static final HiddenApiFlag GREYLIST_MAX_O;
    public static final HiddenApiFlag GREYLIST_MAX_P;
    public static final HiddenApiFlag GREYLIST_MAX_Q;
    public static final HiddenApiFlag GREYLIST_MAX_R;
    public static final HiddenApiFlag CORE_PLATFORM_API;

    public static final int NO_RESTRICTION;

    public static final HiddenApiFlag TEST_API;
    private static final HiddenApiFlag[] RESTRICTION_VALUES;
    private static final HiddenApiFlag[] DOMAIN_VALUES;

    private static final HiddenApiFlag[] VALUES;

    private static final Map<String, HiddenApiFlag> NAME_MAP;

    static {

        WHITELIST = new HiddenApiFlag(0, "whitelist");
        GREYLIST = new HiddenApiFlag(1, "greylist");
        BLACKLIST = new HiddenApiFlag(2, "blacklist");
        GREYLIST_MAX_O = new HiddenApiFlag(3, "greylist-max-o");
        GREYLIST_MAX_P = new HiddenApiFlag(4, "greylist-max-p");
        GREYLIST_MAX_Q = new HiddenApiFlag(5, "greylist-max-q");
        GREYLIST_MAX_R = new HiddenApiFlag(6, "greylist-max-r");

        CORE_PLATFORM_API = new HiddenApiFlag(8, "core-platform-api", true);
        TEST_API = new HiddenApiFlag(16, "test-api", true);

        NO_RESTRICTION = ObjectsUtil.of(0x7);

        RESTRICTION_VALUES = new HiddenApiFlag[]{
                WHITELIST,
                GREYLIST,
                BLACKLIST,
                GREYLIST_MAX_O,
                GREYLIST_MAX_P,
                GREYLIST_MAX_Q,
                GREYLIST_MAX_R
        };
        DOMAIN_VALUES = new HiddenApiFlag[]{
                CORE_PLATFORM_API,
                TEST_API
        };
        VALUES = new HiddenApiFlag[RESTRICTION_VALUES.length + DOMAIN_VALUES.length];

        Map<String, HiddenApiFlag> map = new HashMap<>();
        int index = 0;
        for (HiddenApiFlag flag : RESTRICTION_VALUES) {
            VALUES[index++] = flag;
            map.put(flag.getName(), flag);
        }
        for (HiddenApiFlag flag : DOMAIN_VALUES) {
            VALUES[index++] = flag;
            map.put(flag.getName(), flag);
        }
        NAME_MAP = map;
    }

    private final boolean domainFlag;

    private HiddenApiFlag(int value, String name, boolean domainFlag) {
        super(value, name);
        this.domainFlag = domainFlag;
    }
    private HiddenApiFlag(int value, String name) {
        this(value, name, false);
    }

    @Override
    public boolean isSet(int value) {
        int v = getValue();
        if (domainFlag) {
            return (value & v) == v;
        } else {
            return (value & NO_RESTRICTION) == v;
        }
    }
    public boolean isDomainFlag() {
        return domainFlag;
    }

    public static HiddenApiFlag valueOf(String name) {
        return NAME_MAP.get(name);
    }

    public static Iterator<HiddenApiFlag> valuesOf(int value) {
        if (value == NO_RESTRICTION) {
            return EmptyIterator.of();
        }
        if (value >= 0 && value < NO_RESTRICTION) {
            return SingleIterator.of(RESTRICTION_VALUES[value]);
        }
        return getValues(hiddenApiFlag -> hiddenApiFlag.isSet(value));
    }
    public static Iterator<HiddenApiFlag> getValues() {
        return getValues(null);
    }
    public static Iterator<HiddenApiFlag> getValues(Predicate<HiddenApiFlag> filter) {
        return new ArrayIterator<>(VALUES, filter);
    }
    public static HiddenApiFlag restrictionOf(int value) {
        for (HiddenApiFlag flag : RESTRICTION_VALUES) {
            if (flag.isSet(value)) {
                return flag;
            }
        }
        return null;
    }
    public static HiddenApiFlag domainOf(int value) {
        for (HiddenApiFlag flag : DOMAIN_VALUES) {
            if (flag.isSet(value)) {
                return flag;
            }
        }
        return null;
    }

    public static int parseValues(SmaliReader reader) {
        return combineHiddenApiFlag(parse(reader));
    }
    public static HiddenApiFlag[] parse(SmaliReader reader) {
        Object container = null;
        HiddenApiFlag flag;
        while ((flag = parseNext(reader)) != null) {
            container = ObjectsStore.add(container, flag);
        }
        if (container == null) {
            return null;
        }
        int size = ObjectsStore.size(container);
        reader.skipWhitespaces();
        HiddenApiFlag[] results = new HiddenApiFlag[size];
        ObjectsStore.collect(container, results);
        return results;
    }
    private static HiddenApiFlag parseNext(SmaliReader reader) {
        reader.skipWhitespaces();
        int i = reader.indexOf(' ');
        if (i < 0) {
            return null;
        }
        int position = reader.position();
        HiddenApiFlag hiddenApiFlag = valueOf(reader.readString(i - reader.position()));
        if (hiddenApiFlag == null) {
            reader.position(position);
        }
        return hiddenApiFlag;
    }

    public static int combineHiddenApiFlag(HiddenApiFlag[] flags) {
        int result = NO_RESTRICTION;
        if (flags != null) {
            for (HiddenApiFlag flag : flags) {
                int value = flag.getValue();
                if (result == NO_RESTRICTION) {
                    result = value;
                } else {
                    result |= value;
                }
            }
        }
        return result;
    }

    public static int combineHiddenApiFlag(Iterator<? extends Modifier> iterator) {
        int result = NO_RESTRICTION;
        while (iterator.hasNext()) {
            Modifier modifier = iterator.next();
            if (modifier instanceof AccessFlag) {
                int value = modifier.getValue();
                if (result == NO_RESTRICTION) {
                    result = value;
                } else {
                    result |= value;
                }
            }
        }
        return result;
    }
}
