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
package com.reandroid.dex.model;

import com.reandroid.arsc.io.BlockReader;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.sections.*;
import com.reandroid.dex.smali.SmaliReaderSetting;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.SmaliWriterSetting;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.io.FileUtil;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class DexFile implements Closeable, DexClassRepository, Iterable<DexLayout> {

    private final DexContainerBlock containerBlock;
    private final DexFileLayoutController layoutController;
    private DexDirectory dexDirectory;

    private boolean closed;

    public DexFile(DexContainerBlock containerBlock) {
        this.containerBlock = containerBlock;
        this.layoutController = new DexFileLayoutController(this);
        containerBlock.setLayoutBlockChangedListener(layoutController);
    }


    public boolean isMultiLayout() {
        return getContainerBlock().isMultiLayout();
    }
    @Override
    public Iterator<DexLayout> iterator() {
        return layoutController.iterator();
    }
    public Iterator<DexLayout> clonedIterator() {
        return layoutController.iterator();
    }
    public DexLayout getOrCreateFirst() {
        return getOrCreateAt(0);
    }
    public DexLayout getOrCreateAt(int index) {
        getContainerBlock().ensureSize(index + 1);
        return getLayout(index);
    }
    public DexLayout getFirst() {
        return layoutController.get(0);
    }
    public DexLayout getLayout(int i) {
        return this.layoutController.get(i);
    }
    public int size() {
        return getContainerBlock().size();
    }
    public boolean isEmpty() {
        return getContainerBlock().isEmpty();
    }
    public int getIndex() {
        DexDirectory directory = getDexDirectory();
        if(directory != null) {
            return directory.indexOf(this);
        }
        return -1;
    }

    public int getVersion() {
        return getContainerBlock().getVersion();
    }
    public void setVersion(int version) {
        getContainerBlock().setVersion(version);
    }
    public String getSimpleName() {
        return getContainerBlock().getSimpleName();
    }
    public void setSimpleName(String simpleName) {
        getContainerBlock().setSimpleName(simpleName);
    }
    public DexSource<DexFile> getSource(){
        DexDirectory directory = getDexDirectory();
        if(directory != null){
            return directory.getDexSourceSet().getSource(this);
        }
        return null;
    }
    public DexDirectory getDexDirectory() {
        return dexDirectory;
    }
    public void setDexDirectory(DexDirectory dexDirectory) {
        this.dexDirectory = dexDirectory;
        DexContainerBlock containerBlock = getContainerBlock();
        containerBlock.setTag(this);
        containerBlock.setSimpleName(getSimpleName());
    }
    public DexContainerBlock getContainerBlock() {
        return containerBlock;
    }


    @Override
    public void refresh() {
        getContainerBlock().refresh();
        layoutController.refreshController();
    }

    @Override
    public void refreshFull() {
        layoutController.refreshController();
        getContainerBlock().refreshFull();
        layoutController.refreshController();
    }

    public void clearEmptySections() {
        getContainerBlock().clearEmptySections();
    }
    public Iterator<DexInstruction> getDexInstructions() {
        return new IterableIterator<DexLayout, DexInstruction>(iterator()) {
            @Override
            public Iterator<DexInstruction> iterator(DexLayout element) {
                return element.getDexInstructions();
            }
        };
    }
    public Iterator<DexInstruction> getDexInstructionsCloned() {
        return new IterableIterator<DexLayout, DexInstruction>(iterator()) {
            @Override
            public Iterator<DexInstruction> iterator(DexLayout element) {
                return element.getDexInstructionsCloned();
            }
        };
    }

    @Override
    public DexClassRepository getRootRepository() {
        DexDirectory dexDirectory = getDexDirectory();
        if (dexDirectory != null) {
            return dexDirectory.getRootRepository();
        }
        return this;
    }
    @Override
    public Iterator<DexClassModule> modules() {
        return ObjectsUtil.cast(iterator());
    }

    public int clearDuplicateData(){
        int result = 0;
        for(DexLayout dexLayout : this){
            result += dexLayout.clearDuplicateData();
        }
        return result;
    }
    public int clearUnused() {
        return getContainerBlock().clearUnused();
    }

    @Override
    public int shrink() {
        int result = clearUnused();
        for (DexLayout layout : this) {
            result += layout.shrink();
        }
        return result;
    }

    public boolean merge(DexClass dexClass){
        return merge(new DexMergeOptions(true), dexClass);
    }
    public boolean merge(MergeOptions options, DexClass dexClass){
        return this.merge(options, dexClass.getId());
    }
    public boolean merge(ClassId classId){
        return merge(new DexMergeOptions(true), classId);
    }
    public boolean merge(MergeOptions options, ClassId classId){
        return getOrCreateFirst().merge(options, classId);
    }
    public boolean merge(MergeOptions options, DexFile dexFile){
        if(dexFile == null){
            return false;
        }
        DexLayout dexLayout = dexFile.getFirst();
        if (dexLayout == null) {
            return false;
        }
        return getOrCreateFirst().merge(options, dexLayout);
    }
    public boolean merge(MergeOptions options, DexLayout dexLayout){
        if(dexLayout == null || dexLayout.isEmpty()){
            return false;
        }
        return getOrCreateFirst().merge(options, dexLayout);
    }
    public void combineFrom(DexLayout dexLayout) {
        getContainerBlock().combineFrom(dexLayout.getDexLayoutBlock());
    }
    public void combineFrom(MergeOptions options, DexLayout dexLayout) {
        getContainerBlock().combineFrom(options, dexLayout.getDexLayoutBlock());
    }
    public void combineFrom(DexFile dexFile) {
        getContainerBlock().combineFrom(dexFile.getContainerBlock());
    }
    public void combineFrom(MergeOptions options, DexFile dexFile) {
        getContainerBlock().combineFrom(options, dexFile.getContainerBlock());
    }
    private void requireNotClosed() throws IOException {
        if(isClosed()){
            throw new IOException("Closed");
        }
    }
    public boolean isClosed() {
        return closed;
    }
    @Override
    public void close() {
        closed = true;
        int size = size();
        for (int i = 0; i < size; i++) {
            DexLayout layout = getLayout(i);
            if (layout != null) {
                layout.close();
            }
        }
        getContainerBlock().clear();
    }
    public byte[] getBytes() {
        if(isEmpty()){
            return new byte[0];
        }
        return getContainerBlock().getBytes();
    }
    public void readBytes(BlockReader reader) throws IOException {
        getContainerBlock().readBytes(reader);
    }
    public void readBytes(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        getContainerBlock().readBytes(reader, filter);
    }
    public void write(File file) throws IOException {
        OutputStream outputStream = FileUtil.outputStream(file);;
        write(outputStream);
        outputStream.close();
    }
    public void write(OutputStream outputStream) throws IOException {
        getContainerBlock().writeBytes(outputStream);
    }
    public void parseSmaliDirectory(File dir) throws IOException {
        parseSmaliDirectory(null, dir);
    }
    public void parseSmaliDirectory(SmaliReaderSetting readerSetting, File dir) throws IOException {
        File fileInfo = new File(dir, DexFileInfo.FILE_NAME);
        if (fileInfo.isFile()) {
            DexFileInfo.readJson(fileInfo).applyTo(this);
        }
        List<File> layoutDir = listSmaliLayouts(dir);
        if (layoutDir != null) {
            int size = layoutDir.size();
            for (int i = 0; i < size; i++) {
                File file = layoutDir.get(i);
                DexLayout layout = getOrCreateAt(i);
                layout.parseSmaliDirectory(readerSetting, file);
                shrink();
            }
        } else {
            getOrCreateFirst().parseSmaliDirectory(readerSetting, dir);
        }
    }
    @Deprecated
    public void writeSmali(SmaliWriter writer, File root) throws IOException {
        requireNotClosed();
        root = new File(root, buildSmaliDirectoryName());
        DexFileInfo fileInfo = DexFileInfo.fromDex(this);
        fileInfo.saveToDirectory(root);
        if (!isMultiLayout()) {
            DexLayout first = getFirst();
            if (first != null) {
                first.writeSmali(writer, root);
            }
        } else {
            int size = size();
            for (int i = 0; i < size; i++) {
                DexLayout dexLayout = getLayout(i);
                String name = DexLayout.DIRECTORY_PREFIX + i;
                File dir = new File(root, name);
                dexLayout.writeSmali(writer, dir);
            }
        }
    }
    public void writeSmali(SmaliWriterSetting writerSetting, File root) throws IOException {
        requireNotClosed();
        DexFileInfo fileInfo = DexFileInfo.fromDex(this);
        fileInfo.saveToDirectory(root);
        if (!isMultiLayout()) {
            DexLayout first = getFirst();
            if (first != null) {
                first.writeSmali(writerSetting, root);
            }
        } else {
            int size = size();
            for (int i = 0; i < size; i++) {
                DexLayout dexLayout = getLayout(i);
                String name = DexLayout.DIRECTORY_PREFIX + i;
                File dir = new File(root, name);
                dexLayout.writeSmali(writerSetting, dir);
            }
        }
    }

    private List<File> listSmaliLayouts(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        List<File> results = new ArrayCollection<>();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().equals(DexFileInfo.FILE_NAME)) {
                    continue;
                }
                return null;
            }
            if (isLayoutDirectory(file)) {
                results.add(file);
            } else {
                return null;
            }
        }
        if (results.isEmpty()) {
            return null;
        }
        java.util.Collections.sort(results, CompareUtil.getToStringComparator());
        return results;
    }
    private boolean isLayoutDirectory(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        String name = dir.getName();
        String prefix = DexLayout.DIRECTORY_PREFIX;
        if (!name.startsWith(prefix)) {
            return false;
        }
        name = name.substring(prefix.length());
        try {
            int i = Integer.parseInt(name);
            return i >= 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
    public String getFileName(){
        String simpleName = getSimpleName();
        if(simpleName == null){
            return buildSmaliDirectoryName() + ".dex";
        }
        return FileUtil.getFileName(simpleName);
    }
    public String buildSmaliDirectoryName() {
        String name = getSimpleName();
        if(name != null && name.endsWith(".dex")) {
            return name.substring(0, name.length() - 4);
        }
        DexDirectory dexDirectory = getDexDirectory();
        if(dexDirectory == null) {
            return "classes";
        }
        int i = 0;
        for(DexFile dexFile : dexDirectory){
            if(dexFile == this){
                break;
            }
            i++;
        }
        if(i == 0){
            return "classes";
        }
        i++;
        return "classes" + i;
    }


    public String printSectionInfo(boolean hex) {
        int size = size();
        if (size == 0) {
            return "no layouts";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.append('\n');
            }
            DexLayout layout = getLayout(i);
            builder.append(layout.printSectionInfo(hex));
        }
        return builder.toString();
    }


    public int getDexClassesCountForDebug() {
        int count = 0;
        int size = size();
        for (int i = 0; i < size; i++) {
            count += getLayout(i).getDexClassesCountForDebug();
        }
        return count;
    }
    @Override
    public String toString() {
        int size = size();
        if (size == 0) {
            return "EMPTY DEX FILE";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.append('\n');
            }
            builder.append(getLayout(i));
        }
        if (isMultiLayout()) {
            builder.append('\n');
            builder.append("total classes = ");
            builder.append(getDexClassesCountForDebug());
        }
        return builder.toString();
    }

    public static DexFile createDefault() {
        return new DexFile(DexContainerBlock.createDefault());
    }
    public static DexFile createNew() {
        return new DexFile(new DexContainerBlock());
    }

    public static int getDexFileNumber(String name){
        int i = name.lastIndexOf('/');
        if(i < 0){
            i = name.lastIndexOf('\\');
        }
        if(i >= 0){
            name = name.substring(i + 1);
        }
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
            return Integer.parseInt(num);
        }catch (NumberFormatException ignored){
            return -1;
        }
    }

    public static String getDexName(int i) {
        if (i == 0) {
            return "classes.dex";
        }
        if(i == 1){
            i = 2;
        }
        return "classes" + i + ".dex";
    }



    public static DexFile read(byte[] dexBytes) throws IOException {
        return read(dexBytes, null);
    }
    public static DexFile read(File file) throws IOException {
        DexFile dexFile = read(file, null);
        dexFile.setSimpleName(file.getName());
        return dexFile;
    }
    public static DexFile read(InputStream inputStream) throws IOException {
        return read(inputStream, null);
    }
    public static DexFile read(BlockReader reader) throws IOException {
        return read(reader, null);
    }


    public static DexFile read(byte[] dexBytes, Predicate<SectionType<?>> filter) throws IOException {
        return read(new BlockReader(dexBytes), filter);
    }
    public static DexFile read(File file, Predicate<SectionType<?>> filter) throws IOException {
        DexFile dexFile = read(new BlockReader(file), filter);
        dexFile.setSimpleName(file.getName());
        return dexFile;
    }
    public static DexFile read(InputStream inputStream, Predicate<SectionType<?>> filter) throws IOException {
        return read(new BlockReader(inputStream), filter);
    }
    public static DexFile read(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        DexFile dexFile = new DexFile(new DexContainerBlock());
        dexFile.readBytes(reader, filter);
        return dexFile;
    }
}
