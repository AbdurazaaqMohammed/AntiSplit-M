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
package com.reandroid.utils.io;

import com.reandroid.arsc.ARSCLib;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class FileUtil {

    private static int caseInsensitiveFs;

    public static boolean isCaseInsensitiveFileSystem() {
        int i = caseInsensitiveFs;
        if (i == 0) {
            testCaseInsensitiveFileSystem();
            i = caseInsensitiveFs;
        }
        return i == 2;
    }
    public static boolean isCaseSensitiveFileSystem() {
        return !isCaseInsensitiveFileSystem() && caseInsensitiveFs == 1;
    }
    private static void testCaseInsensitiveFileSystem() {
        synchronized (FileUtil.class) {
            String name = "arsclib_test_case_insensitive";
            File tmp = getTempDir();
            File lowerCase = new File(tmp, name);
            File upperCase = new File(tmp, name.toUpperCase(java.util.Locale.ROOT));
            deleteFileSilent(lowerCase);
            deleteFileSilent(upperCase);
            int i = 3;
            if (!lowerCase.exists() && !upperCase.exists()) {
                try {
                    ensureParentDirectory(lowerCase);
                    FileWriter writer = new FileWriter(lowerCase);
                    writer.write("test");
                    writer.close();
                    if (lowerCase.exists()) {
                        i = upperCase.exists() ? 2 : 1;
                    }
                } catch (Throwable ignored) {}
            }
            deleteFileSilent(lowerCase);
            deleteFileSilent(upperCase);
            caseInsensitiveFs = i;
        }
    }
    private static void deleteFileSilent(File file) {
        try {
            if (file != null && file.exists()) {
                file.delete();
            }
        } catch (Throwable ignored) {}
    }

    public static List<File> listClassesDex(File dir) {
        List<File> results = new ArrayCollection<>();
        if (dir == null || !dir.isDirectory()) {
            return results;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && getClassesDexNumber(file) != -1) {
                    results.add(file);
                }
            }
        }
        java.util.Collections.sort(results, ((file1, file2) -> CompareUtil.compare(
                getClassesDexNumber(file1),
                getClassesDexNumber(file2))));
        return results;
    }
    public static int getClassesDexNumber(File file) {
        if (file != null) {
            return getClassesDexNumber(file.getName());
        }
        return -1;
    }
    public static int getClassesDexNumber(String fileName) {
        if (fileName == null) {
            return -1;
        }
        if (fileName.equals("classes.dex")) {
            return 0;
        }
        String prefix = "classes";
        String ext = ".dex";
        if (!fileName.startsWith(prefix) || !fileName.endsWith(ext)) {
            return -1;
        }
        String numStr = fileName.substring(prefix.length(), fileName.length() - ext.length());
        try {
            int num = Integer.parseInt(numStr);
            if (num < 2) { // no classes0.dex or classes1.dex
                return -1;
            }
            return num;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
    public static File toTmpName(File file) {
        File dir = file.getParentFile();
        String name = file.getName() + ".tmp";
        if (dir == null) {
            return new File(name);
        }
        return new File(dir, name);
    }
    public static File toUniqueTmpName(File file) {
        File dir = file.getParentFile();
        String name = file.getName();
        int max = Integer.MAX_VALUE;
        String suffix = ".tmp";
        String tmpName = name + suffix;
        for (int i = 0; i < max; i++) {
            File result;
            if (dir == null) {
                result = new File(tmpName);
            } else {
                result = new File(dir, tmpName);
            }
            if (!result.exists()) {
                return result;
            }
            tmpName = name + suffix + i;
        }
        throw new IllegalStateException("Failed to create tmp file tried = "
                + max + ": " + file);
    }
    public static String combineFilePath(String parent, String name) {
        return combinePath(File.separatorChar, parent, name);
    }
    public static String combineUnixPath(String parent, String name) {
        return combinePath('/', parent, name);
    }
    public static String combinePath(char separator, String parent, String name) {
        if (StringsUtil.isEmpty(parent)) {
            return name;
        }
        if (StringsUtil.isEmpty(name)) {
            return parent;
        }
        StringBuilder builder = new StringBuilder(parent.length() + name.length() + 1);
        builder.append(parent);
        if (parent.charAt(parent.length() - 1) != separator) {
            builder.append(separator);
        }
        builder.append(name);
        return builder.toString();
    }
    public static String shortPath(File file, int depth) {
        File tmp = file;
        while (depth > 0) {
            File dir = tmp.getParentFile();
            if (dir == null) {
                break;
            }
            tmp = dir;
            depth --;
        }
        if (file == tmp) {
            return file.getName();
        }
        int i = tmp.getAbsolutePath().length() + 1;
        return file.getAbsolutePath().substring(i);
    }
    public static String getParent(String path) {
        if (StringsUtil.isEmpty(path)) {
            return StringsUtil.EMPTY;
        }
        int i = path.lastIndexOf('/');
        if (i < 0) {
            i = path.lastIndexOf('\\');
        }
        if (i <= 0) {
            return StringsUtil.EMPTY;
        }
        return path.substring(0, i + 1);
    }
    public static String getFileName(String path) {
        if (path == null) {
            return null;
        }
        int i = path.lastIndexOf('/');
        if (i < 0) {
            i = path.lastIndexOf('\\');
        }
        if (i >= 0) {
            return path.substring(i + 1);
        }
        return path;
    }
    public static String getNameWoExtension(File file) {
        return getNameWoExtensionForSimpleName(file.getName());
    }
    public static String getNameWoExtension(String name) {
        return getNameWoExtensionForSimpleName(getFileName(name));
    }
    private static String getNameWoExtensionForSimpleName(String simpleName) {
        String ninePng = ".9.png";
        if (simpleName.endsWith(ninePng)) {
            return simpleName.substring(0, simpleName.length() - ninePng.length());
        }
        int i = simpleName.lastIndexOf('.');
        if (i < 0) {
            return simpleName;
        }
        return simpleName.substring(0, i);
    }

    public static String getExtension(File file) {
        return getExtensionForSimpleName(file.getName());
    }
    public static String getExtension(String name) {
        return getExtensionForSimpleName(getFileName(name));
    }
    private static String getExtensionForSimpleName(String simpleName) {
        String ninePng = ".9.png";
        if (simpleName.endsWith(ninePng)) {
            return ninePng;
        }
        int i = simpleName.lastIndexOf('.');
        if (i < 0) {
            return StringsUtil.EMPTY;
        }
        return simpleName.substring(i);
    }
    public static String toReadableFileSize(long size) {
        if (size < 0) {
            return Long.toString(size);
        }
        String[] sizeUnits = getFileSizeUnits();
        String unit = "";
        long result = size;
        long dec = 0;
        long div = 1024;
        int i;
        int length = sizeUnits.length;
        for (i = 0; i < length; i++) {
            unit = sizeUnits[i];
            size = size / div;
            if (size == 0) {
                break;
            }
            dec = (result - (size * div));
            result = size;
            div = 1000;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(result);
        if (i != 0) {
            builder.append('.');
            if (dec < 10) {
                builder.append("00");
            } else if (dec < 100) {
                builder.append("0");
            }
            builder.append(dec);
        }
        builder.append(unit);
        return builder.toString();
    }

    private static String[] getFileSizeUnits() {
        String[] sizeUnits = FILE_SIZE_UNITS;
        if (sizeUnits == null) {
            sizeUnits = new String[]{
                    " bytes",
                    " Kb",
                    " Mb",
                    " Gb",
                    " Tb",
                    " Pb"
            };
            FILE_SIZE_UNITS = sizeUnits;
        }
        return sizeUnits;
    }

    public static FileChannel openReadChannel(File file) throws IOException {
        if (!file.isFile()) {
            throw new FileNotFoundException("No such file: " + file);
        }
        return new FileInputStream(file).getChannel();
    }
    public static FileChannel openWriteChannel(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("File is directory: " + file);
        }
        ensureParentDirectory(file);
        if (!file.exists() || file.delete()) {
            file.createNewFile();
        }
        return new FileOutputStream(file).getChannel();
    }
    public static InputStream inputStream(File file) throws IOException{
        if (!file.isFile()) {
            throw new FileNotFoundException("No such file: " + file);
        }
        return new FileInputStream(file);
    }
    public static OutputStream outputStream(File file) throws IOException{
        ensureParentDirectory(file);
        return new FileOutputStream(file);
    }
    public static void ensureParentDirectory(File file) {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }
    public static void createNewFile(File file) throws IOException {
        ensureParentDirectory(file);
        if (file.isFile()) {
            file.delete();
        }
        file.createNewFile();
    }
    public static void deleteDirectory(File dir) {
        if (dir.isFile()) {
            dir.delete();
            return;
        }
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            dir.delete();
            return;
        }
        for(File file : files) {
            deleteDirectory(file);
        }
        dir.delete();
    }
    public static void deleteEmptyDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            dir.delete();
            return;
        }
        for(File file : files) {
            if (!file.isDirectory()) {
                return;
            }
            deleteEmptyDirectory(file);
        }
        deleteIfEmptyDirectory(dir);
    }
    private static void deleteIfEmptyDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            dir.delete();
        }
    }
    public static File getTempDir() {
        return getTempDir(null);
    }
    public static File getTempDir(String rootName) {
        synchronized (FileUtil.class) {
            if (rootName == null) {
                rootName = getDefRootName();
            }
            File dir = TEMP_DIRS.get(rootName);
            if (dir == null) {
                dir = getWritableTempDir(rootName);
                if (dir != null) {
                    dir.deleteOnExit();
                    TEMP_DIRS.put(rootName, dir);
                }
            }else if (!dir.exists()) {
                dir.mkdir();
                dir.deleteOnExit();
            }
            return dir;
        }
    }
    private static File getWritableTempDir(String rootName) {
        String path = System.getProperty("java.io.tmpdir", null);
        File dir = getWritableTempDir(path, rootName);
        if (dir == null) {
            path = System.getProperty("user.home", null);
            dir = getWritableTempDir(path, rootName);
        }
        if (dir == null) {
            File file = new File("tmp");
            file = new File(file.getAbsolutePath());
            dir = file.getParentFile();
            if (dir == null) {
                dir = file;
            }
            path = dir.getAbsolutePath();
            dir = getWritableTempDir(path, rootName);
        }
        return dir;
    }
    private static File getWritableTempDir(String path, String rootName) {
        if (path == null) {
            return null;
        }
        return getWritableTempDir(new File(path), rootName);
    }
    private static File getWritableTempDir(File baseDir, String rootName) {
        File dir = new File(baseDir, rootName);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return null;
        }
        String testName = "test_" + System.currentTimeMillis() + "-";
        int max = 9999;
        int i;
        for (i = 0; i < max; i++) {
            String name = testName + i;
            File file = new File(dir, name);
            if (file.exists()) {
                continue;
            }
            try {
                if (!file.createNewFile() || !file.delete()) {
                    return null;
                }
                return dir;
            } catch (IOException exception) {
                return null;
            }
        }
        return null;
    }

    public static void setDefaultTempPrefix(String prefix) {
        synchronized (FileUtil.class) {
            if (ObjectsUtil.equals(prefix, def_prefix)) {
                return;
            }
            if (def_prefix != null) {
                TEMP_DIRS.remove(def_prefix);
            }
            def_prefix = prefix;
        }
    }

    private static String getDefRootName() {
        if (def_prefix == null) {
            def_prefix = "tmp_" + ARSCLib.getName() + "-" + ARSCLib.getVersion();
        }
        return def_prefix;
    }

    private static String def_prefix;

    private static final Map<String, File> TEMP_DIRS = new HashMap<>();

    private static String[] FILE_SIZE_UNITS;
}
