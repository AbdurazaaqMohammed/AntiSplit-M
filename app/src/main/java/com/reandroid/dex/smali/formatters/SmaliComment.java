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
package com.reandroid.dex.smali.formatters;

import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.DexDeclaration;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.SmaliWriterSetting;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public interface SmaliComment {

    static void writeDeclarationComment(SmaliWriter writer,
                                        String label,
                                        Iterator<? extends DexDeclaration> iterator) throws IOException {

        List<TypeKey> typeKeyList = CollectionUtil.toList(
                ComputeIterator.of(iterator, DexDeclaration::getDefining));

        java.util.Collections.sort(typeKeyList, CompareUtil.getComparableComparator());

        int size = typeKeyList.size();
        int remaining = 0;
        SmaliWriterSetting setting = writer.getWriterSetting();
        if (setting != null) {
            int max = setting.getMaximumCommentLines();
            if (max >= 0 && max < size) {
                if (max != 0) {
                    remaining = size - max;
                }
                size = max;
            }
        }

        for (int i = 0; i < size; i++) {
            writer.newLine();
            TypeKey typeKey = typeKeyList.get(i);
            writer.appendComment(label);
            writer.appendComment(typeKey.getTypeName());
        }

        if (remaining != 0) {
            writer.newLine();
            writer.appendComment(label + " +" + remaining + " more");
        }
    }
}
