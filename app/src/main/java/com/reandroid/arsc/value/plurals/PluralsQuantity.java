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
package com.reandroid.arsc.value.plurals;


 import com.reandroid.arsc.value.ResValueMap;

 /**
  * Replaced by com.reandroid.arsc.value.AttributeType
  * */
 @Deprecated
 public enum PluralsQuantity {
    OTHER((short) 0x0004),
    ZERO((short) 0x0005),
    ONE((short) 0x0006),
    TWO((short) 0x0007),
    FEW((short) 0x0008),
    MANY((short) 0x0009);

    private final short mId;
    PluralsQuantity(short id) {
        this.mId=id;
    }
    public short getId() {
        return mId;
    }
    @Override
    public String toString(){
        return name().toLowerCase(java.util.Locale.ROOT);
    }
    public static PluralsQuantity valueOf(short id){
        PluralsQuantity[] all=values();
        for(PluralsQuantity pq:all){
            if(id==pq.mId){
                return pq;
            }
        }
        return null;
    }
    public static PluralsQuantity valueOf(ResValueMap valueMap){
        if (valueMap == null) {
            return null;
        }
        int low = valueMap.getNameId() & 0xffff;
        return valueOf((short) low);
    }
    public static PluralsQuantity value(String name){
        if(name==null){
            return null;
        }
        name=name.trim().toUpperCase(java.util.Locale.ROOT);
        PluralsQuantity[] all=values();
        for(PluralsQuantity pq:all){
            if(name.equals(pq.name())){
                return pq;
            }
        }
        return null;
    }
}
