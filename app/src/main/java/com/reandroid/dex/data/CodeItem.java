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
import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.arsc.item.IndirectInteger;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.base.IndirectShort;
import com.reandroid.dex.base.PositionAlignedItem;
import com.reandroid.dex.base.UsageMarker;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.debug.DebugElementBlock;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.DataKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyReference;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.reference.DataItemIndirectReference;
import com.reandroid.dex.common.RegistersTable;
import com.reandroid.dex.ins.InsTryBlock;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliTryItem;
import com.reandroid.dex.smali.model.SmaliMethod;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.util.Iterator;

public class CodeItem extends DataItem implements RegistersTable,
        PositionAlignedItem, KeyReference {

    private final Header header;
    private final InstructionList instructionList;
    private InsTryBlock tryBlock;

    private final DataKey<CodeItem> codeItemKey;

    private MethodDef methodDef;

    public CodeItem() {
        super(3);
        this.header = new Header(this);
        this.instructionList = new InstructionList(this);
        this.tryBlock = null;

        this.codeItemKey = new DataKey<>(this);

        addChildBlock(0, header);
        addChildBlock(1, instructionList);
    }

    @Override
    public DataKey<CodeItem> getKey() {
        return codeItemKey;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void setKey(Key key) {
        DataKey<CodeItem> codeItemKey = (DataKey<CodeItem>) key;
        merge(codeItemKey.getItem());
    }
    @Override
    public SectionType<CodeItem> getSectionType() {
        return SectionType.CODE;
    }

    @Override
    public int getRegistersCount() {
        return header.registersCount.get();
    }
    @Override
    public void setRegistersCount(int count) {
        header.registersCount.set(count);
    }
    @Override
    public int getParameterRegistersCount() {
        return header.parameterRegisters.get();
    }
    @Override
    public void setParameterRegistersCount(int count) {
        header.parameterRegisters.set(count);
    }
    @Override
    public boolean ensureLocalRegistersCount(int locals) {
        if (locals == 0) {
            return true;
        }
        if (locals <= getLocalRegistersCount()) {
            return true;
        }
        int params = getParameterRegistersCount();
        int current = getLocalRegistersCount();
        int diff = locals - current;
        InstructionList instructionList = getInstructionList();
        if (!instructionList.canAddLocalRegisters(diff)) {
            return false;
        }
        if (diff > 0) {
            instructionList.addLocalRegisters(diff);
        }
        setRegistersCount(locals + params);
        return true;
    }


    public Iterator<DebugElementBlock> getDebugLabels() {
        DebugInfo debugInfo = getDebugInfo();
        if (debugInfo != null) {
            return debugInfo.getExtraLines();
        }
        return EmptyIterator.of();
    }
    public DebugInfo getDebugInfo() {
        return header.debugInfoOffset.getItem();
    }
    public DebugInfo getOrCreateDebugInfo() {
        return header.debugInfoOffset.getOrCreateUniqueItem(this);
    }
    public void removeDebugInfo() {
        if (getDebugInfo() == null) {
            return;
        }
        setDebugInfo(null);
    }
    public void setDebugInfo(DebugInfo debugInfo) {
        header.debugInfoOffset.setItem(debugInfo);
    }
    public InstructionList getInstructionList() {
        return instructionList;
    }
    public IntegerReference getTryCountReference() {
        return header.tryBlockCount;
    }

    public Iterable<InstructionLabel> getExternalLabels() {
        return () -> CombiningIterator.two(
                CodeItem.this.getTryBlockLabels(),
                CodeItem.this.getDebugLabels());
    }
    public Iterator<InstructionLabel> getTryBlockLabels() {
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock == null || tryBlock.isNull()) {
            return EmptyIterator.of();
        }
        return tryBlock.getLabels();
    }
    public InsTryBlock getTryBlock() {
        return tryBlock;
    }
    public InsTryBlock getOrCreateTryBlock() {
        initTryBlock();
        return tryBlock;
    }
    public void removeTryBlock() {
        InsTryBlock tryBlock = this.tryBlock;
        if (tryBlock == null) {
            return;
        }
        this.tryBlock = null;
        this.header.tryBlockCount.set(0);
        tryBlock.setParent(null);
    }
    public boolean compactSimilarCatches() {
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock != null) {
            return tryBlock.compactSimilarCatches();
        }
        return false;
    }
    public boolean flattenTryItems() {
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock != null) {
            return tryBlock.flattenCompactCatches();
        }
        return false;
    }
    public boolean splitTryHandlers() {
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock != null) {
            return tryBlock.splitTryHandlers();
        }
        return false;
    }
    public boolean combineTries() {
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock != null) {
            return tryBlock.combineTries();
        }
        return false;
    }
    public MethodDef getMethodDef() {
        return methodDef;
    }
    public void setMethodDef(MethodDef methodDef) {
        this.methodDef = methodDef;
    }

    IntegerReference getInstructionCodeUnitsReference() {
        return header.instructionCodeUnits;
    }
    IntegerReference getInstructionOutsReference() {
        return header.outs;
    }
    void initTryBlock() {
        if (this.tryBlock == null) {
            this.tryBlock = new InsTryBlock(this);
            addChildBlock(2, this.tryBlock);
        }
    }
    @Override
    public DexPositionAlign getPositionAlign() {
        if (this.tryBlock != null) {
            return this.tryBlock.getPositionAlign();
        } else if (this.instructionList != null) {
            return this.instructionList.getBlockAlign();
        }
        return new DexPositionAlign();
    }
    @Override
    public void removeLastAlign() {
        if (this.tryBlock != null) {
            this.tryBlock.getPositionAlign().setSize(0);
        } else if (this.instructionList != null) {
            this.instructionList.getBlockAlign().setSize(0);
        }
    }

    public void replaceKeys(Key search, Key replace) {
        getInstructionList().replaceKeys(search, replace);
    }


    @Override
    public void edit() {
        this.editInternal(this);
    }
    @Override
    public void editInternal(Block user) {
        this.header.editInternal(user);
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        InsTryBlock tryBlock = this.getTryBlock();
        if (tryBlock != null && tryBlock.isEmpty()) {
            removeTryBlock();
        }
    }

    @Override
    public boolean uses(Key key) {
        InsTryBlock tryBlock = getTryBlock();
        if (tryBlock != null && tryBlock.uses(key)) {
            return true;
        }
        return getInstructionList().uses(key);
    }

    @Override
    public Iterator<IdItem> usedIds() {
        DebugInfo debugInfo = getDebugInfo();
        Iterator<IdItem> iterator1;
        if (debugInfo == null) {
            iterator1 = EmptyIterator.of();
        } else {
            iterator1 = debugInfo.usedIds();
        }
        Iterator<IdItem> iterator2;
        InsTryBlock tryBlock = getTryBlock();
        if (tryBlock == null) {
            iterator2 = EmptyIterator.of();
        } else {
            iterator2 = tryBlock.usedIds();
        }
        return CombiningIterator.three(getInstructionList().usedIds(), iterator1, iterator2);
    }

    public void merge(CodeItem codeItem) {
        if (codeItem == this) {
            return;
        }
        this.header.merge(codeItem.header);
        getInstructionList().merge(codeItem.getInstructionList());
        InsTryBlock comingTry = codeItem.getTryBlock();
        if (comingTry != null) {
            InsTryBlock tryBlock = getOrCreateTryBlock();
            tryBlock.merge(comingTry);
        }
    }
    public void fromSmali(SmaliMethod smaliMethod) {
        setRegistersCount(smaliMethod.getRegistersCount());
        setParameterRegistersCount(smaliMethod.getParameterRegistersCount());
        getInstructionList().fromSmali(smaliMethod.getCodeSet());
        Iterator<SmaliTryItem> iterator = smaliMethod.getTryItems();
        InsTryBlock tryBlock = null;
        if (iterator.hasNext()) {
            tryBlock = getOrCreateTryBlock();
        }
        while (iterator.hasNext()) {
            tryBlock.fromProgram(iterator.next());
        }
        if (smaliMethod.hasDebugElements()) {
            DebugInfo debugInfo = getOrCreateDebugInfo();
            debugInfo.getDebugSequence().fromSmali(smaliMethod.getCodeSet());
        }
        if (tryBlock != null) {
            tryBlock.combineTries();
            tryBlock.compactSimilarCatches();
            tryBlock.refresh();
        }
    }
    public void toSmali(SmaliMethod smaliMethod) {
        SmaliMethod.SmaliRegistersCount registersCount = smaliMethod.getSmaliRegistersCount();
        registersCount.setLocalsMode(true);
        registersCount.setValue(getLocalRegistersCount());
        getInstructionList().toSmali(smaliMethod);
    }

    public void appendRegistersCount(SmaliWriter writer) throws IOException {
        writer.setCurrentRegistersTable(this);
        writer.newLine();
        if (writer.isLocalRegistersCount()) {
            SmaliDirective.LOCALS.append(writer);
            writer.appendInteger(getLocalRegistersCount());
        } else {
            SmaliDirective.REGISTERS.append(writer);
            writer.appendInteger(getRegistersCount());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CodeItem codeItem = (CodeItem) obj;
        return header.equals(codeItem.header) &&
                instructionList.equals(codeItem.instructionList) &&
                ObjectsUtil.equals(tryBlock, codeItem.tryBlock);
    }

    @Override
    public int hashCode() {
        int hash = header.hashCode();
        hash = hash * 31 + instructionList.hashCode();
        hash = hash * 31;
        InsTryBlock tryBlock = this.tryBlock;
        if (tryBlock != null) {
            hash = hash + tryBlock.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if (isNull()) {
            return "NULL";
        }
        return header.toString()
                + "\n instructionList=" + instructionList
                + "\n tryBlock=" + tryBlock
                + "\n debug=" + getDebugInfo();
    }

    static class Header extends SectionItem implements BlockRefresh {

        private final CodeItem codeItem;

        final IntegerReference registersCount;
        final IntegerReference parameterRegisters;
        final IntegerReference outs;
        final IntegerReference tryBlockCount;

        final DataItemIndirectReference<DebugInfo> debugInfoOffset;
        final IntegerReference instructionCodeUnits;

        public Header(CodeItem codeItem) {
            super(16);
            this.codeItem = codeItem;
            int offset = -2;
            this.registersCount = new IndirectShort(this, offset += 2);
            this.parameterRegisters = new IndirectShort(this, offset += 2);
            this.outs = new IndirectShort(this, offset += 2);
            this.tryBlockCount = new IndirectShort(this, offset += 2);
            this.debugInfoOffset = new DataItemIndirectReference<>(SectionType.DEBUG_INFO,this, offset += 2, UsageMarker.USAGE_DEBUG);
            this.instructionCodeUnits = new IndirectInteger(this, offset + 4);
        }


        @Override
        public void refresh() {
            debugInfoOffset.addUniqueUser(this.codeItem);
            debugInfoOffset.refresh();
            if (this.codeItem.getTryBlock() == null) {
                this.tryBlockCount.set(0);
            }
        }
        @Override
        public void onReadBytes(BlockReader reader) throws IOException {
            super.onReadBytes(reader);
            this.debugInfoOffset.pullItem();
            this.debugInfoOffset.addUniqueUser(this.codeItem);
            if (this.tryBlockCount.get() != 0) {
                this.codeItem.initTryBlock();
            }
        }

        public void onRemove() {
            debugInfoOffset.setItem(null);
        }

        @Override
        public void editInternal(Block user) {
            debugInfoOffset.editInternal(user);
        }

        public void merge(Header header) {
            registersCount.set(header.registersCount.get());
            parameterRegisters.set(header.parameterRegisters.get());
            outs.set(header.outs.get());
            tryBlockCount.set(header.tryBlockCount.get());
            DebugInfo comingDebug = header.debugInfoOffset.getItem();
            if (comingDebug != null) {
                debugInfoOffset.setKey(comingDebug.getKey());
                debugInfoOffset.addUniqueUser(codeItem);
            }
            instructionCodeUnits.set(header.instructionCodeUnits.get());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Header header = (Header) obj;
            return registersCount.get() == header.registersCount.get() &&
                    parameterRegisters.get() == header.parameterRegisters.get() &&
                    outs.get() == header.outs.get() &&
                    tryBlockCount.get() == header.tryBlockCount.get() &&
                    instructionCodeUnits.get() == header.instructionCodeUnits.get() &&
                    ObjectsUtil.equals(debugInfoOffset.getItem(), header.debugInfoOffset.getItem());
        }

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 31 + registersCount.get();
            hash = hash * 31 + parameterRegisters.get();
            hash = hash * 31 + outs.get();
            hash = hash * 31 + tryBlockCount.get();
            hash = hash * 31 + instructionCodeUnits.get();
            hash = hash * 31;
            DebugInfo info = debugInfoOffset.getItem();
            if (info != null) {
                hash = hash + info.hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            return  "registers=" + registersCount +
                    ", parameters=" + parameterRegisters +
                    ", outs=" + outs +
                    ", tries=" + tryBlockCount +
                    ", debugInfo=" + debugInfoOffset +
                    ", codeUnits=" + instructionCodeUnits;
        }
    }
}
