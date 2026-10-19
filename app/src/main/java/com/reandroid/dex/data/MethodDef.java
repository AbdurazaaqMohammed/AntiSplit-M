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
package com.reandroid.dex.data;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.common.ArraySupplier;
import com.reandroid.dex.base.UsageMarker;
import com.reandroid.dex.common.Modifier;
import com.reandroid.dex.debug.DebugSequence;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.id.MethodId;
import com.reandroid.dex.id.ProtoId;
import com.reandroid.dex.ins.Ins;
import com.reandroid.dex.ins.InsTryBlock;
import com.reandroid.dex.key.AnnotationGroupKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.ProtoKey;
import com.reandroid.dex.program.MethodProgram;
import com.reandroid.dex.reference.DataItemUle128Reference;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.Smali;
import com.reandroid.dex.smali.model.SmaliMethod;
import com.reandroid.dex.smali.model.SmaliMethodParameter;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArraySupplierIterator;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.Iterator;

public class MethodDef extends Def<MethodId> implements MethodProgram {

    private final DataItemUle128Reference<CodeItem> codeOffset;

    public MethodDef() {
        super(1, SectionType.METHOD_ID);
        this.codeOffset = new DataItemUle128Reference<>(SectionType.CODE, UsageMarker.USAGE_DEFINITION);
        addChild(2, codeOffset);
    }

    @Override
    public MethodKey getKey() {
        MethodId id = getId();
        if (id != null) {
            return id.getKey();
        }
        return null;
    }

    @Override
    public ElementType getElementType() {
        return ElementType.METHOD;
    }

