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

import com.reandroid.dex.base.UsageMarker;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.id.ProtoId;
import com.reandroid.dex.key.DualKeyReference;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.reference.IdSectionReference;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliInstruction;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.util.Iterator;

public class Ins4rcc extends Size8Ins implements RegistersSet, DualKeyReference {

    private final IdSectionReference<ProtoId> reference2;

    public Ins4rcc(Opcode<?> opcode) {
        super(opcode);
        final Ins4rcc ins4rcc = this;
        this.reference2 = new IdSectionReference<ProtoId>(ins4rcc, UsageMarker.USAGE_INSTRUCTION) {
            @Override
            public int get() {
                return ins4rcc.getData2();
            }
            @Override
            public void set(int value) {
                ins4rcc.setData2(value);
            }
            @Override
            public SectionType<ProtoId> getSectionType() {
                return ins4rcc.getSectionType2();
            }
            @Override
            protected String buildTrace(ProtoId currentItem) {
                return SizeXIns.buildTrace(ins4rcc, currentItem, get());
            }
        };
    }

    @Override
    public Key getKey2() {
        return reference2.getKey();
    }
    @Override
    public void setKey2(Key key) {
        reference2.setKey(key);
    }
    public ProtoId getSectionId2() {
        return reference2.getItem();
    }
    public void setSectionId2(IdItem idItem) {
        reference2.setItem((ProtoId) idItem);
    }

    @Override
    public int getData(){
        return getShortUnsigned(2);
    }
    @Override
    public void setData(int data){
        setShort(2, data);
    }
    public int getData2(){
        return getShortUnsigned(6);
    }
    public void setData2(int data) {
        setShort(6, data);
    }

    @Override
    public int getRegistersCount() {
        return getByteUnsigned(1);
    }
    @Override
    public void setRegistersCount(int count) {
        setByte(1, count);
    }

    @Override
    public int getRegister(int index) {
        return getShortUnsigned(4) + index;
    }

    @Override
    public void setRegister(int index, int value) {
        if (index != 0) {
            setByte(1, value + 1 - getRegister(0));
        } else {
            setShort(4, value);
        }
    }

    @Override
    public SectionType<ProtoId> getSectionType2() {
        return ObjectsUtil.cast(super.getSectionType2());
    }

    @Override
    void pullSectionItem() {
        super.pullSectionItem();
        this.reference2.pullItem();
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        this.reference2.refresh();
    }

    @Override
    public void replaceKeys(Key search, Key replace) {
        super.replaceKeys(search, replace);
        Key key = getKey2();
        if (key == null) {
            return;
        }
        Key update = key.replaceKey(search, replace);
        if(key != update){
            setKey2(update);
        }
    }
    @Override
    public Iterator<IdItem> usedIds() {
        return CombiningIterator.two(super.usedIds(), SingleIterator.of(getSectionId2()));
    }

    @Override
    public void fromSmali(SmaliInstruction smaliInstruction) {
        super.fromSmali(smaliInstruction);
        setKey2(smaliInstruction.getKey2());
    }

    @Override
    void appendOperand(SmaliWriter writer) throws IOException {
        super.appendOperand(writer);
        writer.append(", ");
        getSectionId2().append(writer);
    }
}