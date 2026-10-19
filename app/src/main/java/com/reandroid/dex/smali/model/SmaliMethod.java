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
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.key.AnnotationGroupKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.ProtoKey;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.key.TypeListKey;
import com.reandroid.dex.program.MethodProgram;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliRegion;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.SmaliWriterSetting;
import com.reandroid.dex.smali.fix.SmaliGotoFix;
import com.reandroid.dex.smali.fix.SmaliOverlappingTryFix;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.exception.IOExceptionMessage;

import java.io.IOException;
import java.util.Iterator;

public class SmaliMethod extends SmaliMember implements MethodProgram, RegistersTable {

    private ProtoKey protoKey;

    private final SmaliParamSet paramSet;
    private final SmaliRegistersCount smaliRegistersCount;
    private final SmaliCodeSet codeSet;

    public SmaliMethod() {
        super();
        this.paramSet = new SmaliParamSet();
        this.smaliRegistersCount = new SmaliRegistersCount();
        this.codeSet = new SmaliCodeSet();

        this.paramSet.setParent(this);
        this.smaliRegistersCount.setParent(this);
        this.codeSet.setParent(this);
    }

    @Override
    public MethodKey getKey() {
        TypeKey typeKey = getDefining();
        if (typeKey != null) {
            return getKey(typeKey);
        }
        return null;
    }
    public void setKey(Key key) {
        MethodKey methodKey = (MethodKey) key;
        setName(methodKey.getNameKey());
        setProtoKey(methodKey.getType());
        setDefining(methodKey.getDeclaring());
    }
    public MethodKey getKey(TypeKey declaring) {
        return MethodKey.create(declaring, getName(), getProtoKey());
    }
    public boolean hasParameterAnnotations() {
        return getParamSet().hasParameterAnnotations();
    }
    public AnnotationGroupKey getParameterAnnotations() {
        return getParamSet().getParameterAnnotations(this);
    }

    public boolean hasInstructions() {
        return getInstructions().hasNext();
    }
    public Iterator<SmaliInstruction> getInstructions() {
        return getCodeSet().getInstructions();
    }
    public boolean hasDebugElements() {
        return getDebugElements().hasNext();
    }
    public Iterator<SmaliDebugElement> getDebugElements() {
        return getCodeSet().getDebugElements();
    }
    public SmaliRegistersCount getSmaliRegistersCount() {
        return smaliRegistersCount;
    }
    @Override
    public ProtoKey getProtoKey() {
        return protoKey;
    }
    public void setProtoKey(ProtoKey protoKey) {
        this.protoKey = protoKey;
    }

    public SmaliParamSet getParamSet() {
        return paramSet;
    }
    @Override
    public int getParametersCount() {
        ProtoKey protoKey = getProtoKey();
        if (protoKey != null) {
            return protoKey.getParametersCount();
        }
        return getParamSet().size();
    }
    @Override
    public SmaliMethodParameter getParameter(int i) {
        return getParamSet().get(i);
    }
    @Override
    public Iterator<SmaliMethodParameter> getParameters() {
        return getParamSet().iterator();
    }

