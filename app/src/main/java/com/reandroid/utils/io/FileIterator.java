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
package com.reandroid.utils.io;

import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArraySort;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class FileIterator implements Iterator<File> {

    private final File file;
    private final Predicate<File> filter;
    private final Comparator<File> comparator;
    private final File[] files;
    private int index;
    private File currentFile;
    private FileIterator currentIterator;

    public FileIterator(File file, Predicate<File> filter, Comparator<File> comparator) {
        this.file = file;
        this.filter = filter;
        this.comparator = comparator;
        File[] files;
        if(file.isDirectory()){
            files = file.listFiles();
            if(files != null && comparator != null){
                ArraySort.sort(files, comparator);
            }
        }else {
            files = null;
        }
        this.files = files;
        this.index = -2;
    }
    public FileIterator(File file, Predicate<File> filter) {
        this(file, filter, null);
    }
    public FileIterator(File file, Comparator<File> comparator) {
        this(file, null, comparator);
    }
    public FileIterator(File file) {
        this(file, null, null);
    }

    @Override
    public boolean hasNext() {
        return getCurrent() != null;
    }

    @Override
    public File next() {
        File currentFile = getCurrent();
        this.currentFile = null;
        return currentFile;
    }
    private File getCurrent(){
        File currentFile = this.currentFile;
        if(currentFile == null){
            currentFile = computeNext();
            this.currentFile = currentFile;
        }
        return currentFile;
    }
    private File computeNext(){
        if(index == -2){
            index = -1;
            File file = this.file;
            if(matchesFile(file)){
                return file;
            }
        }
        FileIterator fileIterator = getCurrentIterator();
        if(fileIterator != null && fileIterator.hasNext()){
            return fileIterator.next();
        }
        return null;
    }
    private FileIterator getCurrentIterator(){
        FileIterator currentIterator = this.currentIterator;
        if(currentIterator != null && currentIterator.hasNext()){
            return currentIterator;
        }
        index ++;
        File[] files = this.files;
        if(files == null || index >= files.length){
            this.currentIterator = null;
            return null;
        }
        int length = files.length;
        int i;
        for(i = index; i < length; i++){
            currentIterator = new FileIterator(files[i], filter, comparator);
            files[i] = null;
            if(!currentIterator.hasNext()){
                currentIterator = null;
                continue;
            }
            break;
        }
        index = i;
        this.currentIterator = currentIterator;
        return currentIterator;
    }
    private boolean matchesFile(File file){
        if(!file.isFile()){
            return false;
        }
        return filter == null || filter.test(file);
    }

    public static final Comparator<File> NAME_COMPARATOR = (file1, file2) -> {
        boolean is_file1 = file1.isFile();
        boolean is_file2 = file2.isFile();
        if(is_file1 && !is_file2){
            return -1;
        }
        if(!is_file1 && is_file2){
            return 1;
        }
        return StringsUtil.toUpperCase(file1.getName())
                .compareTo(StringsUtil.toUpperCase(file2.getName()));
    };
    public static Predicate<File> getExtensionFilter(String extension){
        if(extension == null){
            return CollectionUtil.getAcceptAll();
        }
        return file -> extension.equalsIgnoreCase(FileUtil.getExtension(file));
    }
}
