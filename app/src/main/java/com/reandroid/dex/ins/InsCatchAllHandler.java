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
package com.reandroid.dex.ins;

import com.reandroid.dex.base.Ule128Item;
import com.reandroid.dex.id.TypeId;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.ExceptionHandler;
import com.reandroid.dex.smali.SmaliDirective;

public class InsCatchAllHandler extends InsExceptionHandler {

    public InsCatchAllHandler() {
        super(0);
    }
    InsCatchAllHandler(boolean forCompact) {
        super();
    }

    InsCatchAllHandler newCompact(InsTryItem parent){
        InsCatchAllHandler handler = new Compact(this);
        handler.setIndex(getIndex());
        handler.setParent(parent);
        return handler;
    }

    @Override
    public boolean traps(TypeKey typeKey) {
        return true;
    }
    @Override
    TypeId getTypeId(){
        return null;
    }
    @Override
    public boolean isCatchAll() {
        return true;
    }
    @Override
    public TypeKey getKey() {
        return null;
    }

    @Override
    public SmaliDirective getSmaliDirective(){
        return SmaliDirective.CATCH_ALL;
    }

    static class Compact extends InsCatchAllHandler {

        private final InsCatchAllHandler catchAllHandler;

        Compact(InsCatchAllHandler catchAllHandler){
            super(true);
            this.catchAllHandler = catchAllHandler;
        }

        @Override
        public boolean isRemoved() {
            return super.isRemoved() || catchAllHandler.isRemoved();
        }

        @Override
        Ule128Item getCatchAddressUle128(){
            return catchAllHandler.getCatchAddressUle128();
        }
    }
}