    public SmaliCodeSet getCodeSet() {
        return codeSet;
    }
    public Iterator<SmaliTryItem> getTryItems() {
        return getCodeSet().getTryItems();
    }

    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.METHOD;
    }

    public boolean matches(MethodKey methodKey) {
        if (methodKey == null) {
            return false;
        }
        return matches(methodKey.getName(), methodKey.getParameters(),
                methodKey.getReturnType());
    }
    public boolean matches(String name, ProtoKey protoKey) {
        TypeListKey parameters;
        TypeKey returnType;
        if (protoKey == null) {
            parameters = null;
            returnType = null;
        } else {
            parameters = protoKey.getParameters();
            returnType = protoKey.getReturnType();
        }
        return matches(name, parameters, returnType);
    }
    public boolean matches(String name, TypeListKey parameters, TypeKey returnType) {
        if (ObjectsUtil.equals(getName(), name)) {
            ProtoKey protoKey = getProtoKey();
            if (parameters != null) {
                if (protoKey == null && !parameters.isEmpty()) {
                    return false;
                }
                if (protoKey != null) {
                    if (!protoKey.equalsParameters(parameters)) {
                        return false;
                    }
                }
            }
            if (protoKey == null || returnType == null) {
                return returnType == null;
            }
            return returnType.equals(protoKey.getReturnType());
        }
        return false;
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        getSmaliDirective().append(writer);
        Modifier.append(writer, getModifiers());
        writer.append(getName());
        getProtoKey().append(writer);
        writer.indentPlus();
        if (hasInstructions()) {
            writer.newLine();
            getSmaliRegistersCount().append(writer);
        }
        getParamSet().append(writer);
        if (hasAnnotation()) {
            writer.newLine();
            getAnnotationSet().append(writer);
        }
        getCodeSet().append(writer);
        writer.indentMinus();
        getSmaliDirective().appendEnd(writer);
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, getSmaliDirective());
        setAccessFlags(AccessFlag.parse(reader));
        setHiddenApiFlagsValue(HiddenApiFlag.parseValues(reader));
        setName(StringKey.readSimpleName(reader, '('));
        parseProto(reader);
        if (reader.checkInterned(getKey())) {
            throw new IOExceptionMessage("Method "
                    + getKey() + " has already been interned.\nat " + reader.getCurrentOrigin(false));
        }
        reader.skipWhitespacesOrComment();
        while (parseNoneCode(reader)) {
            reader.skipWhitespacesOrComment();
        }
        getCodeSet().parse(reader);
        SmaliParseException.expect(reader, getSmaliDirective(), true);
        validate();
        runFixes(reader);
    }
    private void runFixes(SmaliReader reader) {
        if (reader.isFixGoto()) {
            SmaliGotoFix.INSTANCE.apply(this);
        }
        // TODO: implement reader.isFixOverlappingTryItem
        SmaliOverlappingTryFix.INSTANCE.apply(this);
    }
    private boolean parseNoneCode(SmaliReader reader) throws IOException {
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        if (directive == SmaliDirective.LOCALS ||
                directive == SmaliDirective.REGISTERS) {
            getSmaliRegistersCount().parse(reader);
            return true;
        }
        if (directive == SmaliDirective.ANNOTATION) {
            getOrCreateSmaliAnnotationSet().parse(reader);
            return true;
        }
        if (directive == SmaliDirective.PARAM) {
            getParamSet().parse(reader);
            return true;
        }
        return false;
    }
    private void parseProto(SmaliReader reader) throws IOException {
        reader.skipWhitespaces();
        setProtoKey(ProtoKey.read(reader));
    }

    @Override
    public int getRegistersCount() {
        return getSmaliRegistersCount().getRegisters();
    }

    @Override
    public int getParameterRegistersCount() {
        int count = isStatic() ? 0 : 1;
        ProtoKey protoKey = getProtoKey();
        if (protoKey != null) {
            count += protoKey.getParameterRegistersCount();
        }
        return count;
    }
    @Override
    public void setRegistersCount(int count) {
    }
    @Override
    public void setParameterRegistersCount(int count) {
    }
    @Override
    public boolean ensureLocalRegistersCount(int count) {
        // FIXME
        return true;
    }
    @Override
    public int getLocalRegistersCount() {
        return getSmaliRegistersCount().getLocals();
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        getParamSet().validate();
        getSmaliRegistersCount().validate();
        getCodeSet().validate();
    }

    @Override
    public String toDebugString() {
        StringBuilder builder = new StringBuilder();
        TypeKey typeKey = getDefining();
        if (typeKey != null) {
            builder.append(typeKey);
            builder.append(", ");
        }
        builder.append("method = ");
        builder.append(getName());
        builder.append(getProtoKey());
        return builder.toString();
    }

    public static SmaliMethod create(MethodProgram methodProgram) {
        SmaliMethod smaliMethod = new SmaliMethod();
        smaliMethod.setKey(methodProgram.getKey());
        smaliMethod.setAccessFlagsValue(methodProgram.getAccessFlagsValue());
        SmaliRegistersCount count = smaliMethod.getSmaliRegistersCount();
        count.setLocalsMode(true);
        count.setValue(methodProgram.getLocalRegistersCount());
        return smaliMethod;
    }

    public static class SmaliRegistersCount extends Smali implements SmaliRegion {

        private SmaliDirective directive;
        private int value;

        public SmaliRegistersCount() {
            this.directive = SmaliDirective.LOCALS;
        }

        public int getLocals() {
            int value = getValue();
            if (!isLocalsMode()) {
                SmaliMethod method = getParent(SmaliMethod.class);
                value -= method.getParameterRegistersCount();
            }
            return value;
        }
        public int getRegisters() {
            int value = getValue();
            if (isLocalsMode()) {
                SmaliMethod method = getParent(SmaliMethod.class);
                value += method.getParameterRegistersCount();
            }
            return value;
        }
        public int getValue() {
            return value;
        }
        public void setValue(int value) {
            this.value = value;
        }

        public boolean isLocalsMode() {
            return getSmaliDirective() == SmaliDirective.LOCALS;
        }
        public void setLocalsMode(boolean localsMode) {
            if (localsMode == isLocalsMode()) {
                return;
            }
            SmaliDirective directive;
            int value;
            if (localsMode) {
                value = getLocals();
                directive = SmaliDirective.LOCALS;
            } else {
                value = getRegisters();
                directive = SmaliDirective.REGISTERS;
            }
            setDirective(directive);
            setValue(value);
        }

        private void setDirective(SmaliDirective directive) {
            this.directive = directive;
        }

        @Override
        public void parse(SmaliReader reader) throws IOException {
            SmaliDirective directive = SmaliDirective.parse(reader);
            if (directive != SmaliDirective.LOCALS && directive != SmaliDirective.REGISTERS) {
                throw new SmaliParseException("expecting '" + SmaliDirective.LOCALS + "', or '"
                        + SmaliDirective.REGISTERS + "'", reader);
            }
            setDirective(directive);
            reader.skipSpaces();
            setValue(reader.readInteger());
        }
        @Override
        public SmaliDirective getSmaliDirective() {
            return directive;
        }

        @Override
        public void append(SmaliWriter writer) throws IOException {
            SmaliWriterSetting setting = writer.getWriterSetting();
            if (setting != null) {
                setLocalsMode(setting.isLocalRegistersCount());
            }
            getSmaliDirective().append(writer);
            writer.appendInteger(getValue());
        }
    }
}
