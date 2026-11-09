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

import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.utils.Crc32OutputStream;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.io.FileUtil;

import java.io.*;
import java.util.Comparator;

public abstract class InputSource {

    /** Reused per-thread copy buffer: avoids allocating a large array per entry. */
    private static final ThreadLocal<byte[]> WRITE_BUFFER = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[256 * 1024];
        }
    };

    private final String name;
    private String alias;
    private long mCrc;
    private long mLength;
    private int method = Archive.DEFLATED;
    private int sort = -1;
    private String[] splitAlias;

    public InputSource(String name){
        this.name = name;
        this.alias = ArchiveUtil.sanitizePath(name);
    }

    public byte[] getBytes(int length) throws IOException{
        InputStream inputStream = openStream();
        byte[] bytes = new byte[length];
        inputStream.read(bytes, 0, length);
        close(inputStream);
        return bytes;
    }
    public void disposeInputSource(){
    }
    public int getSort() {
        return sort;
    }
    public void setSort(int sort) {
        this.sort = sort;
    }
    public void copyAttributes(InputSource other) {
        if(other != null && other != this) {
            this.setSort(other.getSort());
            this.setMethod(other.getMethod());
            this.setAlias(other.getAlias());
        }
    }
    public boolean isUncompressed(){
        return getMethod() != Archive.DEFLATED;
    }
    public void setUncompressed(boolean uncompressed){
        if(uncompressed){
            method = Archive.STORED;
        }else {
            method = Archive.DEFLATED;
        }
    }
    public int getMethod() {
        return method;
    }
    public void setMethod(int method) {
        this.method = method;
    }

    public String getAlias(){
        if(alias!=null){
            return alias;
        }
        return getName();
    }
    public void setAlias(String alias) {
        this.alias = alias;
        this.splitAlias = null;
    }
    public String getSimpleName() {
        return FileUtil.getFileName(getAlias());
    }
    public String getSimpleNameWoExtension() {
        return FileUtil.getNameWoExtension(getAlias());
    }
    public String getParentPath() {
        return FileUtil.getParent(getAlias());
    }
    public String getExtension() {
        return FileUtil.getExtension(getAlias());
    }
    private String[] getSplitAlias(){
        if(this.splitAlias == null){
            String alias = StringsUtil.toLowercase(getAlias());
            this.splitAlias = StringsUtil.split(alias, '/');
        }
        return this.splitAlias;
    }
    public void close(InputStream inputStream) throws IOException {
        inputStream.close();
    }
    public File toFile(File dir){
        String path = getAlias();
        path = path.replace('/', File.separatorChar);
        return new File(dir, path);
    }
    public void write(File file) throws IOException {
        File dir=file.getParentFile();
        if(!dir.exists()){
            dir.mkdirs();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        write(outputStream);
        outputStream.close();
    }
    public long write(OutputStream outputStream) throws IOException {
        return write(outputStream, openStream());
    }
    private long write(OutputStream outputStream, InputStream inputStream) throws IOException {
        long result=0;
        byte[] buffer = WRITE_BUFFER.get();
        int len;
        while ((len=inputStream.read(buffer))>0){
            outputStream.write(buffer, 0, len);
            result+=len;
        }
        close(inputStream);
        return result;
    }
    public String getName(){
        return name;
    }
    public long getLength() throws IOException{
        if(mLength==0){
            calculateCrc();
        }
        return mLength;
    }
    public long getCrc() throws IOException{
        if(mCrc==0){
            calculateCrc();
        }
        return mCrc;
    }
    public abstract InputStream openStream() throws IOException;
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InputSource)) {
            return false;
        }
        InputSource that = (InputSource) o;
        return getName().equals(that.getName());
    }
    @Override
    public int hashCode() {
        return getName().hashCode();
    }
    @Override
    public String toString(){
        return getClass().getSimpleName()+": "+getName();
    }
    private void calculateCrc() throws IOException {
        Crc32OutputStream outputStream = new Crc32OutputStream();
        write(outputStream);
        this.mCrc = outputStream.getValue();
        this.mLength = outputStream.getLength();
    }

    public static int getDexNumber(String name) {
        if(name.equals("classes.dex")){
            return 0;
        }
        String prefix = "classes";
        String ext = ".dex";
        if(!name.startsWith(prefix) || !name.endsWith(ext)){
            return -1;
        }
        String num = name.substring(prefix.length(), name.length() - ext.length());
        try {
            int value = Integer.parseInt(num);
            if (value > 0) {
                return value;
            }
        }catch (NumberFormatException ignored){
        }
        return -1;
    }
    public static int compareDex(String dex1, String dex2){
        int d1 = getDexNumber(dex1);
        int d2 = getDexNumber(dex2);
        if(d1 == d2){
            return 0;
        }
        if(d1 < 0){
            return 1;
        }
        if(d2 < 0){
            return -1;
        }
        return Integer.compare(d1, d2);
    }
    private static int getSortOrder(String[] alias){
        int length = alias.length;
        if(length == 0){
            return LAST_ORDER;
        }
        String name = alias[0];
        if(StringsUtil.isEmpty(name)){
            return LAST_ORDER;
        }
        if(length != 1){
            if(META_INF.equals(name)){
                return ORDER_meta_inf;
            }
            if(LIB.equals(name)){
                return ORDER_lib;
            }
            if(RES.equals(name)){
                return ORDER_res;
            }
            if(ASSETS.equals(name)){
                return ORDER_assets;
            }
            return LAST_ORDER;
        }
        if(ANDROID_MANIFEST.equals(name)){
            return ORDER_android_manifest;
        }
        if(RESOURCES.equals(name)){
            return ORDER_resources;
        }
        if(name.startsWith("classes") && name.endsWith(".dex")){
            return ORDER_classes;
        }
        return LAST_ORDER;
    }
    private static int compareAlias(InputSource source1, InputSource source2){
        String[] alias1 = source1.getSplitAlias();
        String[] alias2 = source2.getSplitAlias();
        int order1 = getSortOrder(alias1);
        int order2 = getSortOrder(alias2);
        if(order1 != order2){
            return CompareUtil.compare(order1, order2);
        }
        if(order1 == ORDER_classes){
            return compareDex(alias1[0], alias2[0]);
        }
        return StringsUtil.compare(alias1, alias2);
    }
    static int compareSortOrAlias(InputSource source1, InputSource source2){
        if(source1 == source2){
            return 0;
        }
        if(source1 == null){
            return 1;
        }
        if(source2 == null){
            return -1;
        }
        int sort1 = source1.getSort();
        int sort2 = source1.getSort();
        if(sort1 == -1 && sort2 == -1){
            return compareAlias(source1, source2);
        }
        if(sort1 == -1){
            return 1;
        }
        if(sort2 == -1){
            return -1;
        }
        return Integer.compare(sort1, sort2);
    }

    public static final Comparator<? super InputSource> ALIAS_COMPARATOR = (Comparator<InputSource>) InputSource::compareSortOrAlias;

    private static final String ANDROID_MANIFEST = StringsUtil.toLowercase(AndroidManifestBlock.FILE_NAME);
    private static final String RESOURCES = StringsUtil.toLowercase(TableBlock.FILE_NAME);
    private static final String META_INF = "meta-inf";
    private static final String LIB = "lib";
    private static final String RES = "res";
    private static final String ASSETS = "assets";

    private static final int ORDER_android_manifest = 0;
    private static final int ORDER_resources = 1;
    private static final int ORDER_meta_inf = 2;
    private static final int ORDER_classes = 3;
    private static final int ORDER_lib = 4;
    private static final int ORDER_res = 5;
    private static final int ORDER_assets = 6;

    private static final int LAST_ORDER = 10;
}
