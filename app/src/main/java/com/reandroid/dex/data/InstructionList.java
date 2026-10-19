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

import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.container.FixedBlockContainer;
import com.reandroid.common.ArraySupplier;
import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.common.Register;
import com.reandroid.dex.common.RegisterType;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.debug.DebugSequence;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.ins.ConstNumber;
import com.reandroid.dex.ins.ConstNumberLong;
import com.reandroid.dex.ins.ConstString;
import com.reandroid.dex.ins.InsExceptionHandler;
import com.reandroid.dex.ins.Ins;
import com.reandroid.dex.ins.InsBlockList;
import com.reandroid.dex.ins.InsNop;
import com.reandroid.dex.ins.NullInstruction;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.ins.RegisterReference;
import com.reandroid.dex.ins.RegistersEditor;
import com.reandroid.dex.ins.RegistersIterator;
import com.reandroid.dex.ins.SizeXIns;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliCodeSet;
import com.reandroid.dex.smali.model.SmaliInstruction;
import com.reandroid.dex.smali.model.SmaliMethod;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ArraySupplierIterator;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class InstructionList extends FixedBlockContainer implements
        Iterable<Ins>, SmaliFormat {

    private final CodeItem codeItem;
    private final InsBlockList insBlockList;
    private final DexPositionAlign blockAlign;

    public InstructionList(CodeItem codeItem) {
        super(2);
        this.codeItem = codeItem;

        DexPositionAlign blockAlign = new DexPositionAlign();
        this.insBlockList = new InsBlockList(blockAlign,
                codeItem.getInstructionCodeUnitsReference(),
                codeItem.getInstructionOutsReference(),
                codeItem.getExternalLabels()
                );

        this.blockAlign = blockAlign;

        addChild(0, insBlockList);
        addChild(1, blockAlign);
    }

    public RegistersEditor editRegisters() {
        return RegistersEditor.fromIns(getCodeItem(), iterator());
    }

    public MethodDef getMethodDef() {
        return getCodeItem().getMethodDef();
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
    public DebugInfo getDebugInfo() {
        return getCodeItem().getDebugInfo();
    }
    public DebugInfo getOrCreateDebugInfo() {
        return getCodeItem().getOrCreateDebugInfo();
    }
    public CodeItem getCodeItem() {
        return codeItem;
    }
    public RegistersTable getRegistersTable() {
        return getCodeItem();
    }
    public void addLocalRegisters(int amount) {
        addLocalRegisters(false, amount);
    }
    public void addLocalRegisters(boolean start, int amount) {
        RegistersTable registersTable = getRegistersTable();
        Iterator<RegistersIterator> iterator = getRegistersIterators();
        while (iterator.hasNext()) {
            RegistersIterator registersIterator = iterator.next();
            int count;
            if (registersIterator.isRange()) {
                count = 1;
            } else {
                count = registersIterator.size();
            }
            for(int i = 0; i < count; i++) {
                RegisterReference reference = registersIterator.get(i);
                if (start || reference.isParameter()) {
                    reference.setRegisterValue(reference.getValue() + amount);
                }
            }
        }
        registersTable.setRegistersCount(registersTable.getRegistersCount() + amount);
    }
    public boolean canAddLocalRegisters(int amount) {
        RegistersTable registersTable = getRegistersTable();
        if (registersTable.getRegistersCount() + amount < 0xf) {
            return true;
        }
        Iterator<RegistersIterator> iterator = getRegistersIterators();
        while (iterator.hasNext()) {
            RegistersIterator registersIterator = iterator.next();
            int count;
            if (registersIterator.isRange()) {
                count = 1;
            } else {
                count = registersIterator.size();
            }
            for(int i = 0; i < count; i++) {
                RegisterReference reference = registersIterator.get(i);
                if (reference.isParameter() && reference.getValue() + amount > reference.getLimit()) {
                    return false;
                }
            }
        }
        return true;
    }
    public List<Register> getLocalFreeRegisters(int startIndex) {
        RegistersTable registersTable = getRegistersTable();
        int count = registersTable.getLocalRegistersCount();
        Iterator<Register> iterator = new ArraySupplierIterator<>(new ArraySupplier<Register>() {
            @Override
            public Register get(int i) {
                return new Register(i, false, registersTable);
            }
            @Override
            public int getCount() {
                return count;
            }
        });
        iterator = FilterIterator.of(iterator, reference -> {
            int registerValue = reference.getValue();
            return registerValue < count && isFreeRegister(registerValue, startIndex);
        });
        List<Register> list = CollectionUtil.toUniqueList(iterator);
        java.util.Collections.sort(list, CompareUtil.getComparableComparator());
        return list;
    }
    public boolean isFreeRegister(int registerValue, int startIndex) {
        Iterator<RegistersIterator> iterator = getRegistersIterators(startIndex);
        while (iterator.hasNext()) {
            RegistersIterator registersIterator = iterator.next();
            for(RegisterReference reference : registersIterator) {
                if (reference.getValue() == registerValue) {
                    return reference.getRegisterType() == RegisterType.WRITE;
                }
            }
        }
        return registerValue != getRegistersTable().getLocalRegistersCount();
    }
    private Iterator<RegistersIterator> getRegistersIterators() {
        return getRegistersIterators(0);
    }
    private Iterator<RegistersIterator> getRegistersIterators(int start) {
        return ComputeIterator.of(iterator(start), ins -> {
            if (ins instanceof SizeXIns) {
                return ((SizeXIns) ins).getRegistersIterator();
            }
            return null;
        });
    }

    public<T1 extends Ins> Iterator<T1> iterator(Opcode<T1> opcode) {
        return iterator(opcode, null);
    }
    @SuppressWarnings("unchecked")
    public<T1 extends Ins> Iterator<T1> iterator(Opcode<T1> opcode, Predicate<? super T1> filter) {
        return ComputeIterator.of(iterator(), ins -> {
            T1 result = null;
            if (ins != null && ins.getOpcode() == opcode) {
                result = (T1) ins;
                if (filter != null && !filter.test(result)) {
                    result = null;
                }
            }
            return result;
        });
    }
    @Override
    public Iterator<Ins> iterator() {
        return getInsBlockList().iterator();
    }
    public Iterator<Ins> clonedIterator() {
        return getInsBlockList().clonedIterator();
    }
    public Iterator<Ins> arrayIterator() {
        return getInsBlockList().arrayIterator();
    }
    public Iterator<Ins> iterator(int start, int size) {
        return getInsBlockList().iterator(start, size);
    }
    public Iterator<Ins> iterator(int start) {
        BlockList<Ins> array = getInsBlockList();
        return array.iterator(start, getCount() - start);
    }
    public Iterator<Ins> iteratorByAddress(int startAddress, int codeUnits) {
        Ins insStart = getAtAddress(startAddress);
        if (insStart == null) {
            return EmptyIterator.of();
        }
        Ins insEnd = getAtAddress(startAddress + codeUnits);
        int count = insEnd.getIndex() - insStart.getIndex();
        return this.iterator(insStart.getIndex(), count);
    }

    private InsBlockList getInsBlockList() {
        return insBlockList;
    }

    void onEditing(InstructionList instructionList) {
        getInsBlockList().onEditingInternal(instructionList.getInsBlockList());
    }
    public Ins get(int i) {
        return getInsBlockList().get(i);
    }
    public int getCount() {
        return getInsBlockList().size();
    }
    public boolean isEmpty() {
        return getInsBlockList().size() == 0;
    }
    public void add(Ins ins) {
        add(getInsBlockList().size(), ins);
    }
    public void add(int index, Ins item) {
        add(true, index, item);
    }
    public void add(boolean shiftLabels, int index, Ins item) {
        InsBlockList insBlockList = getInsBlockList();
        insBlockList.unlink();
        Ins exist = insBlockList.get(index);
        Object lock = null;
        if (exist != null) {
            lock = insBlockList.linkLocked();
        }
        insBlockList.add(index, item);
        if (shiftLabels && exist != null) {
            exist.transferReferenceLabelsTo(item);
        }
        insBlockList.unlinkLocked(lock);
    }
    public void moveTo(Ins ins, int index) {
        int current = ins.getIndex();
        if (index == current) {
            return;
        }
        if (current < 0) {
            throw new IndexOutOfBoundsException("Removed ins, negative index: " + index);
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index: " + index);
        }
        if (index >= getCount()) {
            throw new IndexOutOfBoundsException("Size = " + getCount() + ", " + index);
        }
        InsBlockList insBlockList = getInsBlockList();
        Object locked = insBlockList.linkLocked();
        insBlockList.moveTo(ins, index);
        Ins insAtPosition = get(current);
        ins.transferReferenceLabelsTo(insAtPosition);
        insBlockList.unlinkLocked(locked);
    }
    public ConstNumber createConstIntegerAt(int index, int register, int value) {
        ConstNumber constNumber;
        if (register <= 0x0f && value <= 0x7 && value >= -0x7) {
            constNumber = createAt(index, Opcode.CONST_4);
        } else if (value <= 0x7fff && value >= -0x7fff) {
            constNumber = createAt(index, Opcode.CONST_16);
        } else if ((value & 0x0000ffff) == 0) {
            constNumber = createAt(index, Opcode.CONST_HIGH16);
        } else {
            constNumber = createAt(index, Opcode.CONST);
        }
        constNumber.setRegister(register);
        constNumber.set(value);
        return constNumber;
    }
    public ConstNumberLong createConstLongAt(int index, long value) {
        return createConstLongAt(index, 0, value);
    }
    public ConstNumberLong createConstLongAt(int index, int register, long value) {
        ConstNumberLong constNumber;
        if ((value & 0xffff00000000L) == 0) {
            constNumber = createAt(index, Opcode.CONST_WIDE_HIGH16);
        } else if ((value & 0xffff) == value) {
            constNumber = createAt(index, Opcode.CONST_WIDE_16);
        } else if ((value & 0xffffffffL) == value) {
            constNumber = createAt(index, Opcode.CONST_WIDE_32);
        } else {
            constNumber = createAt(index, Opcode.CONST_WIDE);
        }
        constNumber.setRegister(register);
        constNumber.set(value);
        return constNumber;
    }
    public ConstString createStringAt(int index, StringKey value) {
        return createStringAt(index, 0, value);
    }
    public ConstString createStringAt(int index, String value) {
        return createStringAt(index, 0, value);
    }
    public ConstString createStringAt(int index, int register, String value) {
        return createStringAt(index, register, StringKey.create(value));
    }
    public ConstString createStringAt(int index, int register, StringKey value) {
        ConstString constNumber;
        StringId stringId = getCodeItem().getOrCreateSectionItem(SectionType.STRING_ID, value);
        int id = stringId.getIdx();
        if ((id & 0xffff) == id) {
            constNumber = createAt(index, Opcode.CONST_STRING);
        } else {
            constNumber = createAt(index, Opcode.CONST_STRING_JUMBO);
        }
        constNumber.setRegister(register);
        constNumber.setString(stringId);
        return constNumber;
    }
    public<T1 extends Ins> T1 createAt(int index, Opcode<T1> opcode) {
        return createAt(true, index, opcode);
    }
    public<T1 extends Ins> T1 createAt(boolean shiftLabels, int index, Opcode<T1> opcode) {
        if (index == getCount()) {
            return createNext(opcode);
        }
        T1 item = opcode.newInstance();
        add(shiftLabels, index, item);
        return item;
    }
    public<T1 extends Ins> T1 createNext(Opcode<T1> opcode) {
        T1 item = opcode.newInstance();
        add(item);
        return item;
    }
    public boolean isLonelyInTryCatch(Ins ins) {
        Iterator<InsExceptionHandler.TryStartLabel> iterator = ins.getReferencingLabels(
                InsExceptionHandler.TryStartLabel.class);
        if (!iterator.hasNext()) {
            return false;
        }
        InsBlockList insBlockList = getInsBlockList();
        insBlockList.link(iterator);
        int codeUnits = ins.getCodeUnits();
        boolean result = false;
        while (iterator.hasNext()) {
            InsExceptionHandler.TryStartLabel startLabel = iterator.next();
            int handlerCodeUnits = startLabel.getHandler().getCodeUnit();
            if (handlerCodeUnits <= codeUnits) {
                result = true;
                break;
            }
        }
        insBlockList.unlink(iterator);
        return result;
    }
    public boolean contains(Ins item) {
        return getInsBlockList().containsExact(item);
    }
    public boolean remove(Ins item) {
        return remove(item, false);
    }
    public boolean remove(Ins item, boolean force) {
        InsBlockList insBlockList = getInsBlockList();
        if (!insBlockList.containsExact(item)) {
            return false;
        }
        Object lock = insBlockList.linkLocked();
        if (!force && isLonelyInTryCatch(item)) {
            return replaceWithNop(item) != null;
        }
        int index = item.getIndex();
        Ins next = get(index + 1);
        if (next == null && item.hasReferenceLabels()) {
            next = insBlockList.getOrCreateNullInstruction();
        }
        if (next != null) {
            item.transferReferenceLabelsTo(next);
        }
        insBlockList.remove(item);
        item.setParent(null);
        item.setIndex(-1);
        insBlockList.unlinkLocked(lock);
        return true;
    }
    public InsNop replaceWithNop(Ins ins) {
        return replace(ins, Opcode.NOP);
    }
    public<T1 extends Ins> T1 replace(Ins old, Opcode<T1> opcode) {
        if (!contains(old)) {
            return null;
        }
        T1 item = opcode.newInstance();
        replace(old, item);
        return item;
    }
    public void replace(Ins old, Ins item) {
        if (old == item) {
            return;
        }
        InsBlockList insBlockList = getInsBlockList();
        if (!insBlockList.containsExact(old)) {
            throw new IllegalArgumentException("Not a member of this instruction list");
        }
        Object obj = insBlockList.linkLocked();
        int index = old.getIndex();
        insBlockList.set(index, item);
        old.transferReferenceLabelsTo(item);
        old.setParent(null);
        old.setIndex(-1);
        insBlockList.unlinkLocked(obj);
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        getInsBlockList().unlink();
    }
    public int getCodeUnits() {
        return getInsBlockList().getCodeUnits();
    }
    public DexPositionAlign getBlockAlign() {
        return blockAlign;
    }
    public Ins getAtAddress(int address) {
        return getInsBlockList().getAtAddress(address);
    }
    public Iterator<InstructionLabel> getCodeLabels() {
        return CombiningIterator.two(getInsBlockList().getLabels(), getCodeItem().getTryBlockLabels());
    }
    public void replaceKeys(Key search, Key replace) {
        for(Ins ins : this) {
            ins.replaceKeys(search, replace);
        }
    }
    public boolean uses(Key key) {
        int size = getCount();
        for (int i = 0; i < size; i++) {
            if (get(i).uses(key)) {
                return true;
            }
        }
        return false;
    }
    public Iterator<IdItem> usedIds() {
        return new IterableIterator<Ins, IdItem>(iterator()) {
            @Override
            public Iterator<IdItem> iterator(Ins element) {
                return element.usedIds();
            }
        };
    }
    public void linkReferenceLabels() {
        InsBlockList insBlockList = getInsBlockList();
        insBlockList.link();
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        InsBlockList insBlockList = getInsBlockList();
        Object lock = insBlockList.linkLocked();
        writer.buildLabels(getCodeLabels());
        writer.setStateWritingInstructions(true);
        for (Ins ins : this) {
            writer.newLine();
            ins.append(writer);
        }
        writer.setStateWritingInstructions(false);
        NullInstruction nullInstruction = getInsBlockList().getNullInstruction();
        if (nullInstruction != null) {
            writer.newLine();
            nullInstruction.append(writer);
        }
        insBlockList.unlink(lock, false);
    }
    public void merge(InstructionList instructionList) {
        getInsBlockList().merge(instructionList.getInsBlockList());
        getInsBlockList().updateCodeUnits();
    }
    public void fromSmali(SmaliCodeSet smaliCodeSet) {
        int index = 0;
        int offset = smaliCodeSet.getAddressOffset();
        if (offset != 0) {
            Ins ins = getAtAddress(smaliCodeSet.getAddressOffset());
            if (ins != null) {
                index = ins.getIndex();
            } else if (offset >= getCodeUnits()) {
                index = getCount();
            }
        }
        fromSmali(index, smaliCodeSet);
    }
    public void fromSmali(int index, SmaliCodeSet smaliCodeSet) {
        InsBlockList insBlockList = getInsBlockList();
        Object obj = insBlockList.linkLocked();
        Iterator<SmaliInstruction> iterator = smaliCodeSet.getInstructions();
        while (iterator.hasNext()) {
            SmaliInstruction smaliInstruction = iterator.next();
            Ins ins = createAt(index, smaliInstruction.getOpcode());
            ins.fromSmali(smaliInstruction);
            index ++;
        }
        insBlockList.unlinkLocked(obj);
    }
    public void toSmali(SmaliMethod smaliMethod) {
        InsBlockList insBlockList = getInsBlockList();
        Object obj = insBlockList.linkLocked();
        SmaliCodeSet smaliCodeSet = smaliMethod.getCodeSet();
        int size = insBlockList.size();
        for (int i = 0; i < size; i++) {
            get(i).toSmali(smaliCodeSet);
        }
        insBlockList.unlinkLocked(obj);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InstructionList list = (InstructionList) obj;
        return insBlockList.equals(list.insBlockList);
    }

    @Override
    public int hashCode() {
        return insBlockList.hashCode();
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        SmaliWriter smaliWriter = new SmaliWriter(writer);
        smaliWriter.indentPlus();
        try {
            append(smaliWriter);
            smaliWriter.close();
        } catch (IOException exception) {
            return exception.toString();
        }
        return writer.toString();
    }
}
