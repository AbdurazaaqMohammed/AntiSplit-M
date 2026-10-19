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

import com.reandroid.dex.program.InstructionLabel;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.ArrayCollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SequentialLabelFactory {

    private final Map<String, String> labelMap;

    public SequentialLabelFactory() {
        this.labelMap = new HashMap<>();
    }

    public void build(Iterator<? extends InstructionLabel> iterator) {
        reset();
        Map<String, Set<String>> groupMap = new HashMap<>();
        while (iterator.hasNext()) {
            String name = iterator.next().getLabelName();
            String key = dropSuffix(name);
            if(key == null) {
                continue;
            }
            Set<String> group = groupMap.get(key);
            if(group == null) {
                group = new HashSet<>();
                groupMap.put(key, group);
            }
            group.add(name);
        }
        for(Map.Entry<String, Set<String>> entry : groupMap.entrySet()) {
            build(entry.getKey(), entry.getValue());
        }
    }
    private void build(String key, Set<String> group) {
        Map<String, String> labelMap = this.labelMap;
        List<String> list = new ArrayCollection<>(group);
        java.util.Collections.sort(list, (s, t1) -> CompareUtil.compare(getLabelSuffix(s), getLabelSuffix(t1)));
        int size = list.size();
        for(int i = 0; i < size; i++) {
            labelMap.put(list.get(i), HexUtil.toHex(key, i, 1));
        }
    }
    private String dropSuffix(String name) {
        if(name.length() != 0 && name.charAt(0) == ':') {
            int i = name.lastIndexOf('_');
            if(i > 0) {
                return name.substring(0, i + 1);
            }
        }
        return null;
    }
    static int getLabelSuffix(String name) {
        if(name.length() != 0 && name.charAt(0) == ':') {
            int i = name.lastIndexOf('_');
            if(i > 0) {
                return HexUtil.decodeHex(name.substring(i + 1), 0);
            }
        }
        return 0;
    }
    public void reset() {
        labelMap.clear();
    }
    public String get(String label) {
        String result = labelMap.get(label);
        if(result == null) {
            return label;
        }
        return result;
    }
}
