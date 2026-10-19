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

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.base.BlockCounter;
import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.dex.base.Sle128Item;
import com.reandroid.dex.common.IdUsageIterator;
import com.reandroid.dex.data.FixedDexContainerWithTool;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.ExceptionHandler;
import com.reandroid.dex.program.ProgramType;
import com.reandroid.dex.program.TryItem;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class InsTryItem extends FixedDexContainerWithTool implements TryItem,
        Comparable<InsTryItem>, IdUsageIterator {

    private final HandlerOffsetArray handlerOffsetArray;

    final Sle128Item handlersCount;
    private final BlockList<InsCatchTypedHandler> catchTypedHandlerList;
    private InsCatchAllHandler catchAllHandler;

    private HandlerOffset mHandlerOffset;

    public InsTryItem(HandlerOffsetArray handlerOffsetArray) {
        super(3);

        this.handlerOffsetArray = handlerOffsetArray;
        this.handlersCount = new Sle128Item();
        this.catchTypedHandlerList = new BlockList<>();

        addChild(0, handlersCount);
        addChild(1, catchTypedHandlerList);
    }
    private InsTryItem() {
        super(0);
        this.handlerOffsetArray = null;

        this.handlersCount = null;
        this.catchTypedHandlerList = null;
    }

    public boolean isCompact() {
        return false;
    }
    InstructionList getInstructionList() {
        return getTryBlock().getInstructionList();
    }
    InsTryBlock getTryBlock() {
        return getParent(InsTryBlock.class);
    }

    InsTryItem newCompact() {
        return new Compact(this);
    }

    public boolean compactWith(InsTryItem similar) {
        if (!isSimilarTo(similar)) {
            return false;
        }
        int index = similar.getIndex();
        InsTryBlock tryBlock = this.getTryBlock();
        InsTryItem replace = tryBlock.createNextCopy(this);
        replace.merge(similar);
        tryBlock.remove(similar);
        tryBlock.moveTo(replace, index);
        return true;
    }
    private boolean isSimilarTo(InsTryItem tryItem) {
        if (tryItem == this || this.isCompact() || tryItem.isCompact()) {
            return false;
        }
        if (getParent() != tryItem.getParent()) {
            return false;
        }
        if (!InsExceptionHandler.areSimilar(
                this.getCatchAllHandler(),
                tryItem.getCatchAllHandler())) {
            return false;
        }
        int count = this.getCatchTypedHandlersCount();
        if (count != tryItem.getCatchTypedHandlersCount()) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            if (!InsExceptionHandler.areSimilar(this.getCatchTypedHandler(i),
                    tryItem.getCatchTypedHandler(i))) {
                return false;
            }
        }
        return true;
    }
    public boolean flatten() {
        return false;
    }
    public boolean splitHandlers() {
        if (!hasMultipleHandlers()) {
            return false;
        }
        int index = getIndex() + 1;
        int count = getCatchTypedHandlersCount();
        for (int i = 1; i < count; i++) {
            InsCatchTypedHandler handler = getCatchTypedHandler(1);
            index = transferHandlerToNewTryItem(handler, index);
        }
        transferHandlerToNewTryItem(getCatchAllHandler(), index);
        refresh();
        return true;
    }
    private int transferHandlerToNewTryItem(InsExceptionHandler handler, int index) {
        if (handler == null) {
            return index;
        }
        InsTryBlock tryBlock = getTryBlock();
        InsTryItem destination = tryBlock.createNext();
        destination.mergeOffset(this);
        destination.mergeHandler(handler);
        remove(handler);
        tryBlock.moveTo(destination, index);
        destination.refresh();
        return index + 1;
    }
    public boolean combineWith(InsTryItem tryItem) {
        if (!equalsOffsetAndCodeUnit(tryItem)) {
            return false;
        }
        Iterator<InsExceptionHandler> iterator = tryItem.handlers();
        while (iterator.hasNext()) {
            mergeHandler(iterator.next());
        }
        return true;
    }
    private boolean equalsOffsetAndCodeUnit(InsTryItem tryItem) {
        if (tryItem == this || this.isCompact() || tryItem.isCompact()) {
            return false;
        }
        if (getParent() != tryItem.getParent()) {
            return false;
        }
        HandlerOffset offset = getHandlerOffset();
        HandlerOffset other = tryItem.getHandlerOffset();
        return offset.getStartAddress() == other.getStartAddress() &&
                offset.getCatchCodeUnit() == other.getCatchCodeUnit();
    }
    HandlerOffset getHandlerOffset() {
        HandlerOffset handlerOffset = this.mHandlerOffset;
        if (handlerOffset == null) {
            handlerOffset = getHandlerOffsetArray().getOrCreate(getIndex());
            this.mHandlerOffset = handlerOffset;
            handlerOffset.setTryItem(this);
        }
        return handlerOffset;
    }
    HandlerOffsetArray getHandlerOffsetArray() {
        return handlerOffsetArray;
    }
    BlockList<InsCatchTypedHandler> getCatchTypedHandlerBlockList() {
        return catchTypedHandlerList;
    }
    Iterator<InsCatchTypedHandler> getCatchTypedHandlers() {
        return catchTypedHandlerList.iterator();
    }
    InsTryItem getTryItem() {
        return this;
    }
    void updateCount() {
        Sle128Item handlersCount = this.handlersCount;
        if (handlersCount == null) {
            return;
        }
        int count = catchTypedHandlerList.size();
        if (hasCatchAllHandler()) {
            count = -count;
        }
        handlersCount.set(count);
    }

    public boolean isEmpty() {
        return getCatchAllHandler() == null &&
                getCatchTypedHandlersCount() == 0;
    }
    @Override
    public int getCatchTypedHandlersCount() {
        return getCatchTypedHandlerBlockList().size();
    }
    @Override
    public InsCatchTypedHandler getCatchTypedHandler(int i) {
        return getCatchTypedHandlerBlockList().get(i);
    }
    public boolean traps(TypeKey typeKey) {
        return getExceptionHandler(typeKey) != null;
    }
    public boolean traps(TypeKey typeKey, int address) {
        return getExceptionHandler(typeKey, address) != null;
    }
    public boolean hasExceptionHandlersForAddress(int address) {
        return getExceptionHandlersForAddress(address).hasNext();
    }
    public Iterator<InsExceptionHandler> getExceptionHandlersForAddress(int address) {
        return FilterIterator.of(handlers(),
                handler -> handler.isAddressBounded(address));
    }
    public Iterator<InsExceptionHandler> getExceptionHandlersForCatchAddress(int address) {
        return FilterIterator.of(handlers(),
                handler -> handler.getCatchAddress() == address);
    }
    @Override
    public Iterator<InsExceptionHandler> handlers() {
        return CombiningIterator.two(getCatchTypedHandlers(),
                SingleIterator.of(getCatchAllHandler()));
    }
    public InsExceptionHandler getExceptionHandler(TypeKey typeKey) {
        Iterator<InsExceptionHandler> iterator = handlers();
        while (iterator.hasNext()) {
            InsExceptionHandler handler = iterator.next();
            if (handler.traps(typeKey)) {
                return handler;
            }
        }
        return null;
    }
    public InsExceptionHandler getExceptionHandler(TypeKey typeKey, int address) {
        Iterator<InsExceptionHandler> iterator = handlers();
        while (iterator.hasNext()) {
            InsExceptionHandler handler = iterator.next();
            if (handler.traps(typeKey) && handler.isAddressBounded(address)) {
                return handler;
            }
        }
        return null;
    }
    public int getStartAddress() {
        return getHandlerOffset().getStartAddress();
    }
    public void setStartAddress(int address) {
        getHandlerOffset().setStartAddress(address);
    }
    public int getCatchCodeUnit() {
        return getHandlerOffset().getCatchCodeUnit();
    }
    public void setCatchCodeUnit(int codeUnit) {
        getHandlerOffset().setCatchCodeUnit(codeUnit);
    }

    @Override
    public InsCatchAllHandler getCatchAllHandler() {
        return catchAllHandler;
    }
    @Override
    public ProgramType programType() {
        return ProgramType.DEX;
    }

    public InsCatchAllHandler getOrCreateCatchAll() {
        InsCatchAllHandler handler = getCatchAllHandler();
        if (handler == null) {
            initCatchAllHandler();
            handler = getCatchAllHandler();
        }
        return handler;
    }
    private InsCatchAllHandler initCatchAllHandler() {
        InsCatchAllHandler catchAllHandler = this.getCatchAllHandler();
        if (catchAllHandler == null) {
            catchAllHandler = new InsCatchAllHandler();
            addChild(2, catchAllHandler);
            this.catchAllHandler = catchAllHandler;
        }
        return catchAllHandler;
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        updateCount();
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        int maxPosition = reader.getPosition();

        int position = getHandlerOffsetArray().getItemsStart()
                + getHandlerOffset().getOffset();
        reader.seek(position);
        this.handlersCount.readBytes(reader);
        int count = this.handlersCount.get();
        boolean hasCatchAll = false;
        if (count <= 0) {
            count = -count;
            hasCatchAll = true;
        }
        BlockList<InsCatchTypedHandler> handlerList = this.getCatchTypedHandlerBlockList();
        handlerList.ensureCapacity(count);
        for (int i = 0; i < count; i++) {
            InsCatchTypedHandler handler = new InsCatchTypedHandler();
            handlerList.add(handler);
            handler.readBytes(reader);
        }
        if (hasCatchAll) {
            initCatchAllHandler().readBytes(reader);
        }
        if (maxPosition > reader.getPosition()) {
            // Should never reach here
            reader.seek(maxPosition);
        }
    }
    @Override
    public void onCountUpTo(BlockCounter counter) {
        if (counter.FOUND) {
            return;
        }
        Block end = counter.END;
        if (end instanceof Compact) {
            InsTryItem tryItem = ((Compact) end).getTryItem();
            if (tryItem == this) {
                counter.FOUND = true;
                return;
            }
        }
        super.onCountUpTo(counter);
    }
    public void removeSelf() {
        InsTryBlock tryBlock = getTryBlock();
        if (tryBlock != null) {
            tryBlock.remove(this);
        }
    }
    public boolean isRemoved() {
        if (getParent() == null) {
            return true;
        }
        InsTryBlock tryBlock = getTryBlock();
        return tryBlock == null || tryBlock.getParent() == null;
    }
    public void remove(InsExceptionHandler handler) {
        if (handler == null) {
            return;
        }
        if (handler == this.catchAllHandler) {
            handler.onRemove();
            this.catchAllHandler = null;
        }else if (handler instanceof InsCatchTypedHandler && this.catchTypedHandlerList != null) {
            if (catchTypedHandlerList.contains(handler)) {
                catchTypedHandlerList.remove((InsCatchTypedHandler) handler);
                handler.onRemove();
            }
        }
    }
    public void onRemove() {
        HandlerOffset handlerOffset = this.mHandlerOffset;
        BlockList<InsCatchTypedHandler> list = this.catchTypedHandlerList;
        if (list != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                InsCatchTypedHandler handler = list.get(i);
                handler.onRemove();
                handler.setParent(null);
            }
            list.destroy();
        }
        remove(this.catchAllHandler);
        if (handlerOffset != null) {
            this.mHandlerOffset = null;
            handlerOffset.removeSelf();
        }
        setParent(null);
    }
    public void merge(InsTryItem tryItem) {
        mergeOffset(tryItem);
        mergeHandlers(tryItem);
    }
    public InsCatchTypedHandler createNext() {
        BlockList<InsCatchTypedHandler> handlerList = this.getCatchTypedHandlerBlockList();
        InsCatchTypedHandler handler = new InsCatchTypedHandler();
        handlerList.add(handler);
        updateCount();
        return handler;
    }
    void mergeHandlers(InsTryItem tryItem) {
        BlockList<InsCatchTypedHandler> comingList = tryItem.getCatchTypedHandlerBlockList();
        int size = comingList.size();
        BlockList<InsCatchTypedHandler> handlerList = this.getCatchTypedHandlerBlockList();
        handlerList.ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            InsCatchTypedHandler coming = comingList.get(i);
            InsCatchTypedHandler handler = new InsCatchTypedHandler();
            handlerList.add(handler);
            handler.merge(coming);
        }
        if (tryItem.hasCatchAllHandler()) {
            initCatchAllHandler().merge(tryItem.getCatchAllHandler());
        }
        updateCount();
    }
    void mergeHandler(InsExceptionHandler handler) {
        InsExceptionHandler newHandler;
        if (handler instanceof InsCatchTypedHandler) {
            newHandler = createNext();
        } else {
            newHandler = getOrCreateCatchAll();
        }
        newHandler.merge(handler);
    }
    void mergeOffset(InsTryItem tryItem) {

        HandlerOffset coming = tryItem.getHandlerOffset();
        HandlerOffset handlerOffset = getHandlerOffset();

        handlerOffset.setCatchCodeUnit(coming.getCatchCodeUnit());
        handlerOffset.setStartAddress(coming.getStartAddress());
    }
    public void fromProgram(TryItem tryItem) {
        setStartAddress(tryItem.getStartAddress());
        BlockList<InsCatchTypedHandler> handlerList = this.getCatchTypedHandlerBlockList();
        int count = tryItem.getCatchTypedHandlersCount();
        for (int i = 0; i < count; i++) {
            InsCatchTypedHandler handler = new InsCatchTypedHandler();
            handlerList.add(handler);
            handler.fromProgram(tryItem.getCatchTypedHandler(i));
        }
        ExceptionHandler handler = tryItem.getCatchAllHandler();
        if (handler != null) {
            initCatchAllHandler().fromProgram(handler);
        }
        updateCount();
    }

    @Override
    public boolean uses(Key key) {
        Iterator<InsCatchTypedHandler> iterator = getCatchTypedHandlers();
        while (iterator.hasNext()) {
            TypeKey handler = iterator.next().getKey();
            if (handler != null && handler.uses(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<IdItem> usedIds() {
        return ComputeIterator.of(getCatchTypedHandlers(),
                InsCatchTypedHandler::getTypeId);
    }

    @Override
    public int compareTo(InsTryItem tryItem) {
        if (tryItem == this) {
            return 0;
        }
        int i = CompareUtil.compare(getStartAddress(), tryItem.getStartAddress());
        if (i != 0) {
            return i;
        }
        boolean compact = this.isCompact();
        boolean compactOther = tryItem.isCompact();
        i = CompareUtil.compare(compact, compactOther);
        if (i != 0) {
            return i;
        }
        if (compact) {
            return this.getTryItem().compareTo(tryItem.getTryItem());
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InsTryItem tryItem = (InsTryItem) obj;
        return ObjectsUtil.equals(getCatchTypedHandlerBlockList(),
                tryItem.getCatchTypedHandlerBlockList()) &&
                ObjectsUtil.equals(getCatchAllHandler(), tryItem.getCatchAllHandler());
    }

    @Override
    public int hashCode() {
        return ObjectsUtil.hash(this.getCatchTypedHandlerBlockList(),
                this.getCatchAllHandler());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<InsExceptionHandler> handlers = handlers();
        while (handlers.hasNext()) {
            if (builder.length() != 0) {
                builder.append('\n');
            }
            builder.append(handlers.next());
        }
        return builder.toString();
    }
    static class Compact extends InsTryItem {

        private final InsTryItem tryItem;

        public Compact(InsTryItem tryItem) {
            super();
            this.tryItem = tryItem;
        }

        @Override
        public boolean isCompact() {
            return true;
        }
        @Override
        InsTryBlock getTryBlock() {
            return tryItem.getTryBlock();
        }

        @Override
        InsTryItem newCompact() {
            return tryItem.newCompact();
        }

        @Override
        public boolean compactWith(InsTryItem similar) {
            return false;
        }
        @Override
        public boolean flatten() {
            InsTryBlock tryBlock = getTryBlock();
            if (tryBlock != null) {
                InsTryItem self = this;
                int index = self.getIndex();
                InsTryItem replace = tryBlock.createNext();
                replace.merge(self);
                tryBlock.remove(self);
                tryBlock.moveTo(replace, index);
                replace.refresh();
                return true;
            }
            return false;
        }
        @Override
        HandlerOffsetArray getHandlerOffsetArray() {
            return tryItem.getHandlerOffsetArray();
        }
        @Override
        Iterator<InsCatchTypedHandler> getCatchTypedHandlers() {
            Iterator<InsCatchTypedHandler> iterator = getCatchTypedHandlerBlockList()
                    .iterator();
            final InsTryItem parent = this;
            return ComputeIterator.of(iterator, handler -> handler.newCompact(parent));
        }
        @Override
        public InsCatchTypedHandler getCatchTypedHandler(int i) {
            return super.getCatchTypedHandler(i).newCompact(this);
        }

        @Override
        BlockList<InsCatchTypedHandler> getCatchTypedHandlerBlockList() {
            return tryItem.getCatchTypedHandlerBlockList();
        }
        @Override
        InsTryItem getTryItem() {
            return tryItem.getTryItem();
        }
        @Override
        public InsCatchAllHandler getCatchAllHandler() {
            InsCatchAllHandler catchAllHandler = tryItem.getCatchAllHandler();
            if (catchAllHandler != null) {
                catchAllHandler = catchAllHandler.newCompact(this);
            }
            return catchAllHandler;
        }

        @Override
        public InsCatchAllHandler getOrCreateCatchAll() {
            tryItem.getOrCreateCatchAll();
            return getCatchAllHandler();
        }

        @Override
        public int countBytes() {
            return 0;
        }
        @Override
        public int onWriteBytes(OutputStream stream) throws IOException {
            return 0;
        }
        @Override
        public byte[] getBytes() {
            return null;
        }
        @Override
        protected void onPreRefresh() {
        }
        @Override
        protected void onRefreshed() {
        }
        @Override
        public void onReadBytes(BlockReader reader) throws IOException {
        }
        @Override
        void updateCount() {
        }
        @Override
        void mergeHandlers(InsTryItem tryItem) {
        }
        @Override
        void mergeHandler(InsExceptionHandler handler) {
        }

        @Override
        public int hashCode() {
            return ObjectsUtil.hash(getClass(), super.hashCode());
        }

        @Override
        public String toString() {
            if (getParent() == null) {
                return "NULL";
            }
            return super.toString();
        }
    }
}
