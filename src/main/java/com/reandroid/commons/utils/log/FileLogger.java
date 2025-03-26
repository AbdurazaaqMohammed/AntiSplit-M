package com.reandroid.commons.utils.log;

import com.reandroid.commons.utils.FileUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class FileLogger extends WriterLogger{
    private final Object mLock=new Object();
    private File mFile;
    public FileLogger(File file) throws IOException {
        super(createWriter(file), countLines(file));
        this.mFile=file;
    }
    public File getFile(){
        return mFile;
    }
    public boolean setFile(File file) throws IOException {
        File prev=getFile();
        if(Objects.equals(prev, file)){
            return false;
        }
        synchronized (mLock){
            Writer writer= createWriter(file);
            super.setWriter(writer);
            super.resetCount();
            writer.write("// Previous file: "+prev);
            writer.flush();
            this.mFile=file;
            return true;
        }
    }
    @Override
    void onTotalCount(Writer writer, int totalCount){
        if(totalCount<MAX_LINES){
            return;
        }
        File newFile=FileUtil.getLastNumberedFile(getFile());
        newFile=FileUtil.getNextNumberedFile(newFile);
        try {
            boolean setOk=setFile(newFile);
            if(setOk){
                writer.close();
            }
        } catch (IOException exception) {
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileLogger that = (FileLogger) o;
        return Objects.equals(getFile(), that.getFile());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getFile());
    }
    @Override
    public String toString() {
        return "FileLogger{" + getFile() +'}';
    }

    private static Writer createWriter(File file) throws IOException {
        File dir=file.getParentFile();
        if(dir!=null && !dir.exists()){
            dir.mkdirs();
        }
        FileOutputStream outputStream=new FileOutputStream(file, true);
        OutputStreamWriter writer=new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
        return writer;
    }
    public static FileLogger create(File file){
        try {
            file=FileUtil.getLastNumberedFile(file);
            file=prepare(file);
            FileLogger fileLogger=new FileLogger(file);
            fileLogger.setEnable(true);
            fileLogger.setIgnoreSameLine(true);
            return fileLogger;
        } catch (IOException exception) {
            return null;
        }
    }
    public static File prepare(File file){
        File dir=file.getParentFile();
        if(dir!=null && !dir.exists()){
            dir.mkdirs();
        }
        int lines=countLines(file);
        if(lines<MAX_LINES){
            return file;
        }
        File next=FileUtil.getNextNumberedFile(file);
        if(file.equals(next)){
            // will not happen
            return file;
        }
        return prepare(next);
    }
    private static int countLines(File file){
        try {
            return FileUtil.countLines(file);
        } catch (IOException exception) {
            return 0;
        }
    }
    private static final int MAX_LINES=25000;
}
