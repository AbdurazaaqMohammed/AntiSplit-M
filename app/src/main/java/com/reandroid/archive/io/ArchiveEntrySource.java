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
package com.reandroid.archive.io;

import com.reandroid.archive.Archive;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ArchiveEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class ArchiveEntrySource<T extends ZipInput> extends InputSource {

    private final T zipInput;
    private final ArchiveEntry archiveEntry;

    public ArchiveEntrySource(T zipInput, ArchiveEntry archiveEntry){
        super(archiveEntry.getSanitizedName());
        this.zipInput = zipInput;
        this.archiveEntry = archiveEntry;
        setMethod(archiveEntry.getMethod());
    }

    public T getZipSource(){
        return zipInput;
    }
    public ArchiveEntry getArchiveEntry() {
        return archiveEntry;
    }
    @Override
    public InputStream openStream() throws IOException {
        boolean compressed = isCompressed();
        if(compressed){
            return openInflaterInputStream();
        }
        ArchiveEntry archiveEntry = getArchiveEntry();
        return getZipSource().getInputStream(
                archiveEntry.getFileOffset(),
                archiveEntry.getDataSize());
    }
    private boolean isCompressed() {
        ArchiveEntry archiveEntry = getArchiveEntry();
        int method = archiveEntry.getMethod();
        if(method == Archive.STORED) {
            return false;
        }
        if(method == Archive.DEFLATED) {
            return true;
        }
        try{
            byte[] buffer = new byte[1024];
            openInflaterInputStream().read(buffer, 0, buffer.length);
            archiveEntry.setMethod(Archive.DEFLATED);
            this.setMethod(Archive.DEFLATED);
            return true;
        }catch (Throwable ignored){
            archiveEntry.setMethod(Archive.STORED);
            this.setMethod(Archive.STORED);
            long s1 = archiveEntry.getSize();
            long s2 = archiveEntry.getCompressedSize();
            if(s1 > s2) {
                archiveEntry.setCompressedSize(s1);
            }else if(s2 > s1) {
                archiveEntry.setSize(s2);
            }
            return false;
        }
    }
    private InputStream openInflaterInputStream() throws IOException {
        ArchiveEntry archiveEntry = getArchiveEntry();
        InputStream inputStream = getZipSource().getInputStream(
                archiveEntry.getFileOffset(), archiveEntry.getDataSize());
        return new InflaterInputStream(inputStream,
                new Inflater(true), 64 * 1024);
    }
    @Override
    public long getLength() throws IOException{
        return getArchiveEntry().getDataSize();
    }
    @Override
    public long getCrc() throws IOException{
        return getArchiveEntry().getCrc();
    }
}
