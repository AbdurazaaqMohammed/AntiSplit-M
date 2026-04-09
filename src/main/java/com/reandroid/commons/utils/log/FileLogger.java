package com.reandroid.commons.utils.log;

import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLogger extends WriterLogger {
    
    private final Object mLock = new Object();
    private File mFile;
    
    public FileLogger(File file) throws IOException {
        super(createWriter(file), countLines(file));
        this.mFile = file;
    }
    
    public File getFile() {
        return mFile;
    }
    public boolean setFile(File file) throws IOException {
        File prev = getFile();
        if (ObjectsUtil.equals(prev, file)) {
            return false;
        }
        synchronized (mLock) {
            Writer writer = createWriter(file);
            super.setWriter(writer);
            super.resetCount();
            writer.write("// Previous file: " + prev);
            writer.flush();
            this.mFile = file;
            return true;
        }
    }
    @Override
    void onTotalCount(Writer writer, int totalCount) {
        if (totalCount < MAX_LINES) {
            return;
        }
        File newFile = getLastNumberedFile(getFile());
        newFile = getNextNumberedFile(newFile);
        try {
            if (setFile(newFile)) {
                writer.close();
            }
        } catch (IOException ignored) {
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
        return ObjectsUtil.equals(getFile(), ((FileLogger) o).getFile());
    }
    @Override
    public int hashCode() {
        return ObjectsUtil.hash(getFile());
    }
    @Override
    public String toString() {
        return "FileLogger{" + getFile() +'}';
    }

    private static Writer createWriter(File file) throws IOException {
        return new OutputStreamWriter(FileUtil.outputStream(file), StandardCharsets.UTF_8);
    }
    public static FileLogger create(File file) {
        try {
            file = getLastNumberedFile(file);
            file = prepare(file);
            FileLogger fileLogger= new FileLogger(file);
            fileLogger.setEnable(true);
            fileLogger.setIgnoreSameLine(true);
            return fileLogger;
        } catch (IOException exception) {
            return null;
        }
    }
    public static File prepare(File file) {
        FileUtil.ensureParentDirectory(file);
        int lines = countLines(file);
        if (lines < MAX_LINES) {
            return file;
        }
        File next = getNextNumberedFile(file);
        if (file.equals(next)) {
            // will not happen
            return file;
        }
        return prepare(next);
    }
    private static int countLines(File file) {
        try {
            return countContentLines(file);
        } catch (IOException exception) {
            return 0;
        }
    }
    private static int countContentLines(File file) throws IOException{
        if (file == null) {
            throw new IOException("file == null");
        }
        if (!file.isFile()) {
            return 0;
        }
        int result = 0;
        InputStreamReader inputStreamReader= new InputStreamReader(new FileInputStream(file));
        final BufferedReader reader = new BufferedReader(inputStreamReader);
        while (reader.readLine() != null) {
            result++;
        }
        reader.close();
        return result;
    }

    public static File getNextNumberedFile(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        String ext = "";
        int i = name.lastIndexOf(".");
        if (i>0) {
            ext=name.substring(i);
            name=name.substring(0,i);
        }
        i = getLastNum(name) + 1;
        name = removeLastNum(name);
        File dir=file.getParentFile();
        return new File(dir,buildNumberFileName(name, i, ext));
    }
    public static File getLastNumberedFile(File file) {
        if (file == null) {
            return null;
        }
        if (!file.isFile()) {
            return file;
        }
        String name = file.getName();
        String ext = "";
        int i = name.lastIndexOf(".");
        if (i > 0) {
            ext = name.substring(i);
            name = name.substring(0,i);
        }
        File dir = file.getParentFile();
        File[] files = dir.listFiles();
        if (files == null) {
            return file;
        }
        i = getLastNum(name) + 1;
        name = removeLastNum(name);
        File result=file;
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            String n = f.getName();
            if (!n.startsWith(name) || !n.endsWith(ext)) {
                continue;
            }
            int i2 = getLastNum(f);
            if (i2 <= i) {
                continue;
            }
            result = f;
            i = i2;
        }
        return result;
    }
    private static int getLastNum(File file) {
        String name = file.getName();
        int i = name.lastIndexOf(".");
        if (i > 0) {
            name = name.substring(0,i);
        }
        Pattern pattern = Pattern.compile("^.+_([0-9]{1,6})$");
        Matcher matcher = pattern.matcher(name);
        if (!matcher.find()) {
            return 0;
        }
        String num = matcher.group(1);
        return Integer.parseInt(num);
    }

    private static String buildNumberFileName(String name, int i, String ext) {
        String num;
        if (i < 10000) {
            num = String.format("%04d",i);
        }else {
            num = String.valueOf(i);
        }
        return name + "_" + num + ext;
    }
    private static String removeLastNum(String name) {
        Pattern pattern = Pattern.compile("^.+(_[0-9]{1,6})$");
        Matcher matcher = pattern.matcher(name);
        if (!matcher.find()) {
            return name;
        }
        String num = matcher.group(1);
        int i = name.length() - num.length();
        return name.substring(0, i);
    }
    private static int getLastNum(String name) {
        Pattern pattern = Pattern.compile("^.+_([0-9]{1,6})$");
        Matcher matcher = pattern.matcher(name);
        if (!matcher.find()) {
            return 0;
        }
        String num = matcher.group(1);
        return Integer.parseInt(num);
    }

    private static final int MAX_LINES = 25000;
}
