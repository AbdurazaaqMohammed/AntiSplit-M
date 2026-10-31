package com.abdurazaaqmohammed.AntiSplit.main;

import static com.abdurazaaqmohammed.utils.LegacyUtils.aboveSdk20;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.BackgroundTask;
import com.abdurazaaqmohammed.utils.DeviceSpecsUtil;
import com.abdurazaaqmohammed.utils.FileUtils;
import com.abdurazaaqmohammed.utils.InstallUtil;
import com.abdurazaaqmohammed.utils.LegacyUtils;
import com.abdurazaaqmohammed.utils.UpdateUtil;
import com.abdurazaaqmohammed.utils.UriNames;
import com.github.paul035.LocaleHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.reandroid.utils.io.FileUtil;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_OPEN_ARCHIVE = 1;
    static final int REQUEST_SAVE_AS = 2;
    static final int REQUEST_STORAGE_PERMISSION = 9;
    static final int REQUEST_UPDATE_PERMISSION = 10;

    public long downloadId;
    public Runnable checkUpdateAfterStoragePermission;

    private Handler handler;
    private Resources rss;
    private AppSettings settings;
    private MyAPKLogger logger;
    private ErrorReporter errors;
    private DeviceSpecsUtil deviceSpecs;
    private OutputNaming naming;
    private MergeController merger;

    private View decodeButton;
    private View fromAppsButton;
    private View progressIndicator;
    private View floatingButtons;
    private View cancelButton;
    private View copyButton;
    private View installButton;
    private View saveButton;

    private final ArrayDeque<MergeController.MergedApk> saveQueue = new ArrayDeque<>();
    private boolean saving;
    private File pendingSaveFile;
    private MergeController.MergedApk lastMergedApk;
    private boolean receiverRegistered;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase,
                AppSettings.savedLanguage(newBase)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(Looper.getMainLooper());
        rss = getResources();
        settings = AppSettings.load(this);
        setTheme(settings.theme);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applyWindowInsets();
        applySystemBarAppearance();

        bindViews();
        logger = new MyAPKLogger(handler, rss, settings,
                findViewById(R.id.logField), findViewById(R.id.scrollView));
        errors = new ErrorReporter(this, handler, rss,
                findViewById(R.id.logField), findViewById(R.id.errorField));
        deviceSpecs = new DeviceSpecsUtil(this, logger);
        naming = new OutputNaming(this, settings);
        merger = new MergeController(this, settings, deviceSpecs, logger, naming, handler,
                mergeListener);

        setupToolbar();
        setupButtons();

        BackgroundTask.submit(() -> FileUtil.deleteDirectory(getCacheDir()));
        FileUtils.removeEmptyOutputFolder(this);

        if (settings.checkForUpdates)
            UpdateUtil.checkForUpdates(false, this);

        handleIntent(getIntent());
    }

    private void bindViews() {
        decodeButton = findViewById(R.id.decodeButton);
        fromAppsButton = findViewById(R.id.fromAppsButton);
        progressIndicator = findViewById(R.id.progressIndicator);
        floatingButtons = findViewById(R.id.fabs);
        cancelButton = findViewById(R.id.cancelButton);
        copyButton = findViewById(R.id.copyButton);
        installButton = findViewById(R.id.installButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        Drawable settingsIcon = toolbar.getMenu().findItem(R.id.settingsButton).getIcon();
        if (settingsIcon != null)
            DrawableCompat.setTint(settingsIcon,
                    MaterialColors.getColor(toolbar,
                            com.google.android.material.R.attr.colorOnSurfaceVariant));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.settingsButton) {
                new SettingsSheet(this, settings).show();
                return true;
            }
            return false;
        });
    }

    private void setupButtons() {
        decodeButton.setOnClickListener(v -> openArchivePicker());

        if (aboveSdk20)
            fromAppsButton.setOnClickListener(v -> showInstalledAppsPicker());
        else
            fromAppsButton.setVisibility(View.GONE);

        cancelButton.setOnClickListener(v -> restartApp());
        copyButton.setOnClickListener(v -> errors.copyLog());
        saveButton.setOnClickListener(v -> {
            if (lastMergedApk == null)
                return;
            saveQueue.add(lastMergedApk);
            saveNextApk();
        });
    }

    private void openArchivePicker() {
        clearLog();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_OPEN_ARCHIVE);
    }

    private void showInstalledAppsPicker() {
        new InstalledAppsPicker(this, settings, packageName -> {
            clearLog();
            merger.mergeInstalledApp(packageName);
        }).show();
    }

    private void handleIntent(Intent intent) {
        if (intent == null)
            return;

        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = getStreamExtra(intent);
            if (uri != null)
                handleSingleUri(uri);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            if (intent.getData() != null)
                handleSingleUri(intent.getData());
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            List<Uri> uris = getStreamExtras(intent);
            if (uris != null && !uris.isEmpty())
                handleSelection(uris);
        }

        intent.setAction(null);
        intent.setData(null);
        intent.replaceExtras(new Bundle());
    }

    private Uri getStreamExtra(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        return intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    private ArrayList<Uri> getStreamExtras(Intent intent) {
        if (!intent.hasExtra(Intent.EXTRA_STREAM))
            return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class);
        return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    }

    private void handleSingleUri(Uri uri) {
        if (UriNames.looksLikeApk(uri)) {
            errors.show(rss.getString(R.string.not_split, uri.toString()));
            return;
        }
        startArchiveMerge(uri);
    }

    private void handleSelection(List<Uri> uris) {
        if (uris.isEmpty())
            return;
        if (uris.size() == 1) {
            handleSingleUri(uris.get(0));
            return;
        }

        clearLog();
        boolean allApkFiles = true;
        for (Uri uri : uris) {
            if (!UriNames.looksLikeApk(uri)) {
                allApkFiles = false;
                break;
            }
        }
        if (allApkFiles)
            merger.mergeApkFiles(uris);
        else
            merger.mergeArchives(uris);
    }

    private void startArchiveMerge(Uri archiveUri) {
        clearLog();
        if (settings.showDialog)
            new SplitPicker(this, deviceSpecs, logger, archiveUri,
                    (uri, excludedSplits) -> merger.mergeArchive(uri, excludedSplits)).show();
        else
            merger.mergeArchive(archiveUri, null);
    }

    private final MergeController.Listener mergeListener = new MergeController.Listener() {
        @Override
        public void onMergeStarted() {
            decodeButton.setEnabled(false);
            fromAppsButton.setEnabled(false);
            installButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            setProgressVisible(true);
            cancelButton.setVisibility(View.VISIBLE);
            copyButton.setVisibility(View.VISIBLE);
            floatingButtons.setAlpha(0.5f);
        }

        @Override
        public void onMerged(MergeController.MergedApk mergedApk) {
            if (isFinishing() || isDestroyed())
                return;
            if (mergedApk.installUri != null) {
                installButton.setVisibility(View.VISIBLE);
                installButton.setOnClickListener(v ->
                        InstallUtil.installApk(MainActivity.this, mergedApk.installUri));
            } else {
                installButton.setVisibility(View.GONE);
            }
            lastMergedApk = mergedApk;
            saveButton.setVisibility(settings.saveMode == AppSettings.SAVE_ASK
                    ? View.VISIBLE : View.GONE);
            saveQueue.add(mergedApk);
            saveNextApk();
        }

        @Override
        public void onMergeFailed(Throwable error) {
            errors.show(error, merger.getCurrentSource());
        }

        @Override
        public void onBatchFinished() {
            setProgressVisible(false);
            decodeButton.setEnabled(true);
            fromAppsButton.setEnabled(true);
            cancelButton.setVisibility(View.GONE);
            floatingButtons.setAlpha(1f);
        }
    };

    private void saveNextApk() {
        if (saving)
            return;
        MergeController.MergedApk mergedApk = saveQueue.poll();
        if (mergedApk == null)
            return;
        saving = true;
        try {
            saveMergedApk(mergedApk);
        } catch (RuntimeException e) {
            saving = false;
            errors.show(e, null);
            saveNextApk();
        }
    }

    private void finishSave() {
        saving = false;
        saveNextApk();
    }

    private void saveMergedApk(MergeController.MergedApk mergedApk) {
        boolean askWhereToSave = settings.saveMode == AppSettings.SAVE_ASK
                || !FileUtils.hasStoragePermission(this);
        if (askWhereToSave) {
            Toast.makeText(this, R.string.merged, Toast.LENGTH_LONG).show();
            pendingSaveFile = mergedApk.file;
            if (!launchCreateDocument(mergedApk.outputName)) {
                pendingSaveFile = null;
                saveToOutputFolder(mergedApk.file, mergedApk.outputName);
            }
            return;
        }
        saveToConfiguredFolder(mergedApk);
    }

    private boolean launchCreateDocument(String outputName) {
        try {
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/vnd.android.package-archive")
                    .putExtra(Intent.EXTRA_TITLE, outputName), REQUEST_SAVE_AS);
            return true;
        } catch (ActivityNotFoundException e) {
            errors.show(R.string.no_file_manager);
            return false;
        }
    }

    private void saveToConfiguredFolder(final MergeController.MergedApk mergedApk) {
        final int saveMode = settings.saveMode;
        final String outputFolder = settings.outputFolder;
        final List<Uri> sourceUris = mergedApk.sourceUris;
        BackgroundTask.submit(() -> {
            File directory = saveMode == AppSettings.SAVE_NEXT_TO_SOURCE
                    ? naming.preferredDirectory(sourceUris) : null;
            if (directory == null)
                directory = new File(outputFolder);
            writeMergedFile(mergedApk.file, mergedApk.outputName, directory);
        }, handler,
                this::onSaveSucceeded,
                error -> {
                    errors.show(error, null);
                    finishSave();
                });
    }

    private void saveToOutputFolder(final File mergedFile, final String outputName) {
        BackgroundTask.submit(() -> writeMergedFile(mergedFile, outputName,
                        new File(settings.outputFolder)),
                handler,
                this::onSaveSucceeded,
                error -> {
                    errors.show(error, null);
                    finishSave();
                });
    }

    private void writeMergedFile(File mergedFile, String outputName, File directory)
            throws Exception {
        File target = FileUtils.getUnusedFile(new File(directory, outputName));
        logger.logMessage(rss.getString(R.string.output) + target);
        FileUtils.copyFile(mergedFile, target);
        logger.logMessage(R.string.success_saved);
    }

    private void onSaveSucceeded() {
        Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show();
        finishSave();
    }

    private void writeToUri(final File mergedFile, final Uri destination) {
        BackgroundTask.submit(() -> {
            try (java.io.OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null)
                    throw new java.io.IOException("Unable to open " + destination);
                FileUtils.copyFile(mergedFile, output);
            }
        }, handler,
                this::onSaveSucceeded,
                error -> {
                    errors.show(error, null);
                    finishSave();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_STORAGE_PERMISSION
                || requestCode == REQUEST_UPDATE_PERMISSION) {
            handlePermissionResult(requestCode, FileUtils.hasStoragePermission(this));
            return;
        }

        if (requestCode == REQUEST_OPEN_ARCHIVE) {
            if (resultCode != RESULT_OK || data == null)
                return;
            handlePickedDocuments(data);
            return;
        }

        if (requestCode == REQUEST_SAVE_AS) {
            File mergedFile = pendingSaveFile;
            pendingSaveFile = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null
                    || mergedFile == null) {
                finishSave();
                return;
            }
            writeToUri(mergedFile, data.getData());
        }
    }

    private void handlePickedDocuments(Intent data) {
        ClipData clipData = data.getClipData();
        if (clipData == null) {
            Uri uri = data.getData();
            if (uri == null)
                errors.show(rss.getString(R.string.invalid_file));
            else
                handleSingleUri(uri);
            return;
        }

        List<Uri> uris = new ArrayList<>();
        for (int i = 0; i < clipData.getItemCount(); i++)
            uris.add(clipData.getItemAt(i).getUri());
        handleSelection(uris);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        handlePermissionResult(requestCode, granted);
    }

    private void handlePermissionResult(int requestCode, boolean granted) {
        if (requestCode == REQUEST_UPDATE_PERMISSION) {
            Runnable download = checkUpdateAfterStoragePermission;
            checkUpdateAfterStoragePermission = null;
            if (granted && download != null)
                download.run();
            return;
        }
        if (requestCode == REQUEST_STORAGE_PERMISSION && !granted)
            settings.saveMode = AppSettings.SAVE_ASK;
    }

    public void checkStoragePerm(int requestCode) {
        if (FileUtils.hasStoragePermission(this))
            return;

        Toast.makeText(this, rss.getString(R.string.grant_storage), Toast.LENGTH_LONG).show();
        if (LegacyUtils.supportsWriteExternalStorage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        requestCode);
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        if (receiverRegistered)
            return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else
            registerReceiver(downloadReceiver, filter);
        receiverRegistered = true;
    }

    @Override
    protected void onPause() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(downloadReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
        settings.save(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (merger != null && !merger.isRunning() && !saving) {
            DeviceSpecsUtil.release();
            FileUtil.deleteDirectory(getCacheDir());
        }
        FileUtils.removeEmptyOutputFolder(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!settings.systemTheme)
            return;
        int systemTheme = AppSettings.systemTheme(this);
        if (settings.theme == systemTheme)
            return;
        settings.theme = systemTheme;
        setTheme(systemTheme);
        recreate();
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != downloadId)
                return;
            DownloadManager downloadManager =
                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager == null)
                return;
            Uri uri = downloadManager.getUriForDownloadedFile(id);
            if (uri != null)
                InstallUtil.installApk(MainActivity.this, uri);
        }
    };

    private void restartApp() {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent == null || launchIntent.getComponent() == null) {
                recreate();
                return;
            }
            startActivity(Intent.makeRestartActivityTask(launchIntent.getComponent()));
            Runtime.getRuntime().exit(0);
        } catch (Exception e) {
            recreate();
        }
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

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        boolean light = settings.isLightTheme();
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            window.getDecorView().setForceDarkAllowed(false);
    }

    void recreateForSettingsChange(int theme) {
        settings.theme = theme;
        settings.save(this);
        setTheme(theme);
        recreateWithCleanIntent();
    }

    void recreateForLanguageChange() {
        settings.save(this);
        recreateWithCleanIntent();
    }

    private void recreateWithCleanIntent() {
        getIntent().replaceExtras(new Bundle());
        getIntent().setAction("");
        getIntent().setData(null);
        recreate();
    }

    void clearLog() {
        errors.clear();
        logger.clear();
    }

    void setProgressVisible(boolean visible) {
        progressIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void reportError(Throwable error) {
        errors.show(error, merger == null ? null : merger.getCurrentSource());
    }

    void reportError(CharSequence message) {
        errors.show(message);
    }

    void reportError(int messageRes) {
        errors.show(messageRes);
    }

    public AppSettings getSettings() {
        return settings;
    }

    public Handler getHandler() {
        return handler;
    }
}
