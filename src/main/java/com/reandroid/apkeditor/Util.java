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
package com.reandroid.apkeditor;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;

import java.io.File;
import java.util.Iterator;

public class Util {

    public static File ensureUniqueFile(File file){
        if(!file.exists()){
            return file;
        }
        File dir = file.getParentFile();
        String name = file.getName();
        String ext = "";
        if(file.isFile()){
            int i = name.lastIndexOf('.');
            if(i > 0){
                ext = name.substring(i);
                name = name.substring(0, i);
            }
        }
        int i = 1;
        while (i < 1000 && file.exists()){
            String newName = name + "_" + i + ext;
            if(dir == null){
                file = new File(newName);
            }else {
                file = new File(dir, newName);
            }
            i++;
        }
        return file;
    }
    public static void deleteDir(File dir){
        if(!dir.exists()){
            return;
        }
        if(dir.isFile()){
            dir.delete();
            return;
        }
        if(!dir.isDirectory()){
            return;
        }
        File[] files=dir.listFiles();
        if(files==null){
            deleteEmptyDirectories(dir);
            return;
        }
        for(File file:files){
            deleteDir(file);
        }
        deleteEmptyDirectories(dir);
    }
    public static void deleteEmptyDirectories(File dir){
        if(dir==null || !dir.isDirectory()){
            return;
        }
        File[] filesList = dir.listFiles();
        if(filesList == null || filesList.length == 0){
            dir.delete();
            return;
        }
        int count = filesList.length;
        for(int i = 0; i < count; i++){
            File file = filesList[i];
            if(file.isFile() && file.length() != 0){
                return;
            }
        }
        count = filesList.length;
        for(int i = 0; i < count; i++){
            File file = filesList[i];
            if(file.isDirectory()){
                deleteEmptyDirectories(file);
            }
        }
        filesList = dir.listFiles();
        if(filesList == null || filesList.length == 0){
            dir.delete();
        }
    }
    // This is to respect someone's protection from editing,
    // if you reach here be ethical do not patch it for distribution.
    public static String isProtected(ApkModule apkModule) {
        ZipEntryMap zipEntryMap = apkModule.getZipEntryMap();
        Iterator<InputSource> iterator = zipEntryMap.iteratorWithPath(
                path -> (path.startsWith("classes.dex/") ||  path.startsWith("AndroidManifest.xml/")));
        if(iterator.hasNext()) {
            return EDIT_TYPE_PROTECTED;
        }
        return null;
    }
    public static final String EDIT_TYPE_PROTECTED = "PROTECTED";
}
