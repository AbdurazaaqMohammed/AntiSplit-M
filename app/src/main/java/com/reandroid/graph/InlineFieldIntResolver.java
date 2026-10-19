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
package com.reandroid.graph;

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.PrimitiveKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.*;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class InlineFieldIntResolver extends BaseDexClassProcessor {

    private final Predicate<Integer> resourceIdChecker;
    private int mResolvedCount;

    public InlineFieldIntResolver(DexClassRepository classRepository, Predicate<Integer> resourceIdChecker) {
        super(classRepository);
        this.resourceIdChecker = resourceIdChecker;
    }
    public InlineFieldIntResolver(DexClassRepository classRepository, TableBlock tableBlock) {
        this(classRepository, createChecker(tableBlock));
    }
    public InlineFieldIntResolver(DexClassRepository classRepository) {
        this(classRepository, createDefaultChecker());
    }

    @Override
    public void apply() {
        verbose("Resolving inline resource ids ..");
        reset();
        Iterator<DexClass> iterator = getClassRepository().getDexClasses();
        while(iterator.hasNext()) {
            scanClass(iterator.next());
        }
        verbose("Resolved ids count: " + getResolvedCount());
    }
    public int getResolvedCount() {
        return mResolvedCount;
    }
    private void scanClass(DexClass dexClass) {
        Iterator<DexMethod> iterator = dexClass.declaredMethods();
        while (iterator.hasNext()) {
            scanMethod(iterator.next());
        }
    }
    private void scanMethod(DexMethod dexMethod) {
        Iterator<DexInstruction> iterator = dexMethod.getInstructions();
        while(iterator.hasNext()) {
            DexInstruction instruction = iterator.next();
            Key value = getValueFromStaticField(instruction);
            if(value != null) {
                resolve(instruction, value);
            }
        }
    }
    private Key getValueFromStaticField(DexInstruction instruction) {
        if(!instruction.is(Opcode.SGET)){
            return null;
        }
        FieldKey fieldKey = (FieldKey) instruction.getKey();
        if(!TypeKey.TYPE_I.equals(fieldKey.getType())) {
            return null;
        }
        DexField dexField = (DexField) instruction.findDeclaration();
        if(dexField == null) {
            return null;
        }
        return dexField.getStaticValue();
    }
    private void resolve(DexInstruction instruction, Key value) {
        if (!(value instanceof PrimitiveKey)) {
            return;
        }
        if (!(((PrimitiveKey) value).isInteger())) {
            return;
        }
        int id = ((PrimitiveKey.IntegerKey) value).value();
        if(!resourceIdChecker.test(id)) {
            return;
        }
        Key key = instruction.getKey();
        int register = instruction.getRegister();
        DexInstruction replace = instruction.replace(Opcode.CONST);
        replace.setRegister(register);
        replace.setAsInteger(id);
        this.mResolvedCount ++;
        if(isDebugEnabled()) {
            debug(key + " WITH " + replace.toString());
        }
    }

    private void reset() {
        this.mResolvedCount = 0;
    }

    private static Predicate<Integer> createChecker(TableBlock tableBlock) {
        return id -> {
            int i = id;
            return PackageBlock.isResourceId(i) &&
                    tableBlock.getResource(i) != null;
        };
    }
    private static Predicate<Integer> createDefaultChecker() {
        return PackageBlock::isResourceId;
    }
}
