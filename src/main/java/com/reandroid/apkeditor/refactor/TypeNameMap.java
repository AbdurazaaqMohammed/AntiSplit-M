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
package com.reandroid.apkeditor.refactor;

import com.reandroid.xml.XMLDocument;
import com.reandroid.xml.XMLElement;

import java.util.*;

public class TypeNameMap implements Comparator<TypeNameMap.TypeName> {
    private final Map<Integer, TypeName> map;
    public TypeNameMap(){
        this.map=new HashMap<>();
    }
    public boolean contains(String name){
        for(TypeName typeName:map.values()){
            if(Objects.equals(name, typeName.getName())){
                return true;
            }
        }
        return false;
    }
    public boolean contains(int id){
        id=id&0xffff0000;
        return this.map.containsKey(id);
    }
    public XMLDocument toXMLDocument(){
        XMLDocument xmlDocument=new XMLDocument();
        XMLElement documentElement=new XMLElement("resources");
        xmlDocument.setDocumentElement(documentElement);
        for(TypeName typeName:listTypeNames()){
            documentElement.add(typeName.toXMLElement());
        }
        return xmlDocument;
    }
    public List<TypeName> listTypeNames(){
        List<TypeName> results=new ArrayList<>(this.map.values());
        Collections.sort(results, this);
        return results;
    }
    public int count(){
        return this.map.size();
    }
    public void add(XMLDocument xmlDocument){
        XMLElement documentElement = xmlDocument.getDocumentElement();
        if(documentElement==null){
            return;
        }
        Iterator<? extends XMLElement> iterator = documentElement.getElements();
        while (iterator.hasNext()){
            add(TypeName.fromXMLElement(iterator.next()));
        }
    }
    public void add(int id, String name){
        add(new TypeName(id, name));
    }
    public void add(TypeName typeName){
        map.remove(typeName.getPackageTypeId());
        map.put(typeName.getPackageTypeId(), typeName);
    }
    @Override
    public int compare(TypeName typeName1, TypeName typeName2) {
        return typeName1.compareTo(typeName2);
    }

    public static class TypeName implements Comparable<TypeName>{
        private final int packageTypeId;
        private final String name;
        public TypeName(int packageTypeId, String name){
            this.packageTypeId=(packageTypeId & 0xffff0000);
            this.name=name;
        }
        public XMLElement toXMLElement(){
            XMLElement element=new XMLElement("type");
            element.setAttribute("id", getHexId());
            element.setAttribute("name", getName());
            return element;
        }
        public String getHexId(){
            return String.format("0x%08x", getPackageTypeId());
        }
        public int getPackageTypeId() {
            return packageTypeId;
        }
        public String getName() {
            return name;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeName typeName = (TypeName) o;
            return packageTypeId == typeName.packageTypeId;
        }
        @Override
        public int hashCode() {
            return Objects.hash(packageTypeId);
        }
        @Override
        public String toString(){
            return getHexId()+":"+getName();
        }
        public static TypeName fromXMLElement(XMLElement element){
            int id = decodeHex(element.getAttributeValue("id"));
            String name = element.getAttributeValue("name");
            return new TypeName(id, name);
        }

        @Override
        public int compareTo(TypeName typeName) {
            return Integer.compare(getPackageTypeId()>>16, typeName.getPackageTypeId()>>16);
        }
    }
    private static int decodeHex(String hex){
        long l=Long.decode(hex);
        return (int) l;
    }
}
