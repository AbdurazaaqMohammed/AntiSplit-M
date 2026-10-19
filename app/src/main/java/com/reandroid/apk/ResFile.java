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

import com.reandroid.apk.xmlencoder.XMLEncodeSource;
import com.reandroid.archive.BlockInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.header.InfoHeader;
import com.reandroid.arsc.value.*;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class ResFile implements Iterable<Entry> {

    private final List<Entry> entryList;
    private final InputSource inputSource;
    private boolean mBinXml;
    private boolean mBinXmlChecked;
    private String mFileExtension;
    private Entry mSelectedEntry;

    public ResFile(InputSource inputSource, List<Entry> entryList){
        this.inputSource = inputSource;
        this.entryList = entryList;
    }

    @Override
    public Iterator<Entry> iterator() {
        return getEntries().iterator();
    }
    public Iterator<Entry> iterator(Predicate<? super Entry> filter) {
        return FilterIterator.of(iterator(), filter);
    }
    public int size() {
        return getEntries().size();
    }
    public Entry get(int i) {
        return getEntries().get(i);
    }
    public Entry getFirst() {
        if(size() != 0) {
            return get(0);
        }
        return null;
    }
    public void delete() {
        delete(true);
    }
    public void delete(boolean keepResourceId) {
        for(Entry entry : this) {
            entry.setNull(true);
            if(!keepResourceId){
                entry.getTypeBlock().removeNullEntries(entry.getId());
            }
        }
        getEntries().clear();
    }
    private List<Entry> getEntries(){
        return entryList;
    }
    @Deprecated
    public List<Entry> getEntryList(){
        return getEntries();
    }
    public PackageBlock getPackageBlock(){
        Entry entry = pickOne();
        if(entry != null){
            return entry.getPackageBlock();
        }
        return null;
    }
    public ResXmlDocument getResXmlDocument() {
        if(!isBinaryXml()) {
            return null;
        }
        try {
            return readAsXmlDocument();
        } catch (IOException ignored) {
            return null;
        }
    }
    public ResXmlDocument readAsXmlDocument() throws IOException {
        InputSource inputSource = getInputSource();
        if(inputSource instanceof BlockInputSource){
            BlockInputSource<?> bis = (BlockInputSource<?>) inputSource;
            Block block = bis.getBlock();
            if(block instanceof ResXmlDocument){
                return (ResXmlDocument) block;
            }
        }
        ResXmlDocument xmlDocument = new ResXmlDocument();
        xmlDocument.setPackageBlock(getPackageBlock());
        xmlDocument.readBytes(getInputSource().openStream());
        return xmlDocument;
    }
    public String validateTypeDirectoryName() {
        String root = getRootNameFromPath();
        if(StringsUtil.isEmpty(root)) {
            root = PackageBlock.RES_DIRECTORY_NAME;
        }
        return validateTypeDirectoryName(root);
    }
    public String validateTypeDirectoryName(String root){
        Entry entry = pickOne();
        if(entry == null){
            return null;
        }
        String path = FileUtil.combineUnixPath(root,
                entry.getTypeName() + entry.getResConfig().getQualifiers());
        return FileUtil.combineUnixPath(path, getSimpleName());
    }
    public Entry pickOne(){
        if(mSelectedEntry == null){
            mSelectedEntry = selectMatching();
        }
        return mSelectedEntry;
    }
    public String getFilePath(){
        return getInputSource().getAlias();
    }
    public void setFilePath(String filePath){
        getInputSource().setAlias(filePath);
        for(Entry entry : this){
            ResValue resValue = entry.getResValue();
            if(resValue != null) {
                resValue.setValueAsString(filePath);
            }
        }
    }
    public InputSource getInputSource() {
        return inputSource;
    }
    public boolean isBinaryXml(){
        if(mBinXmlChecked){
            return mBinXml;
        }
        mBinXmlChecked = true;
        InputSource inputSource = getInputSource();
        if((inputSource instanceof XMLEncodeSource)
                || (inputSource instanceof JsonXmlInputSource)){
            mBinXml = true;
        }else if (inputSource instanceof BlockInputSource){
            BlockInputSource<?> bis = (BlockInputSource<?>) inputSource;
            Block block = bis.getBlock();
            if(block instanceof ResXmlDocument){
                mBinXml = true;
            }
        }
        if(!mBinXml){
            try {
                mBinXml = ResXmlDocument.isResXmlBlock(
                        inputSource.getBytes(InfoHeader.INFO_MIN_SIZE));
            } catch (IOException ignored) {
            }
            // Header could be obfuscated lets try load the whole document
            if(!mBinXml && getFilePath().endsWith(".xml")){
                try {
                    ResXmlDocument resXmlDocument = readAsXmlDocument();
                    mBinXml = !resXmlDocument.getStringPool().isEmpty();
                } catch (IOException ignored) {
                }
            }
        }
        return mBinXml;
    }
    public File buildOutFile(File dir){
        String path = getFilePath();
        path = path.replace('/', File.separatorChar);
        return new File(dir, path);
    }
    public String buildPath() {
        String root = getRootNameFromPath();
        if(StringsUtil.isEmpty(root)) {
            root = PackageBlock.RES_DIRECTORY_NAME;
        }
        return buildPath(root);
    }
    public String buildPath(String root){
        Entry entry = pickOne();
        StringBuilder builder = new StringBuilder();
        if(root != null){
            builder.append(root);
            if(!root.endsWith("/")){
                builder.append('/');
            }
        }
        builder.append(entry.getTypeName());
        builder.append(entry.getResConfig().getQualifiers());
        builder.append('/');
        builder.append(entry.getName());
        builder.append(getFileExtension());
        return builder.toString();
    }
    private Entry selectMatching() {
        int size = size();
        if(size < 2) {
            return getFirst();
        }
        String typeName = getTypeNameFromPath();
        if(typeName == null) {
            return getFirst();
        }
        List<Entry> typeMatchingList = CollectionUtil.toList(iterator(
                entry -> typeName.equals(entry.getTypeName())));
        if(typeMatchingList.isEmpty()) {
            return getFirst();
        }
        if(typeMatchingList.size() == 1) {
            return typeMatchingList.get(0);
        }
        String entryName = getEntryNameFromPath();
        if(entryName == null){
            return typeMatchingList.get(0);
        }
        List<Entry> nameMatchingList = CollectionUtil.toList(iterator(
                entry -> entryName.equals(entry.getName())));
        if(nameMatchingList.isEmpty()) {
            return typeMatchingList.get(0);
        }
        return selectConfigMatching(nameMatchingList);
    }
    private Entry selectConfigMatching(List<Entry> candidates) {
        if(candidates.size() == 1) {
            return candidates.get(0);
        }
        ResConfig resConfig = getResConfigFromPath();
        Entry result = null;
        for(Entry entry : candidates) {
            ResConfig config = entry.getResConfig();
            if(config.equals(resConfig)) {
                return entry;
            }
            if(result == null ||
                    (!result.getResConfig().isDefault() && config.isDefault())) {
                result = entry;
            }
        }
        return result;
    }
    private ResConfig getResConfigFromPath() {
        String[] split = splitPath();
        ResConfig resConfig = null;
        if(split.length == 3) {
            String qualifiers = split[1];
            int i = qualifiers.indexOf('-');
            if(i > 0) {
                qualifiers = qualifiers.substring(i);
            }else {
                qualifiers = StringsUtil.EMPTY;
            }
            resConfig = ResConfig.parse(qualifiers);
        }
        return resConfig;
    }
    public String getTypeNameFromPath() {
        String[] split = splitPath();
        String type = null;
        if(split.length == 3) {
            type = split[1];
            int i = type.indexOf('-');
            if(i > 0) {
                type = type.substring(0, i);
            }
        }
        return type;
    }
    public String getEntryNameFromPath() {
        String[] split = splitPath();
        String name = null;
        if(split.length == 3) {
            name = split[2];
            String ninePng = EXT_9_PNG;
            if(name.endsWith(ninePng)){
                name = name.substring(0, name.length() - ninePng.length());
            }else {
                int i = name.lastIndexOf('.');
                if(i > 0) {
                    name = name.substring(0, i);
                }
            }
        }
        return name;
    }
    public String getRootNameFromPath() {
        String[] split = splitPath();
        if(split.length > 1){
            return split[0];
        }
        return null;
    }
    public String getSimpleName() {
        String[] split = splitPath();
        return split[split.length - 1];
    }
    private String[] splitPath() {
        return StringsUtil.split(getFilePath(), '/', true);
    }
    public String getFileExtension() {
        String ext = this.mFileExtension;
        if(ext == null) {
            ext = computeFileExtension();
            this.mFileExtension = ext;
        }
        return ext;
    }
    private String computeFileExtension(){
        if(isBinaryXml()){
            return ".xml";
        }
        String path = getFilePath();
        if(path.endsWith(EXT_9_PNG)){
            return EXT_9_PNG;
        }
        int i = path.lastIndexOf('.');
        if(i > 0){
            return path.substring(i);
        }
        String ext = null;
        try {
            ext = FileMagic.getExtensionFromMagic(getInputSource());
        } catch (IOException ignored) {}

        if(ext == null) {
            ext = StringsUtil.EMPTY;
        }
        return ext;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(!(obj instanceof ResFile)) {
            return false;
        }
        ResFile resFile = (ResFile) obj;
        return getFilePath().equals(resFile.getFilePath());
    }
    @Override
    public int hashCode() {
        return getFilePath().hashCode();
    }
    @Override
    public String toString(){
        return getFilePath();
    }

    public static String EXT_9_PNG = ObjectsUtil.of(".9.png");
}
