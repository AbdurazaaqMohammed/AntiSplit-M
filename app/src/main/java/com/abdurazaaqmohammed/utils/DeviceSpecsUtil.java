package com.abdurazaaqmohammed.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.reandroid.apk.APKLogger;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DeviceSpecsUtil {

    private static final String[] DENSITIES = {
            "ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "tvdpi", "nodpi", "anydpi"
    };

    private static volatile File cachedArchive;
    private static volatile Uri cachedArchiveUri;

    private final Context context;
    private final APKLogger logger;
    private final String language;
    private final String density;

    public DeviceSpecsUtil(Context context, APKLogger logger) {
        this.context = context;
        this.logger = logger;
        this.language = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        this.density = deviceDensity(context);
    }

    public List<String> getSplitsForDevice(Uri archiveUri) throws IOException {
        List<String> splits = getListOfSplits(archiveUri);
        List<String> excluded = new ArrayList<>();

        boolean archiveHasArchitecture = false;
        boolean selectedHasArchitecture = false;
        boolean selectedHasDensity = false;

        for (String split : splits) {
            if (isArchitecture(split))
                archiveHasArchitecture = true;
            if (isBaseApk(split))
                continue;
            if (shouldIncludeSplit(split)) {
                if (isArchitecture(split))
                    selectedHasArchitecture = true;
                if (isDensity(split))
                    selectedHasDensity = true;
            } else {
                excluded.add(split);
            }
        }

        if (archiveHasArchitecture && !selectedHasArchitecture) {
            logger.logMessage("Could not find device architecture, selecting all architectures");
            for (String split : splits) {
                if (isArchitecture(split))
                    excluded.remove(split);
            }
        }

        if (!selectedHasDensity) {
            for (String split : splits) {
                if (hasDensity(split, "hdpi"))
                    excluded.remove(split);
            }
        }
        return excluded;
    }

    public List<String> getListOfSplits(Uri archiveUri) throws IOException {
        return readSplitNames(archiveUri, localFileFor(archiveUri));
    }

    public static File getCachedArchive(Uri archiveUri) {
        File file = cachedArchive;
        if (file == null || !file.isFile() || archiveUri == null
                || !archiveUri.equals(cachedArchiveUri))
            return null;
        return file;
    }

    public static void release() {
        cachedArchive = null;
        cachedArchiveUri = null;
    }

    private List<String> readSplitNames(Uri archiveUri, File archiveFile) throws IOException {
        List<String> splits = new ArrayList<>();
        try (ArchiveFile archive = new ArchiveFile(archiveFile)) {
            for (InputSource source : archive.getInputSources()) {
                String name = source.getName();
                if (name.endsWith(".apk"))
                    splits.add(name);
            }
        }
        cachedArchive = archiveFile;
        cachedArchiveUri = archiveUri;
        return splits;
    }

    private File localFileFor(Uri uri) throws IOException {
        String path = null;
        try {
            path = com.starry.FileUtils.getPath(uri, context);
        } catch (Exception ignored) {
        }
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.canRead())
                return file;
        }

        File copy = new File(context.getCacheDir(),
                "splits_" + System.currentTimeMillis() + '_' + UUID.randomUUID() + ".zip");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null)
                throw new IOException("Unable to open " + uri);
            FileUtils.copyFile(input, copy);
        }
        return copy;
    }

    public static boolean isArchitecture(String splitName) {
        String name = splitName.toLowerCase(Locale.ROOT);
        return name.contains("armeabi") || name.contains("arm64")
                || name.contains("x86") || name.contains("mips")
                || name.contains("riscv");
    }

    public static boolean isBaseApk(String splitName) {
        String name = splitName.toLowerCase(Locale.ROOT);
        String stem = name.endsWith(".apk") ? name.substring(0, name.length() - 4) : name;

        if (!stem.startsWith("base"))
            return !stem.startsWith("config") && !stem.startsWith("split");

        String qualifier = stem.substring("base".length());
        if (qualifier.startsWith("-") || qualifier.startsWith(".") || qualifier.startsWith("_"))
            qualifier = qualifier.substring(1);
        if (qualifier.startsWith("config")) {
            String rest = qualifier.substring("config".length());
            if (rest.startsWith("-") || rest.startsWith(".") || rest.startsWith("_"))
                rest = rest.substring(1);
            if (!rest.isEmpty())
                qualifier = rest;
        }
        if (qualifier.isEmpty())
            return true;
        return !isArchitecture(qualifier) && !isDensityQualifier(qualifier)
                && !isLanguageQualifier(qualifier);
    }

    public boolean shouldIncludeSplit(String splitName) {
        return isBaseApk(splitName)
                || shouldIncludeLanguage(splitName)
                || shouldIncludeArchitecture(splitName)
                || shouldIncludeDensity(splitName);
    }

    public boolean shouldIncludeLanguage(String splitName) {
        if (isArchitecture(splitName) || isDensity(splitName))
            return false;
        for (String token : tokens(splitName)) {
            if (token.equals(language) || token.startsWith(language + "_")
                    || token.startsWith("b+" + language + "+"))
                return true;
        }
        return false;
    }

    public boolean shouldIncludeArchitecture(String splitName) {
        String abi = primaryAbi();
        if (TextUtils.isEmpty(abi))
            return false;
        String normalizedAbi = abi.toLowerCase(Locale.ROOT).replace('-', '_');
        for (String token : tokens(splitName)) {
            if (token.replace('-', '_').equals(normalizedAbi))
                return true;
        }
        return false;
    }

    public boolean shouldIncludeDensity(String splitName) {
        return tokens(splitName).contains(density);
    }

    public static boolean isDensity(String splitName) {
        for (String token : tokens(splitName)) {
            if (isDensityQualifier(token))
                return true;
        }
        return false;
    }

    public static boolean hasDensity(String splitName, String density) {
        return tokens(splitName).contains(density);
    }

    private static boolean isDensityQualifier(String value) {
        for (String candidate : DENSITIES) {
            if (candidate.equals(value))
                return true;
        }
        return false;
    }

    private static boolean isLanguageQualifier(String value) {
        return value.matches("[a-z]{2,3}([_-][a-z]{2,4}|[_-]\\d{3})?");
    }

    private static List<String> tokens(String splitName) {
        List<String> tokens = new ArrayList<>();
        for (String token : splitName.toLowerCase(Locale.ROOT).split("[._\\-+]")) {
            if (!token.isEmpty())
                tokens.add(token);
        }
        return tokens;
    }

    private static String primaryAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] abis = Build.SUPPORTED_ABIS;
            return abis.length == 0 ? "" : abis[0];
        }
        return Build.CPU_ABI;
    }

    private static String deviceDensity(Context context) {
        switch (context.getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi";
            case DisplayMetrics.DENSITY_MEDIUM:
            case DisplayMetrics.DENSITY_140:
                return "mdpi";
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_280:
            case DisplayMetrics.DENSITY_260:
            case DisplayMetrics.DENSITY_300:
                return "xhdpi";
            case DisplayMetrics.DENSITY_340:
            case DisplayMetrics.DENSITY_360:
            case DisplayMetrics.DENSITY_390:
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_420:
            case DisplayMetrics.DENSITY_440:
            case DisplayMetrics.DENSITY_450:
            case DisplayMetrics.DENSITY_XXHIGH:
                return "xxhdpi";
            case DisplayMetrics.DENSITY_520:
            case DisplayMetrics.DENSITY_560:
            case DisplayMetrics.DENSITY_600:
            case DisplayMetrics.DENSITY_XXXHIGH:
                return "xxxhdpi";
            case DisplayMetrics.DENSITY_TV:
                return "tvdpi";
            default:
                return "hdpi";
        }
    }
}