    @Override
    public String getName() {
        MethodId methodId = getId();
        if (methodId != null) {
            return methodId.getName();
        }
        return null;
    }
    public void setName(String name) {
        if (ObjectsUtil.equals(getName(), name)) {
            return;
        }
        getId().setName(name);
    }
    @Override
    public ProtoKey getProtoKey() {
        MethodId methodId = getId();
        if (methodId != null) {
            return methodId.getProto();
        }
        return null;
    }
    @Override
    public int getParametersCount() {
        MethodId methodId = getId();
        if (methodId != null) {
            return methodId.getParametersCount();
        }
        return 0;
    }
    @Override
    public MethodParameterDef getParameter(int index) {
        if (index < 0 || index >= getParametersCount()) {
            return null;
        }
        return new MethodParameterDef(this, index);
    }
    @Override
    public Iterator<MethodParameterDef> getParameters() {
        return ArraySupplierIterator.of(new ArraySupplier<MethodParameterDef>() {
            @Override
            public MethodParameterDef get(int i) {
                return MethodDef.this.getParameter(i);
            }
            @Override
            public int getCount() {
                return MethodDef.this.getParametersCount();
            }
        });
    }
    @Override
    public int getRegistersCount() {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            return codeItem.getRegistersCount();
        }
        return 0;
    }
    @Override
    public int getParameterRegistersCount() {
        MethodId methodId = getId();
        if (methodId != null) {
            int count = methodId.getParameterRegistersCount();
            if (!isStatic()) {
                count = count + 1;
            }
            return count;
        }
        return 0;
    }
    public boolean hasParameter(int index) {
        return index >= 0 && index < getParametersCount();
    }
    public AnnotationGroupKey getParametersAnnotation() {
        AnnotationsDirectory directory = getAnnotationsDirectory();
        if (directory != null) {
            return directory.getParameters(this);
        }
        return AnnotationGroupKey.empty();
    }
    public void setParametersAnnotation(AnnotationGroupKey key) {
        if (key == null || key.isBlank()) {
            AnnotationsDirectory directory = getAnnotationsDirectory();
            if (directory != null) {
                directory = getOrCreateUniqueAnnotationsDirectory();
                directory.setParameters(this, key);
            }
        } else {
            getOrCreateUniqueAnnotationsDirectory()
                    .setParameters(this, key);
        }
    }

    @Override
    void onRemove() {
        CodeItem codeItem = codeOffset.getItem();
        if (codeItem != null) {
            codeItem.setMethodDef(null);
            this.codeOffset.setItem(null);
        }
        super.onRemove();
    }

    public void removeParameter(int index) {
        MethodParameterDef parameter = getParameter(index);
        if (parameter == null) {
            return;
        }
        parameter.onRemoved();
        MethodKey methodKey = getKey();
        if (methodKey == null) {
            return;
        }
        methodKey = methodKey.removeParameter(index);
        setItem(methodKey);
    }
    public void setDebugInfo(DebugInfo debugInfo) {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            codeItem.setDebugInfo(debugInfo);
        }
    }
    public DebugInfo getDebugInfo() {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            return codeItem.getDebugInfo();
        }
        return null;
    }
    public DebugInfo getOrCreateDebugInfo() {
        return getOrCreateCodeItem().getOrCreateDebugInfo();
    }
    public DebugSequence getDebugSequence() {
        DebugInfo debugInfo = getDebugInfo();
        if (debugInfo != null) {
            return debugInfo.getDebugSequence();
        }
        return null;
    }
    public DebugSequence getOrCreateDebugSequence() {
        return getOrCreateDebugInfo().getDebugSequence();
    }
    public boolean hasDebugInfo() {
        return getDebugInfo() != null;
    }
    public boolean hasDebugSequence() {
        DebugInfo debugInfo = getDebugInfo();
        if (debugInfo != null) {
            return debugInfo.hasSequence();
        }
        return false;
    }
    public ProtoId getProtoId() {
        MethodId methodId = getId();
        if (methodId != null) {
            return methodId.getProtoId();
        }
        return null;
    }
    public Iterator<Ins> getInstructions() {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.iterator();
        }
        return EmptyIterator.of();
    }
    public Ins getInstruction(int i) {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.get(i);
        }
        return null;
    }
    public Ins getInstructionAt(int address) {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.getAtAddress(address);
        }
        return null;
    }
    public int getInstructionsCount() {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.getCount();
        }
        return 0;
    }

    public InstructionList getOrCreateInstructionList() {
        return getOrCreateCodeItem().getInstructionList();
    }
    public InstructionList getInstructionList() {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            return codeItem.getInstructionList();
        }
        return null;
    }
    public InsTryBlock getTryBlock() {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            return codeItem.getTryBlock();
        }
        return null;
    }
    public InsTryBlock getOrCreateTryBlock() {
        return getOrCreateCodeItem().getOrCreateTryBlock();
    }
    public CodeItem getOrCreateCodeItem() {
        CodeItem current = codeOffset.getItem();
        CodeItem codeItem = codeOffset.getOrCreateUniqueItem(this);
        if (current == null) {
            codeItem.setMethodDef(this);
            int registers = getParameterRegistersCount();
            codeItem.setRegistersCount(registers);
            codeItem.setParameterRegistersCount(registers);
        }
        return codeItem;
    }
    public CodeItem getCodeItem() {
        CodeItem codeItem = codeOffset.getItem();
        if (codeItem != null) {
            codeItem.setMethodDef(this);
        }
        return codeItem;
    }
    public void clearCode() {
        codeOffset.setItem(null);
    }
    public void clearDebug() {
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            codeItem.removeDebugInfo();
        }
    }
    private void linkCodeItem() {
        CodeItem codeItem = codeOffset.getItem();
        if (codeItem != null) {
            codeItem.addUniqueUser(this);
            codeItem.setMethodDef(this);
        }
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        super.onReadBytes(reader);
        linkCodeItem();
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        MethodKey methodKey = getKey();
        if (methodKey == null) {
            throw new IOException("Null MethodKey");
        }
        writer.onWriteMethod(methodKey);

        writer.newLine();
        getSmaliDirective().append(writer);
        writer.appendModifiers(getModifiers());
        methodKey.appendDefinition(writer);

        writer.indentPlus();
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            codeItem.appendRegistersCount(writer);
        }
        writer.appendAll(getParameters(), false);
        getAnnotation().append(writer);
        if (codeItem != null) {
            codeItem.getInstructionList()
                    .append(writer);
        }
        writer.indentMinus();
        getSmaliDirective().appendEnd(writer);
        writer.setCurrentRegistersTable(null);
    }
    @Override
    public void replaceKeys(Key search, Key replace) {
        super.replaceKeys(search, replace);
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            codeItem.replaceKeys(search, replace);
        }
    }

    @Override
    public void edit() {
        CodeItem shared = codeOffset.getItem();
        CodeItem unique = codeOffset.getUniqueItem(this);
        if (unique != null) {
            unique.setMethodDef(this);
            if (shared != unique) {
                unique.edit();
                shared.getInstructionList()
                        .onEditing(unique.getInstructionList());
            }
            unique.flattenTryItems();
        }
    }
    @Override
    public void editInternal(Block user) {
        this.edit();
    }

    @Override
    public boolean uses(Key key) {
        Key k = getKey();
        if (key.equals(k)) {
            return false;
        }
        if (k.uses(key)) {
            return true;
        }
        CodeItem codeItem = getCodeItem();
        if (codeItem != null && codeItem.uses(key)) {
            return true;
        }
        return false;
    }

    @Override
    public Iterator<IdItem> usedIds() {
        Iterator<IdItem> iterator;
        CodeItem codeItem = getCodeItem();
        if (codeItem == null) {
            iterator = EmptyIterator.of();
        }else {
            iterator = codeItem.usedIds();
        }
        return CombiningIterator.singleOne(getId(), iterator);
    }
    @Override
    public void merge(Def<?> def) {
        super.merge(def);
        MethodDef comingMethod = (MethodDef) def;
        CodeItem comingCode = comingMethod.getCodeItem();
        if (comingCode != null) {
            this.codeOffset.setKey(comingCode.getKey());
        }
    }
    @Override
    public void fromSmali(Smali smali) {
        SmaliMethod smaliMethod = (SmaliMethod) smali;
        setKey(smaliMethod.getKey());
        setAccessFlagsValue(smaliMethod.getAccessFlagsValue());
        setHiddenApiFlagsValue(smaliMethod.getHiddenApiFlagsValue());
        if (smaliMethod.hasInstructions()) {
            getOrCreateCodeItem().fromSmali(smaliMethod);
        }
        if (smaliMethod.hasAnnotation()) {
            setAnnotation(smaliMethod.getAnnotationSetKey());
        }
        Iterator<SmaliMethodParameter> iterator = smaliMethod.getParameters();
        while (iterator.hasNext()) {
            SmaliMethodParameter smaliMethodParameter = iterator.next();
            int index = smaliMethodParameter.getDefinitionIndex();
            if (index < 0) {
                MethodKey methodKey = smaliMethod.getKey();
                throw new RuntimeException("Parameter out of range, class = " +
                        methodKey.getDeclaring() + ", method = " + methodKey.getName() +
                        methodKey.getType() + "\n" + smaliMethodParameter);
            }
            MethodParameterDef parameter = getParameter(index);
            parameter.fromSmali(smaliMethodParameter, false);
        }
        setParametersAnnotation(smaliMethod.getParameterAnnotations());
        linkCodeItem();
    }

    @Override
    public SmaliMethod toSmali() {
        SmaliMethod smaliMethod = new SmaliMethod();
        smaliMethod.setKey(getKey());
        smaliMethod.setAccessFlagsValue(getAccessFlagsValue());
        smaliMethod.setHiddenApiFlagsValue(getHiddenApiFlagsValue());
        smaliMethod.setAnnotation(getAnnotation());
        CodeItem codeItem = getCodeItem();
        if (codeItem != null) {
            codeItem.toSmali(smaliMethod);
        }
        return smaliMethod;
    }

    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.METHOD;
    }

    @Override
    public String toString() {
        if (isReading()) {
            return getSmaliDirective() + " " +
                    Modifier.toString(getModifiers()) +
                    getKey();
        }
        MethodId methodId = getId();
        if (methodId != null) {
            return getSmaliDirective() + " " +
                    Modifier.toString(getModifiers()) +
                    methodId.toString();
        }
        return getSmaliDirective() + " " +
                Modifier.toString(getAccessFlags()) +
                " " + getRelativeIdValue();
    }
}
