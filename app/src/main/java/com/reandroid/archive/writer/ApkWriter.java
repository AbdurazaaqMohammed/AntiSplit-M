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
package com.reandroid.archive.writer;

import com.reandroid.apk.APKLogger;
import com.reandroid.archive.ArchiveInfo;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.WriteProgress;
import com.reandroid.archive.ZipSignature;
import com.reandroid.archive.block.*;
import com.reandroid.archive.io.ZipOutput;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

public abstract class ApkWriter<T extends ZipOutput, OUT extends OutputSource> implements Closeable {
    private final Object mLock = new Object();
    private final T zipOutput;
    private final InputSource[] inputSources;
    private ZipAligner zipAligner;
    private ApkSignatureBlock apkSignatureBlock;
    private APKLogger apkLogger;
    private WriteProgress writeProgress;
    private final HeaderInterceptorChain interceptorChain;
    private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

    public ApkWriter(T zipOutput, InputSource[] sources){
        this.zipOutput = zipOutput;
        this.inputSources = sources;
        this.zipAligner = ZipAligner.apkAligner();
        this.interceptorChain = HeaderInterceptorChain.createDefault();
    }

    public void write()throws IOException {
        synchronized (mLock){
            OUT[] outList = buildOutputEntries();

            prepareOutputs(outList);
            writeApkList(outList);

            closeBuffer();

            writeSignatureBlock();
            writeCEHList(outList);

            this.close();
        }
    }
    private void writeApkList(OUT[] outputList) throws IOException{
        int length = outputList.length;
        logMessage("Writing files: " + length);
        APKLogger logger = this.getApkLogger();
        ZipAligner zipAligner = getZipAligner();
        for(int i = 0; i < length; i++){
            OUT out = outputList[i];
            out.setAPKLogger(logger);
            writeApk(out, zipAligner);
            if(i % 100 == 0){
                out.logFileWrite();
            }
        }
    }
    void closeBuffer() throws IOException{
    }
    private void writeCEHList(OUT[] outputList) throws IOException{
        EndRecord endRecord = new EndRecord();
        endRecord.setSignature(ZipSignature.END_RECORD);
        long offset = position();
        endRecord.setOffsetOfCentralDirectory(offset);
        int count = outputList.length;
        endRecord.setNumberOfDirectories(count);
        endRecord.setTotalNumberOfDirectories(count);
        ZipOutput zipOutput = getZipOutput();
        for(int i = 0; i < count; i++){
            OUT outputSource = outputList[i];
            outputSource.writeCEH(zipOutput);
        }
        long cedLength = position() - offset;
        endRecord.setLengthOfCentralDirectory(cedLength);
        OutputStream outputStream = getOutputStream();
        Zip64Record zip64Record = endRecord.getZip64Record();
        if(zip64Record != null){
            long offsetOfRecord = position();
            logMessage("ZIP64: " + zip64Record);
            zip64Record.writeBytes(outputStream);
            Zip64Locator zip64Locator = endRecord.getZip64Locator();
            zip64Locator.setOffsetZip64Record(offsetOfRecord);
            logMessage("ZIP64: " + zip64Locator);
            zip64Locator.writeBytes(outputStream);
        }
        endRecord.writeBytes(getOutputStream());
    }
    OUT[] buildOutputEntries(){
        InputSource[] sources = this.getInputSources();
        int length = sources.length;
        OUT[] results = createOutArray(length);
        HeaderInterceptorChain interceptorChain = this.getInterceptorChain();
        if(interceptorChain.isDisabled()){
            interceptorChain = null;
        }
        for(int i = 0; i < length; i++){
            InputSource inputSource = sources[i];
            OUT out = toOutputSource(inputSource);
            out.setHeaderInterceptor(interceptorChain);
            out.setCompressionLevel(getCompressionLevel());
            results[i] = out;
        }
        return results;
    }

    abstract void writeApk(OUT outputSource, ZipAligner zipAligner) throws IOException;
    abstract void prepareOutputs(OUT[] outList) throws IOException;
    abstract OUT toOutputSource(InputSource inputSource);
    abstract OUT[] createOutArray(int length);

    long position() throws IOException {
        return zipOutput.position();
    }
    OutputStream getOutputStream() throws IOException {
        return zipOutput.getOutputStream();
    }
    public T getZipOutput() {
        return zipOutput;
    }
    InputSource[] getInputSources() {
        return inputSources;
    }
    public int getCompressionLevel() {
        return compressionLevel;
    }
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }
    public ZipAligner getZipAligner(){
        return zipAligner;
    }
    public void setZipAligner(ZipAligner zipAligner) {
        this.zipAligner = zipAligner;
    }

    public void setApkSignatureBlock(ApkSignatureBlock apkSignatureBlock) {
        this.apkSignatureBlock = apkSignatureBlock;
    }
    public ApkSignatureBlock getApkSignatureBlock() {
        return apkSignatureBlock;
    }
    void writeSignatureBlock() throws IOException {
        ApkSignatureBlock signatureBlock = this.getApkSignatureBlock();
        if(signatureBlock == null){
            return;
        }
        logMessage("Writing signature block ...");
        long offset = position();
        if(ZipHeader.isZip64Length(offset)){
            logMessage("ZIP64 mode, skip writing signature block!");
            return;
        }
        int alignment = 4096;
        int filesPadding = (int) ((alignment - (offset % alignment)) % alignment);
        OutputStream outputStream = getOutputStream();
        if(filesPadding > 0){
            outputStream.write(new byte[filesPadding]);
        }
        signatureBlock.updatePadding();
        signatureBlock.writeBytes(outputStream);
    }

    @Override
    public void close() throws IOException {
        this.zipOutput.close();
    }

    public void setWriteProgress(WriteProgress writeProgress){
        this.writeProgress = writeProgress;
    }

    public HeaderInterceptorChain getInterceptorChain() {
        return interceptorChain;
    }
    public void setArchiveInfo(ArchiveInfo archiveInfo) {
        this.getInterceptorChain().setArchiveInfo(archiveInfo);
    }
    public void setHeaderInterceptor(HeaderInterceptor interceptor) {
        this.getInterceptorChain().setHeaderInterceptor(interceptor);
    }
    public void setDataDescriptorFactory(DataDescriptorFactory dataDescriptorFactory) {
        getInterceptorChain().setDataDescriptorFactory(dataDescriptorFactory);
    }

    void onCompressFileProgress(String path, int mode, long writtenBytes) {
        if(writeProgress!=null){
            writeProgress.onCompressFile(path, mode, writtenBytes);
        }
    }
    APKLogger getApkLogger(){
        return apkLogger;
    }
    public void setAPKLogger(APKLogger logger) {
        this.apkLogger = logger;
    }
    void logMessage(String msg) {
        if(apkLogger!=null){
            apkLogger.logMessage(msg);
        }
    }
}
