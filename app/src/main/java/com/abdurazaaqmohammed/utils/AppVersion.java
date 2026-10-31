package com.abdurazaaqmohammed.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public final class AppVersion {

    private AppVersion() {
    }

    public static String name(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String versionName = packageManager
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return versionName == null ? "" : versionName;
        } catch (Exception e) {
            return "";
        }
    }
}
