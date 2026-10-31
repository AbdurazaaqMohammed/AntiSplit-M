package com.abdurazaaqmohammed.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    public static File copyFileFromAssetsAndGetFile(String fileName, Context context) throws IOException {
        File destinationFile = new File(context.getFilesDir(), fileName);
        if (!destinationFile.exists())
            try (InputStream is = context.getAssets().open(fileName)) {
                copyFile(is, destinationFile);
            }
        return destinationFile;
    }

    public static File getUnusedFile(File file) {
        int index = 0;
        String fullName = file.getName();
        int dot = fullName.lastIndexOf('.');
        String name = dot > 0 ? fullName.substring(0, dot) : fullName;
        String extension = dot > 0 ? fullName.substring(dot + 1) : "";
        File parent = file.getParentFile();
        File candidate = file;
        while (candidate.exists()) {
            index++;
            String newName = name.replaceFirst("_\\d+$", "") + '_' + index
                    + (extension.isEmpty() ? "" : '.' + extension);
            candidate = parent == null ? new File(newName) : new File(parent, newName);
        }
        return candidate;
    }

    public static File getUnusedFile(String file) {
        return getUnusedFile(new File(file));
    }

    public static OutputStream getOutputStream(String filepath) throws IOException {
        return getOutputStream(new File(filepath));
    }

    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        try (InputStream is = getInputStream(sourceFile);
             OutputStream os = getOutputStream(destinationFile)) {
            copyFile(is, os);
        }
    }

    public static void copyFile(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int length;
        while ((length = is.read(buffer)) != -1)
            os.write(buffer, 0, length);
        os.flush();
    }

    public static InputStream getInputStream(File file) throws IOException {
        return new FileInputStream(file);
    }

    public static InputStream getInputStream(String filePath) throws IOException {
        return getInputStream(new File(filePath));
    }

    public static void copyFile(InputStream is, File destinationFile) throws IOException {
        try (OutputStream os = getOutputStream(destinationFile)) {
            copyFile(is, os);
        }
    }

    public static void copyFile(File in, OutputStream os) throws IOException {
        try (InputStream is = getInputStream(in)) {
            copyFile(is, os);
        }
    }

    public static OutputStream getOutputStream(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Unable to create directory: " + parent);
        return new FileOutputStream(file);
    }

    public static boolean hasStoragePermission(Context context) {
        return !doesNotHaveStoragePerm(context);
    }

    public static boolean doesNotHaveStoragePerm(Context context) {
        if (Build.VERSION.SDK_INT <= 22)
            return false;
        if (LegacyUtils.supportsWriteExternalStorage)
            return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED;
        return !Environment.isExternalStorageManager();
    }

    public static String sanitizeFileName(String name) {
        if (name == null)
            return "merged.apk";
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("^\\.+", "_")
                .trim();
        return sanitized.isEmpty() ? "merged.apk" : sanitized;
    }

    public static File getAntisplitMFolder() {
        File folder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        if (folder.exists() || folder.mkdirs())
            return folder;
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloads.exists() || downloads.mkdirs())
            return downloads;
        return Environment.getExternalStorageDirectory();
    }

    public static void removeEmptyOutputFolder(Context context) {
        if (doesNotHaveStoragePerm(context))
            return;
        File folder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        File[] children = folder.listFiles();
        if (children == null)
            return;
        for (File child : children) {
            if (child.isFile() && child.length() == 0)
                child.delete();
        }
        File[] remaining = folder.listFiles();
        if (remaining == null || remaining.length == 0)
            folder.delete();
    }
}
