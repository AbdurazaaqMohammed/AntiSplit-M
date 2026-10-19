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

import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.ExceptionHandler;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class SmaliCatchTypedHandler extends SmaliExceptionHandler {

    private TypeKey type;

    public SmaliCatchTypedHandler() {
        super();
    }

    @Override
    public boolean isCatchAll() {
        return false;
    }

    @Override
    public TypeKey getKey() {
        return type;
    }
    public void setKey(TypeKey type) {
        this.type = type;
    }
    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.CATCH;
    }

    @Override
    public InstructionLabelType getLabelType() {
        return InstructionLabelType.CATCH_HANDLER;
    }

    @Override
    public void appendType(SmaliWriter writer) throws IOException {
        TypeKey typeKey = getKey();
        if (typeKey != null) {
            typeKey.append(writer);
        } else {
            writer.append("#Error: null type");
        }
        writer.append(' ');
    }
    @Override
    protected void parseType(SmaliReader reader) throws IOException {
        setKey(TypeKey.read(reader));
    }

    @Override
    public void fromProgram(ExceptionHandler handler) {
        setKey(handler.getKey());
        super.fromProgram(handler);
    }
}
