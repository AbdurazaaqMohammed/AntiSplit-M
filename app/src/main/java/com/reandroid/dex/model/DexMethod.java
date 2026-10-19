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
package com.reandroid.dex.model;

import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.dex.common.Register;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.data.CodeItem;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.data.MethodDef;
import com.reandroid.dex.id.MethodId;
import com.reandroid.dex.ins.Ins;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.ins.SizeXIns;
import com.reandroid.dex.ins.InsTryBlock;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.MethodProgram;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliInstruction;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.EmptyList;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.MergingIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class DexMethod extends DexDeclaration implements MethodProgram, BlockRefresh {

    private final DexClass dexClass;
    private final MethodDef methodDef;
    private int mEditIndex;

    public DexMethod(DexClass dexClass, MethodDef methodDef) {
        this.dexClass = dexClass;
        this.methodDef = methodDef;
    }

    public DexMethod getDeclared() {
        DexClass dexClass = getDexClass().getSuperClass();
        if (dexClass != null) {
            DexMethod dexMethod = dexClass.getMethod(getKey());
            if (dexMethod != null) {
                return dexMethod.getDeclared();
            }
        }
        dexClass = getDexClass();
        Iterator<DexClass> iterator = dexClass.getInterfaceClasses();
        while (iterator.hasNext()) {
            dexClass = iterator.next();
            DexMethod dexMethod = dexClass.getMethod(getKey());
            if (dexMethod != null) {
                return dexMethod.getDeclared();
            }
        }
        return this;
    }

    public DexMethod getSuperMethod() {
        return CollectionUtil.getFirst(getSuperMethods());
    }
    public Iterator<DexMethod> getSuperMethods() {
        MethodKey key = getKey();
        return ComputeIterator.of(getDexClass().getSuperTypes(),
                dexClass -> dexClass.getDeclaredMethod(key));
    }
    public Iterator<DexMethod> getOverriding() {
        return CombiningIterator.two(getExtending(), getImplementations());
    }
    public DexMethod getBridgingMethod() {
        MethodKey bridging = getBridging();
        if (bridging != null) {
            return getDexClass().getDeclaredMethod(bridging);
        }
        return null;
    }
    public MethodKey getBridging() {
        if (isBridge()) {
            DexInstruction instruction = CollectionUtil.getSingle(
                    FilterIterator.of(getInstructions(),
                            ins -> ins.is(Opcode.INVOKE_VIRTUAL) ||
                                    ins.is(Opcode.INVOKE_VIRTUAL_RANGE)));
            if (instruction != null) {
                MethodKey key = getKey();
                MethodKey methodKey = (MethodKey) instruction.getKey();
                if (equalsForBridging(key, methodKey)) {
                    return methodKey;
                }
            }
        }
        return null;
    }
    public MethodKey getBridge() {
        DexMethod bridge = getBridgeMethod();
        if (bridge != null) {
            return bridge.getKey();
        }
        return null;
    }
    public DexMethod getBridgeMethod() {
        if (!isBridge() && isVirtual()) {
            MethodKey methodKey = getKey();
            return CollectionUtil.getSingle(
                    FilterIterator.of(getDexClass().getVirtualMethods(), dexMethod -> {
                                DexMethod bridging = dexMethod.getBridgingMethod();
                                if (bridging != null) {
                                    return methodKey.equals(bridging.getKey());
                                }
                                return false;
                            }
                    )
            );
        }
        return null;
    }
    private boolean equalsForBridging(MethodKey key1, MethodKey key2) {
        if (!key1.getDeclaring().equals(key2.getDeclaring()) || key1.equals(key2)) {
            return false;
        }
        int count = key1.getParametersCount();
        if (count != key2.getParametersCount()) {
            return false;
        }
        TypeKey typeKey = key1.getReturnType();
        if (differentPrimitiveForBridging(typeKey, key2.getReturnType())) {
            return false;
        }
        boolean allPrimitive = typeKey.isPrimitive();
        for (int i = 0; i < count; i++) {
            typeKey = key1.getParameter(i);
            if (differentPrimitiveForBridging(key1.getParameter(i), key2.getParameter(i))) {
                return false;
            }
            if (allPrimitive) {
                allPrimitive = typeKey.isPrimitive();
            }
        }
        return !allPrimitive;
    }
    private boolean differentPrimitiveForBridging(TypeKey key1, TypeKey key2) {
        if (key1.isPrimitive()) {
            return !key1.equals(key2);
        }
        return key2.isPrimitive();
    }
    public Iterator<DexMethod> getExtending() {
        return new MergingIterator<>(ComputeIterator.of(getDexClass().getExtending(),
                dexClass -> dexClass.getExtending(getKey())));
    }
    public Iterator<DexMethod> getImplementations() {
        return new MergingIterator<>(ComputeIterator.of(getDexClass().getImplementations(),
                dexClass -> dexClass.getImplementations(getKey())));
    }
    public Iterator<MethodKey> getOverridingKeys() {
        return new MergingIterator<>(ComputeIterator.of(getDexClass().getOverriding(),
                dexClass -> dexClass.getOverridingKeys(DexMethod.this.getKey())));
    }

    @Override
    public String getName() {
        return getDefinition().getName();
    }
    public void setName(String name) {
        getDefinition().setName(name);
    }

    public Iterator<DexInstruction> getInstructionsIfKey(Predicate<? super Key> predicate) {
        return getInstructionsIfIns(ins -> {
            if (ins instanceof SizeXIns) {
                Key key = ((SizeXIns) ins).getKey();
                return key != null && predicate.test(key);
            }
            return false;
        });
    }
    public Iterator<DexInstruction> getInstructionsIfMethodKey(Predicate<? super MethodKey> predicate) {
        return getInstructionsIfIns(ins -> {
            if (ins instanceof SizeXIns) {
                Key key = ((SizeXIns) ins).getKey();
                if (key instanceof MethodKey) {
                    return predicate.test((MethodKey) key);
                }
                return false;
            }
            return false;
        });
    }
    public Iterator<DexInstruction> getInstructionsIfFieldKey(Predicate<? super FieldKey> predicate) {
        return getInstructionsIfIns(ins -> {
            if (ins instanceof SizeXIns) {
                Key key = ((SizeXIns) ins).getKey();
                if (key instanceof FieldKey) {
                    return predicate.test((FieldKey) key);
                }
                return false;
            }
            return false;
        });
    }
    public Iterator<DexInstruction> getInstructionsIfTypeKey(Predicate<? super TypeKey> predicate) {
        return getInstructionsIfIns(ins -> {
            if (ins instanceof SizeXIns) {
                Key key = ((SizeXIns) ins).getKey();
                if (key instanceof TypeKey) {
                    return predicate.test((TypeKey) key);
                }
                return false;
            }
            return false;
        });
    }
    public Iterator<DexInstruction> getInstructionsIfStringKey(Predicate<? super StringKey> predicate) {
        return getInstructionsIfIns(ins -> {
            if (ins instanceof SizeXIns) {
                Key key = ((SizeXIns) ins).getKey();
                if (key instanceof StringKey) {
                    return predicate.test((StringKey) key);
                }
                return false;
            }
            return false;
        });
    }
    /**
     * Use: getInstructionsWithOpcode
     * */
    @Deprecated
    public Iterator<DexInstruction> getInstructions(Opcode<?> opcode) {
        return getInstructionsWithOpcode(opcode);
    }
    public Iterator<DexInstruction> getInstructionsWithOpcode(Opcode<?> opcode) {
        return getInstructionsIfOpcode(op -> op == opcode);
    }
    public Iterator<DexInstruction> getInstructionsWithOpcode(Opcode<?> opcode, Opcode<?> alt) {
        return getInstructionsIfOpcode(op -> op == opcode || op == alt);
    }
    public Iterator<DexInstruction> getInstructionsIfOpcode(Predicate<Opcode<?>> predicate) {
        return getInstructionsIfIns(ins -> predicate.test(ins.getOpcode()));
    }
    /**
     * Use: getInstructionsIfIns
     * */
    @Deprecated
    public Iterator<DexInstruction> getInstructions(Predicate<? super Ins> filter) {
        return getInstructionsIfIns(filter);
    }
    public Iterator<DexInstruction> getInstructionsIfIns(Predicate<? super Ins> filter) {
        return DexInstruction.createAll(this,
                FilterIterator.of(getDefinition().getInstructions(), filter));
    }
    public Iterator<DexInstruction> getInstructions() {
        return DexInstruction.createAll(this, getDefinition().getInstructions());
    }
    public DexInstruction getInstructionWithLineNumber(int lineNumber) {
        if (hasDebugSequence()) {
            Iterator<DexInstruction> iterator = getInstructions();
            while (iterator.hasNext()) {
                DexInstruction instruction = iterator.next();
                if (instruction.hasLineNumber(lineNumber)) {
                    return instruction;
                }
            }
        }
        return null;
    }
    public Iterator<DexInstruction> getInstructionsWithLineNumber(int lineNumber) {
        if (hasDebugSequence()) {
            return FilterIterator.of(getInstructions(), ins -> ins.hasLineNumber(lineNumber));
        }
        return EmptyIterator.of();
    }

    int getEditIndex() {
        return mEditIndex;
    }
    void setEditIndex(int index) {
        this.mEditIndex = index;
    }

    public void clearCode() {
        getDefinition().clearCode();
    }
    public void clearDebug() {
        getDefinition().clearDebug();
    }
    public Iterator<DexTry> getDexTry() {
        return getDexTry(-1);
    }
    public Iterator<DexTry> getDexTry(int address) {
        InsTryBlock tryBlock = getDefinition().getTryBlock();
        if (tryBlock == null) {
            return EmptyIterator.of();
        }
        return DexTry.create(this, address,
                tryBlock.getTriesForAddress(address));
    }
    public DexTry createDexTry() {
        InsTryBlock tryBlock = getDefinition().getOrCreateTryBlock();
        return DexTry.create(this, tryBlock.createNext());
    }
    public DexInstruction getInstruction(int i) {
        return create(getDefinition().getInstruction(i));
    }
    public DexInstruction getInstructionAt(int address) {
        return create(getDefinition().getInstructionAt(address));
    }
    public DexInstruction addInstruction(Opcode<?> opcode) {
        return create(getDefinition().getOrCreateInstructionList().createNext(opcode));
    }
    public DexInstruction parseInstruction(String smaliString) throws IOException {
        return parseInstruction(SmaliReader.of(smaliString));
    }
    public DexInstruction parseInstruction(SmaliReader reader) throws IOException {
        int index = getInstructionsCount();
        return parseInstruction(index, reader);
    }
    public DexInstruction parseInstruction(int index, SmaliReader reader) throws IOException {
        SmaliInstruction smaliInstruction = new SmaliInstruction(Opcode.parseSmali(reader, false));
        smaliInstruction.parse(reader);
        InstructionList instructionList = getDefinition().getOrCreateInstructionList();
        Ins ins = instructionList.createAt(index, smaliInstruction.getOpcode());
        ins.fromSmali(smaliInstruction);
        return create(ins);
    }
    public DexInstruction createInstruction(int index, Opcode<?> opcode) {
        return create(getDefinition().getOrCreateInstructionList().createAt(index, opcode));
    }
    public int getInstructionsCount() {
        return getDefinition().getInstructionsCount();
    }
    public RegistersTable getRegistersTable() {
        return getDefinition().getCodeItem();
    }
    public RegistersTable getOrCreateRegistersTable() {
        return getDefinition().getOrCreateCodeItem();
    }
    public List<Register> getLocalFreeRegisters(int instructionIndex) {
        InstructionList instructionList = getInstructionList();
        if (instructionList != null) {
            return instructionList.getLocalFreeRegisters(instructionIndex);
        }
        return EmptyList.of();
    }
    public void ensureLocalRegistersCount(int locals) {
        if (locals == 0) {
            return;
        }
        RegistersTable registersTable = getRegistersTable();
        if (registersTable != null) {
            if (locals <= registersTable.getLocalRegistersCount()) {
                return;
            }
        }
        registersTable = getOrCreateRegistersTable();
        registersTable.ensureLocalRegistersCount(locals);
    }
    public int refreshParameterRegistersCount() {
        RegistersTable registersTable = getRegistersTable();
        if (registersTable == null) {
            return 0;
        }
        int parameterCount = getKey().getParameterRegistersCount();
        if (!isStatic()) {
            parameterCount = parameterCount + 1;
        }
        int locals = registersTable.getLocalRegistersCount();
        registersTable.setParameterRegistersCount(parameterCount);
        registersTable.setRegistersCount(locals + parameterCount);
        return parameterCount;
    }
    private InstructionList getInstructionList() {
        return getDefinition().getInstructionList();
    }
    @Override
    public int getRegistersCount() {
        RegistersTable registersTable = getRegistersTable();
        if (registersTable != null) {
            return registersTable.getRegistersCount();
        }
        return 0;
    }
    @Override
    public int getParameterRegistersCount() {
        RegistersTable registersTable = getRegistersTable();
        if (registersTable != null) {
            return registersTable.getParameterRegistersCount();
        }
        return 0;
    }
    @Override
    public int getLocalRegistersCount() {
        RegistersTable registersTable = getRegistersTable();
        if (registersTable != null) {
            return registersTable.getLocalRegistersCount();
        }
        return 0;
    }
    public void setParameterRegistersCount(int count) {
        CodeItem codeItem = getDefinition().getOrCreateCodeItem();
        if (codeItem != null) {
            codeItem.setParameterRegistersCount(count);
        }
    }
    public void setLocalRegistersCount(int count) {
        CodeItem codeItem = getDefinition().getOrCreateCodeItem();
        if (codeItem != null) {
            codeItem.setRegistersCount(codeItem.getParameterRegistersCount() + count);
        }
    }
    private DexInstruction create(Ins ins) {
        return DexInstruction.create(this, ins);
    }

    @Override
    public MethodKey getKey() {
        return getId().getKey();
    }
    @Override
    public MethodId getId() {
        return getDefinition().getId();
    }
    @Override
    public DexClass getDexClass() {
        return dexClass;
    }
    @Override
    public MethodDef getDefinition() {
        return methodDef;
    }
    public boolean hasDebugInfo() {
        return getDefinition().hasDebugInfo();
    }
    public boolean hasDebugSequence() {
        return getDefinition().hasDebugSequence();
    }

    @Override
    public int getHiddenApiFlagsValue() {
        return getDefinition().getHiddenApiFlagsValue();
    }
    @Override
    public void setHiddenApiFlagsValue(int value) {
        getDefinition().setHiddenApiFlagsValue(value);
    }

    @Override
    public int getParametersCount() {
        return getDefinition().getParametersCount();
    }
    @Override
    public DexMethodParameter getParameter(int i) {
        return DexMethodParameter.create(this, getDefinition().getParameter(i));
    }
    @Override
    public Iterator<DexMethodParameter> getParameters() {
        return ComputeIterator.of(getDefinition().getParameters(),
                parameter -> DexMethodParameter.create(DexMethod.this, parameter));
    }

    public void removeParameter(int index) {
        getDefinition().removeParameter(index);
    }
    public boolean hasParameter(int index) {
        return getDefinition().hasParameter(index);
    }

    @Override
    public void removeSelf() {
        getDefinition().removeSelf();
    }

    @Override
    public void refresh() {
        if (!isRemoved()) {
            MethodDef definition = getDefinition();
            definition.refresh();
            InstructionList instructionList = definition.getInstructionList();
            if (instructionList != null) {
                instructionList.refresh();
            }
        }
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        getDefinition().append(writer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DexMethod dexMethod = (DexMethod) obj;
        return MethodId.equals(true, getId(), dexMethod.getId());
    }
}
