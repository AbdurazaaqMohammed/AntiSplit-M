package com.starry;

import static com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    /*
    MIT License

    Copyright (c) 2023 Stɑrry Shivɑm

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

    https://github.com/starry-shivam/FileUtils/blob/main/file-utils/src/main/java/com/starry/file_utils/FileUtils.kt
     */

    public static OutputStream getOutputStream(Uri uri, MainActivity context) throws IOException {
        String uriPath;
        if(doesNotHaveStoragePerm(context) || (uriPath = uri.getPath()) == null || uriPath.startsWith("/document/msf:")) return context.getContentResolver().openOutputStream(uri);
        try {
            String filePath = getPath(uri, context);
            File file = filePath == null ? null : new File(filePath);
            return file != null && file.canWrite() ? com.abdurazaaqmohammed.utils.FileUtils.getOutputStream(file) : context.getContentResolver().openOutputStream(uri);
        } catch (Exception e) {
            return context.getContentResolver().openOutputStream(uri);
        }
    }

    public static InputStream getInputStream(Uri uri, MainActivity context) throws IOException {
        if(doesNotHaveStoragePerm(context)) return context.getContentResolver().openInputStream(uri);
        String filePath = getPath(uri, context);
        File file = filePath == null ? null : new File(filePath);
        return file != null && file.canRead() ? com.abdurazaaqmohammed.utils.FileUtils.getInputStream(file) : context.getContentResolver().openInputStream(uri);
    }

    private static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    private static String getPathFromExtSD(String[] pathData) {
        String type = pathData[0];
        String relativePath = File.separator + pathData[1];
        String fullPath;

        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory().toString() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        if ("home".equalsIgnoreCase(type)) {
            fullPath = "/storage/emulated/0/Documents" + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        if (secondaryStorage != null) {
            fullPath = secondaryStorage + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        String externalStorage = System.getenv("EXTERNAL_STORAGE");
        if (externalStorage != null) {
            fullPath = externalStorage + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        try {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDownloadsDocument(Uri uri) {
       try {
           return "com.android.providers.downloads.documents".equals(uri.getAuthority());
       } catch (Exception e) {
           return false;
       }
    }

    private static boolean isMediaDocument(Uri uri) {
       try{
           return "com.android.providers.media.documents".equals(uri.getAuthority());
       } catch (Exception e) {
           return false;
       }
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        try{
            return "com.google.android.apps.photos.content".equals(uri.getAuthority());
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPath(Uri uri, MainActivity context) throws IOException {
        String selection;
        String[] selectionArgs;

        if (isExternalStorageDocument(uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String fullPath = getPathFromExtSD(split);
            if (fullPath == null || !fileExists(fullPath)) {
                fullPath = copyFileToInternalStorageAndGetPath(uri, context);
            }
            return TextUtils.isEmpty(fullPath) ? null : fullPath;
        }

        if (isDownloadsDocument(uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if (docId.startsWith("msf:")) {
                final String[] split = docId.split(":");
                selection = "_id=?";
                selectionArgs = new String[] { split[1] };
                String relativePath = getDataColumn(context, MediaStore.Files.getContentUri("external"), selection, selectionArgs);
                if(TextUtils.isEmpty(relativePath)) try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String fileName = cursor.getString(0);
                        String path = Environment.getExternalStorageDirectory() + "/Download/" + fileName;
                        if (!TextUtils.isEmpty(path)) {
                            return path;
                        }
                    }
                } else return relativePath;
            }

            String id = DocumentsContract.getDocumentId(uri);
            if (!TextUtils.isEmpty(id)) {
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }

//                String[] contentUriPrefixesToTry = {
//                        "content://downloads/public_downloads",
//                        "content://downloads/my_downloads"
//                };
//
//                for (String contentUriPrefix : contentUriPrefixesToTry) {
//                    try {
//                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
//                        return getDataColumn(context, contentUri, null, null);
//                    } catch (NumberFormatException e) {
//                        return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
//                    }
//                }
            }
        }

        if (isMediaDocument(uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else if ("document".equals(type)) {
                contentUri = MediaStore.Files.getContentUri("external");
            }

            selection = "_id=?";
            selectionArgs = new String[]{split[1]};
            return getDataColumn(context, contentUri, selection, selectionArgs);
        }

        String scheme = uri.getScheme();
        if(!TextUtils.isEmpty(scheme)) {
            if ("content".equalsIgnoreCase(scheme)) {
                return doesNotHaveStoragePerm(context) ?
                                copyFileToInternalStorageAndGetPath(uri, context) :
                                getDataColumn(context, uri, null, null);
            }

            if ("file".equalsIgnoreCase(scheme)) {
                return uri.getPath();
            }
        }


        return copyFileToInternalStorageAndGetPath(uri, context);
    }

    public static String copyFileToInternalStorageAndGetPath(Uri uri, MainActivity context) throws IOException {
        return copyFileToInternalStorage(uri, context).getPath();
    }

    public static File copyFileToInternalStorage(Uri uri, MainActivity context) throws IOException {
        File output = new File(context.getCacheDir(), context.getOriginalFileName(uri));
        if(output.exists() && output.length() > 999) return output;
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null)
            throw new IOException("Unable to open " + uri);
        try (InputStream cursor = inputStream;
             OutputStream outputStream = com.abdurazaaqmohammed.utils.FileUtils.getOutputStream(output)) {
            int read;
            byte[] buffers = new byte[8192];
            while ((read = cursor.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
        }
        return output;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {column};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception ignored) {}
        return null;
    }
}