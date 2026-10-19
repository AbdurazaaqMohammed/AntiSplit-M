package com.abdurazaaqmohammed.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.InputSource;
import com.starry.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DeviceSpecsUtil {

    private final MainActivity context;
    public final String lang;
    private final String densityType;
    public static ArchiveFile zipFile = null;

    public DeviceSpecsUtil(MainActivity context) {
        this.context = context;
        this.lang = Locale.getDefault().getLanguage();
        this.densityType = getDeviceDpi();
    }

    public List<String> getSplitsForDevice(Uri uri) throws IOException {
        List<String> splits = getListOfSplits(uri);
        List<String> toExclude = new ArrayList<>();
        if (splits.size() <= 4)
            return toExclude;

        boolean splitApkContainsArch = false;
        boolean selectedSplitsContainsArch = false;
        boolean selectedSplitsContainsDpi = false;

        for (String split : splits) {
            if (isArch(split))
                splitApkContainsArch = true;
            if (isBaseApk(split))
                continue;
            if (shouldIncludeSplit(split)) {
                if (isArch(split))
                    selectedSplitsContainsArch = true;
                if (split.contains("dpi"))
                    selectedSplitsContainsDpi = true;
            } else {
                toExclude.add(split);
            }
        }

        if (splitApkContainsArch && !selectedSplitsContainsArch) {
            context.getLogger().logMessage("Could not find device architecture, selecting all architectures");
            for (String split : splits) {
                if (isArch(split))
                    toExclude.remove(split);
            }
        }

        if (!selectedSplitsContainsDpi) {
            for (String split : splits) {
                if (hasDpi(split, "hdpi"))
                    toExclude.remove(split);
            }
        }
        return toExclude;
    }

    private List<String> getListOfSplitsFromFile(File file) throws IOException {
        List<String> splits = new ArrayList<>();
        releaseZipFile();
        // Do not close this ZipFile, it is used later by the merger
        ArchiveFile archiveFile = new ArchiveFile(file);
        zipFile = archiveFile;
        for (InputSource inputSource : archiveFile.getInputSources()) {
            String name = inputSource.getName();
            if (name.endsWith(".apk"))
                splits.add(name);
        }
        return splits;
    }

    public List<String> getListOfSplits(Uri splitAPKUri) throws IOException {
        File file;
        try {
            String path = FileUtils.getPath(splitAPKUri, context);
            if (!TextUtils.isEmpty(path) && (file = new File(path)).canRead())
                return getListOfSplitsFromFile(file);
        } catch (Exception ignored) {
        }

        try (InputStream is = context.getContentResolver().openInputStream(splitAPKUri)) {
            if (is == null)
                throw new IOException("Unable to open " + splitAPKUri);
            String uriString = splitAPKUri.toString();
            com.abdurazaaqmohammed.utils.FileUtils.copyFile(is,
                    file = new File(context.getCacheDir(), System.currentTimeMillis()
                            + "_checking_splits_" + UUID.randomUUID()));
            return getListOfSplitsFromFile(file);
        }
    }

    public static void releaseZipFile() {
        ArchiveFile archiveFile = zipFile;
        zipFile = null;
        if (archiveFile != null) {
            try {
                archiveFile.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isArch(String thisSplit) {
        String name = thisSplit.toLowerCase(Locale.ROOT);
        return name.contains("armeabi") || name.contains("arm64")
                || name.contains("x86") || name.contains("mips")
                || name.contains("riscv");
    }

    public static boolean isBaseApk(String name) {
        return name.equals("base.apk") || !name.startsWith("config") && !name.startsWith("split");
    }

    public boolean shouldIncludeSplit(String name) {
        return isBaseApk(name) || shouldIncludeLang(name) || shouldIncludeArch(name)
                || shouldIncludeDpi(name);
    }

    public boolean shouldIncludeLang(String name) {
        if (isArch(name) || name.toLowerCase(Locale.ROOT).contains("dpi"))
            return false;
        String language = lang.toLowerCase(Locale.ROOT);
        for (String part : name.toLowerCase(Locale.ROOT).split("\\.")) {
            if (part.equals(language) || part.startsWith(language + "_")
                    || part.startsWith("b+" + language + "+"))
                return true;
        }
        return false;
    }

    public boolean shouldIncludeArch(String name) {
        String abi = Build.CPU_ABI;
        if (TextUtils.isEmpty(abi))
            return false;
        String abiNormalized = abi.toLowerCase(Locale.ROOT).replace('-', '_');
        for (String part : name.toLowerCase(Locale.ROOT).split("\\.")) {
            if (part.replace('-', '_').equals(abiNormalized))
                return true;
        }
        return false;
    }

    public boolean shouldIncludeDpi(String name) {
        String density = densityType.endsWith(".apk")
                ? densityType.substring(0, densityType.length() - 4) : densityType;
        for (String part : name.toLowerCase(Locale.ROOT).split("\\.")) {
            if (part.equals(density))
                return true;
        }
        return false;
    }

    private static boolean hasDpi(String name, String density) {
        for (String part : name.toLowerCase(Locale.ROOT).split("\\.")) {
            if (part.equals(density))
                return true;
        }
        return false;
    }

    public String getDeviceDpi() {
        String densityType;
        if (TextUtils.isEmpty(densityType = context.getSharedPreferences("set", Context.MODE_PRIVATE)
                .getString("deviceDpi", ""))) {
            switch (context.getResources().getDisplayMetrics().densityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    densityType = "ldpi";
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                case DisplayMetrics.DENSITY_140:
                    densityType = "mdpi";
                    break;
                case DisplayMetrics.DENSITY_XHIGH:
                case DisplayMetrics.DENSITY_280:
                case DisplayMetrics.DENSITY_260:
                case DisplayMetrics.DENSITY_300:
                    densityType = "xhdpi";
                    break;
                case DisplayMetrics.DENSITY_340:
                case DisplayMetrics.DENSITY_360:
                case DisplayMetrics.DENSITY_390:
                case DisplayMetrics.DENSITY_400:
                case DisplayMetrics.DENSITY_420:
                case DisplayMetrics.DENSITY_440:
                case DisplayMetrics.DENSITY_450:
                case DisplayMetrics.DENSITY_XXHIGH:
                    densityType = "xxhdpi";
                    break;
                case DisplayMetrics.DENSITY_520:
                case DisplayMetrics.DENSITY_560:
                case DisplayMetrics.DENSITY_600:
                case DisplayMetrics.DENSITY_XXXHIGH:
                    densityType = "xxxhdpi";
                    break;
                case DisplayMetrics.DENSITY_TV:
                    densityType = "tvdpi";
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                case DisplayMetrics.DENSITY_220:
                case DisplayMetrics.DENSITY_200:
                case DisplayMetrics.DENSITY_180:
                default:
                    densityType = "hdpi";
                    break;
            }
            densityType += ".apk";
            context.getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                    .putString("deviceDpi", densityType).apply();
        }
        return densityType;
    }
}
