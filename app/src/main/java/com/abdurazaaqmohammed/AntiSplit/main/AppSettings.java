package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.FileUtils;

import java.util.zip.Deflater;

public final class AppSettings {

    public static final int SAVE_ASK = 0;
    public static final int SAVE_NEXT_TO_SOURCE = 1;
    public static final int SAVE_TO_FOLDER = 2;

    static final String PREFS_NAME = "set";
    static final String KEY_LANGUAGE = "lang";

    public int compressionLevel = Deflater.DEFAULT_COMPRESSION;
    public int saveMode = SAVE_ASK;
    public int sortMode;
    public int theme;

    public String outputFolder;
    public String suffix = "_antisplit";
    public String language;
    public String lastVersionChecked;

    public boolean showDialog;
    public boolean signApk = true;
    public boolean selectSplitsForDevice;
    public boolean force;
    public boolean logEnabled = true;
    public boolean checkForUpdates = true;
    public boolean systemTheme = true;

    public static AppSettings load(Context context) {
        SharedPreferences prefs = prefs(context);
        AppSettings settings = new AppSettings();

        settings.language = prefs.getString(KEY_LANGUAGE, null);
        settings.theme = prefs.getInt("theme", systemTheme(context));
        if (settings.theme == 0)
            settings.theme = systemTheme(context);
        settings.checkForUpdates = prefs.getBoolean("checkForUpdates", true);
        settings.signApk = prefs.getBoolean("signApk", true);
        settings.force = prefs.getBoolean("force", false);
        settings.showDialog = prefs.getBoolean("showDialog", false);
        settings.selectSplitsForDevice = prefs.getBoolean("selectSplitsForDevice", false);
        settings.logEnabled = prefs.getBoolean("logEnabled", true);
        settings.systemTheme = prefs.getBoolean("systemTheme", true);
        settings.saveMode = Math.max(SAVE_ASK,
                Math.min(SAVE_TO_FOLDER, prefs.getInt("saveMode", SAVE_ASK)));
        settings.sortMode = prefs.getInt("sortMode", 0);
        settings.compressionLevel = prefs.getInt("compressionLevel", Deflater.DEFAULT_COMPRESSION);
        settings.suffix = prefs.getString("suffix", "_antisplit");
        settings.lastVersionChecked = prefs.getString("lastVerChecked", null);

        String outputFolder = prefs.getString("outputFolder", null);
        settings.outputFolder = outputFolder != null
                ? outputFolder
                : FileUtils.getAntisplitMFolder().getPath();
        return settings;
    }

    public void save(Context context) {
        prefs(context).edit()
                .putString(KEY_LANGUAGE, language)
                .putInt("theme", theme)
                .putBoolean("checkForUpdates", checkForUpdates)
                .putBoolean("signApk", signApk)
                .putBoolean("force", force)
                .putBoolean("showDialog", showDialog)
                .putBoolean("selectSplitsForDevice", selectSplitsForDevice)
                .putBoolean("logEnabled", logEnabled)
                .putBoolean("systemTheme", systemTheme)
                .putInt("saveMode", saveMode)
                .putInt("sortMode", sortMode)
                .putInt("compressionLevel", compressionLevel)
                .putString("suffix", suffix)
                .putString("lastVerChecked", lastVersionChecked)
                .putString("outputFolder", outputFolder)
                .apply();
    }

    public boolean isLightTheme() {
        return theme == R.style.Theme_MyApp_Light;
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String savedLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, null);
    }

    public static int systemTheme(Context context) {
        boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return dark ? R.style.Theme_MyApp_Dark : R.style.Theme_MyApp_Light;
    }
}
