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

import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ZipFileOutput extends ZipOutput{
    private final File file;
    private FileChannel fileChannel;
    private FileChannelOutputStream outputStream;
    public ZipFileOutput(File file) throws IOException {
        initFile(file);
        this.file = file;
    }
    public File getFile() {
        return file;
    }
    public void write(FileChannel input, long length) throws IOException{
        FileChannel fileChannel = getFileChannel();
        long pos = fileChannel.position();

        long totalTransferred = 0;
        long remaining = length;

        while (remaining > 0) {
            long transferred = fileChannel.transferFrom(input, pos + totalTransferred, remaining);
            totalTransferred += transferred;
            remaining -= transferred;
        }

        fileChannel.position(pos + totalTransferred);
    }

    @Override
    public long position() throws IOException {
        return getFileChannel().position();
    }
    @Override
    public void position(long pos) throws IOException {
        getFileChannel().position(pos);
    }
    @Override
    public void close() throws IOException {
        FileChannel fileChannel = this.fileChannel;
        if(fileChannel != null){
            fileChannel.close();
        }
    }
    @Override
    public boolean isOpen() {
        FileChannel fileChannel = this.fileChannel;
        if(fileChannel != null){
            return fileChannel.isOpen();
        }
        return false;
    }
    private FileChannel getFileChannel() throws IOException {
        FileChannel fileChannel = this.fileChannel;
        if(fileChannel != null){
            return fileChannel;
        }
        synchronized (this){
            fileChannel = FileUtil.openWriteChannel(file);
            this.fileChannel = fileChannel;
            return fileChannel;
        }
    }

    @Override
    public void write(InputStream inputStream) throws IOException {
        FileChannel fileChannel = getFileChannel();
        long pos = fileChannel.position();
        int bufferLength = 256 * 1024;
        byte[] buffer = new byte[bufferLength];
        long result = 0;
        int read;
        while ((read = inputStream.read(buffer, 0, bufferLength)) > 0){
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, read);
            fileChannel.write(byteBuffer);
            result += read;
        }
        inputStream.close();
        fileChannel.position(pos + result);
    }
    @Override
    public FileChannelOutputStream getOutputStream() throws IOException {
        FileChannelOutputStream outputStream = this.outputStream;
        if(outputStream == null){
            outputStream = new FileChannelOutputStream(getFileChannel());
            this.outputStream = outputStream;
        }
        return outputStream;
    }


    private static void initFile(File file) throws IOException{
        if(file.isDirectory()){
            throw new IOException("Not file: " + file);
        }
        File dir = file.getParentFile();
        if(dir != null && !dir.exists()){
            dir.mkdirs();
        }
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
    }
}
