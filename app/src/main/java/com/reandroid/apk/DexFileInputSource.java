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
package com.reandroid.apk;

import com.reandroid.archive.InputSource;
import com.reandroid.archive.RenamedInputSource;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DexFileInputSource extends RenamedInputSource<InputSource> implements Comparable<DexFileInputSource>{
    public DexFileInputSource(String name, InputSource inputSource){
        super(name, inputSource);
    }
    public int getDexNumber(){
        return InputSource.getDexNumber(getAlias());
    }
    @Override
    public int compareTo(DexFileInputSource source) {
        return Integer.compare(getDexNumber(), source.getDexNumber());
    }

    public static void sort(List<DexFileInputSource> sourceList){
        java.util.Collections.sort(sourceList, CompareUtil.getComparableComparator());
    }

    public static void sortDexFiles(List<File> fileList){
        java.util.Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return InputSource.compareDex(file1.getName(), file2.getName());
            }
        });
    }
    public static List<File> listDexFiles(File dir){
        List<File> results = new ArrayList<>();
        if(!dir.isDirectory()){
            return results;
        }
        File[] files = dir.listFiles();
        if(files == null){
            return results;
        }
        for(File file : files){
            if(file.isFile() && isDexName(file.getName())){
                results.add(file);
            }
        }
        sortDexFiles(results);
        return results;
    }

    public static boolean isDexName(String name){
        return InputSource.getDexNumber(name)>=0;
    }
    static String getDexName(int i){
        if(i==0){
            return "classes.dex";
        }
        return "classes" + i + ".dex";
    }

    public static final String DEX_DIRECTORY_NAME = ObjectsUtil.of("dex");
}
