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

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.utils.CompareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkUtil {
    public static String replaceRootDir(String path, String dirName){
        int i=path.indexOf('/')+1;
        path=path.substring(i);
        if(dirName != null && dirName.length()>0){
            if(!dirName.endsWith("/")){
                dirName=dirName+"/";
            }
            path=dirName+path;
        }
        return path;
    }
    public static String jsonToArchiveResourcePath(File dir, File jsonFile){
        String path = toArchivePath(dir, jsonFile);
        String ext = ApkUtil.JSON_FILE_EXTENSION;
        if(path.endsWith(ext)){
            int i2 = path.length() - ext.length();
            path = path.substring(0, i2);
        }
        return path;
    }
    public static String toArchivePath(File dir, File file){
        String dirPath = dir.getAbsolutePath()+File.separator;
        String path = file.getAbsolutePath().substring(dirPath.length());
        path=path.replace(File.separatorChar, '/');
        return path;
    }
    public static List<File> recursiveFiles(File dir, String ext){
        List<File> results=new ArrayList<>();
        if(dir.isFile()){
            if(hasExtension(dir, ext)){
                results.add(dir);
            }
            return results;
        }
        if(!dir.isDirectory()){
            return results;
        }
        File[] files=dir.listFiles();
        if(files==null){
            return results;
        }
        for(File file:files){
            if(file.isFile()){
                if(!hasExtension(file, ext)){
                    continue;
                }
                results.add(file);
                continue;
            }
            results.addAll(recursiveFiles(file, ext));
        }
        return results;
    }
    public static List<File> recursiveFiles(File dir){
        return recursiveFiles(dir, null);
    }
    public static List<File> listDirectories(File dir){
        List<File> results=new ArrayList<>();
        File[] files=dir.listFiles();
        if(files==null){
            return results;
        }
        for(File file:files){
            if(file.isDirectory()){
                results.add(file);
            }
        }
        return results;
    }
    public static List<File> listPackageDirectories(File resourcesDirectory){
        List<File> results = new ArrayList<>();
        File[] files = resourcesDirectory.listFiles();
        if(files == null){
            return results;
        }
        for(File dir:files){
            if(isPackageDirectory(dir)){
                results.add(dir);
            }
        }
        java.util.Collections.sort(results, CompareUtil.getComparableComparator());
        return results;
    }
    public static List<File> listPublicXmlFiles(File resourcesDirectory){
        List<File> results = new ArrayList<>();
        File[] files = resourcesDirectory.listFiles();
        if(files == null){
            return results;
        }
        for(File dir:files){
            List<File> resDirList = listDirectories(dir);
            for(File resDir : resDirList){
                File file = getPublicXmlFile(resDir);
                if(file != null){
                    results.add(file);
                }
            }
        }
        java.util.Collections.sort(results, CompareUtil.getComparableComparator());
        return results;
    }
    private static File getPublicXmlFile(File resDir){
        if(!resDir.isDirectory()){
            return null;
        }
        File valuesDir = new File(resDir, PackageBlock.VALUES_DIRECTORY_NAME);
        if(!valuesDir.isDirectory()){
            return null;
        }
        File file = new File(valuesDir, PackageBlock.PUBLIC_XML);
        if(!file.isFile()){
            return null;
        }
        return file;
    }
    public static List<File> listValuesDirectory(File resDir){
        return listValuesDirectory(resDir, true);
    }
    public static List<File> listValuesDirectory(File resDir, boolean includeVariants){
        List<File> results = new ArrayList<>();
        if(!resDir.isDirectory()){
            return results;
        }
        File[] files = resDir.listFiles();
        if(files == null){
            return results;
        }
        for(File dir:files){
            if(!dir.isDirectory()){
                continue;
            }
            if(isValuesDirectoryName(dir.getName(), includeVariants)){
                results.add(dir);
            }
        }
        java.util.Collections.sort(results, CompareUtil.getComparableComparator());
        return results;
    }
    public static boolean isValuesDirectoryName(String name, boolean checkVariant){
        if(PackageBlock.VALUES_DIRECTORY_NAME.equals(name)){
            return true;
        }
        if(!checkVariant){
            return false;
        }
        return name.startsWith(PackageBlock.VALUES_DIRECTORY_NAME + "-");
    }
    private static boolean isPackageDirectory(File dir){
        if(!dir.isDirectory()){
            return false;
        }
        return new File(dir, PackageBlock.JSON_FILE_NAME).isFile();
    }
    public static List<File> listFiles(File dir, String ext){
        List<File> results=new ArrayList<>();
        File[] files=dir.listFiles();
        if(files==null){
            return results;
        }
        for(File file:files){
            if(file.isFile()){
                if(!hasExtension(file, ext)){
                    continue;
                }
                results.add(file);
            }
        }
        return results;
    }
    private static boolean hasExtension(File file, String ext){
        if(ext==null){
            return true;
        }
        String name=file.getName().toLowerCase(java.util.Locale.ROOT);
        ext=ext.toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(ext);
    }
    public static String toModuleName(File file){
        String name=file.getName();
        int i=name.lastIndexOf('.');
        if(i>0){
            name=name.substring(0,i);
        }
        return name;
    }

    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String ROOT_NAME = "root";
    public static final String DEF_MODULE_NAME = "base";
    public static final String NAME_value_type = "value_type";
    public static final String NAME_data = "data";

    public static final String SIGNATURE_DIR_NAME = "signatures";
}
