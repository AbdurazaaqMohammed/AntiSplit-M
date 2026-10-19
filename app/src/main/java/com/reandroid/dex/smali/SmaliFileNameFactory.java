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

// Copied from https://github.com/JesusFreke/smali/blob/master/util/src/main/java/org/jf/util/ClassFileNameHandler.java

package com.reandroid.dex.smali;

import com.reandroid.dex.key.TypeKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class SmaliFileNameFactory {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int NUMERIC_SUFFIX_RESERVE = 6;


    private final DirectoryEntry top;
    private final String fileExtension;
    private final CaseSensitivity caseSensitivity;

    public SmaliFileNameFactory(File path) {
        this(path, ".smali");
    }
    public SmaliFileNameFactory(File path, String fileExtension) {
        this.fileExtension = fileExtension;
        this.caseSensitivity = new CaseSensitivity();
        this.top = new DirectoryEntry(caseSensitivity, path);
    }

    public SmaliFileNameFactory(File path, String fileExtension, boolean caseSensitive,
                                boolean modifyWindowsReservedFilenames) {
        this.fileExtension = fileExtension;
        this.caseSensitivity = new CaseSensitivity();
        this.caseSensitivity.setCaseSensitive(caseSensitive);
        this.caseSensitivity.setModifyWindowsReservedFilenames(modifyWindowsReservedFilenames);
        this.top = new DirectoryEntry(caseSensitivity, path);
    }

    private int getMaxFilenameLength() {
        return MAX_FILENAME_LENGTH - NUMERIC_SUFFIX_RESERVE;
    }

    public File getUniqueFilenameForClass(TypeKey typeKey) throws IOException {
        return getUniqueFilenameForClass(typeKey.getTypeName());
    }
    public File getUniqueFilenameForClass(String className) throws IOException {
        int length = className.length() - 1;
        if (className.charAt(0) != 'L' || className.charAt(length) != ';') {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        int packageElementCount = 1;
        for (int i = 1; i < length; i++) {
            if (className.charAt(i) == '/') {
                packageElementCount ++;
            }
        }

        String[] packageElements = new String[packageElementCount];
        int elementIndex = 0;
        int elementStart = 1;
        for (int i = 1; i < length; i++) {
            if (className.charAt(i) == '/') {
                if (i - elementStart == 0) {
                    throw new RuntimeException("Not a valid dalvik class name: " + className);
                }

                packageElements[elementIndex++] = className.substring(elementStart, i);
                elementStart = ++i;
            }
        }

        if (elementStart >= length) {
            throw new RuntimeException("Not a valid dalvik class name: " + className);
        }

        packageElements[elementIndex] = className.substring(elementStart, className.length()-1);

        return addUniqueChild(top, packageElements, 0);
    }

    private File addUniqueChild(DirectoryEntry parent, String[] packageElements,
                                int packageElementIndex) throws IOException {
        if (packageElementIndex == packageElements.length - 1) {
            FileEntry fileEntry = new FileEntry(parent,
                    packageElements[packageElementIndex] + fileExtension);
            parent.addChild(fileEntry);

            String physicalName = fileEntry.getPhysicalName();

            return new File(parent.file, physicalName);
        } else {
            DirectoryEntry directoryEntry = new DirectoryEntry(parent, packageElements[packageElementIndex]);
            directoryEntry = (DirectoryEntry)parent.addChild(directoryEntry);
            return addUniqueChild(directoryEntry, packageElements, packageElementIndex+1);
        }
    }

    private static int utf8Length(String str) {
        int utf8Length = 0;
        int i=0;
        while (i<str.length()) {
            int c = str.codePointAt(i);
            utf8Length += utf8Length(c);
            i += Character.charCount(c);
        }
        return utf8Length;
    }

    private static int utf8Length(int codePoint) {
        if (codePoint < 0x80) {
            return 1;
        } else if (codePoint < 0x800) {
            return 2;
        } else if (codePoint < 0x10000) {
            return 3;
        } else {
            return 4;
        }
    }


    static String shortenPathComponent(String pathComponent, int bytesToRemove) {
        bytesToRemove++;

        int[] codePoints;
        try {
            IntBuffer intBuffer = ByteBuffer.wrap(pathComponent.getBytes("UTF-32BE")).asIntBuffer();
            codePoints = new int[intBuffer.limit()];
            intBuffer.get(codePoints);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        int midPoint = codePoints.length/2;

        int firstEnd = midPoint;
        int secondStart = midPoint + 1;
        int bytesRemoved = utf8Length(codePoints[midPoint]);

        if (((codePoints.length % 2) == 0) && bytesRemoved < bytesToRemove) {
            bytesRemoved += utf8Length(codePoints[secondStart]);
            secondStart++;
        }

        while ((bytesRemoved < bytesToRemove) &&
                (firstEnd > 0 || secondStart < codePoints.length)) {
            if (firstEnd > 0) {
                firstEnd--;
                bytesRemoved += utf8Length(codePoints[firstEnd]);
            }

            if (bytesRemoved < bytesToRemove && secondStart < codePoints.length) {
                bytesRemoved += utf8Length(codePoints[secondStart]);
                secondStart++;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < firstEnd; i++) {
            sb.appendCodePoint(codePoints[i]);
        }
        sb.append('#');
        for (int i = secondStart; i < codePoints.length; i++) {
            sb.appendCodePoint(codePoints[i]);
        }

        return sb.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("windows");
    }

    private static final Pattern reservedFileNameRegex = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$",
            Pattern.CASE_INSENSITIVE);
    private static boolean isReservedFileName(String className) {
        return reservedFileNameRegex.matcher(className).matches();
    }

    static class CaseSensitivity {
        private boolean caseSensitivityTested;
        private boolean caseSensitive;

        private boolean modifyWindowsReservedFilenames;

        public CaseSensitivity() {
            this.modifyWindowsReservedFilenames = isWindows();
        }

        public boolean isCaseSensitive(File file) {
            if (!caseSensitivityTested) {
                caseSensitivityTested = true;
                try {
                    this.caseSensitive = testCaseSensitivity(file);
                } catch (IOException ignored) {
                }
            }
            return caseSensitive;
        }

        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitivityTested = true;
            this.caseSensitive = caseSensitive;
        }

        public boolean isModifyWindowsReservedFilenames() {
            return modifyWindowsReservedFilenames;
        }
        public void setModifyWindowsReservedFilenames(boolean value) {
            this.modifyWindowsReservedFilenames = value;
        }

        private boolean testCaseSensitivity(File path) throws IOException {
            int num = 1;
            File f;
            File f2;
            do {
                f = new File(path, "test." + num);
                f2 = new File(path, "TEST." + num++);
            } while(f.exists() || f2.exists());

            try {
                try {
                    FileWriter writer = new FileWriter(f);
                    writer.write("test");
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    safeDelete(f, f2);
                    throw ex;
                }

                if (f2.exists()) {
                    safeDelete(f, f2);
                    return false;
                }
                if (f2.createNewFile()) {
                    safeDelete(f, f2);
                    return true;
                }
                try {
                    CharBuffer buf = CharBuffer.allocate(32);
                    FileReader reader = new FileReader(f2);

                    while (reader.read(buf) != -1 && buf.length() < 4);

                    if (buf.length() == 4 && buf.toString().equals("test")) {
                        return false;
                    }
                    return false;
                } catch (FileNotFoundException ex) {
                    return true;
                }
            } finally {
                safeDelete(f, f2);
            }
        }
        private void safeDelete(File file1, File file2) {
            try {
                file1.delete();
            } catch (Exception ignored) {}
            try {
                file2.delete();
            } catch (Exception ignored) {}
        }
    }
    private abstract class FileSystemEntry {
        public final CaseSensitivity caseSensitivity;
        public final DirectoryEntry parent;
        public final String logicalName;
        protected String physicalName = null;

        private FileSystemEntry(CaseSensitivity caseSensitivity, DirectoryEntry parent, String logicalName) {
            this.caseSensitivity = caseSensitivity;
            this.parent = parent;
            this.logicalName = logicalName;
        }

        public String getNormalizedName(boolean preserveCase) {
            String elementName = logicalName;
            if (!preserveCase && parent != null && !parent.isCaseSensitive()) {
                elementName = elementName.toLowerCase(java.util.Locale.ROOT);
            }

            if (caseSensitivity.isModifyWindowsReservedFilenames() && isReservedFileName(elementName)) {
                elementName = addSuffixBeforeExtension(elementName, "#");
            }

            int utf8Length = utf8Length(elementName);
            if (utf8Length > getMaxFilenameLength()) {
                elementName = shortenPathComponent(elementName, utf8Length - getMaxFilenameLength());
            }
            return elementName;
        }

        public String getPhysicalName() {
            return physicalName;
        }

        public void setSuffix(int suffix) throws IOException {
            if (suffix < 0 || suffix > 99999) {
                throw new IllegalArgumentException("suffix must be in [0, 100000)");
            }

            if (this.physicalName != null) {
                throw new IllegalStateException("The suffix can only be set once");
            }
            String physicalName = getPhysicalNameWithSuffix(suffix);
            File file = new File(parent.file, physicalName).getCanonicalFile();
            this.physicalName = file.getName();
            createIfNeeded();
        }

        protected abstract void createIfNeeded() throws IOException;

        public abstract String getPhysicalNameWithSuffix(int suffix);
    }

    private class DirectoryEntry extends FileSystemEntry {
        private File file = null;

        private final Map<String, Set<FileSystemEntry>> children = new HashMap<>();
        private final Map<String, FileSystemEntry> physicalToEntry = new HashMap<>();
        private final Map<String, Integer> lastSuffixMap = new HashMap<>();

        public DirectoryEntry(CaseSensitivity caseSensitivity, File path) {
            super(caseSensitivity, null, path.getName());
            file = path;
            physicalName = file.getName();
        }

        public DirectoryEntry(DirectoryEntry parent, String logicalName) {
            super(parent.caseSensitivity, parent, logicalName);
        }

        public synchronized FileSystemEntry addChild(FileSystemEntry entry) throws IOException {
            String normalizedChildName = entry.getNormalizedName(false);
            Set<FileSystemEntry> entries = children.get(normalizedChildName);
            if (entries == null) {
                entries = new HashSet<>();
                children.put(normalizedChildName, entries);
            }
            if (entry instanceof DirectoryEntry) {
                for (FileSystemEntry childEntry: entries) {
                    if (childEntry.logicalName.equals(entry.logicalName)) {
                        return childEntry;
                    }
                }
            }

            Integer lastSuffix = lastSuffixMap.get(normalizedChildName);
            if (lastSuffix == null) {
                lastSuffix = -1;
            }

            int suffix = lastSuffix;
            while (true) {
                suffix++;

                String entryPhysicalName = entry.getPhysicalNameWithSuffix(suffix);
                File entryFile = new File(this.file, entryPhysicalName);
                entryPhysicalName = entryFile.getCanonicalFile().getName();

                if (!this.physicalToEntry.containsKey(entryPhysicalName)) {
                    entry.setSuffix(suffix);
                    lastSuffixMap.put(normalizedChildName, suffix);
                    physicalToEntry.put(entry.getPhysicalName(), entry);
                    break;
                }
            }
            entries.add(entry);
            return entry;
        }

        @Override
        public String getPhysicalNameWithSuffix(int suffix) {
            if (suffix > 0) {
                return getNormalizedName(true) + "." + suffix;
            }
            return getNormalizedName(true);
        }

        @Override
        protected void createIfNeeded() throws IOException {
            String physicalName = getPhysicalName();
            if (parent != null && physicalName != null) {
                file = new File(parent.file, physicalName).getCanonicalFile();
                file.mkdirs();
            }
        }

        protected boolean isCaseSensitive() {
            if (getPhysicalName() == null || file == null) {
                throw new IllegalStateException("Must call setSuffix() first");
            }
            File path = file;
            if (path.exists() && path.isFile()) {
                if (!path.delete()) {
                    throw new IllegalArgumentException("Can't delete  '" +
                            path.getAbsolutePath() + "' to make it into a directory");
                }
            }

            if (!path.exists() && !path.mkdirs()) {
                throw new IllegalArgumentException("Couldn't create directory " + path.getAbsolutePath());
            }
            return caseSensitivity.isCaseSensitive(path);
        }

    }

    private class FileEntry extends FileSystemEntry {

        private FileEntry(DirectoryEntry parent, String logicalName) {
            super(parent.caseSensitivity, parent, logicalName);
        }

        @Override
        public String getPhysicalNameWithSuffix(int suffix) {
            if (suffix > 0) {
                return addSuffixBeforeExtension(getNormalizedName(true), '.' + Integer.toString(suffix));
            }
            return getNormalizedName(true);
        }

        @Override
        protected void createIfNeeded() throws IOException {
            String physicalName = getPhysicalName();
            if (parent != null && physicalName != null) {
                File file = new File(parent.file, physicalName).getCanonicalFile();
                file.createNewFile();
            }
        }
    }

    private static String addSuffixBeforeExtension(String pathElement, String suffix) {
        int extensionStart = pathElement.lastIndexOf('.');

        StringBuilder newName = new StringBuilder(pathElement.length() + suffix.length() + 1);
        if (extensionStart < 0) {
            newName.append(pathElement);
            newName.append(suffix);
        } else {
            newName.append(pathElement.subSequence(0, extensionStart));
            newName.append(suffix);
            newName.append(pathElement.subSequence(extensionStart, pathElement.length()));
        }
        return newName.toString();
    }
}

