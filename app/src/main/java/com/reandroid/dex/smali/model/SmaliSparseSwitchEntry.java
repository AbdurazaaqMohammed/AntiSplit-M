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

import com.reandroid.dex.program.InstructionLabelType;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;

import java.io.IOException;

public class SmaliSparseSwitchEntry extends SmaliSwitchEntry {

    private final SmaliValueInteger smaliValue;

    public SmaliSparseSwitchEntry() {
        super(InstructionLabelType.S_SWITCH_DATA);
        this.smaliValue = new SmaliValueInteger();
        this.smaliValue.setParent(this);
    }

    public int getValue() {
        return this.smaliValue.getValue();
    }
    public void setValue(int value) {
        this.smaliValue.setValue(value);
    }

    public SmaliValueInteger getSmaliValue() {
        return smaliValue;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendOptional(getSmaliValue());
        writer.append(" -> ");
        super.append(writer);
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        getSmaliValue().parse(reader);
        reader.skipSpaces();
        SmaliParseException.expect(reader, '-');
        SmaliParseException.expect(reader, '>');
        reader.skipSpaces();
        super.parse(reader);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SmaliSparseSwitchEntry)) {
            return true;
        }
        return getSmaliValue().equals(((SmaliSparseSwitchEntry) obj).getSmaliValue());
    }
}
