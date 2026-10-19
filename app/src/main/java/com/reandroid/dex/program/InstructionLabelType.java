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
package com.reandroid.dex.program;

import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;

public class InstructionLabelType implements Comparable<InstructionLabelType> {

    public static final InstructionLabelType LINE;
    public static final InstructionLabelType DEBUG;
    public static final InstructionLabelType TRY_END;
    public static final InstructionLabelType CATCH_HANDLER;
    public static final InstructionLabelType CATCH_ALL_HANDLER;
    public static final InstructionLabelType CATCH;
    public static final InstructionLabelType CATCH_ALL;
    public static final InstructionLabelType COND;
    public static final InstructionLabelType GOTO;
    public static final InstructionLabelType ARRAY;
    public static final InstructionLabelType ARRAY_DATA;
    public static final InstructionLabelType P_SWITCH_DATA;
    public static final InstructionLabelType S_SWITCH_DATA;
    public static final InstructionLabelType P_SWITCH;
    public static final InstructionLabelType S_SWITCH;
    public static final InstructionLabelType INSTRUCTION;
    public static final InstructionLabelType INSTRUCTION_OTHER;
    public static final InstructionLabelType TRY_START;

    public static final InstructionLabelType OTHER;

    static {

        LINE = new InstructionLabelType(0, ".line ");
        DEBUG = new InstructionLabelType(1, "");

        TRY_END = new InstructionLabelType(8, ":try_end_");
        CATCH_HANDLER = new InstructionLabelType(9, ".catch ");
        CATCH_ALL_HANDLER = new InstructionLabelType(10, ".catchall ");

        CATCH = new InstructionLabelType(16, ":catch_");
        CATCH_ALL = new InstructionLabelType(17, ":catchall_");

        COND = new InstructionLabelType(24, ":cond_");
        GOTO = new InstructionLabelType(25, ":goto_");

        ARRAY = new InstructionLabelType(32, ":array_");
        ARRAY_DATA = new InstructionLabelType(33, ":array_data_");
        P_SWITCH_DATA = new InstructionLabelType(34, ":pswitch_data_");
        S_SWITCH_DATA = new InstructionLabelType(35, ":sswitch_data_");

        P_SWITCH = new InstructionLabelType(40, ":pswitch_");
        S_SWITCH = new InstructionLabelType(41, ":sswitch_");

        INSTRUCTION = new InstructionLabelType(48, ":label_");
        INSTRUCTION_OTHER = new InstructionLabelType(49, "");

        TRY_START = new InstructionLabelType(56, ":try_start_");

        OTHER = new InstructionLabelType(72, "");
    }

    private final int order;
    private final String prefix;

    private InstructionLabelType(int order, String prefix) {
        this.order = order;
        this.prefix = prefix;
    }

    public int getOrder() {
        return order;
    }
    public String prefix() {
        return prefix;
    }
    public boolean isHandler() {
        return CATCH_HANDLER == this || CATCH_ALL_HANDLER == this;
    }
    public boolean isLocation() {
        String prefix = prefix();
        return prefix.length() != 0 && prefix.charAt(0) == ':';
    }

    public String buildLabelName(int address) {
        return buildLabelName(address, true);
    }
    public String buildLabelName(int address, boolean hex) {
        String prefix = this.prefix();
        if (StringsUtil.isEmpty(prefix)) {
            return "";
        }
        if (hex) {
            return HexUtil.toHex(prefix, address, 1);
        }
        return prefix + address;
    }
    @Override
    public int compareTo(InstructionLabelType labelType) {
        if (labelType == this || labelType == null) {
            return 0;
        }
        return CompareUtil.compare(this.order, labelType.order);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InstructionLabelType labelType = (InstructionLabelType) obj;
        return order == labelType.order && ObjectsUtil.equals(prefix, labelType.prefix);
    }

    @Override
    public int hashCode() {
        return 31 + order + ObjectsUtil.hash(prefix);
    }

    @Override
    public String toString() {
        return prefix;
    }
}
