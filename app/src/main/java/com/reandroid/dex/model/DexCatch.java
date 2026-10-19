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

import com.reandroid.dex.ins.InsCatchAllHandler;
import com.reandroid.dex.ins.InsExceptionHandler;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class DexCatch extends DexCode {

    private final DexTry dexTry;
    private final InsExceptionHandler exceptionHandler;

    public DexCatch(DexTry dexTry, InsExceptionHandler exceptionHandler) {
        super();
        this.dexTry = dexTry;
        this.exceptionHandler = exceptionHandler;
    }

    public TypeKey getKey(){
        return getExceptionHandler().getKey();
    }
    public void setKey(TypeKey typeKey){
        getExceptionHandler().setKey(typeKey);
    }
    public boolean traps(TypeKey typeKey) {
        return getExceptionHandler().traps(typeKey);
    }
    public boolean isCatchAll() {
        return getExceptionHandler() instanceof InsCatchAllHandler;
    }
    public int getCatchAddress(){
        return getExceptionHandler().getCatchAddress();
    }
    public void setCatchAddress(int address){
        getExceptionHandler().setCatchAddress(address);
    }
    public DexInstruction getCaughtInstruction(){
        return getDexMethod().getInstructionAt(getCatchAddress());
    }
    @Override
    public void removeSelf(){
        getExceptionHandler().removeSelf();
    }
    @Override
    public boolean isRemoved() {
        return getDexMethod().isRemoved() ||
                getExceptionHandler().isRemoved();
    }

    @Override
    public DexMethod getDexMethod() {
        return getDexTry().getDexMethod();
    }
    public DexTry getDexTry() {
        return dexTry;
    }
    public InsExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public boolean uses(Key key) {
        TypeKey typeKey = getKey();
        if(typeKey != null){
            return typeKey.uses(key);
        }
        return false;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        getExceptionHandler().getHandlerLabel().appendLabelName(writer);
    }
}
