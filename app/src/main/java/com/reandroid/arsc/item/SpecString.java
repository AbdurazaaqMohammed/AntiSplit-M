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
package com.reandroid.arsc.item;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.value.Entry;
import com.reandroid.utils.CompareUtil;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class SpecString extends StringItem {
    public SpecString(boolean utf8) {
        super(utf8);
    }

    public int resolveResourceId(String typeName){
        Iterator<Entry> itr = getEntries(typeName);
        if(itr.hasNext()){
            return itr.next().getResourceId();
        }
        return 0;
    }

    public Iterator<Entry> getEntries(Predicate<Entry> filter){
        return getUsers(Entry.class, filter);
    }
    public Iterator<Entry> getEntries(final int typeId){
        return getUsers(Entry.class, new Predicate<Entry>() {
            @Override
            public boolean test(Entry item) {
                return typeId == item.getTypeId();
            }
        });
    }
    public Iterator<Entry> getEntries(final String typeName){
        return getUsers(Entry.class, new Predicate<Entry>() {
            @Override
            public boolean test(Entry item) {
                return typeName == null
                        || typeName.equals(item.getTypeName());
            }
        });
    }
    public Iterator<Entry> getEntries(final Block parentContext){
        return getUsers(Entry.class, new Predicate<Entry>() {
            @Override
            public boolean test(Entry item) {
                return item.getParentInstance(parentContext.getClass())
                        == parentContext;
            }
        });
    }
    @Override
    public StyleItem getOrCreateStyle(){
        // Spec (resource name) don't have style unless to obfuscate/confuse other decompilers
        return null;
    }
    @Override
    public int compareStringValue(StringItem stringItem) {
        if (stringItem == null) {
            return -1;
        }
        if (stringItem == this) {
            return 0;
        }
        return CompareUtil.compare(this.get(), stringItem.get());
    }
}
