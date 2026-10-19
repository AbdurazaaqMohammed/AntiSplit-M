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
package com.reandroid.archive;

import com.reandroid.apk.APKLogger;
import com.reandroid.archive.block.*;
import com.reandroid.archive.io.*;
import com.reandroid.archive.model.CentralFileDirectory;
import com.reandroid.archive.model.LocalFileDirectory;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.io.FilePermissions;
import com.reandroid.utils.io.FileUtil;
import com.reandroid.utils.io.IOUtil;

import java.io.*;
import java.util.*;
import com.reandroid.utils.function.Predicate;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public abstract class Archive<T extends ZipInput> implements Closeable {

    private final T zipInput;
    private final ArchiveEntry[] entryList;
    private final EndRecord endRecord;
    private final ApkSignatureBlock apkSignatureBlock;

    public Archive(T zipInput) throws IOException {
        this.zipInput = zipInput;
        CentralFileDirectory cfd = new CentralFileDirectory();
        cfd.visit(zipInput);
        this.endRecord = cfd.getEndRecord();
        LocalFileDirectory lfd = new LocalFileDirectory(cfd);
        lfd.visit(zipInput);
        this.entryList  = lfd.buildArchiveEntryList();
        this.apkSignatureBlock = lfd.getApkSigBlock();
    }

    public ZipEntryMap createZipEntryMap(){
        return new ZipEntryMap(mapEntrySource());
    }

    public InputSource[] getInputSources(){
        // TODO: make InputSource for directory entry
        return getInputSources(ArchiveEntry::isFile);
    }
    public InputSource[] getInputSources(Predicate<? super ArchiveEntry> filter){
        Iterator<InputSource> iterator = ComputeIterator.of(iterator(filter), this::createInputSource);
        List<InputSource> sourceList = CollectionUtil.toList(iterator);
        return sourceList.toArray(new InputSource[sourceList.size()]);
    }

    public PathTree<InputSource> getPathTree(){
        PathTree<InputSource> root = PathTree.newRoot();
        Iterator<ArchiveEntry> iterator = getFiles();
        while (iterator.hasNext()){
            ArchiveEntry entry = iterator.next();
            InputSource inputSource = createInputSource(entry);
            root.add(inputSource.getAlias(), inputSource);
        }
        return root;
    }
    public LinkedHashMap<String, InputSource> mapEntrySource(){
        LinkedHashMap<String, InputSource> map = new LinkedHashMap<>(size());
        Iterator<ArchiveEntry> iterator = getFiles();
        while (iterator.hasNext()){
            ArchiveEntry entry = iterator.next();
            InputSource inputSource = createInputSource(entry);
            map.put(inputSource.getAlias(), inputSource);
        }
        return map;
    }

    public T getZipInput() {
        return zipInput;
    }

    abstract InputSource createInputSource(ArchiveEntry entry);
    public InputSource getEntrySource(String path){
        if(path == null){
            return null;
        }
        ArchiveEntry[] entryList = this.entryList;
        int length = entryList.length;
        for(int i = 0; i < length; i++){
            ArchiveEntry entry = entryList[i];
            if(entry.isDirectory()){
                continue;
            }
            if(path.equals(entry.getName())){
                return createInputSource(entry);
            }
        }
        return null;
    }
    public InputStream openRawInputStream(ArchiveEntry archiveEntry) throws IOException {
        return zipInput.getInputStream(archiveEntry.getFileOffset(), archiveEntry.getDataSize());
    }
    public InputStream openInputStream(ArchiveEntry archiveEntry) throws IOException {
        InputStream rawInputStream = openRawInputStream(archiveEntry);
        if(!archiveEntry.isCompressed()){
            return rawInputStream;
        }
        return new InflaterInputStream(rawInputStream,
                new Inflater(true), 1024*1000);
    }
    public Iterator<ArchiveEntry> getFiles() {
        return iterator(ArchiveEntry::isFile);
    }
    public Iterator<ArchiveEntry> iterator() {
        return new ArrayIterator<>(entryList);
    }
    public Iterator<ArchiveEntry> iterator(Predicate<? super ArchiveEntry> filter) {
        return new ArrayIterator<>(entryList, filter);
    }
    public int size(){
        return entryList.length;
    }
    public ApkSignatureBlock getApkSignatureBlock() {
        return apkSignatureBlock;
    }
    public EndRecord getEndRecord() {
        return endRecord;
    }

    public int extractAll(File dir) throws IOException {
        return extractAll(dir, null, null);
    }
    public int extractAll(File dir, APKLogger logger) throws IOException {
        return extractAll(dir, null, logger);
    }
    public int extractAll(File dir, Predicate<ArchiveEntry> filter) throws IOException {
        return extractAll(dir, filter, null);
    }
    public int extractAll(File dir, Predicate<ArchiveEntry> filter, APKLogger logger) throws IOException {
        Iterator<ArchiveEntry> iterator = iterator(filter);
        int result = 0;
        while (iterator.hasNext()){
            ArchiveEntry archiveEntry = iterator.next();
            extract(toFile(dir, archiveEntry), archiveEntry, logger);
            result ++;
        }
        return result;
    }
    public void extract(File file, ArchiveEntry archiveEntry) throws IOException{
        extract(file, archiveEntry, null);
    }
    public void extract(File file, ArchiveEntry archiveEntry, APKLogger logger) throws IOException {
        if(archiveEntry.isDirectory()) {
            // TODO: make directories considering file collision
            return;
        }
        FileUtil.ensureParentDirectory(file);
        if(logger != null){
            long size = archiveEntry.getDataSize();
            if(size > LOG_LARGE_FILE_SIZE){
                logger.logVerbose("Extracting ["
                        + FileUtil.toReadableFileSize(size) + "] "+ archiveEntry.getName());
            }
        }
        if(archiveEntry.getMethod() != Archive.STORED){
            extractCompressed(file, archiveEntry);
        }else {
            extractStored(file, archiveEntry);
        }
        applyAttributes(archiveEntry, file);
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void applyAttributes(ArchiveEntry archiveEntry, File file) {
        FilePermissions permissions = archiveEntry.getFilePermissions();
        if(permissions.get() != 0) {
            permissions.apply(file);
        }
        long time = Archive.dosToJavaDate(archiveEntry.getDosTime()).getTime();
        file.setLastModified(time);
    }
    abstract void extractStored(File file, ArchiveEntry archiveEntry) throws IOException;
    private void extractCompressed(File file, ArchiveEntry archiveEntry) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        IOUtil.writeAll(openInputStream(archiveEntry), outputStream);
    }
    private File toFile(File dir, ArchiveEntry archiveEntry){
        String name = archiveEntry.getName().replace('/', File.separatorChar);
        return new File(dir, name);
    }
    @Override
    public void close() throws IOException {
        this.zipInput.close();
    }

    public static<T1 extends InputSource> PathTree<T1> buildPathTree(T1[] inputSources){
        PathTree<T1> root = PathTree.newRoot();
        int length = inputSources.length;
        for(int i = 0; i < length; i ++){
            T1 item = inputSources[i];
            root.add(item.getAlias(), item);
        }
        return root;
    }

    public static Date dosToJavaDate(long dosTime) {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    public static long javaToDosTime(long javaTime) {
        return javaToDosTime(new Date(javaTime));
    }
    public static long javaToDosTime(Date date) {
        if(date == null || date.getTime() == 0){
            return 0;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if(year < 1980){
            return 0;
        }
        int result = cal.get(Calendar.DATE);
        result = (cal.get(Calendar.MONTH) + 1 << 5) | result;
        result = ((cal.get(Calendar.YEAR) - 1980) << 9) | result;
        int time = cal.get(Calendar.SECOND) >> 1;
        time = (cal.get(Calendar.MINUTE) << 5) | time;
        time = (cal.get(Calendar.HOUR_OF_DAY) << 11) | time;
        return ((long) result << 16) | time;
    }

    private static final long LOG_LARGE_FILE_SIZE = 1024 * 1000 * 20;


    public static final int STORED = ObjectsUtil.of(0);
    public static final int DEFLATED = ObjectsUtil.of(8);
}
