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
package com.reandroid.identifiers;

import com.reandroid.arsc.coder.xml.XmlCoder;
import com.reandroid.utils.HexUtil;

public class Identifier implements Comparable<Identifier>{
    private int id;
    private String name;
    private Identifier mParent;
    private Object mTag;
    public Identifier(int id, String name){
        this.id = id;
        this.name = name;
    }

    public Object getTag() {
        return mTag;
    }
    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    Identifier getParent() {
        return mParent;
    }
    void setParent(Identifier parent) {
        if(parent == this){
            return;
        }
        this.mParent = parent;
    }
    public String getHexId(){
        return HexUtil.toHex2((byte) getId());
    }
    long getUniqueId(){
        return getId();
    }

    @Override
    public int compareTo(Identifier identifier) {
        return Long.compare(getUniqueId(), identifier.getUniqueId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Identifier other = (Identifier) obj;
        return this.getUniqueId() == other.getUniqueId();
    }
    @Override
    public int hashCode() {
        return Long.hashCode(getUniqueId());
    }
    @Override
    public String toString(){
        return getName() + "(" + getHexId() + ")";
    }

    public static boolean isAapt() {
        // TODO: make separate setting
        return XmlCoder.getInstance().getSetting().isAapt();
    }
    static final String XML_TAG_RESOURCES = "resources";
    static final String XML_TAG_PUBLIC = "public";

    static final String XML_ATTRIBUTE_ID = "id";
    static final String XML_ATTRIBUTE_NAME = "name";
    static final String XML_ATTRIBUTE_PACKAGE = "package";
    static final String XML_ATTRIBUTE_TYPE = "type";

}
