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

import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.common.HiddenApiFlag;
import com.reandroid.dex.common.Modifier;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.FieldProgram;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.exception.IOExceptionMessage;

import java.io.IOException;
import java.util.Iterator;

public class SmaliField extends SmaliMember implements FieldProgram {

    private TypeKey type;
    private SmaliValue value;

    public SmaliField() {
        super();
    }

    @Override
    public FieldKey getKey() {
        TypeKey defining = getDefining();
        if (defining != null) {
            return getKey(defining);
        }
        return null;
    }
    public void setKey(Key key) {
        FieldKey fieldKey = (FieldKey) key;
        setName(fieldKey.getNameKey());
        setType(fieldKey.getType());
        setDefining(fieldKey.getDeclaring());
    }
    public FieldKey getKey(TypeKey declaring) {
        return FieldKey.create(declaring, getName(), getType());
    }

    public TypeKey getType() {
        return type;
    }
    public void setType(TypeKey type) {
        this.type = type;
    }

    @Override
    public Key getStaticValue() {
        SmaliValue value = getValue();
        if (value != null) {
            return value.getKey();
        }
        return null;
    }
    public void setStaticValue(Key key) {
        setStaticValue(SmaliValueFactory.createForValue(key));
    }

    public SmaliValue getValue() {
        return value;
    }

    public void setStaticValue(SmaliValue value) {
        SmaliValue oldValue = this.value;
        this.value = value;
        if (value != null) {
            value.setParent(this);
        }
        if (oldValue != null && oldValue != value) {
            oldValue.setParent(null);
        }
    }
    void fixUninitializedFinalValue() {
        if (this.getValue() != null || !isStatic() || !isFinal()) {
            return;
        }
        SmaliClass smaliClass = getSmaliClass();
        FieldKey fieldKey = getKey();
        if (smaliClass == null || fieldKey == null) {
            return;
        }
        if (!isInitializedInStaticConstructor(smaliClass, fieldKey)) {
            setStaticValue(SmaliValueFactory.createForField(fieldKey.getType()));
        }
    }
    private boolean isInitializedInStaticConstructor(SmaliClass smaliClass, FieldKey fieldKey) {
        SmaliMethod method = smaliClass.getStaticConstructor();
        if (method == null) {
            return false;
        }
        Iterator<SmaliInstruction> iterator = method.getInstructions();
        while (iterator.hasNext()) {
            SmaliInstruction instruction = iterator.next();
            if (fieldKey.equals(instruction.getKey())) {
                return instruction.getOpcode().isFieldStaticPut();
            }
        }
        return false;
    }

    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.FIELD;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        getSmaliDirective().append(writer);
        Modifier.append(writer, getModifiers());
        writer.append(getName());
        writer.append(':');
        getType().append(writer);
        SmaliValue value = getValue();
        if (value != null) {
            writer.append(" = ");
            value.append(writer);
        }
        SmaliAnnotationSet annotationSet = getAnnotationSet();
        if (annotationSet != null && !annotationSet.isEmpty()) {
            writer.indentPlus();
            writer.newLine();
            annotationSet.append(writer);
            writer.indentMinus();
            getSmaliDirective().appendEnd(writer);
        }
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        SmaliParseException.expect(reader, getSmaliDirective());
        setAccessFlags(AccessFlag.parse(reader));
        setHiddenApiFlagsValue(HiddenApiFlag.parseValues(reader));
        setName(StringKey.readSimpleName(reader, ':'));
        reader.skip(1);
        setType(TypeKey.read(reader));
        if (reader.checkInterned(getKey())) {
            throw new IOExceptionMessage("Field "
                    + getKey() + " has already been interned.\nat " + reader.getCurrentOrigin(false));
        }
        parseValue(reader);
        parseAnnotationSet(reader);
    }
    private void parseValue(SmaliReader reader) throws IOException {
        reader.skipWhitespaces();
        if (reader.finished() || reader.get() != '=') {
            return;
        }
        reader.skip(1); // =
        reader.skipWhitespaces();
        SmaliValue value = SmaliValueFactory.create(reader);
        setStaticValue(value);
        value.parse(reader);
    }
    private void parseAnnotationSet(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        if (directive != SmaliDirective.ANNOTATION) {
            getSmaliDirective().skipEnd(reader);
            return;
        }
        int position = reader.position();
        SmaliAnnotationSet annotationSet = new SmaliAnnotationSet();
        annotationSet.parse(reader);
        reader.skipWhitespacesOrComment();
        if (getSmaliDirective().isEnd(reader)) {
            setSmaliAnnotationSet(annotationSet);
            SmaliDirective.parse(reader);
        }else {
            // put back, it is method annotation
            reader.position(position);
        }
    }
    @Override
    public String toDebugString() {
        StringBuilder builder = new StringBuilder();
        TypeKey typeKey = getDefining();
        if (typeKey != null) {
            builder.append(typeKey);
            builder.append(", ");
        }
        builder.append("field = ");
        builder.append(getName());
        builder.append(':');
        builder.append(getType());
        SmaliValue value = getValue();
        if (value != null) {
            builder.append(" = ");
            builder.append(value.toDebugString());
        }
        return builder.toString();
    }
}
