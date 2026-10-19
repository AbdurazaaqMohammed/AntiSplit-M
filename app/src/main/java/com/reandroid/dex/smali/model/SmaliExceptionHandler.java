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
import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliRegion;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.collection.ArrayIterator;

import java.io.IOException;
import java.util.Iterator;

public abstract class SmaliExceptionHandler extends SmaliCode implements
        ExceptionHandler, SmaliRegion, Iterable<InstructionLabel> {

    private final SmaliLabel startLabel;
    private final SmaliLabel endLabel;
    private final SmaliLabel catchLabel;

    private final Object[] labels;

    public SmaliExceptionHandler() {
        super();
        this.startLabel = new SmaliLabelSource(InstructionLabelType.TRY_START);
        this.endLabel = new SmaliLabelSource(InstructionLabelType.TRY_END);
        final SmaliExceptionHandler handler = this;
        this.catchLabel = new SmaliLabelSource() {
            @Override
            public InstructionLabelType getLabelType() {
                InstructionLabelType type = handler.getLabelType();
                if (type == InstructionLabelType.CATCH_HANDLER) {
                    return InstructionLabelType.CATCH;
                }
                return InstructionLabelType.CATCH_ALL;
            }
        };
        this.labels = new Object[] {startLabel, endLabel, handler, catchLabel};

        this.startLabel.setParent(this);
        this.endLabel.setParent(this);
        this.catchLabel.setParent(this);
    }

    @Override
    public int getTargetAddress() {
        SmaliTryItem tryItem = getTryItem();
        if (tryItem == null) {
            return -1;
        }
        return tryItem.getAddress();
    }
    @Override
    public void setTargetAddress(int address) {
        throw new RuntimeException("Method not implemented");
    }
    @Override
    public SmaliLabel getStartLabel() {
        return startLabel;
    }
    @Override
    public SmaliLabel getEndLabel() {
        return endLabel;
    }

    @Override
    public SmaliLabel getCatchLabel() {
        return catchLabel;
    }
    @Override
    public Iterator<InstructionLabel> getLabels() {
        return iterator();
    }
    @Override
    public Iterator<InstructionLabel> iterator() {
        return ArrayIterator.of(labels);
    }


    public void setLabelNames(String start, String end, String catchLabel) {
        getStartLabel().setLabelName(start);
        getEndLabel().setLabelName(end);
        getCatchLabel().setLabelName(catchLabel);
    }
    public void setLabelNames(SmaliLabel start, SmaliLabel end, SmaliLabel catchLabel) {
        setLabelNames(start.getLabelName(), end.getLabelName(), catchLabel.getLabelName());
    }

    SmaliTryItem getTryItem() {
        return getParentInstance(SmaliTryItem.class);
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        getSmaliDirective().append(writer);
        appendType(writer);
        writer.append('{');
        getStartLabel().append(writer);
        writer.append(" .. ");
        getEndLabel().append(writer);
        writer.append('}');
        writer.append(' ');
        getCatchLabel().append(writer);
    }
    protected void appendType(SmaliWriter writer) throws IOException {

    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespaces();
        SmaliParseException.expect(reader, getSmaliDirective());
        parseType(reader);
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, '{');
        reader.skipWhitespaces();
        getStartLabel().parse(reader);
        reader.skipWhitespaces();
        SmaliParseException.expect(reader, '.');
        SmaliParseException.expect(reader, '.');
        getEndLabel().parse(reader);
        reader.skipWhitespaces();
        SmaliParseException.expect(reader, '}');
        getCatchLabel().parse(reader);
    }

    protected void parseType(SmaliReader reader) throws IOException {
    }

    public void fromProgram(ExceptionHandler handler) {
        if (isCatchAll() != handler.isCatchAll()) {
            throw new IllegalArgumentException("Mismatch catch handler");
        }
        getStartLabel().setLabelName(handler.getStartLabel().getLabelName());
        getEndLabel().setLabelName(handler.getEndLabel().getLabelName());
        getCatchLabel().setLabelName(handler.getCatchLabel().getLabelName());
    }

    @Override
    public void validate() throws IOException {
        getStartLabel().validate();
        getEndLabel().validate();
        getCatchLabel().validate();
    }

    @Override
    public String toString() {
        return getLabelName();
    }
}
