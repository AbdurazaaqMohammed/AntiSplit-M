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

    /**
     * Canonical ABIs, longest / most specific first. Order matters: "x86" is a
     * substring of "x86_64" and "armeabi" is a substring of "armeabi-v7a", so
     * the more specific names must be tested first.
     */
    private static final String[] KNOWN_ABIS = {
            "arm64-v8a", "armeabi-v7a", "x86_64", "riscv64", "mips64",
            "armeabi", "x86", "mips"
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
        return getSplitsForDevice(getListOfSplits(archiveUri));
    }

    /**
     * Computes the splits that must be excluded for this device. This is the single
     * source of truth shared by the automatic merge and the manual split picker.
     * It always keeps at least one architecture split (native libraries live there)
     * and at least one density split.
     */
    public List<String> getSplitsForDevice(List<String> splits) {
        List<String> excluded = new ArrayList<>();
        List<String> archToKeep = selectArchitectureSplits(splits);
        List<String> densityToKeep = selectDensitySplits(splits);
        boolean selectedHasDensity = false;

        for (String split : splits) {
            if (isBaseApk(split))
                continue;
            boolean include;
            if (isArchitecture(split)) {
                include = archToKeep.contains(split);
            } else if (isDensity(split)) {
                include = densityToKeep.contains(split);
                if (include)
                    selectedHasDensity = true;
            } else {
                include = shouldIncludeLanguage(split);
            }
            if (!include)
                excluded.add(split);
        }

        if (!selectedHasDensity) {
            for (String split : densityToKeep)
                excluded.remove(split);
        }
        return excluded;
    }

    /**
     * Architecture splits carry the app's native libraries. Split names use both
     * '_' and '-' as separators (e.g. split_config.arm64_v8a.apk), so comparing
     * whole tokens against Build.SUPPORTED_ABIS never matches. Compare compacted
     * ABI names instead, and fall back to keeping every architecture split when
     * the device ABI is not present in the bundle - dropping all of them produces
     * an APK with no lib/ directory and runtime "couldn't find DSO to load" errors.
     */
    public List<String> selectArchitectureSplits(List<String> splits) {
        List<String> all = new ArrayList<>();
        List<String> matching = new ArrayList<>();
        String deviceAbi = compactAbi(primaryAbi());
        for (String split : splits) {
            String abi = extractAbi(split);
            if (abi == null)
                continue;
            all.add(split);
            if (deviceAbi.length() != 0 && compactAbi(abi).equals(deviceAbi))
                matching.add(split);
        }
        if (matching.isEmpty()) {
            if (logger != null && !all.isEmpty())
                logger.logMessage("Could not find device architecture, selecting all architectures");
            return all;
        }
        return matching;
    }

    private List<String> selectDensitySplits(List<String> splits) {
        List<String> matching = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (String split : splits) {
            if (!isDensity(split))
                continue;
            all.add(split);
            if (shouldIncludeDensity(split))
                matching.add(split);
        }
        if (!matching.isEmpty())
            return matching;
        // Keep the highest density available so resource references always resolve.
        String best = null;
        int bestRank = -1;
        for (String split : all) {
            for (String token : tokens(split)) {
                for (int i = 0; i < DENSITIES.length; i++) {
                    if (DENSITIES[i].equals(token) && i > bestRank) {
                        bestRank = i;
                        best = split;
                    }
                }
            }
        }
        if (best != null)
            matching.add(best);
        return matching;
    }

    /** Returns the best architecture split to keep when all were deselected. */
    public static String bestArchitectureSplit(List<String> splits) {
        String deviceAbi = compactAbi(primaryAbi());
        String fallback = null;
        for (String split : splits) {
            String abi = extractAbi(split);
            if (abi == null)
                continue;
            if (fallback == null)
                fallback = split;
            if (deviceAbi.length() != 0 && compactAbi(abi).equals(deviceAbi))
                return split;
        }
        return fallback;
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
        return extractAbi(splitName) != null;
    }

    /**
     * Extracts a canonical ABI from a split file name. All non alphanumeric
     * characters are ignored, so "arm64_v8a", "arm64-v8a" and "arm64.v8a"
     * all resolve to "arm64-v8a".
     */
    public static String extractAbi(String splitName) {
        if (splitName == null)
            return null;
        String compact = compactAbi(splitName);
        for (String abi : KNOWN_ABIS) {
            if (compact.contains(compactAbi(abi)))
                return abi;
        }
        return null;
    }

    private static String compactAbi(String value) {
        if (value == null)
            return "";
        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
                builder.append(c);
        }
        return builder.toString();
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

    /** Device-aware variant used by the split picker; keeps at least one ABI split. */
    public boolean shouldIncludeSplit(String splitName, List<String> allSplits) {
        if (isBaseApk(splitName))
            return true;
        if (isArchitecture(splitName))
            return shouldIncludeArchitecture(splitName, allSplits);
        if (isDensity(splitName))
            return selectDensitySplits(allSplits).contains(splitName);
        return shouldIncludeLanguage(splitName);
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
        String splitAbi = extractAbi(splitName);
        return splitAbi != null && compactAbi(abi).equals(compactAbi(splitAbi));
    }

    public boolean shouldIncludeArchitecture(String splitName, List<String> allSplits) {
        return selectArchitectureSplits(allSplits).contains(splitName);
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
