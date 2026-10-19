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

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.arsc.item.TypeString;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.dex.data.FieldDef;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.PrimitiveKey;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.EmptyIterator;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.reandroid.utils.function.Predicate;

public class RField extends DexField implements Comparable<RField> {

    public RField(RClass rClass, FieldDef fieldDef) {
        super(rClass, fieldDef);
    }

    public void serializePublicXml(XmlSerializer serializer) throws IOException {
        ResourceEntry resourceEntry = toResourceEntry();
        resourceEntry.serializePublicXml(serializer);
    }
    public String toJavaDeclare() {
        return toJavaDeclare(true);
    }
    public String toJavaDeclare(boolean makeFinal) {
        String name = getName();
        StringBuilder builder = new StringBuilder(38 + name.length());
        builder.append("public static ");
        if(makeFinal){
            builder.append("int ");
        }
        builder.append(getName());
        builder.append(" = ");
        builder.append(HexUtil.toHex8(getResourceId()));
        builder.append(';');
        return builder.toString();
    }
    public ResourceEntry toResourceEntry() {
        return new DexResourceEntry(this);
    }
    public int getResourceId() {
        IntegerReference reference = getStaticIntegerValue();
        if (reference != null) {
            return reference.get();
        }
        return 0;
    }
    public void setResourceId(int resourceId) {
        setStaticValue(PrimitiveKey.of(resourceId));
    }
    public String getResourceName(){
        String name = getName();
        if(TypeString.isTypeStyle(getResourceType())) {
            name = toStyleResourceName(name);
        }
        return name;
    }
    public String getResourceType(){
        return getDexClass().getResourceType();
    }
    @Override
    public RClass getDexClass() {
        return (RClass) super.getDexClass();
    }

    @Override
    public int compareTo(RField rField) {
        if(rField == null) {
            return -1;
        }
        return Integer.compare(getResourceId(), rField.getResourceId());
    }

    @Override
    public int hashCode() {
        return getResourceId();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RField rField = (RField) obj;
        return getResourceId() == rField.getResourceId();
    }
    @Override
    public String toString() {
        return toJavaDeclare();
    }

    static boolean isResourceIdValue(Key key) {
        if(key instanceof PrimitiveKey){
            PrimitiveKey primitiveKey = (PrimitiveKey) key;
            if (primitiveKey.isInteger()) {
                return PackageBlock.isResourceId(((PrimitiveKey.IntegerKey)primitiveKey).value());
            }
        }
        return false;
    }

    public static String toStyleResourceName(String fieldName) {
        int length = fieldName.length();
        if(length < 3 || fieldName.indexOf('_') < 0){
            return fieldName;
        }
        StringBuilder builder = new StringBuilder(length);
        length = length - 1;
        for(int i = 0; i < length; i++){
            char ch = fieldName.charAt(i);
            if(ch == '_'){
                char next = fieldName.charAt(i + 1);
                if(next <= 'Z' && next >= 'A'){
                    ch = '.';
                }
            }
            builder.append(ch);
        }
        builder.append(fieldName.charAt(length));
        return builder.toString();
    }
    public static String sanitizeResourceName(String resourceName) {
        if(resourceName.charAt(0) == '$'){
            resourceName = resourceName.substring(1);
        }
        resourceName = resourceName.replace('.', '_');
        return resourceName;
    }
    public static Map<Integer, RField> mapRFields(Iterator<RField> iterator) {
        Map<Integer, RField> map = new HashMap<>();
        while (iterator.hasNext()){
            RField rField = iterator.next();
            map.put(rField.getResourceId(), rField);
        }
        return map;
    }

    public static class DexResourceEntry extends ResourceEntry {

        private final RField rField;

        public DexResourceEntry(RField rField) {
            super(RClass.EMPTY_TABLE.pickOrEmptyPackage(), rField.getResourceId());
            this.rField = rField;
        }

        @Override
        public String getType() {
            return rField.getResourceType();
        }
        @Override
        public String getName() {
            return getRField().getResourceName();
        }
        @Override
        public int getResourceId() {
            return getRField().getResourceId();
        }
        @Override
        public PackageBlock getPackageBlock() {
            return RClass.EMPTY_TABLE.pickOrEmptyPackage();
        }
        @Override
        public String getPackageName(){
            return null;
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
        @Override
        public Iterator<Entry> iterator(boolean skipNull) {
            return EmptyIterator.of();
        }
        @Override
        public Iterator<Entry> iterator(Predicate<? super Entry> filter) {
            return EmptyIterator.of();
        }
        public RField getRField() {
            return rField;
        }
    }
}
