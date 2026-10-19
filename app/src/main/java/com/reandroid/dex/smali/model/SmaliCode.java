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
package com.reandroid.dex.smali.model;

import com.reandroid.common.Origin;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliReader;

import java.io.IOException;

public class SmaliCode extends Smali {

    public SmaliCode(){
        super();
    }

    public SmaliCodeSet getCodeSet(){
        return getParentInstance(SmaliCodeSet.class);
    }
    public SmaliCode previous() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.get(getIndex() - 1);
        }
        return null;
    }
    public SmaliCode next() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.get(getIndex() + 1);
        }
        return null;
    }
    @Override
    public void parse(SmaliReader reader) throws IOException {

    }
    public String buildOrigin() {
        StringBuilder builder = new StringBuilder();
        Origin origin = getOrigin();
        if (origin != null) {
            builder.append('\n');
            builder.append(origin);
        } else {
            SmaliMethod method = getParentInstance(SmaliMethod.class);
            if (method != null) {
                MethodKey key = method.getKey();
                if (key != null) {
                    builder.append(" on method: ");
                    builder.append(key);
                }
            }
        }
        return builder.toString();
    }


    public static SmaliCode createCode(SmaliReader reader) {
        int position  = reader.position();
        reader.skipWhitespacesOrComment();
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        SmaliCode smaliCode = null;
        if (directive != null) {
            smaliCode = SmaliCode.createFor(directive);
        } else if (reader.get() == ':') {
            smaliCode = new SmaliLabelDestination();
        } else {
            Opcode<?> opcode = Opcode.parseSmali(reader, false);
            if (opcode != null) {
                smaliCode = SmaliInstruction.createInstruction(opcode);
            }
        }
        if (smaliCode == null) {
            reader.position(position);
        }
        return smaliCode;
    }

    public static SmaliCode createFor(SmaliDirective directive) {
        if (directive == SmaliDirective.LINE) {
            return new SmaliLineNumber();
        }
        if (directive == SmaliDirective.CATCH || directive == SmaliDirective.CATCH_ALL) {
            return new SmaliTryItem();
        }
        if (directive == SmaliDirective.PARAM) {
            return new SmaliMethodParameter();
        }
        if (directive == SmaliDirective.END_LOCAL) {
            return new SmaliDebugEndLocal();
        }
        if (directive == SmaliDirective.LOCAL) {
            return new SmaliDebugLocal();
        }
        if (directive == SmaliDirective.RESTART_LOCAL) {
            return new SmaliDebugRestartLocal();
        }
        if (directive == SmaliDirective.ARRAY_DATA) {
            return new SmaliPayloadArray();
        }
        if (directive == SmaliDirective.PACKED_SWITCH) {
            return new SmaliPayloadPackedSwitch();
        }
        if (directive == SmaliDirective.SPARSE_SWITCH) {
            return new SmaliPayloadSparseSwitch();
        }
        if (directive == SmaliDirective.PROLOGUE) {
            return new SmaliDebugPrologue();
        }
        if (directive == SmaliDirective.EPILOGUE) {
            return new SmaliDebugEpilogue();
        }
        return null;
    }
}
