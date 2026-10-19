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

import com.reandroid.dex.key.TypeKey;

public interface ExceptionHandler extends InstructionLabel, InstructionLabelSet {

    InstructionLabel getStartLabel();
    InstructionLabel getEndLabel();
    InstructionLabel getCatchLabel();

    boolean isCatchAll();
    TypeKey getKey();

    @Override
    default InstructionLabelType getLabelType() {
        if (isCatchAll()) {
            return InstructionLabelType.CATCH_ALL_HANDLER;
        }
        return InstructionLabelType.CATCH_HANDLER;
    }
    @Override
    default String getLabelName() {
        return Util.buildLabelName(this);
    }
    default int getStartAddress() {
        return getStartLabel().getTargetAddress();
    }
    @Override
    default int getTargetAddress() {
        return getEndLabel().getTargetAddress();
    }
    @Override
    default Instruction getTargetInstruction() {
        return getEndLabel().getTargetInstruction();
    }

    class Util {
        public static String buildLabelName(ExceptionHandler handler) {
            StringBuilder builder = new StringBuilder();
            builder.append(handler.getLabelType().prefix());
            if (!handler.isCatchAll()) {
                builder.append(handler.getKey());
                builder.append(' ');
            }
            builder.append("{");
            builder.append(handler.getStartLabel().getLabelName());
            builder.append(" .. ");
            builder.append(handler.getEndLabel().getLabelName());
            builder.append("} ");
            builder.append(handler.getCatchLabel().getLabelName());
            return builder.toString();
        }
    }
}
