package com.abdurazaaqmohammed.AntiSplit.main;

import static com.abdurazaaqmohammed.utils.LegacyUtils.aboveSdk20;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.CompareUtils;
import com.abdurazaaqmohammed.utils.DeviceSpecsUtil;
import com.abdurazaaqmohammed.utils.InstallUtil;
import com.abdurazaaqmohammed.utils.LegacyUtils;
import com.abdurazaaqmohammed.utils.RunUtil;
import com.abdurazaaqmohammed.utils.UpdateUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.reandroid.Merger;
import com.reandroid.apk.ApkBundle;
import com.reandroid.utils.io.FileUtil;
import com.starry.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.Deflater;

import com.github.paul035.LocaleHelper;

/** @noinspection deprecation */public class MainActivity extends AppCompatActivity {
    private int compressionLevel;
    private int saveMode = 0;
    private String outputFolder;
    private boolean showDialog;
    private boolean signApk;
    private boolean selectSplitsForDevice;
    private volatile Uri splitAPKUri;
    private volatile ArrayList<Uri> uris;
    private boolean UrisAreSplitApks = true;
    @SuppressWarnings("unused") private boolean urisAreSplitApks() { return UrisAreSplitApks; }
    private boolean multipleArchives;
    private boolean receiverRegistered;
    private volatile boolean criticalErrorOccurred;
    private boolean checkForUpdates;
    public volatile String lastVerChecked;
    volatile boolean logEnabled;
    private boolean force;
    private String lang;
    private int theme;
    private int sortMode;
    private DeviceSpecsUtil deviceSpecsUtil;
    private String pkgName;
    private String suffix;
    private boolean systemTheme;

    private Resources rss;

    public Resources getRss() {
        return rss;
    }

    private Handler handler;

    public Handler getHandler() {
        return handler;
    }

    private MyAPKLogger logger;

    public MyAPKLogger getLogger() {
        return logger;
    }

    private String openedFile;
    private List<String> splitsUnselectedInDialog;
    private Runnable pendingAfterStoragePermission;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences settings = newBase.getSharedPreferences("set", Context.MODE_PRIVATE);
        super.attachBaseContext(LocaleHelper.setLocale(newBase, settings.getString("lang", null)));
    }

    /** @noinspection AssignmentUsedAsCondition */@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        lang = settings.getString("lang", null);
        rss = getResources();

        boolean dark = (rss.getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        setTheme(theme = settings.getInt("theme", dark
                ? R.style.Theme_MyApp_Dark
                : R.style.Theme_MyApp_Light));

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applyWindowInsets();
        applySystemBarAppearance();

        deviceSpecsUtil = new DeviceSpecsUtil(this);

        TextView logField = findViewById(R.id.logField);
        NestedScrollView scrollView = findViewById(R.id.scrollView);
        logger = new MyAPKLogger(this, logField, scrollView);

        RunUtil.runInBackground(() -> FileUtil.deleteDirectory(getCacheDir()));

        // Fetch settings from SharedPreferences
        lastVerChecked = getSharedPreferences("set", Context.MODE_PRIVATE).getString("lastVerChecked", null);
        if ((checkForUpdates = settings.getBoolean("checkForUpdates", true)))
            UpdateUtil.checkForUpdates(false, this);
        signApk = settings.getBoolean("signApk", true);
        force = settings.getBoolean("force", false);
        showDialog = settings.getBoolean("showDialog", false);
        selectSplitsForDevice = settings.getBoolean("selectSplitsForDevice", false);
        logEnabled = settings.getBoolean("logEnabled", true);
        saveMode = settings.getInt("saveMode", 0);
        systemTheme = settings.getBoolean("systemTheme", true);
        sortMode = settings.getInt("sortMode", 0);
        compressionLevel = settings.getInt("compressionLevel", Deflater.DEFAULT_COMPRESSION);
        suffix = settings.getString("suffix", "_antisplit");
        outputFolder = settings.getString("outputFolder",
                com.abdurazaaqmohammed.utils.FileUtils.getAntisplitMFolder().getPath());

        View selectFromInstalledApps = findViewById(R.id.fromAppsButton);
        if (aboveSdk20)
            selectFromInstalledApps.setOnClickListener(this::installedAppsClickListener);
        else
            selectFromInstalledApps.setVisibility(View.GONE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        Drawable settingsIcon = toolbar.getMenu().findItem(R.id.settingsButton).getIcon();
        if (settingsIcon != null) {
            DrawableCompat.setTint(settingsIcon,
                    MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.settingsButton) {
                settingsButtonListener(toolbar);
                return true;
            }
            return false;
        });

        findViewById(R.id.decodeButton).setOnClickListener(v -> {
            clearLogFields();
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            // .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip",
            // "application/vnd.android.package-archive", "application/octet-stream"}) //
            // XAPK usually octet-stream
                    , 1);
        });
        cleanupAppFolder();

        // Check if user shared or opened file with the app.
        final Intent openIntent = getIntent();
        final String action = openIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            splitAPKUri = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ? openIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class)
                    : openIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            splitAPKUri = openIntent.getData();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && openIntent.hasExtra(Intent.EXTRA_STREAM)) {
            uris = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ? openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class)
                    : openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null && !uris.isEmpty()) {
                final Uri uri = uris.get(0);
                String path = uri.getPath();
                if (UrisAreSplitApks = TextUtils.isEmpty(path) || !path.endsWith(".apk")) {
                    multipleArchives = uris.size() > 1;
                    splitAPKUri = uri;
                } else
                    process();
            }
        }
        if (splitAPKUri != null) {
            if (showDialog)
                showApkSelectionDialog();
            else
                process();
        }
    }

    private boolean isLightTheme() {
        return theme == R.style.Theme_MyApp_Light;
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void applySystemBarAppearance() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window,
                window.getDecorView());
        boolean light = isLightTheme();
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.getDecorView().setForceDarkAllowed(false);
    }

    public void styleAlertDialog(AlertDialog ad) {
        styleAlertDialog(ad, true);
    }

    public void styleAlertDialog(AlertDialog ad, boolean fixedHeight) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed())
                return;
            if (!ad.isShowing())
                ad.show();
            Window window = ad.getWindow();
            if (window == null)
                return;
            DisplayMetrics metrics = rss.getDisplayMetrics();
            int width = Math.min((int) (metrics.widthPixels * 0.92f),
                    (int) (560 * metrics.density));
            if (fixedHeight)
                window.setLayout(width, (int) (metrics.heightPixels * 0.85f));
            else
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (systemTheme) {
            int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                if(theme != R.style.Theme_MyApp_Dark) {
                    setTheme(theme = R.style.Theme_MyApp_Dark);
                    recreate();
                }
            } else if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                if(theme != R.style.Theme_MyApp_Light) {
                    setTheme(theme = R.style.Theme_MyApp_Light);
                    recreate();
                }
            }
        }
    }

    public void checkStoragePerm(int requestCode) {
        if (!com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(this))
            return;
        getHandler().post(() -> Toast.makeText(this, rss.getString(R.string.grant_storage),
                Toast.LENGTH_LONG).show());
        if (LegacyUtils.supportsWriteExternalStorage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return;
        }
        try {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())), requestCode);
        } catch (ActivityNotFoundException e) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                    requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == 10) {
            if (granted)
                runUpdateCheckAfterPermission();
            return;
        }
        if (requestCode != 9)
            return;
        if (granted)
            runPendingStorageAction();
        else
            onStoragePermissionDenied();
    }

    private void runPendingStorageAction() {
        Runnable pending = pendingAfterStoragePermission;
        pendingAfterStoragePermission = null;
        if (pending != null)
            getHandler().post(pending);
    }

    private void onStoragePermissionDenied() {
        saveMode = 0;
        boolean retrySave = pendingAfterStoragePermission != null;
        pendingAfterStoragePermission = null;
        if (retrySave)
            getHandler().post(() -> selectDirToSaveAPKOrSaveNow(0));
    }

    @Override
    protected void onPause() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(onDownloadComplete);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
        getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                .putBoolean("logEnabled", logEnabled)
                .putInt("saveMode", saveMode)
                .putBoolean("showDialog", showDialog)
                .putBoolean("signApk", signApk)
                .putBoolean("force", force)
                .putBoolean("systemTheme", systemTheme)
                .putBoolean("selectSplitsForDevice", selectSplitsForDevice)
                .putInt("theme", theme)
                .putInt("sortMode", sortMode)
                .putInt("compressionLevel", compressionLevel)
                .putBoolean("checkForUpdates", checkForUpdates)
                .putString("lang", lang)
                .putString("lastVerChecked", lastVerChecked)
                .putString("suffix", suffix)
                .putString("outputFolder", outputFolder)
                .apply();
        super.onPause();
    }

    /** @noinspection ResultOfMethodCallIgnored */private void cleanupAppFolder() {
        if (com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(this))
            return;
        File appFolder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        if (!appFolder.exists())
            return;
        File[] children = appFolder.listFiles();
        if (children == null)
            return;
        if (children.length == 0)
            appFolder.delete();
        else {
            for (File child : children)
                if (child.isFile() && child.length() == 0)
                    child.delete();
            File[] remainingChildren = appFolder.listFiles();
            if (remainingChildren == null || remainingChildren.length == 0)
                appFolder.delete();
        }
    }

    @Override
    protected void onDestroy() {
        DeviceSpecsUtil.releaseZipFile();
        FileUtil.deleteDirectory(getCacheDir());
        cleanupAppFolder();
        super.onDestroy();
    }

    File merged;

    private void process() {
        findViewById(R.id.installButton).setVisibility(View.GONE);
        findViewById(R.id.saveButton).setVisibility(View.GONE);
        toggleAnimation(true);
        criticalErrorOccurred = false;

        if (!TextUtils.isEmpty(pkgName)) {
            processFromInstalledApp();
            return;
        }

        MainActivity context = MainActivity.this;

        final File cacheDir = getCacheDir();
        Merger merger = new Merger(cacheDir, this, compressionLevel);

        new RunUtil(handler, context, null).runInBackground(() -> {
            try {
                if (UrisAreSplitApks) {
                    List<String> splitsToNotInclude = selectSplitsForDevice
                            ? deviceSpecsUtil.getSplitsForDevice(splitAPKUri)
                            : splitsUnselectedInDialog;
                    merged = merger.run(splitAPKUri, splitsToNotInclude, signApk, force);
                    selectDirToSaveAPKOrSaveNow();
                } else
                    try (ApkBundle bundle = new ApkBundle(compressionLevel)) {
                        ArrayList<Uri> uriArrayList = uris;
                        if (uriArrayList == null || uriArrayList.isEmpty())
                            throw new IOException(rss.getString(R.string.invalid_file));
                        for (int i = 0; i < uriArrayList.size(); i++) {
                            Uri uri = uriArrayList.get(i);
                            try (InputStream is = FileUtils.getInputStream(uri, context)) {
                                String fileName = getOriginalFileName(uri);
                                if (TextUtils.isEmpty(fileName))
                                    fileName = "split_" + i + "_.apk";
                                com.abdurazaaqmohammed.utils.FileUtils.copyFile(is,
                                        new File(cacheDir, fileName));
                            }
                        }

                        bundle.loadApkDirectory(cacheDir);
                        merged = merger.run(bundle, signApk, force);
                        selectDirToSaveAPKOrSaveNow();
                    }
                toggleAnimation(false);
                return true;
            } catch (Throwable e) {
                showError(e);
                return false;
            }
        }, () -> {
            pkgName = null; // reset
            if (multipleArchives && uris != null && !uris.isEmpty()) {
                uris.remove(0);
                if (!uris.isEmpty()) {
                    DeviceSpecsUtil.releaseZipFile();
                    splitAPKUri = uris.get(0);
                    if (showDialog)
                        showApkSelectionDialog();
                    else
                        process();
                    return;
                }
            }
            multipleArchives = false;
            setUIAfterMerging(merger);
        }, true);

        findViewById(R.id.fabs).setAlpha(0.5f);
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> {
            try {
                startActivity(Intent.makeRestartActivityTask(
                        Objects.requireNonNull(getPackageManager().getLaunchIntentForPackage(getPackageName()))
                                .getComponent()));
                Runtime.getRuntime().exit(0);
            } catch (Exception e) {
                recreate();
            }
        });

        View copyButton = findViewById(R.id.copyButton);
        copyButton.setVisibility(View.VISIBLE);
        copyButton.setOnClickListener(
                v -> copyText(new StringBuilder().append(((TextView) findViewById(R.id.logField)).getText())
                        .append('\n').append(((TextView) findViewById(R.id.errorField)).getText())));
    }

    private void processFromInstalledApp() {
        findViewById(R.id.installButton).setVisibility(View.GONE);
        findViewById(R.id.saveButton).setVisibility(View.GONE);
        toggleAnimation(true);
        criticalErrorOccurred = false;

        final File cacheDir = getCacheDir();
        UrisAreSplitApks = false;
        processInstalledApp(pkgName, cacheDir);
    }

    private void processInstalledApp(String pkgNameParam, File cacheDir) {
        toggleAnimation(true);
        criticalErrorOccurred = false;

        final String pkgNameFinal = pkgNameParam;
        Merger merger = new Merger(cacheDir, this, compressionLevel);

        new RunUtil(handler, this, null).runInBackground(() -> {
            try {
                if (!TextUtils.isEmpty(pkgNameFinal)) {
                    try (ApkBundle bundle = new ApkBundle(compressionLevel)) {
                        // Selected from apps list
                        PackageManager packageManager = getPackageManager();
                        bundle.loadApkDirectory(
                                new File(packageManager.getPackageInfo(pkgNameFinal, 0).applicationInfo.sourceDir)
                                        .getParentFile());
                        merged = merger.run(bundle, signApk, force);
                        UrisAreSplitApks = false;
                        selectDirToSaveAPKOrSaveNow(0);
                    }
                }
                toggleAnimation(false);
                return true;
            } catch (Throwable e) {
                showError(e);
                return false;
            }
        }, () -> {
            this.pkgName = null; // reset
            setUIAfterMerging(merger);
        }, true);

        findViewById(R.id.fabs).setAlpha(0.5f);
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> {
            try {
                startActivity(Intent.makeRestartActivityTask(
                        Objects.requireNonNull(getPackageManager().getLaunchIntentForPackage(getPackageName()))
                                .getComponent()));
                Runtime.getRuntime().exit(0);
            } catch (Exception e) {
                recreate();
            }
        });

        View copyButton = findViewById(R.id.copyButton);
        copyButton.setVisibility(View.VISIBLE);
        copyButton.setOnClickListener(
                v -> copyText(new StringBuilder().append(((TextView) findViewById(R.id.logField)).getText())
                        .append('\n').append(((TextView) findViewById(R.id.errorField)).getText())));
    }

    private void processOneSplitApkUri(Uri uri) {
        splitAPKUri = uri;
        UrisAreSplitApks = !TextUtils.isEmpty(uri.getPath()) && uri.getPath().endsWith(".apk");
        if (showDialog) {
            showApkSelectionDialog();
        } else {
            DeviceSpecsUtil.releaseZipFile();
            process();
        }
    }

    public void toggleAnimation(boolean on) {
        runOnUiThread(() -> findViewById(R.id.progressIndicator).setVisibility(on ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9 || requestCode == 10) {
            boolean missingPermission = com.abdurazaaqmohammed.utils.FileUtils
                    .doesNotHaveStoragePerm(this);
            if (requestCode == 10) {
                if (!missingPermission)
                    runUpdateCheckAfterPermission();
            } else if (missingPermission)
                onStoragePermissionDenied();
            else
                runPendingStorageAction();
            return;
        }
        if (resultCode != RESULT_OK) {
            clearLogFields();
            cleanupAppFolder();
        } else if (data != null)
            switch (requestCode) {
                case 1:
                    // opened through button in the app
                    ClipData clipData = data.getClipData();
                    if (clipData == null) {
                        Uri uri = data.getData();
                        if (uri == null)
                            showError("input Uri null");
                        else {
                            String uriString = uri.toString();
                            if (uriString.endsWith(".apk")) {
                                showError(rss.getString(R.string.not_split, uriString));
                            } else
                                processOneSplitApkUri(uri);
                        }
                    } else {
                        // multiple files selected
                        Uri first = clipData.getItemAt(0).getUri();
                        String path = first.getPath();
                        uris = new ArrayList<>();
                        if (path != null && path.endsWith(".apk")) {
                            UrisAreSplitApks = false;
                        } // else processOneSplitApkUri(first);
                        uris.add(first);
                        for (int i = 1; i < clipData.getItemCount(); i++)
                            uris.add(clipData.getItemAt(i).getUri());

                        multipleArchives = UrisAreSplitApks && uris.size() > 1;
                        if (UrisAreSplitApks)
                            processOneSplitApkUri(first);
                        else
                            process();
                    }
                    break;
                case 2:
                    // NOT going to process a file now we are saving it
                    new RunUtil(handler, this, null).runInBackground(() -> {
                        try (OutputStream outputStream = getContentResolver().openOutputStream(data.getData())) {
                            com.abdurazaaqmohammed.utils.FileUtils.copyFile(merged, outputStream);
                            return true;
                        } catch (Exception e) {
                            showError(e);
                            return false;
                        }
                    }, () -> {
                        if (!criticalErrorOccurred) {
                            final String success = rss.getString(R.string.success_saved);
                            logger.logMessage(success);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, success, Toast.LENGTH_SHORT).show());
                        }
                    }, true);
                    break;
            }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        if (receiverRegistered)
            return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(onDownloadComplete, filter, Context.RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(onDownloadComplete, filter);
        receiverRegistered = true;
    }

    private void runUpdateCheckAfterPermission() {
        Runnable runnable = checkUpdateAfterStoragePermission;
        if (runnable != null)
            runnable.run();
    }

    public long downloadId;
    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (id == downloadId) {
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager == null)
                    return;
                Uri uri = downloadManager.getUriForDownloadedFile(id);
                if (uri != null)
                    InstallUtil.installApk(MainActivity.this, uri);
            }
        }
    };

    public Runnable checkUpdateAfterStoragePermission;

    public void showApkSelectionDialog() {
        toggleAnimation(true);
        logger.logMessage(R.string.searching);
        RunUtil.runInBackground(() -> {
            try {
                List<String> splits = deviceSpecsUtil.getListOfSplits(splitAPKUri);
                Collections.sort(splits, CompareUtils::compareByName);
                final int initialSize = splits.size();
                String[] apkNames = new String[initialSize + 5];
                boolean[] checkedItems = new boolean[initialSize + 5];

                apkNames[0] = rss.getString(R.string.all);
                apkNames[1] = rss.getString(R.string.for_device);
                apkNames[2] = rss.getString(R.string.arch_for_device);
                apkNames[3] = rss.getString(R.string.dpi_for_device);
                apkNames[4] = rss.getString(R.string.lang_for_device);
                for (int i = 5; i < initialSize + 5; i++) {
                    apkNames[i] = splits.get(i - 5);
                    checkedItems[i] = false;
                }

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this).setTitle(rss.getString(R.string.select_splits))
                        .setMultiChoiceItems(apkNames, checkedItems, (dialog, which, isChecked) -> {
                            switch (which) {
                                case 0:
                                    // "Select All" option
                                    for (int i = 5; i < checkedItems.length; i++)
                                        ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                                    break;
                                case 1:
                                    // device specs option
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = (isChecked
                                                && deviceSpecsUtil.shouldIncludeSplit(apkNames[i])));
                                    }
                                    boolean didNotFindAppropriateDpi = true;
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        if (checkedItems[i] && apkNames[i].contains("dpi")) {
                                            didNotFindAppropriateDpi = false;
                                            break;
                                        }
                                    }
                                    if (didNotFindAppropriateDpi) {
                                        for (int i = 5; i < checkedItems.length; i++) {
                                            if (apkNames[i].contains("hdpi")) {
                                                ((AlertDialog) dialog).getListView().setItemChecked(i,
                                                        checkedItems[i] = isChecked);
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case 2:
                                    // arch for device
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        if (deviceSpecsUtil.shouldIncludeArch(apkNames[i]))
                                            ((AlertDialog) dialog).getListView().setItemChecked(i,
                                                    checkedItems[i] = isChecked);
                                    }
                                    break;
                                case 3:
                                    // dpi for device
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        if (deviceSpecsUtil.shouldIncludeDpi(apkNames[i]))
                                            ((AlertDialog) dialog).getListView().setItemChecked(i,
                                                    checkedItems[i] = isChecked);
                                    }
                                    boolean didNotFoundAppropriateDpi = true;
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        if (checkedItems[i] && apkNames[i].contains("dpi")) {
                                            didNotFoundAppropriateDpi = false;
                                            break;
                                        }
                                    }
                                    if (didNotFoundAppropriateDpi) {
                                        for (int i = 5; i < checkedItems.length; i++) {
                                            if (apkNames[i].contains("hdpi")) {
                                                ((AlertDialog) dialog).getListView().setItemChecked(i,
                                                        checkedItems[i] = isChecked);
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case 4:
                                    // lang for device
                                    for (int i = 5; i < checkedItems.length; i++) {
                                        if (deviceSpecsUtil.shouldIncludeLang(apkNames[i]))
                                            ((AlertDialog) dialog).getListView().setItemChecked(i,
                                                    checkedItems[i] = isChecked);
                                    }
                                    break;
                                default:
                                    ListView listView = ((AlertDialog) dialog).getListView();
                                    if (!isChecked)
                                        listView.setItemChecked(0, checkedItems[0] = false); // Uncheck "Select All" if any
                                    // individual item is unchecked
                                    for (int i = 1; i <= 4; i++) {
                                        if (checkedItems[i] && !deviceSpecsUtil.shouldIncludeSplit(apkNames[which])) {
                                            listView.setItemChecked(i, checkedItems[i] = false); // uncheck device arch if
                                            // non device arch selected
                                        }
                                    }
                                    break;
                            }
                        }).setPositiveButton(rss.getString(android.R.string.ok), (dialog, which) -> {
                            for (int i = 1; i < checkedItems.length; i++) {
                                if (checkedItems[i])
                                    splits.remove(apkNames[i]);
                            }

                            if (splits.size() == initialSize) {
                                UrisAreSplitApks = true; // reset
                                showError(rss.getString(R.string.nothing));
                            } else {
                                splitsUnselectedInDialog = splits;
                                process();
                            }
                        }).setNegativeButton(rss.getString(R.string.cancel), null);
                getHandler().post(() -> {
                    toggleAnimation(false);
                    AlertDialog alertDialog = builder.create();
                    styleAlertDialog(alertDialog);
                    // Select base.apk by default
                    // Not force include base.apk because there may be some use case where you
                    // actually need to merge other splits only
                    for (int i = 5; i < apkNames.length; i++) {
                        if (DeviceSpecsUtil.isBaseApk(apkNames[i])) {
                            alertDialog.getListView().setItemChecked(i, checkedItems[i] = true);
                            break;
                        }
                    }
                });
            } catch (Exception e) {
                DeviceSpecsUtil.releaseZipFile();
                toggleAnimation(false);
                showError(e);
            }
        });
    }

    private void setUIAfterMerging(Merger merger) {
        UrisAreSplitApks = true; // reset
        multipleArchives = false;
        findViewById(R.id.fabs).setAlpha(1f);
        findViewById(R.id.cancelButton).setVisibility(View.GONE);
        View installButton = findViewById(R.id.installButton);
        View saveButton = findViewById(R.id.saveButton);
        if (criticalErrorOccurred) {
            installButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
        } else {
            if (signApk && merger.signedApk != null) {
                installButton.setVisibility(View.VISIBLE);
                installButton.setOnClickListener(v -> InstallUtil.installApk(MainActivity.this, merger.signedApk));
            } else {
                installButton.setVisibility(View.GONE);
            }

            if (saveMode == 0) {
                saveButton.setVisibility(View.VISIBLE);
                saveButton.setOnClickListener(view -> {
                    try {
                        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                                .addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("application/vnd.android.package-archive")
                                .putExtra(Intent.EXTRA_TITLE, getOutputFileName()), 2);
                    } catch (ActivityNotFoundException e) {
                        showError(rss.getString(R.string.no_file_manager));
                        saveNow(true);
                    }
                });
            } else {
                saveButton.setVisibility(View.GONE);
            }
        }
    }

    private void copyText(CharSequence text) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("log", text));
        Toast.makeText(this, R.string.copied_log, Toast.LENGTH_SHORT).show();
    }

    public void showError(Throwable e) {
        toggleAnimation(false);
        if (e instanceof OutOfMemoryError) {
            showOutOfMemoryError();
            cleanupAppFolder();
            return;
        }
        if (e == null || e instanceof ClosedByInterruptException || isFinishing() || isDestroyed())
            return;

        final String mainErr = TextUtils.isEmpty(e.getMessage())
                ? e.getClass().getSimpleName() : e.getMessage();
        final boolean errEmpty = TextUtils.isEmpty(e.getMessage());
        criticalErrorOccurred = errEmpty || !mainErr.equals(rss.getString(R.string.sign_failed));

        StringBuilder stackTrace = new StringBuilder();
        if (!errEmpty) {
            stackTrace.append(mainErr);
            boolean zipError = mainErr.contains("Failed to find end record");
            if (!zipError) {
                Uri u = splitAPKUri;
                if (u != null) try {
                    String mimeType = getContentResolver().getType(u);
                    zipError = (TextUtils.isEmpty(mimeType)
                            || (!mimeType.equals("application/zip")
                            && !mimeType.equals("application/octet-stream")
                            && !mimeType.equals("application/vnd.apkm")));
                } catch (Exception ignored) {
                }
            }

            if (zipError) {
                stackTrace.append(" - ")
                        .append(rss.getString(R.string.check_file_valid))
                        .append(rss.getString(R.string.select_from_installed_apps))
                        .append('\n');
            }
        }
        if (!TextUtils.isEmpty(openedFile)) {
            stackTrace.append(openedFile).append('\n');
            openedFile = null;
        }
        StackTraceElement[] elements = e.getStackTrace();
        int count = Math.min(elements.length, 30);
        for (int i = 0; i < count; i++)
            stackTrace.append(elements[i]).append('\n');

        final StringBuilder fullLog = new StringBuilder(stackTrace);
        getHandler().post(() -> {
            if (isFinishing() || isDestroyed())
                return;
            String currentVer;
            try {
                currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception ex) {
                currentVer = "2.3.2";
            }
            fullLog.append('\n')
                    .append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
                    .append(rss.getString(R.string.app_name)).append(' ')
                    .append(currentVer).append('\n')
                    .append("Storage permission granted: ")
                    .append(!com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(this))
                    .append('\n').append(((TextView) findViewById(R.id.logField)).getText());

            String issueText = fullLog.toString();
            if (issueText.length() > 6000)
                issueText = issueText.substring(0, 6000);
            final CharSequence issueBody = Uri.encode(issueText);

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_button_layout, null);
            ((TextView) dialogView.findViewById(R.id.errorD)).setText(stackTrace);
            styleAlertDialog(new MaterialAlertDialogBuilder(this)
                    .setTitle(mainErr)
                    .setView(dialogView)
                    .setPositiveButton(rss.getString(R.string.copy_log), (dialog, which) -> {
                        copyText(fullLog);
                        dialog.dismiss();
                    })
                    .setNegativeButton(rss.getString(R.string.create_issue), (dialog, which) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://github.com/AbdurazaaqMohammed/AntiSplit-M/issues/new?title=Crash%20Report&body="
                                        + issueBody)));
                        dialog.dismiss();
                    })
                    .setNeutralButton(rss.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .create());
            cleanupAppFolder();
        });
    }

    private void showOutOfMemoryError() {
        criticalErrorOccurred = true;
        getHandler().post(() -> {
            if (isFinishing() || isDestroyed())
                return;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.out_of_memory_title)
                    .setMessage(R.string.out_of_memory_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void showError(String err) {
        toggleAnimation(false);
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(err);
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    private String getRawFileName(Uri uri) {
        if (uri == null)
            return null;
        String result = null;

        String scheme = uri.getScheme();
        if (!TextUtils.isEmpty(scheme) && "content".equals(scheme)) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null)
                try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception ignored) {
                }
        }

        if (TextUtils.isEmpty(result)) {
            result = uri.getPath();
            if (!TextUtils.isEmpty(result)) {
                int cut = result.lastIndexOf('/');
                if (cut != -1)
                    result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String getOriginalFileName(Uri uri) {
        String result = getRawFileName(uri);
        logger.logMessage(openedFile = result);

        if (TextUtils.isEmpty(result))
            result = "file_" + System.currentTimeMillis();
        String base = result.replaceFirst("\\.(?:zip|xapk|aspk|apk[sm])$", "");
        return com.abdurazaaqmohammed.utils.FileUtils.sanitizeFileName(base + suffix + ".apk");
    }

    private String getOutputFileName() {
        if (!TextUtils.isEmpty(pkgName)) {
            String versionName = "";
            try {
                versionName = " v" + getPackageManager().getPackageInfo(pkgName, 0).versionName;
            } catch (Exception ignored) {
            }            return com.abdurazaaqmohammed.utils.FileUtils
                    .sanitizeFileName(pkgName + versionName + suffix + ".apk");
        }
        if (UrisAreSplitApks && splitAPKUri != null)
            return getOriginalFileName(splitAPKUri);

        if (!UrisAreSplitApks && uris != null && !uris.isEmpty())
            return getNameFromNonSplitApks();
        return com.abdurazaaqmohammed.utils.FileUtils
                .sanitizeFileName("merged" + suffix + ".apk");
    }

    private void selectDirToSaveAPKOrSaveNow() {
        selectDirToSaveAPKOrSaveNow(saveMode);
    }

    private void selectDirToSaveAPKOrSaveNow(int saveMode) {
        getHandler().post(() -> selectDirToSaveAPKOrSaveNowOnUi(saveMode));
    }

    private void selectDirToSaveAPKOrSaveNowOnUi(int saveMode) {
        if (isFinishing() || isDestroyed())
            return;
        boolean askWhereToSave = saveMode == 0;
        if (askWhereToSave || com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(this)) {
            Toast.makeText(this, R.string.merged, Toast.LENGTH_LONG).show();
            try {
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/vnd.android.package-archive")
                        .putExtra(Intent.EXTRA_TITLE, getOutputFileName()), 2);
            } catch (ActivityNotFoundException e) {
                showError(rss.getString(R.string.no_file_manager));
                saveNow(true);
            }
        } else {
            saveNow(false);
        }
    }

    private void saveNow(boolean notFoundCreateDocument) {
        final File mergedFile = merged;
        if (mergedFile == null) {
            showError(rss.getString(R.string.invalid_file));
            return;
        }
        if (com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(this)) {
            pendingAfterStoragePermission = () -> saveNow(notFoundCreateDocument);
            checkStoragePerm(9);
            return;
        }
        RunUtil.runInBackground(() -> {
            try {
                File f = null;
                if (TextUtils.isEmpty(pkgName) && UrisAreSplitApks && splitAPKUri != null) {
                    try {
                        String sourcePath = FileUtils.getPath(splitAPKUri, MainActivity.this);
                        if (!TextUtils.isEmpty(sourcePath)
                                && !sourcePath.startsWith("/data/")
                                && !sourcePath.contains("/Android/data")) {
                            File sourceFile = new File(sourcePath);
                            String name = sourceFile.getName()
                                    .replaceFirst("\\.(?:zip|xapk|aspk|apk[sm])$", "")
                                    + suffix + ".apk";
                            f = new File(sourceFile.getParentFile(),
                                    com.abdurazaaqmohammed.utils.FileUtils
                                            .sanitizeFileName(name));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (TextUtils.isEmpty(pkgName) && uris != null && !uris.isEmpty()) {
                    try {
                        String sourcePath = FileUtils.getPath(uris.get(0), MainActivity.this);
                        if (!TextUtils.isEmpty(sourcePath)
                                && !sourcePath.startsWith("/data/")
                                && !sourcePath.contains("/Android/data")) {
                            int cut = sourcePath.lastIndexOf(File.separator);
                            if (cut > 0)
                                f = new File(sourcePath.substring(0, cut), getNameFromNonSplitApks());
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (f == null)
                    f = new File(outputFolder, getOutputFileName());

                f = com.abdurazaaqmohammed.utils.FileUtils.getUnusedFile(f);
                logger.logMessage(rss.getString(R.string.output) + f);
                com.abdurazaaqmohammed.utils.FileUtils.copyFile(mergedFile, f);
                if (!criticalErrorOccurred) {
                    final String success = rss.getString(R.string.success_saved);
                    logger.logMessage(success);
                    runOnUiThread(() -> Toast.makeText(this, success, Toast.LENGTH_SHORT).show());
                }
            } catch (Throwable e) {
                if (notFoundCreateDocument) {
                    showError(e);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                TextUtils.isEmpty(e.getMessage())
                                        ? rss.getString(R.string.save_failed) : e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        selectDirToSaveAPKOrSaveNow(0);
                    });
                }
            }
        });
    }

    private String getNameFromNonSplitApks() {
        String realName = null;
        String base = "base.apk";
        if (uris != null) for (Uri uri : uris) {
            final String fileName = getRawFileName(uri);
            if (TextUtils.isEmpty(fileName))
                continue;
            if (base.equals(fileName)) {
                try {
                    File file = new File(FileUtils.getPath(uri, this));
                    PackageInfo info = getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    if (info != null) {
                        realName = info.packageName;
                        break;
                    }
                } catch (Exception ignored) {
                }
            } else if (!fileName.startsWith("config") && !fileName.startsWith("split")) {
                realName = fileName;
                break;
            }
        }
        String result = TextUtils.isEmpty(realName) ? "unknown" + suffix + ".apk"
                : realName.replaceFirst("\\.apk$", suffix + ".apk");
        return com.abdurazaaqmohammed.utils.FileUtils.sanitizeFileName(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void installedAppsClickListener(View v) {
        clearLogFields();
        toggleAnimation(true);
        PackageManager pm = getPackageManager();
        RunUtil.runInBackground(() -> {
            try {
                List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
                List<AppInfo> appInfoList = new ArrayList<>();
                for (PackageInfo packageInfo : packageInfoList) {
                    try {
                        ApplicationInfo ai = packageInfo.applicationInfo;
                        if (ai == null)
                            ai = pm.getApplicationInfo(packageInfo.packageName, 0);
                        if (ai.splitSourceDirs != null) {
                            appInfoList.add(new AppInfo(
                                    String.valueOf(pm.getApplicationLabel(ai)),
                                    packageInfo.packageName,
                                    packageInfo.lastUpdateTime,
                                    packageInfo.firstInstallTime));
                        }
                    } catch (Exception ignored) {
                    }
                }
                sortAppList(appInfoList);
                getHandler().post(() -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    toggleAnimation(false);
                    showInstalledAppsDialog(appInfoList);
                });
            } catch (Throwable e) {
                showError(e);
            }
        });
    }

    private void sortAppList(List<AppInfo> appInfoList) {
        if (sortMode == 0)
            Collections.sort(appInfoList, CompareUtils::compareAppInfoByName);
        else
            Collections.sort(appInfoList, (p1, p2) -> {
                long field1 = (sortMode == 1) ? p1.lastUpdated : p1.firstInstall;
                long field2 = (sortMode == 1) ? p2.lastUpdated : p2.firstInstall;
                return Long.compare(field2, field1);
            });
    }

    private void showInstalledAppsDialog(List<AppInfo> appInfoList) {
        AlertDialog ad = new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.select_from_installed_apps)
                .setNegativeButton(rss.getString(R.string.cancel), null)
                .create();

        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_search, null);
        ListView listView = dialogView.findViewById(R.id.list_view);
        final AppListArrayAdapter adapter = new AppListArrayAdapter(MainActivity.this, appInfoList, true);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ad.dismiss();
            pkgName = adapter.filteredAppInfoList.get(position).packageName;
            processFromInstalledApp();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ad.dismiss();
            pkgName = adapter.filteredAppInfoList.get(position).packageName;
            processFromInstalledApp();
            return true;
        });

        EditText searchBar = dialogView.findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialogView.findViewById(R.id.filter_button).setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.sort_name) {
                    sortMode = 0;
                } else if (itemId == R.id.date_updated) {
                    sortMode = 1;
                } else {
                    sortMode = 2;
                }
                sortAppList(appInfoList);
                adapter.setItems(appInfoList);
                return true;
            });
            popupMenu.show();
        });

        ad.setView(dialogView);
        styleAlertDialog(ad);
    }

    private void clearLogFields() {
        ((TextView) findViewById(R.id.logField)).setText("");
        ((TextView) findViewById(R.id.errorField)).setText("");
        if (logger != null)
            logger.clear();
    }

    /** @noinspection AssignmentUsedAsCondition */private void settingsButtonListener(View v) {
        ScrollView settingsDialog = (ScrollView) LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.settings_dialog, null);

        TextInputEditText suffixInput = settingsDialog.findViewById(R.id.suffixInput);
        suffixInput.setText(suffix);
        suffixInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                suffix = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        MaterialButtonToggleGroup themeButtons = settingsDialog.findViewById(R.id.themeToggleGroup);
        themeButtons.check(
                systemTheme ? R.id.systemThemeButton
                        : theme == R.style.Theme_MyApp_Light ? R.id.lightThemeButton
                        : theme == R.style.Theme_MyApp_Dark ? R.id.darkThemeButton
                        : R.id.blackThemeButton);

        for (int i = 0; i < themeButtons.getChildCount(); i++) {
            View child = themeButtons.getChildAt(i);
            if (child instanceof MaterialButton) {
                child.setOnLongClickListener(v3 -> {
                    int buttonId = v3.getId();
                    if (buttonId == R.id.lightThemeButton)
                        Toast.makeText(this, R.string.light_theme, Toast.LENGTH_SHORT).show();
                    else if (buttonId == R.id.darkThemeButton)
                        Toast.makeText(this, R.string.dark_theme, Toast.LENGTH_SHORT).show();
                    else if (buttonId == R.id.blackThemeButton)
                        Toast.makeText(this, R.string.black_theme, Toast.LENGTH_SHORT).show();
                    else if (buttonId == R.id.systemThemeButton)
                        Toast.makeText(this, R.string.system_theme, Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }

        themeButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;
            systemTheme = false;
            if (checkedId == R.id.lightThemeButton) {
                theme = R.style.Theme_MyApp_Light;
            } else if (checkedId == R.id.darkThemeButton) {
                theme = R.style.Theme_MyApp_Dark;
            } else if (checkedId == R.id.blackThemeButton) {
                theme = R.style.Theme_MyApp_Black;
            } else {
                systemTheme = true;
                theme = ((rss.getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
                        ? R.style.Theme_MyApp_Dark : R.style.Theme_MyApp_Light;
            }

            getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                    .putInt("theme", theme)
                    .putBoolean("systemTheme", systemTheme)
                    .apply();
            setTheme(theme);
            getIntent().replaceExtras(new Bundle());
            getIntent().setAction("");
            getIntent().setData(null);
            recreate();
        });

        Button checkUpdateNow = settingsDialog.findViewById(R.id.checkUpdateNow);
        CompoundButton updateSwitch = settingsDialog.findViewById(R.id.updateToggle);
        updateSwitch.setChecked(checkForUpdates);
        updateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> checkUpdateNow
                .setVisibility((checkForUpdates = isChecked) ? View.GONE : View.VISIBLE));
        checkUpdateNow.setVisibility(checkForUpdates ? View.GONE : View.VISIBLE);
        checkUpdateNow.setOnClickListener(v1 -> UpdateUtil.checkForUpdates(true, this));

        CompoundButton logSwitch = settingsDialog.findViewById(R.id.logToggle);
        logSwitch.setChecked(logEnabled);
        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> logEnabled = isChecked);

        CompoundButton signToggle = settingsDialog.findViewById(R.id.signToggle);
        signToggle.setChecked(signApk);
        signToggle.setOnCheckedChangeListener((buttonView, isChecked) -> signApk = isChecked);

        CompoundButton forceToggle = settingsDialog.findViewById(R.id.forceToggle);
        forceToggle.setChecked(force);
        forceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> force = isChecked);

        CompoundButton selectSplitsAutomaticallySwitch = settingsDialog
                .findViewById(R.id.selectSplitsForDeviceToggle);
        CompoundButton showDialogSwitch = settingsDialog.findViewById(R.id.showDialogToggle);

        showDialogSwitch.setChecked(showDialog);
        showDialogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (showDialog = isChecked) {
                selectSplitsAutomaticallySwitch.setChecked(selectSplitsForDevice = false);
            } else {
                splitsUnselectedInDialog = null;
            }
        });

        selectSplitsAutomaticallySwitch.setChecked(selectSplitsForDevice);
        selectSplitsAutomaticallySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (selectSplitsForDevice = isChecked)
                showDialogSwitch.setChecked(showDialog = false);
        });

        settingsDialog.findViewById(R.id.langPicker).setOnClickListener(v2 -> {
            String[] langs = rss.getStringArray(R.array.langs);
            String[] display = rss.getStringArray(R.array.langs_display);

            AlertDialog ad = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(R.string.lang)
                    .setSingleChoiceItems(display, -1, (dialog, which) -> {
                        String selected = langs[which];
                        getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                                .putString("lang", selected).apply();
                        lang = selected;
                        dialog.dismiss();
                        getIntent().replaceExtras(new Bundle());
                        getIntent().setAction("");
                        getIntent().setData(null);
                        recreate();
                    }).create();
            MainActivity.this.styleAlertDialog(ad);
            for (int i = 0; i < langs.length; i++) {
                if (lang != null && lang.equals(langs[i])) {
                    ad.getListView().setItemChecked(i, true);
                    break;
                }
            }
        });

        AlertDialog settingsAlert = new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.settings)
                .setView(settingsDialog)
                .setPositiveButton(rss.getString(R.string.close), (dialog, which) -> dialog.dismiss())
                .create();
        MainActivity.this.styleAlertDialog(settingsAlert);

        AutoCompleteTextView autoCompleteTextView = settingsDialog.findViewById(R.id.auto_complete_tv);
        String[] items = {rss.getString(R.string.ask), rss.getString(R.string.same),
                rss.getString(R.string.pick_folder)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdownitem, items);
        autoCompleteTextView.setText(items[Math.min(saveMode, items.length - 1)], false);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            saveMode = position;
            if (position != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                checkStoragePerm(9);
            if (position == 2) {
                TextInputLayout inputLayout = new TextInputLayout(this);
                inputLayout.setHint(rss.getString(R.string.pick_folder));
                TextInputEditText editText = new TextInputEditText(inputLayout.getContext());
                editText.setText(outputFolder);
                editText.setSingleLine(true);
                inputLayout.addView(editText);
                styleAlertDialog(new MaterialAlertDialogBuilder(this)
                        .setTitle(rss.getString(R.string.pick_folder))
                        .setView(inputLayout)
                        .setNegativeButton(rss.getString(R.string.cancel), null)
                        .setPositiveButton(rss.getString(android.R.string.ok), (dialog, which) -> {
                            CharSequence text = editText.getText();
                            if (TextUtils.isEmpty(text)) {
                                showError(rss.getString(R.string.no_filepath) + text);
                            } else {
                                String string = text.toString().trim();
                                File folder = new File(string);
                                if (folder.exists() || folder.mkdirs()) {
                                    outputFolder = string;
                                } else {
                                    showError(rss.getString(R.string.no_filepath) + text);
                                    saveMode = 0;
                                }
                            }
                        }).create(), false);
            }
        });

        AutoCompleteTextView autoCompleteTextView2 = settingsDialog.findViewById(R.id.auto_complete_tv2);
        String[] items2 = {rss.getString(R.string.default_c), "0", "1", "2", "3", "4",
                "5", "6", "7", "8", "9"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, R.layout.dropdownitem, items2);
        int compressionIndex = Math.max(0, Math.min(compressionLevel + 1, items2.length - 1));
        autoCompleteTextView2.setText(items2[compressionIndex], false);
        autoCompleteTextView2.setAdapter(adapter2);
        autoCompleteTextView2.setOnItemClickListener(
                (parent, view, position, id) -> compressionLevel = position - 1);
    }
}
