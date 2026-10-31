package com.abdurazaaqmohammed.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import java.util.Locale;

public final class UriNames {

    private static final String ARCHIVE_EXTENSION =
            "\\.(?:zip|xapk|aspk|apks|apkm|apk)$";

    private UriNames() {
    }

    public static String displayName(Context context, Uri uri) {
        if (uri == null)
            return null;

        String name = null;
        if ("content".equals(uri.getScheme())) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0)
                            name = cursor.getString(index);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (TextUtils.isEmpty(name)) {
            String path = uri.getPath();
            if (!TextUtils.isEmpty(path)) {
                int cut = path.lastIndexOf('/');
                name = cut < 0 ? path : path.substring(cut + 1);
            }
        }
        return name;
    }

    public static String withoutExtension(String fileName) {
        if (fileName == null)
            return null;
        return fileName.replaceFirst("(?i)" + ARCHIVE_EXTENSION, "");
    }

    public static String withSuffix(String fileName, String suffix) {
        String base = withoutExtension(fileName);
        if (TextUtils.isEmpty(base))
            base = "merged";
        return FileUtils.sanitizeFileName(base + (suffix == null ? "" : suffix) + ".apk");
    }

    public static String cacheName(Context context, Uri uri, String fallback) {
        String name = displayName(context, uri);
        if (TextUtils.isEmpty(name))
            name = fallback;
        return FileUtils.sanitizeFileName(name);
    }

    public static boolean looksLikeApk(Uri uri) {
        String path = uri == null ? null : uri.getPath();
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".apk");
    }
}
