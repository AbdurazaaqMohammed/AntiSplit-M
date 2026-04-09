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

import com.reandroid.apkeditor.utils.StringHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class RefactorUtil {
    public static boolean isObfuscated(Collection<String> entryNames){
        if(hasDuplicates(entryNames)){
            return true;
        }
        if(isAllEqualLength(entryNames)){
            return true;
        }
        if(isAllShortLength(entryNames)){
            return true;
        }
        if(isSequentialNames(entryNames)){
            return true;
        }
        if(!isAllGoodName(entryNames)){
            return true;
        }
        return false;
    }
    private static boolean hasDuplicates(Collection<String> entryNames){
        if(entryNames instanceof HashSet){
            return false;
        }
        return (new HashSet<>(entryNames)).size() != entryNames.size();
    }
    private static boolean isAllEqualLength(Collection<String> entryNames){
        int length=0;
        for(String name:entryNames){
            int len=name.length();
            if(length==0){
                length=len;
                continue;
            }
            if(len!=length){
                return false;
            }
            length=len;
        }
        return length!=0;
    }
    private static boolean isAllShortLength(Collection<String> entryNames){
        int length=0;
        for(String name:entryNames){
            int len=name.length();
            if(length==0){
                length=len;
                continue;
            }
            if(len>3){
                return false;
            }
        }
        return entryNames.size()>10;
    }
    public static boolean isSequentialNames(Collection<String> entryNames){
        if(entryNames.size()==0){
            return false;
        }
        List<String> sortedList= StringHelper.sortAscending(new ArrayList<>(entryNames));
        int sequence_break=0;
        int prevHash=0;
        for(String name:sortedList){
            int hash=name.hashCode();
            if(prevHash==0){
                prevHash=hash;
                continue;
            }
            if(hash!=(prevHash+1)){
                sequence_break++;
            }
            prevHash=hash;
        }
        if(sequence_break==0){
            return true;
        }
        int half=sortedList.size()/2;
        return sequence_break<half;
    }
    public static boolean isGeneratedName(String name, int resourceId){
        String hex=String.format("0x%08x", resourceId);
        return name.endsWith(hex);
    }
    public static boolean isGeneratedName(String name){
        return PATTERN_GENERATED_NAME.matcher(name).matches();
    }
    public static String generateUniqueName(String type, int resourceId){
        return type+"_"+String.format("0x%08x", resourceId);
    }
    public static boolean isAllGoodName(Collection<String> nameList){
        for(String name:nameList){
            if(!PATTERN_GOOD_NAME.matcher(name).matches()){
                return false;
            }
        }
        return nameList.size()>0;
    }
    public static boolean isGoodName(String name){
        if(name==null){
            return false;
        }
        return PATTERN_GOOD_NAME.matcher(name).matches();
    }
    private static final Pattern PATTERN_GOOD_NAME =Pattern.compile("^[A-Za-z]{2,15}[_.A-Za-z0-9]*$");
    private static final Pattern PATTERN_GENERATED_NAME=Pattern.compile("^.+_(0x[0-9a-f]{7,8})$");
}
