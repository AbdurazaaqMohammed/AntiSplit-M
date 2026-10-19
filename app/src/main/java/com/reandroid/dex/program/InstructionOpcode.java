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

public interface InstructionOpcode extends ProgramTypeDescriptor {
    int getValue();
    String getName();

    boolean isArrayGet();
    boolean isArrayOp();
    boolean isArrayPut();
    boolean isConst();
    boolean isConstInteger();
    boolean isConstNumber();
    boolean isConstString();
    boolean isConstWide();
    boolean isFieldGet();
    boolean isFieldInstanceGet();
    boolean isFieldInstanceOp();
    boolean isFieldInstancePut();
    boolean isFieldOp();
    boolean isFieldPut();
    boolean isFieldStaticGet();
    boolean isFieldStaticOp();
    boolean isFieldStaticPut();
    boolean isGoto();
    boolean isIfTest();
    boolean isMethodExit();
    boolean isMethodInvoke();
    boolean isMethodInvokeDirect();
    boolean isMethodInvokeInterface();
    boolean isMethodInvokeStatic();
    boolean isMethodInvokeSuper();
    boolean isMethodInvokeVirtual();
    boolean isMove();
    boolean isMoveResult();
    boolean isPayload();
    boolean isRange();
    boolean isReturn();
    boolean isSwitch();
}
