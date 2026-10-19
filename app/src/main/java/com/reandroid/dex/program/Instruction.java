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

import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;

public interface Instruction extends InstructionStatement {
    InstructionOpcode getOpcode();
    int getAddress();
    int getCodeUnits();
    void addReferencingLabel(Object label);
    boolean isRemoved();
    // TODO: implement everywhere
    default ReferenceLabelSet getReferenceLabelSet() {
        return null;
    }

    default boolean is(InstructionOpcode opcode) {
        return getOpcode() == opcode;
    }
    default boolean isArrayGet() {
        return getOpcode().isArrayOp();
    }
    default boolean isArrayOp() {
        return getOpcode().isArrayOp();
    }
    default boolean isArrayPut() {
        return getOpcode().isArrayPut();
    }
    default boolean isConst() {
        return getOpcode().isConst();
    }

    default boolean isConstInteger() {
        return getOpcode().isConstInteger();
    }
    default boolean isConstNumber() {
        return getOpcode().isConstNumber();
    }
    default boolean isConstString() {
        return getOpcode().isConstString();
    }
    default boolean isConstWide() {
        return getOpcode().isConstWide();
    }
    default boolean isFieldGet() {
        return getOpcode().isFieldGet();
    }
    default boolean isFieldInstanceGet() {
        return getOpcode().isFieldInstanceGet();
    }
    default boolean isFieldInstanceOp() {
        return getOpcode().isFieldInstanceOp();
    }
    default boolean isFieldInstancePut() {
        return getOpcode().isFieldInstancePut();
    }
    default boolean isFieldOp() {
        return getOpcode().isFieldOp();
    }
    default boolean isFieldPut() {
        return getOpcode().isFieldPut();
    }
    default boolean isFieldStaticGet() {
        return getOpcode().isFieldStaticGet();
    }
    default boolean isFieldStaticOp() {
        return getOpcode().isFieldStaticOp();
    }
    default boolean isFieldStaticPut() {
        return getOpcode().isFieldStaticPut();
    }
    default boolean isGoto() {
        return getOpcode().isGoto();
    }
    default boolean isIfTest() {
        return getOpcode().isMethodExit();
    }
    default boolean isMethodExit() {
        return getOpcode().isMethodExit();
    }
    default boolean isMethodInvoke() {
        return getOpcode().isMethodInvoke();
    }
    default boolean isMethodInvokeDirect() {
        return getOpcode().isMethodInvokeDirect();
    }
    default boolean isMethodInvokeInterface() {
        return getOpcode().isMethodInvokeInterface();
    }
    default boolean isMethodInvokeStatic() {
        return getOpcode().isMethodInvokeStatic();
    }
    default boolean isMethodInvokeSuper() {
        return getOpcode().isMethodInvokeSuper();
    }
    default boolean isMethodInvokeVirtual() {
        return getOpcode().isMethodInvokeVirtual();
    }
    default boolean isMove() {
        return getOpcode().isMove();
    }
    default boolean isMoveResult() {
        return getOpcode().isMoveResult();
    }
    default boolean isPayload() {
        return getOpcode().isPayload();
    }
    default boolean isRange() {
        return getOpcode().isRange();
    }
    default boolean isReturn() {
        return getOpcode().isReturn();
    }
    default boolean isSwitch() {
        return getOpcode().isSwitch();
    }

    Key getAsKey();

    default TypeKey getKeyAsType() {
        Key key = getAsKey();
        if (key instanceof TypeKey) {
            return (TypeKey) key;
        }
        return null;
    }
    default MethodKey getKeyAsMethod() {
        Key key = getAsKey();
        if (key instanceof MethodKey) {
            return (MethodKey) key;
        }
        return null;
    }
    default FieldKey getKeyAsField() {
        Key key = getAsKey();
        if (key instanceof FieldKey) {
            return (FieldKey) key;
        }
        return null;
    }
    default StringKey getKeyAsString() {
        Key key = getAsKey();
        if (key instanceof StringKey) {
            return (StringKey) key;
        }
        return null;
    }
}
