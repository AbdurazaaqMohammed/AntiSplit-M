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
package com.reandroid.archive.block;

import com.reandroid.archive.block.pad.SchemePadding;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.io.FileUtil;

import java.io.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ApkSignatureBlock extends LengthPrefixedList<SignatureInfo>
        implements Comparator<SignatureInfo> {

    public ApkSignatureBlock(SignatureFooter signatureFooter){
        super(true);
        setBottomBlock(signatureFooter);
    }
    public ApkSignatureBlock(){
        this(new SignatureFooter());
    }

    public Iterator<CertificateBlock> getCertificates() {
        return new IterableIterator<SignatureInfo, CertificateBlock>(this.iterator()) {
            @Override
            public Iterator<CertificateBlock> iterator(SignatureInfo element) {
                return element.getCertificates();
            }
        };
    }
    public void sortSignatures(){
        sort(this);
    }
    public void updatePadding(){
        SchemePadding schemePadding = getOrCreateSchemePadding();
        schemePadding.setPadding(0);
        sortSignatures();
        refresh();
        int size = countBytes();
        int alignment = 4096;
        int padding = (alignment - (size % alignment)) % alignment;
        schemePadding.setPadding(padding);
        refresh();
    }
    private SchemePadding getOrCreateSchemePadding(){
        SignatureInfo signatureInfo = getSignature(SignatureId.PADDING);
        if(signatureInfo == null){
            signatureInfo = new SignatureInfo();
            signatureInfo.setId(SignatureId.PADDING);
            signatureInfo.setSignatureScheme(new SchemePadding());
            add(signatureInfo);
        }
        SignatureScheme scheme = signatureInfo.getSignatureScheme();
        if(!(scheme instanceof SchemePadding)){
            scheme = new SchemePadding();
            signatureInfo.setSignatureScheme(scheme);
        }
        return (SchemePadding) scheme;
    }
    public SignatureInfo getSignature(SignatureId signatureId){
        for(SignatureInfo signatureInfo : this){
            if(signatureInfo.getId().equals(signatureId)){
                return signatureInfo;
            }
        }
        return null;
    }
    public SignatureFooter getSignatureFooter(){
        return (SignatureFooter) getBottomBlock();
    }
    @Override
    public SignatureInfo newInstance() {
        return new SignatureInfo();
    }
    @Override
    protected void onRefreshed(){
        SignatureFooter footer = getSignatureFooter();
        footer.updateMagic();
        super.onRefreshed();
        footer.setSignatureSize(getDataSize());
    }

    public void writeRaw(File file) throws IOException{
        refresh();
        OutputStream outputStream = FileUtil.outputStream(file);
        writeBytes(outputStream);
        outputStream.close();
    }
    public List<File> writeSplitRawToDirectory(File dir) throws IOException{
        refresh();
        List<File> writtenFiles = new ArrayCollection<>(size());
        for(SignatureInfo signatureInfo : this){
            File file = signatureInfo.writeRawToDirectory(dir);
            writtenFiles.add(file);
        }
        return writtenFiles;
    }
    public void read(File file) throws IOException {
        super.readBytes(new BlockReader(file));
    }
    public void scanSplitFiles(File dir) throws IOException {
        if(!dir.isDirectory()){
            throw new IOException("No such directory");
        }
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(!file.isFile()){
                    return false;
                }
                String name = file.getName().toLowerCase(java.util.Locale.ROOT);
                return name.endsWith(SignatureId.FILE_EXT_RAW);
            }
        };
        File[] files = dir.listFiles(filter);
        if(files == null){
            return;
        }
        for(File file:files){
            addSplitRaw(file);
        }
        sortSignatures();
    }
    public SignatureInfo addSplitRaw(File signatureInfoFile) throws IOException {
        SignatureInfo signatureInfo = new SignatureInfo();
        signatureInfo.read(signatureInfoFile);
        add(signatureInfo);
        return signatureInfo;
    }
    @Override
    public int compare(SignatureInfo info1, SignatureInfo info2) {
        return info1.getId().compareTo(info2.getId());
    }

    public static final String FILE_EXT = ".sig";
}
