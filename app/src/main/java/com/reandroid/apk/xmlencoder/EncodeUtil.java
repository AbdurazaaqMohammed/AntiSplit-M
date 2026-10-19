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
package com.reandroid.apk.xmlencoder;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class EncodeUtil {
    public static void sortValuesXml(List<File> fileList){
        Comparator<File> cmp= (f1, f2) -> {
            String n1=getValuesXmlCompare(f1);
            String n2=getValuesXmlCompare(f2);
            return n1.compareTo(n2);
        };
        java.util.Collections.sort(fileList, cmp);
    }
    private static String getValuesXmlCompare(File file){
        String name=file.getName().toLowerCase(java.util.Locale.ROOT);
        if(name.equals("public.xml")){
            return "0";
        }
        if(name.equals("ids.xml")){
            return "1";
        }
        if(name.contains("attr")){
            return "2";
        }
        return "3 "+name;
    }
    public static boolean isEmpty(String text){
        if(text==null){
            return true;
        }
        text=text.trim();
        return text.length()==0;
    }
    public static String getEntryPathFromResFile(File resFile){
        File typeDir=resFile.getParentFile();
        File resDir=typeDir.getParentFile();
        return resDir.getName()
                +"/"+typeDir.getName()
                +"/"+resFile.getName();
    }
    public static String getEntryNameFromResFile(File resFile){
        String name=resFile.getName();
        String ninePatch=".9.png";
        if(name.endsWith(ninePatch)){
            return name.substring(0, name.length()-ninePatch.length());
        }
        int i=name.lastIndexOf('.');
        if(i>0){
            name = name.substring(0, i);
        }
        return name;
    }
    public static String getQualifiersFromResFile(File resFile){
        String name=resFile.getParentFile().getName();
        int i=name.indexOf('-');
        if(i>0){
            return name.substring(i);
        }
        return "";
    }
    public static String getTypeNameFromResFile(File resFile){
        String name=resFile.getParentFile().getName();
        int i=name.indexOf('-');
        if(i>0){
            name=name.substring(0, i);
        }
        if(!name.equals("plurals") && name.endsWith("s")){
            name=name.substring(0, name.length()-1);
        }
        return name;
    }
    public static String sanitizeType(String type){
        if(type.length() < 2){
            return type;
        }
        char first = type.charAt(0);
        if(first == '^' || first == '+' || first == '*'){
            type = type.substring(1);
        }
        char[] chars = type.toCharArray();
        StringBuilder builder = new StringBuilder();
        for(char ch : chars){
            if(ch <= 'z' && ch >= 'a'){
                builder.append(ch);
            }else {
                break;
            }
        }
        type = builder.toString();
        if(!"plurals".equals(type) && type.endsWith("s")){
            type = type.substring(0, type.length()-1);
        }
        return type;
    }

}
