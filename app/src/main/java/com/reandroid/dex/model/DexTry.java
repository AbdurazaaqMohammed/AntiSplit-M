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

import com.reandroid.dex.base.DexException;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.ins.InsExceptionHandler;
import com.reandroid.dex.ins.ExceptionLabel;
import com.reandroid.dex.ins.Ins;
import com.reandroid.dex.ins.InsTryItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;

import java.io.IOException;
import java.util.Iterator;

public class DexTry extends DexCode {

    private final DexMethod dexMethod;
    private final InsTryItem tryItem;
    private final int address;

    public DexTry(DexMethod dexMethod, InsTryItem tryItem, int address) {
        super();
        this.dexMethod = dexMethod;
        this.tryItem = tryItem;
        this.address = address;
    }
    public DexTry(DexMethod dexMethod, InsTryItem tryItem) {
        this(dexMethod, tryItem, -1);
    }

    public DexInstruction getFirst(){
        return CollectionUtil.getFirst(getInstructions());
    }
    public void setFirst(DexInstruction instruction){
        setStartAddress(instruction.getAddress());
    }
    public DexInstruction getLast(){
        return CollectionUtil.getLast(getInstructions());
    }
    public void setLast(DexInstruction instruction){
        setEndAddress(instruction.getAddress() + instruction.getCodeUnits());
    }

    public Iterator<DexInstruction> getInstructions(){
        InstructionList instructionList = getDexMethod()
                .getDefinition()
                .getInstructionList();
        if(instructionList == null){
            return EmptyIterator.of();
        }
        Iterator<Ins> iterator = FilterIterator.of(instructionList.iterator(),
                ins -> {
                    int address = ins.getAddress();
                    return address >= getStartAddress() && address < getEndAddress();
                });
        return DexInstruction.createAll(getDexMethod(), iterator);
    }
    public int getStartAddress(){
        return getTryItem().getStartAddress();
    }
    public void setStartAddress(int address){
        getTryItem().setStartAddress(address);
    }
    public int getEndAddress(){
        InsTryItem tryItem = getTryItem();
        return tryItem.getStartAddress() + tryItem.getCatchCodeUnit();
    }
    public void setEndAddress(int address){
        InsTryItem tryItem = getTryItem();
        int start = tryItem.getStartAddress();
        if(address < start){
            throw new DexException("Invalid try end address "
                    + address + "<" + start);
        }
        getTryItem().setCatchCodeUnit(address - start);
    }
    public DexCatch getCatchAll(){
        return create(getTryItem().getCatchAllHandler());
    }
    public DexCatch getOrCreateCatchAll() {
        InsTryItem tryItem = getTryItem();
        boolean hasCatchAll = tryItem.hasCatchAllHandler();
        DexCatch dexCatch = create(getTryItem().getOrCreateCatchAll());
        if(!hasCatchAll){
            dexCatch.setCatchAddress(getEndAddress());
        }
        return dexCatch;
    }
    public DexCatch getCatch(TypeKey typeKey) {
        return create(getTryItem().getExceptionHandler(typeKey));
    }
    public int getCatchCount() {
        InsTryItem tryItem = getTryItem();
        int count = 0;
        if(tryItem.hasCatchAllHandler()){
            count = 1;
        }
        count += tryItem.getCatchTypedHandlersCount();
        return count;
    }
    public Iterator<DexCatch> getCatches() {
        return getCatches(this.address);
    }
    public Iterator<DexCatch> getCatches(int address) {
        return ComputeIterator.of(getTryItem().getExceptionHandlersForAddress(address),
                this::create);
    }
    public Iterator<DexCatch> getCatchesAt(int address) {
        return ComputeIterator.of(getTryItem().getExceptionHandlersForCatchAddress(address),
                this::create);
    }
    public boolean trapsCatchAll() {
        return traps(null, this.address);
    }
    public boolean trapsCatchAll(int address) {
        return traps(null, address);
    }
    public boolean traps(TypeKey typeKey) {
        return traps(typeKey, this.address);
    }
    public boolean traps(TypeKey typeKey, int address) {
        return getTryItem().traps(typeKey, address);
    }
    DexCatch create(InsExceptionHandler handler) {
        if(handler != null) {
            return new DexCatch(this, handler);
        }
        return null;
    }
    @Override
    public void removeSelf(){
        getTryItem().removeSelf();
    }
    @Override
    public boolean isRemoved() {
        return getDexMethod().isRemoved() ||
                getTryItem().isRemoved();
    }

    public InsTryItem getTryItem() {
        return tryItem;
    }

    @Override
    public boolean uses(Key key) {
        Iterator<DexCatch> iterator = getCatches();
        while (iterator.hasNext()){
            DexCatch dexCatch = iterator.next();
            if(dexCatch.uses(key)){
                return true;
            }
        }
        return false;
    }

    @Override
    public DexMethod getDexMethod() {
        return dexMethod;
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        InsTryItem tryItem = getTryItem();
        Iterator<InsExceptionHandler> handlers = tryItem.handlers();
        if(!handlers.hasNext()){
            writer.appendComment("Empty try-catch");
            writer.newLine();
            return;
        }
        Object previous = null;
        while (handlers.hasNext()){
            ExceptionLabel label = handlers.next().getStartLabel();
            if(label.equalsLabel(previous)){
                continue;
            }
            previous = label;
            writer.newLine();
            label.appendLabelName(writer);
        }
        writer.indentPlus();
        Iterator<DexInstruction> instructions = getInstructions();
        writer.appendAll(instructions, true);
        writer.indentMinus();
        handlers = tryItem.handlers();
        previous = null;
        while (handlers.hasNext()){
            ExceptionLabel label = handlers.next().getEndLabel();
            if(label.equalsLabel(previous)){
                continue;
            }
            previous = label;
            writer.newLine();
            label.appendLabelName(writer);
        }
        handlers = tryItem.handlers();
        while (handlers.hasNext()){
            ExceptionLabel label = handlers.next().getHandlerLabel();
            writer.newLine();
            label.appendLabelName(writer);
        }
    }

    public static Iterator<DexTry> create(DexMethod dexMethod, Iterator<InsTryItem> iterator) {
        return create(dexMethod, -1, iterator);
    }
    public static Iterator<DexTry> create(DexMethod dexMethod, int address, Iterator<InsTryItem> iterator){
        if(dexMethod == null){
            return EmptyIterator.of();
        }
        return ComputeIterator.of(iterator, tryItem -> create(dexMethod, tryItem, address));
    }
    public static DexTry create(DexMethod dexMethod, InsTryItem tryItem) {
        return create(dexMethod, tryItem, -1);
    }
    public static DexTry create(DexMethod dexMethod, InsTryItem tryItem, int address) {
        if(dexMethod == null || tryItem == null){
            return null;
        }
        return new DexTry(dexMethod, tryItem, address);
    }
}
