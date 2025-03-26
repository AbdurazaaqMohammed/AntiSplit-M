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
package com.reandroid.apkeditor.utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StringHelper {
    public static String trueOrNull(Boolean value){
        if(value == null){
            return null;
        }
        return trueOrNull(value.booleanValue());
    }
    public static String trueOrNull(boolean value){
        if(!value){
            return null;
        }
        return String.valueOf(true);
    }
    public static List<String> sortAscending(List<String> nameList){
        Comparator<String> cmp=new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        };
        Collections.sort(nameList, cmp);
        return nameList;
    }
    public static void printNameAndValues(StringBuilder builder, String tab, int totalWidth, Object[][] objTable){
        printNameAndValues(builder, tab, "", totalWidth, objTable);
    }
    public static void printNameAndValues(StringBuilder builder, String tab, String separator, int totalWidth, Object[][] objTable){
        String[][] table = convertNameAndValue(objTable);
        if(table==null){
            return;
        }
        int leftWidth=0;
        for(String[] col:table){
            int len=col[0].length();
            if(len>leftWidth){
                leftWidth=len;
            }
        }
        int bnColumns=0;
        leftWidth=leftWidth+bnColumns;
        int maxRight=totalWidth-leftWidth;
        for(int i=0;i<table.length;i++){
            String[] col=table[i];
            if(i!=0){
                builder.append("\n");
            }
            printRow(false, builder, tab, leftWidth, maxRight, col[0], separator, col[1]);
        }
    }
    private static String[][] convertNameAndValue(Object[][] table){
        if(table==null){
            return null;
        }
        List<String[]> results = new ArrayList<>();
        for(Object[] objRow:table){
            String[] row = convertNameAndValueRow(objRow);
            if(row!=null){
                results.add(row);
            }
        }
        if(results.size()==0){
            return null;
        }
        return results.toArray(new String[0][]);
    }
    private static String[] convertNameAndValueRow(Object[] objRow){
        if(objRow==null){
            return null;
        }
        int len = objRow.length;
        if(len!=2){
            return null;
        }
        if(objRow[0] == null || objRow[1] == null){
            return null;
        }
        String[] result = new String[len];
        result[0] = objRow[0].toString();
        result[1] = objRow[1].toString();
        if(result[0] == null || result[1] == null){
            return null;
        }
        return result;
    }
    public static void printTwoColumns(StringBuilder builder, String tab, int totalWidth, String[][] table){
        printTwoColumns(builder, tab, "  ", totalWidth, table);
    }
    public static void printTwoColumns(StringBuilder builder, String tab, String columnSeparator, int totalWidth, String[][] table){
        int leftWidth = 0;
        for(String[] col:table){
            int len = col[0].length();
            if(len > leftWidth){
                leftWidth = len;
            }
        }
        int maxRight = totalWidth - leftWidth;
        for(int i=0;i<table.length;i++){
            String[] col=table[i];
            if(i!=0){
                builder.append("\n");
            }
            printRow(true, builder, tab, leftWidth, maxRight, col[0], columnSeparator, col[1]);
        }
    }
    private static void printRow(boolean indentLeft, StringBuilder builder, String tab, int leftWidth, int maxRight, String left, String separator, String right){
        builder.append(tab);
        if(indentLeft){
            builder.append(left);
        }
        fillSpace(builder, leftWidth-left.length());
        if(!indentLeft){
            builder.append(left);
        }
        builder.append(separator);
        char[] rightChars=right.toCharArray();
        int rightWidth=0;
        boolean spacePrefixSeen = false;
        for(int i=0;i<rightChars.length;i++){
            char ch=rightChars[i];
            if(i==0){
                builder.append(ch);
                rightWidth++;
                continue;
            }
            if(ch=='\n' || (rightWidth > 0 && rightWidth%maxRight==0)){
                builder.append('\n');
                builder.append(tab);
                fillSpace(builder, leftWidth+separator.length());
                rightWidth = 0;
                spacePrefixSeen = false;
            }
            if(ch!='\n'){
                boolean skipFirstSpace=(rightWidth==0 && ch==' ');
                if(!skipFirstSpace || spacePrefixSeen){
                    builder.append(ch);
                    rightWidth++;
                }else{
                    spacePrefixSeen = true;
                }
            }
        }
    }
    private static void fillSpace(StringBuilder builder, int count){
        for(int i=0;i<count;i++){
            builder.append(' ');
        }
    }
}
