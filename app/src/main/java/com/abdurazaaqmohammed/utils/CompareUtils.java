package com.abdurazaaqmohammed.utils;

import com.abdurazaaqmohammed.AntiSplit.main.AppInfo;

import java.util.Locale;

public final class CompareUtils {

    private CompareUtils() {
    }

    public static int compareAppInfoByName(AppInfo first, AppInfo second) {
        return first.name.toLowerCase(Locale.ROOT)
                .compareTo(second.name.toLowerCase(Locale.ROOT));
    }

    public static int compareByName(String first, String second) {
        return first.toLowerCase(Locale.ROOT)
                .compareTo(second.toLowerCase(Locale.ROOT));
    }
}
