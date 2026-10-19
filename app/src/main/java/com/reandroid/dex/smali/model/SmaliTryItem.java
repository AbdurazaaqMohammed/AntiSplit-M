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
package com.reandroid.dex.smali.model;

import com.reandroid.dex.program.ExceptionHandler;
import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.dex.program.InstructionLabelSet;
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.program.TryItem;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.util.Iterator;

public class SmaliTryItem extends SmaliCode implements TryItem,
        InstructionLabel, InstructionLabelSet {

    private final SmaliSet<SmaliCatchTypedHandler> catchTypedSet;
    private SmaliCatchAllHandler catchAllHandler;

    public SmaliTryItem() {
        super();
        this.catchTypedSet = new SmaliSet<>();
        this.catchTypedSet.setParent(this);
    }

    @Override
    public Iterator<SmaliExceptionHandler> handlers() {
        return CombiningIterator.two(getCatchTypedSet().iterator(),
                SingleIterator.of(getCatchAllHandler()));
    }
    @Override
    public int getStartAddress() {
        SmaliExceptionHandler handler = getFirstHandler();
        if (handler != null) {
            return handler.getStartLabel().getTargetAddress();
        }
        return -1;
    }
    public int getAddress() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            SmaliInstruction instruction = codeSet.getNextInstruction(getIndex());
            if (instruction != null) {
                return instruction.getAddress();
            }
        }
        return -1;
    }
    @Override
    public int getCatchTypedHandlersCount() {
        return getCatchTypedSet().size();
    }
    @Override
    public SmaliCatchTypedHandler getCatchTypedHandler(int i) {
        return getCatchTypedSet().get(i);
    }
    @Override
    public SmaliCatchAllHandler getCatchAllHandler() {
        return catchAllHandler;
    }
    public void setCatchAllHandler(SmaliCatchAllHandler catchAllHandler) {
        SmaliCatchAllHandler current = this.catchAllHandler;
        if (catchAllHandler == current) {
            return;
        }
        this.catchAllHandler = catchAllHandler;
        if (catchAllHandler != null) {
            catchAllHandler.setParent(this);
        }
        if (current != null) {
            current.setParent(null);
        }
    }
    public SmaliCatchAllHandler getOrCreateCatchAllHandler() {
        SmaliCatchAllHandler handler = getCatchAllHandler();
        if (handler == null) {
            handler = new SmaliCatchAllHandler();
            setCatchAllHandler(handler);
        }
        return handler;
    }
    public SmaliSet<SmaliCatchTypedHandler> getCatchTypedSet() {
        return catchTypedSet;
    }

    public SmaliLabel getStartLabel() {
        SmaliExceptionHandler handler = getFirstHandler();
        if (handler != null) {
            return handler.getStartLabel().getDestinationLabel();
        }
        return null;
    }
    public SmaliLabel getEndLabel() {
        SmaliExceptionHandler handler = getFirstHandler();
        if (handler != null) {
            return handler.getEndLabel().getDestinationLabel();
        }
        return null;
    }

    @Override
    public int getTargetAddress() {
        return getAddress();
    }

    @Override
    public void setTargetAddress(int address) {
        throw new RuntimeException("Method not implemented");
    }

    @Override
    public SmaliInstruction getTargetInstruction() {
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet != null) {
            return codeSet.getNextInstruction(getIndex());
        }
        return null;
    }
    @Override
    public String getLabelName() {
        StringBuilder builder = new StringBuilder();
        boolean append = false;
        Iterator<SmaliExceptionHandler> iterator = handlers();
        while (iterator.hasNext()) {
            SmaliExceptionHandler handler = iterator.next();
            if (append) {
                builder.append('\n');
            }
            builder.append(handler.getLabelName());
            append = true;
        }
        return builder.toString();
    }
    @Override
    public InstructionLabelType getLabelType() {
        if (getCatchAllHandler() != null) {
            return InstructionLabelType.CATCH_ALL_HANDLER;
        }
        return InstructionLabelType.CATCH_HANDLER;
    }
    @Override
    public int compareLabel(InstructionLabel label) {
        return getLabelType().compareTo(label.getLabelType());
    }

    private SmaliExceptionHandler getFirstHandler() {
        SmaliExceptionHandler handler = getCatchTypedSet().getFirst();
        if (handler == null) {
            handler = getCatchAllHandler();
        }
        return handler;
    }

    /** Separates multiple exception handlers
     *  and keeps only one handler per try item
     */
    public boolean flatten() {
        SmaliSet<SmaliCatchTypedHandler> catchSet = getCatchTypedSet();
        SmaliCatchAllHandler catchAll = getCatchAllHandler();
        int count = catchSet.size();
        if (catchAll != null) {
            count = count + 1;
        }
        if (count < 2) {
            return false;
        }
        SmaliTryItem previous = this;
        while (catchSet.size() > 1) {
            int i = catchSet.size() - 1;
            SmaliCatchTypedHandler handler = catchSet.get(i);
            previous = flatten(previous, handler);
            catchSet.remove(i);
        }
        if (catchAll != null) {
            flatten(previous, catchAll);
            setCatchAllHandler(null);
        }
        return true;
    }
    private SmaliTryItem flatten(SmaliTryItem previous, SmaliExceptionHandler handler) {
        SmaliCodeSet codeSet = getCodeSet();

        SmaliTryItem tryItem = new SmaliTryItem();
        SmaliLabel start = previous.getStartLabel();
        SmaliLabel end = previous.getEndLabel();
        SmaliLabel catchLabel = handler.getCatchLabel()
                .getDestinationLabel();

        start = codeSet.newLabelAtIndex(start.getIndex() + 1, start.getLabelType());
        end = codeSet.newLabelAtIndex(end.getIndex() + 1, end.getLabelType());
        catchLabel = codeSet.newLabelAtIndex(catchLabel.getIndex() + 1,
                catchLabel.getLabelType());

        SmaliExceptionHandler resultHandler;
        if (handler instanceof SmaliCatchAllHandler) {
            SmaliCatchAllHandler catchAll = new SmaliCatchAllHandler();
            tryItem.setCatchAllHandler(catchAll);
            resultHandler = catchAll;
        } else {
            SmaliCatchTypedHandler codeCatch = new SmaliCatchTypedHandler();
            codeCatch.setKey(handler.getKey());
            tryItem.getCatchTypedSet().add(codeCatch);
            resultHandler = codeCatch;
        }
        resultHandler.setLabelNames(start, end, catchLabel);

        codeSet.add(previous.getIndex() + 1, tryItem);
        return tryItem;
    }

    public SmaliTryItem splitAtIndex(Smali indexEnd) {
        SmaliCodeSet codeSet = getCodeSet();

        SmaliLabel startLabel = getStartLabel();

        SmaliTryItem splitItem = new SmaliTryItem();

        SmaliLabel startLabelSplit = codeSet.newLabelAtIndex(
                startLabel.getIndex(), InstructionLabelType.TRY_START);

        SmaliLabel endLabelSplit = codeSet.newLabelAtIndex(
                indexEnd.getIndex(), InstructionLabelType.TRY_END);

        Iterator<SmaliCatchTypedHandler> iterator = getCatchTypedSet().iterator();
        while (iterator.hasNext()) {
            SmaliCatchTypedHandler handler = iterator.next();

            SmaliCatchTypedHandler handlerSplit = new SmaliCatchTypedHandler();
            handlerSplit.setLabelNames(startLabelSplit, endLabelSplit,
                    handler.getCatchLabel());
            handlerSplit.setKey(handler.getKey());

            splitItem.getCatchTypedSet().add(handlerSplit);
        }

        SmaliCatchAllHandler catchAll = getCatchAllHandler();
        if (catchAll != null) {
            SmaliCatchAllHandler catchAllSplit = new SmaliCatchAllHandler();
            catchAllSplit.setLabelNames(startLabelSplit,
                    endLabelSplit, catchAll.getCatchLabel());
            splitItem.setCatchAllHandler(catchAllSplit);
        }
        codeSet.add(lastIndexOfHandler(codeSet, endLabelSplit.getIndex() + 1), splitItem);
        codeSet.moveTo(startLabel, splitItem.getIndex());

        return splitItem;
    }
    private int lastIndexOfHandler(SmaliCodeSet codeSet, int fromIndex) {
        int index = fromIndex;
        int size = codeSet.size();
        for (int i = fromIndex; i < size; i++) {
            SmaliCode code = codeSet.get(i);
            if (code instanceof SmaliInstruction) {
                break;
            }
            if (code instanceof SmaliTryItem) {
                index ++;
            }
        }
        return index;
    }

    public boolean compact() {
        boolean result = false;
        SmaliCodeSet codeSet = getCodeSet();
        int index = this.getIndex() + 1;
        SmaliInstruction end = codeSet.getNextInstruction(index);
        for (int i = index; i < end.getIndex(); i++) {
            SmaliCode code = codeSet.get(i);
            if (code != this && code instanceof SmaliTryItem) {
                if (compactWith((SmaliTryItem) code)) {
                    result = true;
                    i --;
                } else {
                    break;
                }
            }
        }
        return result;
    }
    public boolean compactWith(SmaliTryItem tryItem) {
        if (tryItem == this || !canCompactWith(tryItem)) {
            return false;
        }
        SmaliCodeSet codeSet = getCodeSet();
        if (codeSet == null) {
            return false;
        }
        SmaliSet<SmaliCatchTypedHandler> set = this.getCatchTypedSet();
        SmaliSet<SmaliCatchTypedHandler> sourceSet = tryItem.getCatchTypedSet();
        while (sourceSet.size() != 0) {
            set.add(sourceSet.remove(0));
        }
        SmaliCatchAllHandler sourceCatchAll = tryItem.getCatchAllHandler();
        if (sourceCatchAll != null) {
            setCatchAllHandler(sourceCatchAll);
            tryItem.setCatchAllHandler(null);
        }
        return codeSet.remove(tryItem);
    }
    private boolean canCompactWith(SmaliTryItem tryItem) {
        return tryItem != null
                && tryItem != this
                && tryItem.getParent() == this.getParent()
                && tryItem.getIndex() >= this.getIndex()
                && tryItem.getAddress() == this.getAddress()
                && tryItem.getStartAddress() == this.getStartAddress();
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendAll(getCatchTypedSet().iterator());
        SmaliCatchAllHandler catchAll = getCatchAllHandler();
        if (catchAll != null) {
            writer.newLine();
            catchAll.append(writer);
        }
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        parseCatches(reader);
        parseCatchAll(reader);
    }
    private void parseCatches(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliSet<SmaliCatchTypedHandler> catchSet = getCatchTypedSet();
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        while (directive == SmaliDirective.CATCH) {
            int position = reader.position();
            SmaliCatchTypedHandler codeCatch = new SmaliCatchTypedHandler();
            codeCatch.parse(reader);
            if (isDifferentGroup(codeCatch)) {
                reader.position(position);
                break;
            }
            catchSet.add(codeCatch);
            reader.skipWhitespacesOrComment();
            directive = SmaliDirective.parse(reader, false);
        }
    }
    private void parseCatchAll(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        if (directive == SmaliDirective.CATCH_ALL) {
            SmaliCatchAllHandler catchAll = new SmaliCatchAllHandler();
            int position = reader.position();
            catchAll.parse(reader);
            if (isDifferentGroup(catchAll)) {
                reader.position(position);
            } else {
                setCatchAllHandler(catchAll);
            }
        }
    }
    private boolean isDifferentGroup(SmaliExceptionHandler exceptionHandler) {
        SmaliExceptionHandler handler = getFirstHandler();
        return handler != null && !handler.getStartLabel()
                .equals(exceptionHandler.getStartLabel());
    }

    public void fromProgram(TryItem tryItem) {
        SmaliSet<SmaliCatchTypedHandler> catchTypedSet = this.getCatchTypedSet();
        int count = tryItem.getCatchTypedHandlersCount();
        for (int i = 0; i < count; i++) {
            SmaliCatchTypedHandler handler = new SmaliCatchTypedHandler();
            catchTypedSet.add(handler);
            handler.fromProgram(tryItem.getCatchTypedHandler(i));
        }
        ExceptionHandler handler = tryItem.getCatchAllHandler();
        if (handler != null) {
            getOrCreateCatchAllHandler().fromProgram(handler);
        }
    }
    public boolean fromProgram(ExceptionHandler handler) {
        SmaliExceptionHandler smaliHandler;
        if (handler.isCatchAll()) {
            smaliHandler = new SmaliCatchAllHandler();
        } else {
            smaliHandler = new SmaliCatchTypedHandler();
        }
        smaliHandler.fromProgram(handler);
        if (!isDifferentGroup(smaliHandler)) {
            return false;
        }
        if (smaliHandler instanceof SmaliCatchAllHandler) {
            setCatchAllHandler((SmaliCatchAllHandler) smaliHandler);
        } else {
            getCatchTypedSet().add((SmaliCatchTypedHandler) smaliHandler);
        }
        return true;
    }

    @Override
    public void validate() throws IOException {
        super.validate();
        getCatchTypedSet().validate();
        SmaliCatchAllHandler catchAllHandler = getCatchAllHandler();
        if (catchAllHandler != null) {
            catchAllHandler.validate();
        }
    }
}
