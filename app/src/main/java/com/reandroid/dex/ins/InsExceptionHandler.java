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
import com.reandroid.dex.data.FixedDexContainerWithTool;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.*;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliRegion;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliExceptionHandler;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.util.Iterator;

public abstract class InsExceptionHandler extends FixedDexContainerWithTool
        implements SmaliRegion, Iterable<InstructionLabel>, ExceptionHandler {

    private final Ule128Item catchAddress;

    private final ExceptionLabel startLabel;
    private final ExceptionLabel endLabel;
    private final ExceptionLabel handlerLabel;
    private final ExceptionLabel catchLabel;

    private Object[] labels;

    private InsExceptionHandler(int childesCount, Ule128Item catchAddress, int index) {
        super(childesCount);
        this.catchAddress = catchAddress;
        if (catchAddress != null) {
            addChild(index, catchAddress);
        }

        this.startLabel = new TryStartLabel(this);
        this.endLabel = new TryEndLabel(this);
        this.handlerLabel = new InsHandlerLabel(this);
        this.catchLabel = new CatchLabel(this);

        this.labels = new Object[]{this.startLabel, this.endLabel,
                this.handlerLabel, this.catchLabel};
    }
    InsExceptionHandler(int childesCount) {
        this(childesCount + 1, new Ule128Item(), childesCount);
    }

    InsExceptionHandler() {
        this(0, null, 0);
    }

    public void setKey(TypeKey typeKey) {
    }

    @Override
    public ProgramType programType() {
        return ProgramType.DEX;
    }

    public boolean isAddressBounded(int address) {
        if (address == -1) {
            return true;
        }
        return address >= getStartAddress() && address <= getTargetAddress();
    }
    public int getInstructionCount() {
        return CollectionUtil.count(getTryInstructions());
    }
    public Iterator<Ins> getTryInstructions() {
        InstructionList instructionList = getInstructionList();
        if (instructionList == null) {
            return EmptyIterator.of();
        }
        return instructionList.iteratorByAddress(
                getStartLabel().getTargetAddress(), getCodeUnit());
    }
    private InstructionList getInstructionList() {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            return tryItem.getInstructionList();
        }
        return null;
    }

    public abstract boolean traps(TypeKey typeKey);
    abstract TypeId getTypeId();
    public abstract SmaliDirective getSmaliDirective();
    Ule128Item getCatchAddressUle128() {
        return catchAddress;
    }

    @Override
    public Iterator<InstructionLabel> getLabels() {
        return iterator();
    }
    @Override
    public Iterator<InstructionLabel> iterator() {
        return ArrayIterator.of(labels);
    }
    public ExceptionLabel getHandlerLabel() {
        return handlerLabel;
    }
    @Override
    public ExceptionLabel getStartLabel() {
        return startLabel;
    }
    @Override
    public ExceptionLabel getEndLabel() {
        return endLabel;
    }
    @Override
    public ExceptionLabel getCatchLabel() {
        return catchLabel;
    }

    public void refreshAddresses() {
        Instruction handlerIns = getHandlerLabel().getTargetInstruction();
        Instruction startIns = getStartLabel().getTargetInstruction();
        Instruction endIns = getEndLabel().getTargetInstruction();
        Instruction catchIns = getCatchLabel().getTargetInstruction();

        if (handlerIns != null && startIns != null && endIns != null && catchIns != null) {

            int handlerAddress = handlerIns.getAddress();
            int startAddress = startIns.getAddress();
            int catchAddress = catchIns.getAddress();

            setStartAddress(startIns.getAddress());
            setCatchAddress(catchAddress);
            setCodeUnit(handlerAddress - startAddress);
        }
    }

    public int getCatchAddress() {
        return getCatchAddressUle128().get();
    }
    public void setCatchAddress(int address) {
        getCatchAddressUle128().set(address);
    }
    @Override
    public int getTargetAddress() {
        return getStartAddress() + getCodeUnit();
    }
    @Override
    public void setTargetAddress(int address) {
        setCodeUnit(address - getStartAddress());
    }
    @Override
    public int getOwnerAddress() {
        return getTargetAddress();
    }
    @Override
    public Ins getTargetInstruction() {
        return (Ins) handlerLabel.getTargetInstruction();
    }
    @Override
    public void setTargetInstruction(Instruction ins) {
        handlerLabel.setTargetInstruction(ins);
    }

    @Override
    public int getStartAddress() {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            return tryItem.getStartAddress();
        }
        return 0;
    }
    public void setStartAddress(int address) {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            tryItem.setStartAddress(address);
        }
    }
    public int getCodeUnit() {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            return tryItem.getCatchCodeUnit();
        }
        return 0;
    }
    public void setCodeUnit(int value) {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            tryItem.getHandlerOffset().setCatchCodeUnit(value);
        }
    }
    InsTryItem getTryItem() {
        return getParentInstance(InsTryItem.class);
    }

    public void onRemove() {
        labels = null;
        setParent(null);
    }
    public void removeSelf() {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            tryItem.remove(this);
        }
    }
    public boolean isRemoved() {
        return getParent() == null;
    }
    int compareHandler(InsExceptionHandler handler) {
        if (handler == this) {
            return 0;
        }
        int i = CompareUtil.compare(this.getTargetAddress(), handler.getTargetAddress());
        if (i != 0) {
            return i;
        }
        InsTryItem tryItem1 = getTryItem();
        InsTryItem tryItem2 = handler.getTryItem();
        i = CompareUtil.compare(tryItem1.getIndex(), tryItem2.getIndex());
        if (i != 0) {
            return i;
        }
        i = CompareUtil.compare(this.isCatchAll(), handler.isCatchAll());
        if (i != 0) {
            return i;
        }
        return CompareUtil.compare(this.getIndex(),
                handler.getIndex());
    }
    public void merge(InsExceptionHandler handler) {
        catchAddress.set(handler.getCatchAddress());
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {

    }
    public void fromSmali(SmaliExceptionHandler smaliExceptionHandler) {
        getHandlerLabel().setTargetAddress(smaliExceptionHandler.getTargetAddress());
        getStartLabel().setTargetAddress(smaliExceptionHandler.getStartLabel().getTargetAddress());
        getEndLabel().setTargetAddress(smaliExceptionHandler.getEndLabel().getTargetAddress());
        getCatchLabel().setTargetAddress(smaliExceptionHandler.getCatchLabel().getTargetAddress());
    }
    public void fromProgram(ExceptionHandler handler) {
        if (isCatchAll() != handler.isCatchAll()) {
            throw new IllegalArgumentException("Mismatch catch handler");
        }
        getHandlerLabel().setTargetAddress(handler.getTargetAddress());
        getStartLabel().setTargetAddress(handler.getStartLabel().getTargetAddress());
        getEndLabel().setTargetAddress(handler.getEndLabel().getTargetAddress());
        getCatchLabel().setTargetAddress(handler.getCatchLabel().getTargetAddress());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InsExceptionHandler handler = (InsExceptionHandler) obj;
        return getStartAddress() == handler.getStartAddress() &&
                getTargetAddress() == handler.getTargetAddress() &&
                getCatchAddress() == handler.getCatchAddress() &&
                ObjectsUtil.equals(getKey(), handler.getKey());
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + getStartAddress();
        hash = hash * 31 + getTargetAddress();
        hash = hash * 31 + getCatchAddress();
        hash = hash * 31;
        TypeKey key = getKey();
        if (key != null) {
            hash = hash + key.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return getHandlerLabel().toString();
    }

    static abstract class AbstractExceptionLabel implements ExceptionLabel {

        private Instruction targetInstruction;
        private final InsExceptionHandler handler;

        AbstractExceptionLabel(InsExceptionHandler handler) {
            this.handler = handler;
        }

        @Override
        public InsExceptionHandler getHandler() {
            return handler;
        }
        @Override
        public Instruction getTargetInstruction() {
            Instruction instruction = this.targetInstruction;
            if (instruction != null && instruction.isRemoved()) {
                instruction = null;
                this.targetInstruction = null;
            }
            return instruction;
        }
        @Override
        public void setTargetInstruction(Instruction target) {
            if (target != this.targetInstruction) {
                this.targetInstruction = target;
                if (target != null) {
                    target.addReferencingLabel(this);
                }
            }
        }
        @Override
        public Instruction getOwnerInstruction() {
            return null;
        }
        @Override
        public void updateTarget() {
            // update on handler is enough
        }
    }
    public static class InsHandlerLabel extends AbstractExceptionLabel {

        InsHandlerLabel(InsExceptionHandler handler) {
            super(handler);
        }

        @Override
        public int getOwnerAddress() {
            return getHandler().getOwnerAddress();
        }

        @Override
        public int getTargetAddress() {
            return getHandler().getTargetAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress) {
            getHandler().setTargetAddress(targetAddress);
        }
        @Override
        public InstructionLabelType getLabelType() {
            return getHandler().getLabelType();
        }
        @Override
        public void updateTarget() {
            getHandler().refreshAddresses();
        }
        @Override
        public String getLabelName() {
            return getHandler().getLabelName();
        }
        @Override
        public boolean equalsLabel(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            InsHandlerLabel label = (InsHandlerLabel) obj;
            return this.getHandler() == label.getHandler();
        }
        @Override
        public int compareLabel(InstructionLabel label) {
            if (label == this) {
                return 0;
            }
            int i = InstructionLabel.compareLabels(this, label);
            if (i == 0) {
                InsHandlerLabel handlerLabel = (InsHandlerLabel) label;
                i = this.getHandler().compareHandler(handlerLabel.getHandler());
            }
            return i;
        }

        @Override
        public void appendLabelName(SmaliWriter writer) throws IOException {
            InsExceptionHandler handler = this.getHandler();
            handler.getSmaliDirective().append(writer);
            TypeId typeId = handler.getTypeId();
            if (typeId != null) {
                typeId.append(writer);
                writer.append(' ');
            }
            writer.append("{");
            writer.appendLabelName(handler.getStartLabel().getLabelName());
            writer.append(" .. ");
            writer.appendLabelName(handler.getEndLabel().getLabelName());
            writer.append("} ");
            writer.appendLabelName(handler.getCatchLabel().getLabelName());
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class TryStartLabel extends AbstractExceptionLabel {

        TryStartLabel(InsExceptionHandler handler) {
            super(handler);
        }

        @Override
        public int getOwnerAddress() {
            return getHandler().getTargetAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getStartAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress) {
            getHandler().setStartAddress(targetAddress);
        }
        @Override
        public InstructionLabelType getLabelType() {
            return InstructionLabelType.TRY_START;
        }
        @Override
        public boolean equalsLabel(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            TryStartLabel label = (TryStartLabel) obj;
            if (this.getHandler() == label.getHandler()) {
                return true;
            }
            return getTargetAddress() == label.getTargetAddress();
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class TryEndLabel extends AbstractExceptionLabel {

        TryEndLabel(InsExceptionHandler handler) {
            super(handler);
        }

        @Override
        public int getOwnerAddress() {
            return getHandler().getTargetAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getTargetAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress) {
            getHandler().setTargetAddress(targetAddress);
        }
        @Override
        public InstructionLabelType getLabelType() {
            return InstructionLabelType.TRY_END;
        }

        @Override
        public String getLabelName() {
            return getLabelType().buildLabelName(getHandler().getStartLabel().getTargetAddress());
        }

        @Override
        public boolean equalsLabel(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            TryEndLabel label = (TryEndLabel) obj;
            if (this.getHandler() == label.getHandler()) {
                return true;
            }
            return getTargetAddress() == label.getTargetAddress();
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class CatchLabel extends AbstractExceptionLabel {

        CatchLabel(InsExceptionHandler handler) {
            super(handler);
        }

        @Override
        public int getOwnerAddress() {
            return getHandler().getStartLabel().getOwnerAddress();
        }

        @Override
        public int getTargetAddress() {
            return getHandler().getCatchAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress) {
            getHandler().setCatchAddress(targetAddress);
        }
        public boolean isCatchAll() {
            return getHandler().isCatchAll();
        }

        @Override
        public InstructionLabelType getLabelType() {
            if (getHandler().isCatchAll()) {
                return InstructionLabelType.CATCH_ALL;
            }
            return InstructionLabelType.CATCH;
        }

        @Override
        public boolean equalsLabel(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            CatchLabel label = (CatchLabel) obj;
            if (this.getHandler() == label.getHandler()) {
                return true;
            }
            return getTargetAddress() == label.getTargetAddress() &&
                    isCatchAll() == label.isCatchAll();
        }

        @Override
        public int compareLabel(InstructionLabel label) {
            if (label == this) {
                return 0;
            }
            int i = InstructionLabel.compareLabels(this, label);
            if (i == 0) {
                CatchLabel catchLabel = (CatchLabel) label;
                i = CompareUtil.compare(this.isCatchAll(), catchLabel.isCatchAll());
            }
            return i;
        }

        @Override
        public String toString() {
            return getLabelName();
        }
    }

    static boolean areSimilar(InsExceptionHandler handler1, InsExceptionHandler handler2) {
        if (handler1 == null || handler2 == null) {
            return handler1 == handler2;
        }
        return handler1.getCatchAddress() == handler2.getCatchAddress() &&
                ObjectsUtil.equals(handler1.getKey(), handler2.getKey());
    }
}
