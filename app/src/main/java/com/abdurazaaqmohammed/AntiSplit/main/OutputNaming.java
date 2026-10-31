package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.abdurazaaqmohammed.utils.FileUtils;
import com.abdurazaaqmohammed.utils.UriNames;

import java.io.File;
import java.util.List;

public final class OutputNaming {

    private static final String DEFAULT_NAME = "merged";

    private final Context context;
    private final AppSettings settings;

    public OutputNaming(Context context, AppSettings settings) {
        this.context = context;
        this.settings = settings;
    }

    private String suffix() {
        return settings.suffix == null ? "" : settings.suffix;
    }

    public String forArchive(Uri archiveUri) {
        String name = UriNames.displayName(context, archiveUri);
        if (TextUtils.isEmpty(name))
            name = "file_" + System.currentTimeMillis();
        return UriNames.withSuffix(name, suffix());
    }

    public String forInstalledApp(String packageName) {
        String versionName = "";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            if (!TextUtils.isEmpty(info.versionName))
                versionName = " v" + info.versionName;
        } catch (Exception ignored) {
        }
        return FileUtils.sanitizeFileName(packageName + versionName + suffix() + ".apk");
    }

    public String forApkFiles(List<Uri> apkFiles) {
        String baseName = null;
        if (apkFiles != null) {
            for (Uri uri : apkFiles) {
                String fileName = UriNames.displayName(context, uri);
                if (TextUtils.isEmpty(fileName))
                    continue;
                if ("base.apk".equals(fileName)) {
                    baseName = packageNameOf(uri);
                    if (baseName != null)
                        break;
                } else if (!fileName.startsWith("config") && !fileName.startsWith("split")) {
                    baseName = fileName;
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(baseName))
            return FileUtils.sanitizeFileName(DEFAULT_NAME + suffix() + ".apk");
        return UriNames.withSuffix(baseName, suffix());
    }

    public File preferredDirectory(List<Uri> sourceUris) {
        if (sourceUris == null || sourceUris.isEmpty())
            return null;
        return parentDirectory(sourceUris.get(0));
    }

    private File parentDirectory(Uri uri) {
        if (uri == null)
            return null;
        try {
            String path = com.starry.FileUtils.getPath(uri, context);
            if (TextUtils.isEmpty(path) || path.startsWith("/data/") || path.contains("/Android/data"))
                return null;
            File source = new File(path);
            File parent = source.getParentFile();
            return parent != null && parent.canWrite() ? parent : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String packageNameOf(Uri uri) {
        try {
            String path = com.starry.FileUtils.getPath(uri, context);
            if (TextUtils.isEmpty(path))
                return null;
            PackageManager packageManager = context.getPackageManager();
            PackageInfo info = packageManager.getPackageArchiveInfo(path, 0);
            return info == null ? null : info.packageName;
        } catch (Exception ignored) {
            return null;
        }
    }
}
