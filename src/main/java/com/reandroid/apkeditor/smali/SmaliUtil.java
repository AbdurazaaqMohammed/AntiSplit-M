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
package com.reandroid.apkeditor.smali;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SmaliUtil {

    static void sortDexFiles(List<File> fileList){
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                int i1 = getDexNumber(file1.getName());
                int i2 = getDexNumber(file2.getName());
                if(i1 == i2){
                    return 0;
                }
                if(i1 < 0 || i1 < i2){
                    return -1;
                }
                return 1;
            }
        });
    }
    static String getDexFileName(int i){
        if(i==0){
            return "classes.dex";
        }
        return "classes" + i + ".dex";
    }
    static boolean isClassesDir(File dir){
        if(!dir.isDirectory()){
            return false;
        }
        return getDexNumber(dir.getName()) >= 0;
    }
    static int getDexNumber(String name){
        if(name.equals("classes") || name.equals("classes.dex")){
            return 0;
        }
        String prefix = "classes";
        if(!name.startsWith(prefix)){
            return -1;
        }
        name = name.substring(prefix.length());
        String ext = ".dex";
        if(name.endsWith(ext)){
            name =  name.substring(0, name.length() - ext.length());
        }
        try {
            return Integer.parseInt(name);
        }catch (NumberFormatException ignored){
            return -1;
        }
    }

    static final String CACHE_DIR = ".cache";
}
