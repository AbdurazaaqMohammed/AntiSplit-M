package com.abdurazaaqmohammed.AntiSplit.main;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static com.abdurazaaqmohammed.AntiSplit.main.LegacyUtils.supportsActionBar;
import static com.abdurazaaqmohammed.AntiSplit.main.LegacyUtils.supportsArraysCopyOfAndDownloadManager;
import static com.reandroid.apkeditor.merge.LogUtil.logEnabled;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.reandroid.apkeditor.merge.LogUtil;
import com.reandroid.apkeditor.merge.Merger;
import com.starry.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.github.paul035.LocaleHelper;
import yuku.ambilwarna.AmbilWarnaDialog;

/** @noinspection deprecation*/
public class MainActivity extends Activity implements Merger.LogListener {
    private static boolean ask = true;
    private static boolean showDialog;
    private static boolean signApk;
    private static boolean selectSplitsForDevice;
    private Uri splitAPKUri;
    private ArrayList<Uri> uris;
    private boolean urisAreSplitApks = true;
    public static int textColor;
    public static int bgColor;
    public static boolean errorOccurred;
    //public static boolean revanced;
    public static boolean checkForUpdates;
    public static String lang;
    public DeviceSpecsUtil DeviceSpecsUtil;

    public void setButtonBorder(Button button) {
        ShapeDrawable border = new ShapeDrawable(new RectShape());
        Paint paint = border.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(textColor);
        paint.setStrokeWidth(4);

        button.setTextColor(textColor);
        button.setBackgroundDrawable(border);
    }

    private void setColor(int color, boolean isTextColor, ScrollView settingsMenu) {
        final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
        boolean fromSettingsMenu = settingsMenu != null;
        //if(fromSettingsMenu) settingsMenu.setBackgroundColor(color);
        if(isTextColor) {
            textColor = color;
            ((TextView) findViewById(R.id.errorField)).setTextColor(color);
            ((TextView) findViewById(R.id.logField)).setTextColor(color);
            if(fromSettingsMenu) {
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.logToggle : R.id.logToggleText)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.showDialogToggle : R.id.showDialogToggleText)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.signToggle : R.id.signToggleText)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.selectSplitsForDeviceToggle : R.id.selectSplitsForDeviceToggleText)).setTextColor(color);
                //((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.revancedToggle : R.id.revancedText)).setTextColor(color);
                ((TextView) settingsMenu.findViewById(supportsSwitch ? R.id.updateToggle : R.id.updateToggleText)).setTextColor(color);
            }
        } else findViewById(R.id.main).setBackgroundColor(bgColor = color);
        Button decodeButton = findViewById(R.id.decodeButton);
        setButtonBorder(decodeButton);
        LightingColorFilter themeColor = new LightingColorFilter(0xFF000000, textColor);
        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setColorFilter(themeColor);

        ((ImageView) findViewById(R.id.loadingImage)).setColorFilter(themeColor);
        if(fromSettingsMenu) {
            setButtonBorder(settingsMenu.findViewById(R.id.langPicker));
            setButtonBorder(settingsMenu.findViewById(R.id.changeTextColor));
            setButtonBorder(settingsMenu.findViewById(R.id.changeBgColor));
            setButtonBorder(settingsMenu.findViewById(R.id.checkUpdateNow));
            if(!supportsSwitch) {
                //setButtonBorder(settingsMenu.findViewById(R.id.revancedToggle));
                setButtonBorder(settingsMenu.findViewById(R.id.logToggle));
                setButtonBorder(settingsMenu.findViewById(R.id.ask));
                setButtonBorder(settingsMenu.findViewById(R.id.showDialogToggle));
                setButtonBorder(settingsMenu.findViewById(R.id.selectSplitsForDeviceToggle));
                setButtonBorder(settingsMenu.findViewById(R.id.signToggle));
                setButtonBorder(settingsMenu.findViewById(R.id.updateToggle));
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    /** @noinspection AssignmentUsedAsCondition*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        ActionBar ab;
        File externalCacheDir;
        if (LegacyUtils.supportsExternalCacheDir && ((externalCacheDir = getExternalCacheDir()) != null)) deleteDir(externalCacheDir);

        deleteDir(getCacheDir());
        if(LegacyUtils.supportsActionBar && (ab = getActionBar()) != null) {
            /*Spannable text = new SpannableString(getString(R.string.app_name));
            text.setSpan(new ForegroundColorSpan(textColor), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ab.setTitle(text);
            ab.setBackgroundDrawable(new ColorDrawable(bgColor));*/
            ab.hide();
        }

        setContentView(R.layout.activity_main);
        DeviceSpecsUtil = new DeviceSpecsUtil(this);

        // Fetch settings from SharedPreferences
        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        setColor(settings.getInt("textColor", 0xffffffff), true, null);
        setColor(settings.getInt("backgroundColor", 0xff000000), false, null);

        checkForUpdates = settings.getBoolean("checkForUpdates", true);
        //revanced = settings.getBoolean("revanced", false);
        signApk = settings.getBoolean("signApk", true);
        showDialog = settings.getBoolean("showDialog", false);
        selectSplitsForDevice = settings.getBoolean("selectSplitsForDevice", false);
        logEnabled = settings.getBoolean("logEnabled", true);
        ask = settings.getBoolean("ask", true);
        LogUtil.setLogListener(this);

        lang = settings.getString("lang", "en");
        if(Objects.equals(lang, Locale.getDefault().getLanguage())) rss = getResources();
        else updateLang(LocaleHelper.setLocale(MainActivity.this, lang).getResources(), null);

        ImageView settingsButton = findViewById(R.id.settingsButton);
        Button decodeButton = findViewById(R.id.decodeButton);
        ((LinearLayout) findViewById(R.id.topButtons)).setGravity(Gravity.CENTER_VERTICAL);

        settingsButton.setOnClickListener(v -> {
            ScrollView l = (ScrollView) LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
            l.setBackgroundColor(bgColor);

            setColor(textColor, true, l);

            ((TextView) l.findViewById(R.id.langPicker)).setText(rss.getString(R.string.lang));
            final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
            ((TextView) l.findViewById(supportsSwitch ? R.id.logToggle : R.id.logToggleText)).setText(rss.getString(R.string.enable_logs));
            ((TextView) l.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setText(rss.getString(R.string.ask));
            ((TextView) l.findViewById(supportsSwitch ? R.id.showDialogToggle : R.id.showDialogToggleText)).setText(rss.getString(R.string.show_dialog));
            ((TextView) l.findViewById(supportsSwitch ? R.id.signToggle : R.id.signToggleText)).setText(rss.getString(R.string.sign_apk));
            ((TextView) l.findViewById(supportsSwitch ? R.id.selectSplitsForDeviceToggle : R.id.selectSplitsForDeviceToggleText)).setText(rss.getString(R.string.automatically_select));
            ((TextView) l.findViewById(supportsSwitch ? R.id.updateToggle : R.id.updateToggleText)).setText(rss.getString(R.string.auto_update));
          //  ((TextView) l.findViewById(supportsSwitch ? R.id.revancedToggle : R.id.revancedText)).setText(rss.getString(R.string.fix));
            ((TextView) l.findViewById(R.id.changeTextColor)).setText(rss.getString(R.string.change_text_color));
            ((TextView) l.findViewById(R.id.changeBgColor)).setText(rss.getString(R.string.change_background_color));
            ((TextView) l.findViewById(R.id.checkUpdateNow)).setText(rss.getString(R.string.check_update_now));

            Button checkUpdateNow = l.findViewById(R.id.checkUpdateNow);
            CompoundButton updateSwitch = l.findViewById(R.id.updateToggle);
            updateSwitch.setChecked(checkForUpdates);
            updateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> checkUpdateNow.setVisibility((checkForUpdates = isChecked) ? View.GONE : View.VISIBLE));
            if(checkForUpdates) {
                checkUpdateNow.setVisibility(View.GONE);
                new CheckForUpdatesTask(this, false).execute();
            } else {
                checkUpdateNow.setVisibility(View.VISIBLE);
            }
            checkUpdateNow.setOnClickListener(v1 -> new CheckForUpdatesTask(this, true).execute());

            /*CompoundButton revancedSwitch = l.findViewById(R.id.revancedToggle);
            revancedSwitch.setChecked(revanced);
            revancedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> revanced = isChecked);*/

            CompoundButton logSwitch = l.findViewById(R.id.logToggle);
            logSwitch.setChecked(logEnabled);
            logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> logEnabled = isChecked);

            CompoundButton signToggle = l.findViewById(R.id.signToggle);
            signToggle.setChecked(signApk);
            signToggle.setOnCheckedChangeListener((buttonView, isChecked) -> signApk = isChecked);

            CompoundButton selectSplitsAutomaticallySwitch = l.findViewById(R.id.selectSplitsForDeviceToggle);
            CompoundButton showDialogSwitch = l.findViewById(R.id.showDialogToggle);

            showDialogSwitch.setChecked(showDialog);
            showDialogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showDialog = isChecked;
                if(isChecked) {
                    selectSplitsForDevice = false;
                    selectSplitsAutomaticallySwitch.setChecked(false);
                }
            });

            selectSplitsAutomaticallySwitch.setChecked(selectSplitsForDevice);
            selectSplitsAutomaticallySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectSplitsForDevice = isChecked;
                if(isChecked) {
                    showDialog = false;
                    showDialogSwitch.setChecked(false);
                }
            });

            CompoundButton askSwitch = l.findViewById(R.id.ask);
            if(LegacyUtils.doesNotSupportInbuiltAndroidFilePicker) {
                ask = false;
                askSwitch.setVisibility(View.GONE);
            }
            else {
                askSwitch.setChecked(ask);
                askSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    ask = isChecked;
                    if(!isChecked) checkStoragePerm();
                });
            }

            Button langPicker = l.findViewById(R.id.langPicker);
            setButtonBorder(langPicker);
            langPicker.setOnClickListener(v2 -> {
                int curr = -1;

                String[] langs = rss.getStringArray(R.array.langs);

                String[] display = rss.getStringArray(R.array.langs_display);

                styleAlertDialog(new AlertDialog.Builder(this).setSingleChoiceItems(display, curr, (dialog, which) -> {
                    updateLang(LocaleHelper.setLocale(MainActivity.this, lang = langs[which]).getResources(), l);
                    dialog.dismiss();
                }).create(), display, true);
            });

            l.findViewById(R.id.changeBgColor).setOnClickListener(v3 -> showColorPickerDialog(false, bgColor, l));
            l.findViewById(R.id.changeTextColor).setOnClickListener(v4 -> showColorPickerDialog(true, textColor, l));
            TextView title = new TextView(this);
            title.setText(rss.getString(R.string.settings));
            title.setTextColor(textColor);
            title.setTextSize(25);
            styleAlertDialog(
                    new AlertDialog.Builder(this).setCustomTitle(title).setView(l)
                            .setPositiveButton(rss.getString(R.string.close), (dialog, which) -> dialog.dismiss()).create(), null, false);
        });

        decodeButton.setOnClickListener(v -> {
            if(LegacyUtils.doesNotSupportInbuiltAndroidFilePicker) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.MULTI_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = Environment.getExternalStorageDirectory();
                properties.error_dir = Environment.getExternalStorageDirectory();
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[] {"apk", "zip", "apks", "aspk", "apks", "xapk", "apkm"};
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties, textColor, bgColor);
                dialog.setTitle(rss.getString(R.string.select));
                dialog.setDialogSelectionListener(files -> {
                    urisAreSplitApks = !files[0].endsWith(".apk");
                    uris = new ArrayList<>();
                    for(String file : files) {
                        Uri uri = Uri.fromFile(new File(file));
                        if (urisAreSplitApks) processOneSplitApkUri(uri);
                        else uris.add(uri);
                    }
                    dialog.dismiss();
                });
                runOnUiThread(dialog::show);
            }

            else MainActivity.this.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/vnd.android.package-archive", "application/octet-stream"})
                    , 1);
        } // XAPK is octet-stream
        );
        decodeButton.post(() -> {
            int buttonHeight = decodeButton.getHeight();
            int size = (int) (buttonHeight * 0.75);
            ViewGroup.LayoutParams params = settingsButton.getLayoutParams();
            params.height = size;
            params.width = size;
            settingsButton.setLayoutParams(params);
        });


        // Check if user shared or opened file with the app.
        final Intent openIntent = getIntent();
        final String action = openIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            splitAPKUri = openIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            splitAPKUri = openIntent.getData();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && openIntent.hasExtra(Intent.EXTRA_STREAM)) {
            uris = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class) : openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                final Uri uri = uris.get(0);
                String path = uri.getPath();
                urisAreSplitApks = TextUtils.isEmpty(path) || !path.endsWith(".apk");
                if(urisAreSplitApks) splitAPKUri = uri;
                else selectDirToSaveAPKOrSaveNow();
            }
        }
        if (splitAPKUri != null) {
            if(showDialog) showApkSelectionDialog();
            else selectDirToSaveAPKOrSaveNow();
        }
    }

    public void styleAlertDialog(AlertDialog ad, String[] display, boolean isLang) {
        GradientDrawable border = new GradientDrawable();
        border.setColor(bgColor); // Background color
        border.setStroke(5, textColor); // Border width and color
        border.setCornerRadius(16);

        runOnUiThread(() -> {
            ad.show();
            if(display != null) ad.getListView().setAdapter(new CustomArrayAdapter(this, display, textColor, isLang));
            Window w = ad.getWindow();

            Button positiveButton = ad.getButton(AlertDialog.BUTTON_POSITIVE);
            if(positiveButton != null) positiveButton.setTextColor(textColor);

            Button negativeButton = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
            if(negativeButton != null) negativeButton.setTextColor(textColor);

            Button neutralButton = ad.getButton(AlertDialog.BUTTON_NEUTRAL);
            if(neutralButton != null) neutralButton.setTextColor(textColor);

            if (w != null) {
                View dv = w.getDecorView();
                dv.getBackground().setColorFilter(new LightingColorFilter(0xFF000000, bgColor));
                w.setBackgroundDrawable(border);

                int padding = 16;
                dv.setPadding(padding, padding, padding, padding);
            }
        });
    }

    public static Resources rss;

    private void updateLang(Resources res, ScrollView settingsDialog) {
        rss = res;
        Button decodeButton = findViewById(R.id.decodeButton);
        decodeButton.setText(res.getString(R.string.merge));
        setButtonBorder(decodeButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setContentDescription(res.getString(R.string.settings));
        decodeButton.post(() -> {
            int buttonHeight = decodeButton.getHeight();
            int size = (int) (buttonHeight * 0.75);
            ViewGroup.LayoutParams params = settingsButton.getLayoutParams();
            params.height = size;
            params.width = size;
            settingsButton.setLayoutParams(params);
        });
        ((LinearLayout) findViewById(R.id.topButtons)).setGravity(Gravity.CENTER_VERTICAL);

        if(settingsDialog != null) {
            ((TextView) settingsDialog.findViewById(R.id.langPicker)).setText(res.getString(R.string.lang));
            final boolean supportsSwitch = Build.VERSION.SDK_INT > 13;
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.logToggle : R.id.logToggleText)).setText(res.getString(R.string.enable_logs));
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.ask : R.id.askText)).setText(res.getString(R.string.ask));
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.showDialogToggle : R.id.showDialogToggleText)).setText(res.getString(R.string.show_dialog));
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.signToggle : R.id.signToggleText)).setText(res.getString(R.string.sign_apk));
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.selectSplitsForDeviceToggle : R.id.selectSplitsForDeviceToggleText)).setText(res.getString(R.string.automatically_select));
            ((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.updateToggle : R.id.updateToggleText)).setText(res.getString(R.string.auto_update));
            //((TextView) settingsDialog.findViewById(supportsSwitch ? R.id.revancedToggle : R.id.revancedText)).setText(res.getString(R.string.fix));
            ((TextView) settingsDialog.findViewById(R.id.changeTextColor)).setText(res.getString(R.string.change_text_color));
            ((TextView) settingsDialog.findViewById(R.id.changeBgColor)).setText(res.getString(R.string.change_background_color));
            ((TextView) settingsDialog.findViewById(R.id.checkUpdateNow)).setText(res.getString(R.string.check_update_now));
        }
    }

    private void showColorPickerDialog(boolean isTextColor, int currentColor, ScrollView from) {
        new AmbilWarnaDialog(this, currentColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog1, int color) {
                setColor(color, isTextColor, from);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog1) {
                // cancel was selected by the user
            }
        }).show();
    }

    final File getAntisplitMFolder() {
        final File antisplitMFolder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        return antisplitMFolder.exists() || antisplitMFolder.mkdir() ? antisplitMFolder : new File(Environment.getExternalStorageDirectory(), "Download");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkStoragePerm() {
        if(doesNotHaveStoragePerm(this)) {
            Toast.makeText(this, rss.getString(R.string.grant_storage), Toast.LENGTH_LONG).show();
            if(LegacyUtils.supportsWriteExternalStorage) requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            else startActivityForResult(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    public static boolean doesNotHaveStoragePerm(Context context) {
        if (Build.VERSION.SDK_INT < 23) return false;
        return LegacyUtils.supportsWriteExternalStorage ?
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED :
            !Environment.isExternalStorageManager();
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor e = getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                .putBoolean("logEnabled", logEnabled)
                .putBoolean("ask", ask)
                .putBoolean("showDialog", showDialog)
                .putBoolean("signApk", signApk)
                .putBoolean("selectSplitsForDevice", selectSplitsForDevice)
                //.putBoolean("revanced", revanced)
                .putBoolean("checkForUpdates", checkForUpdates)
                .putInt("textColor", textColor)
                .putInt("backgroundColor", bgColor)
                .putString("lang", lang);
        if (supportsArraysCopyOfAndDownloadManager) e.apply();
        else e.commit();
        super.onPause();
    }

    @Override
    public void onLog(String msg) {
        runOnUiThread(() -> {
            ((TextView)findViewById(R.id.logField)).append(msg + '\n');
            ScrollView scrollView = findViewById(R.id.scrollView);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onLog(int resID) {
        onLog(getString(resID));
    }

    private Handler handler;

    /** @noinspection ResultOfMethodCallIgnored, DataFlowIssue */
    public static void deleteDir(File dir) {
        // There should never be folders in here.
        for (String child : dir.list()) new File(dir, child).delete();
    }

    @Override
    protected void onDestroy() {
        File dir = getCacheDir();
        deleteDir(dir);
        if (LegacyUtils.supportsExternalCacheDir && (dir = getExternalCacheDir()) != null) deleteDir(dir);
        super.onDestroy();
    }

    private List<String> splitsToUse = null;
    private static class ProcessTask extends AsyncTask<Uri, Void, Void> {
        private final WeakReference<MainActivity> activityReference;
        private final DeviceSpecsUtil DeviceSpecsUtil;

        // only retain a weak reference to the activity
        ProcessTask(MainActivity context, com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil deviceSpecsUtil) {
            activityReference = new WeakReference<>(context);
            DeviceSpecsUtil = deviceSpecsUtil;
            toggleAnimation(context, true);
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            if (activity == null) return null;

            final File cacheDir = LegacyUtils.supportsExternalCacheDir ? activity.getExternalCacheDir() : activity.getCacheDir();
                if (cacheDir != null && activity.urisAreSplitApks) deleteDir(cacheDir);

                List<String> splits = activity.splitsToUse;
                try {
                    if (activity.urisAreSplitApks) {
                        if (selectSplitsForDevice) {

                            splits = DeviceSpecsUtil.getListOfSplits(activity.splitAPKUri);
                            if (splits.size() == 4 || splits.size() == 3) splits.clear();
                            else {
                                List<String> copy = List.copyOf(splits);
                                List<String> toRemove = new ArrayList<>();
                                boolean splitApkContainsArch = false;
                                for (int i = 0; i < splits.size(); i++) {
                                    final String thisSplit = splits.get(i);
                                    if (!splitApkContainsArch && com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit)) {
                                        splitApkContainsArch = true;
                                    }
                                    if (DeviceSpecsUtil.shouldIncludeSplit(thisSplit))
                                        toRemove.add(thisSplit);
                                }
                                if (splitApkContainsArch) {
                                    boolean selectedSplitsContainsArch = false;
                                    for (int i = 0; i < copy.size(); i++) {
                                        final String thisSplit = copy.get(i);
                                        if (com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit) && toRemove.contains(thisSplit)) {
                                            selectedSplitsContainsArch = true;
                                            break;
                                        }
                                    }
                                    if (!selectedSplitsContainsArch) {
                                        LogUtil.logMessage("Could not find device architecture, selecting all architectures");
                                        for (int i = 0; i < splits.size(); i++) {
                                            final String thisSplit = splits.get(i);
                                            if (com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit))
                                                toRemove.add(thisSplit); // select all to be sure
                                        }
                                    }
                                }

                                boolean didNotFindDpi = true;
                                for (int i = 0; i < copy.size(); i++) {
                                    String thisSplit = copy.get(i);
                                    if (thisSplit.contains("dpi") && toRemove.contains(thisSplit)) {
                                        didNotFindDpi = false;
                                        break;
                                    }
                                }
                                if (didNotFindDpi) {
                                    for (int i = 0; i < splits.size(); i++) {
                                        String thisSplit = splits.get(i);
                                        if (thisSplit.contains("hdpi")) toRemove.add(thisSplit);
                                    }
                                }
                                //if((toRemove.size() == 3 && !toRemove.contains(lang)) || toRemove.size() == 4);
                                splits.removeAll(toRemove);
                            }
                        }
                    } else {
                        // These are the splits from inside the APK, just copy the splits to cache folder
                        for (Uri uri : activity.uris) {
                            try (InputStream is = FileUtils.getInputStream(uri, activity)) {
                                FileUtils.copyFile(is, new File(cacheDir, getOriginalFileName(activity, uri)));
                            }
                        }
                    }
                    Merger.run(
                            activity.urisAreSplitApks ? activity.splitAPKUri : null,
                            cacheDir,
                            uris[0],
                            activity,
                            splits,
                            signApk);
                } catch (Exception e) {
                    activity.showError(e);
                }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MainActivity activity = activityReference.get();
            toggleAnimation(activity, false);
            if(activity.urisAreSplitApks) activity.getHandler().post(() -> {
                try {
                    activity.uris.remove(0);
                    activity.splitAPKUri = activity.uris.get(0);
                    if(showDialog) activity.showApkSelectionDialog();
                    else activity.selectDirToSaveAPKOrSaveNow();
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    // End of list, I don't know why but isEmpty is not working
                    activity.showSuccess();
                }
            });
            else activity.showSuccess();
        }
    }

    public static void toggleAnimation(MainActivity context, boolean on) {
        ImageView loadingImage = context.findViewById(R.id.loadingImage);
        context.runOnUiThread(() -> {
            if(on) {
                ((LinearLayout) context.findViewById(R.id.wrapImg)).setGravity(Gravity.CENTER);
                loadingImage.setVisibility(View.VISIBLE);
                loadingImage.startAnimation(AnimationUtils.loadAnimation(context, R.anim.loading));
            }
            else {
                loadingImage.setVisibility(View.GONE);
                loadingImage.clearAnimation();
            }
        });
    }

    private void processOneSplitApkUri(Uri uri) {
        splitAPKUri = uri;
        if (showDialog) showApkSelectionDialog();
        else selectDirToSaveAPKOrSaveNow();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) switch (requestCode) {
            case 0:
                checkStoragePerm();
                break;
            case 1:
                // opened through button in the app

                ClipData clipData = data.getClipData();
                if (clipData == null) processOneSplitApkUri(data.getData());
                else {
                    //multiple files selected
                    Uri first = clipData.getItemAt(0).getUri();
                    String path = first.getPath();
                    if (path != null && path.endsWith(".apk")) {
                        urisAreSplitApks = false;
                        uris = new ArrayList<>();
                        uris.add(first);
                    } else processOneSplitApkUri(first);

                    for (int i = 1; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        if (urisAreSplitApks) processOneSplitApkUri(uri);
                        else uris.add(uri);
                    }
                    if (!urisAreSplitApks) selectDirToSaveAPKOrSaveNow();
                }
                break;
            case 2:
                // going to process and save a file now
                ((TextView) findViewById(R.id.logField)).setText("");
                process(data.getData());
                break;
        }
    }

    private void process(Uri outputUri) {
        ProcessTask processTask = new ProcessTask(this, DeviceSpecsUtil);
        processTask.execute(outputUri);

        ImageView cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setColorFilter(new LightingColorFilter(0xFF000000, textColor));
        ViewSwitcher vs = findViewById(R.id.viewSwitcher);
        vs.setVisibility(View.VISIBLE);
        if(Objects.equals(vs.getCurrentView(), findViewById(R.id.copyLog))) vs.showNext();

        cancelButton.setOnClickListener(v -> {
            if(supportsActionBar) {
                Context ctx = getApplicationContext();
                PackageManager pm = ctx.getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage(ctx.getPackageName());
                Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
                ctx.startActivity(mainIntent);
                Runtime.getRuntime().exit(0);
            } else {
                processTask.cancel(true);
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            //viewSwitcher.setVisibility(View.GONE);
        });
    }

    private static class CheckForUpdatesTask extends AsyncTask<Void, Void, String[]> {
        private final WeakReference<MainActivity> context;
        private final boolean toast;

        CheckForUpdatesTask(MainActivity context, boolean toast) {
            this.context = new WeakReference<>(context);
            this.toast = toast;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) new URL("https://api.github.com/repos/AbdurazaaqMohammed/AntiSplit-M/releases").openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    String ver = "";
                    String changelog = "";
                    String dl = "";
                    while ((line = reader.readLine()) != null) {
                        if(line.contains("browser_download_url")) {
                            dl = line.split("\"")[3];
                            ver = line.split("/")[7];
                        } else if(line.contains("body")) {
                            changelog = line.split("\"")[3];
                            break;
                        }
                    }

                    return new String[]{ver, changelog, dl};
                }
            } catch (Exception e) {
                return null;
            }
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        @Override
        protected void onPostExecute(String[] result) {
            MainActivity activity = context.get();
            if(result == null) {
                if (toast) activity.runOnUiThread(() -> Toast.makeText(activity, "Failed to check for update", Toast.LENGTH_SHORT).show());
            } else try {
                String latestVersion = result[0];
                String currentVer;
                try {
                    currentVer = ((Context) activity).getPackageManager().getPackageInfo(((Context) activity).getPackageName(), 0).versionName;
                } catch (Exception e) {
                    currentVer = "1.6.6";
                }
                boolean newVer = false;
                char[] curr = TextUtils.isEmpty(currentVer) ? new char[] {1, 6, 6} : currentVer.replace(".", "").toCharArray();
                char[] latest = latestVersion.replace(".", "").toCharArray();
                for(int i = 0; i < curr.length; i++) {
                    if(latest[i] > curr[i]) {
                        newVer = true;
                        break;
                    }
                }

                if(newVer) {
                    String ending = ".apk";
                    String filename = "AntiSplit-M.v" + latestVersion + ending;
                    String link = result[2].endsWith(ending) ? result[2] : result[2] + File.separator + filename;
                    TextView changelogText = new TextView(activity);
                    String linebreak = "<br />";
                    changelogText.setText(Html.fromHtml(rss.getString(R.string.new_ver) + " (" + latestVersion  + ")" + linebreak + "Changelog:" + linebreak + result[1].replace("\\r\\n", linebreak)));
                    changelogText.setTextColor(textColor);
                    TextView title = new TextView(activity);
                    title.setText(rss.getString(R.string.update));
                    title.setTextColor(textColor);
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity).setCustomTitle(title).setView(changelogText).setPositiveButton(rss.getString(R.string.dl), (dialog, which) -> {
                        if (supportsArraysCopyOfAndDownloadManager) {
                            DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link))
                            .setTitle(filename).setDescription(filename).setMimeType("application/vnd.android.package-archive");
                            if (Build.VERSION.SDK_INT < 29) activity.checkStoragePerm();
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                            if (LegacyUtils.supportsActionBar) request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            downloadManager.enqueue(request);
                        } else activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(link)));
                    });
                    if(supportsArraysCopyOfAndDownloadManager) builder.setNeutralButton("Go to GitHub Release", (dialog, which) -> activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/AbdurazaaqMohammed/AntiSplit-M/releases/latest"))));
                    activity.styleAlertDialog(builder.setNegativeButton(rss.getString(R.string.cancel), (dialog, which) -> dialog.dismiss()).create(),
                            null, false);
                } else if (toast) activity.runOnUiThread(() -> Toast.makeText(activity, rss.getString(R.string.no_update_found), Toast.LENGTH_SHORT).show());
            } catch (Exception ignored) {
                if (toast) activity.runOnUiThread(() -> Toast.makeText(activity, "Failed to check for update", Toast.LENGTH_SHORT).show());
            }
        }
    }

    public void showApkSelectionDialog() {
        try {
            List<String> splits = DeviceSpecsUtil.getListOfSplits(splitAPKUri);
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

            TextView title = new TextView(this);
            title.setTextColor(textColor);
            title.setTextSize(20);
            title.setText(rss.getString(R.string.select_splits));
            title.setPadding(15,15,15,15);

            styleAlertDialog(new AlertDialog.Builder(this).setCustomTitle(title).setMultiChoiceItems(apkNames, checkedItems, (dialog, which, isChecked) -> {
                switch (which) {
                    case 0:
                        // "Select All" option
                        for (int i = 5; i < checkedItems.length; i++) ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        break;
                    case 1:
                        // device specs option
                        for (int i = 5; i < checkedItems.length; i++) {
                            ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = (isChecked && DeviceSpecsUtil.shouldIncludeSplit(apkNames[i])));
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
                                    ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                                    break;
                                }
                            }
                        }
                        break;
                    case 2:
                        //arch for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if(DeviceSpecsUtil.shouldIncludeArch(apkNames[i])) ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        }
                    break;
                    case 3:
                        //dpi for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if(DeviceSpecsUtil.shouldIncludeDpi(apkNames[i])) ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
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
                                    ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                                    break;
                                }
                            }
                        }
                    break;
                    case 4:
                        //lang for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if(DeviceSpecsUtil.shouldIncludeLang(apkNames[i])) ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        }
                    break;
                    default:
                        ListView listView = ((AlertDialog) dialog).getListView();
                        if (!isChecked) listView.setItemChecked(0, checkedItems[0] = false); // Uncheck "Select All" if any individual item is unchecked
                        for (int i = 1; i <= 4; i++) {
                            if (checkedItems[i] && !DeviceSpecsUtil.shouldIncludeSplit(apkNames[which])) {
                                listView.setItemChecked(i, checkedItems[i] = false); // uncheck device arch if non device arch selected
                            }
                        }
                        break;
                }
            }).setPositiveButton("OK", (dialog, which) -> {
                for (int i = 1; i < checkedItems.length; i++) {
                    if (checkedItems[i]) splits.remove(apkNames[i]); // ?????
                }

                if (splits.size() == initialSize) {
                    urisAreSplitApks = true; // reset
                    showError(rss.getString(R.string.nothing));
                } else {
                    splitsToUse = splits;
                    selectDirToSaveAPKOrSaveNow();
                }
            }).setNegativeButton("Cancel", null).create(), apkNames, false);
        } catch (IOException e) {
            showError(e);
        }
    }

    private void showSuccess() {
        if(!errorOccurred) {
            final String success = rss.getString(R.string.success_saved);
            LogUtil.logMessage(success);
            runOnUiThread(() -> Toast.makeText(this, success, Toast.LENGTH_SHORT).show());
        }

        ViewSwitcher vs = findViewById(R.id.viewSwitcher);
        vs.setVisibility(View.VISIBLE);
        if(Objects.equals(vs.getCurrentView(), findViewById(R.id.cancelButton))) vs.showNext();
        ImageView b = findViewById(R.id.copyLog);
        b.setColorFilter(new LightingColorFilter(0xFF000000, textColor));
        b.setOnClickListener(v -> copyText(new StringBuilder().append(((TextView) findViewById(R.id.logField)).getText()).append('\n').append(((TextView) findViewById(R.id.errorField)).getText())));
    }

    private void copyText(CharSequence text) {
        if(LegacyUtils.supportsActionBar) ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("log", text));
        else ((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(text);
        Toast.makeText(this, rss.getString(R.string.copied_log), Toast.LENGTH_SHORT).show();
    }

    private void showError(Exception e) {
        if(!(e instanceof ClosedByInterruptException)) {
            final String mainErr = e.toString();
            errorOccurred = !mainErr.equals(rss.getString(R.string.sign_failed));
            toggleAnimation(this, false);
            StringBuilder stackTrace = new StringBuilder().append(mainErr).append('\n');
            for(StackTraceElement line : e.getStackTrace()) stackTrace.append(line).append('\n');
            StringBuilder fullLog = new StringBuilder(stackTrace.toString());
            fullLog.append('\n').append(((TextView) findViewById(R.id.logField)).getText());
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setNegativeButton(rss.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(rss.getString(R.string.create_issue), (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AbdurazaaqMohammed/AntiSplit-M/issues/new?title=Crash%20Report&body=" + fullLog))))
                    .setNeutralButton(rss.getString(R.string.copy_log), (dialog, which) -> copyText(fullLog));
            runOnUiThread(() -> {
                TextView title = new TextView( this);
                title.setText(mainErr);
                title.setTextColor(textColor);
                title.setTextSize(20);

                TextView msg = new TextView(this);
                msg.setText(stackTrace);
                msg.setTextColor(textColor);
                ScrollView sv = new ScrollView(this);
                sv.setBackgroundColor(bgColor);
                msg.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (rss.getDisplayMetrics().heightPixels * 0.6)));
                sv.addView(msg);
                styleAlertDialog(b.setCustomTitle(title).setView(sv).create(), null, false);
            /*TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(stackTrace);
            Toast.makeText(this, mainErr, Toast.LENGTH_SHORT).show();*/
            });
        }
    }

    private void showError(String err) {
        toggleAnimation(this, false);
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(err);
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    public static String getOriginalFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = Objects.requireNonNull(result).lastIndexOf('/'); // Ensure it throw the NullPointerException here to be caught
                if (cut != -1) result = result.substring(cut + 1);
            }
            return result.replaceFirst("\\.(?:xapk|aspk|apk[sm])", "_antisplit.apk");
        } catch (Exception ignored) {
            return "filename_not_found";
        }
    }

    @SuppressLint("InlinedApi")
    private void selectDirToSaveAPKOrSaveNow() {
        if (ask) startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_TITLE, urisAreSplitApks ? getOriginalFileName(this, splitAPKUri) : getNameFromNonSplitApks()), 2);
        else {
            checkStoragePerm();
            try {
                String originalFilePath;
                if(urisAreSplitApks) originalFilePath = FileUtils.getPath(splitAPKUri, this);
                else {
                    String path = FileUtils.getPath(uris.get(0), this);
                    originalFilePath = (TextUtils.isEmpty(path) ? getAntisplitMFolder() : path.substring(0, path.lastIndexOf(File.separator))) + File.separator + getNameFromNonSplitApks();
                }
                File f;
                String newFilePath = TextUtils.isEmpty(originalFilePath) ?
                        getAntisplitMFolder() + File.separator + getOriginalFileName(this, splitAPKUri) // If originalFilePath is null urisAreSplitApks must be true because getNameFromNonSplitApks will always return something
                        : originalFilePath.replaceFirst("\\.(?:xapk|aspk|apk[sm])", "_antisplit.apk");
                if(TextUtils.isEmpty(newFilePath) ||
                        newFilePath.startsWith("/data/")
                       // || !(f = new File(newFilePath)).createNewFile() || f.canWrite()
                        ) {
                    f = new File(getAntisplitMFolder(), newFilePath.substring(newFilePath.lastIndexOf(File.separator) + 1));
                    showError(rss.getString(R.string.no_filepath) + newFilePath);
                } else f = new File(newFilePath);
                ((TextView) findViewById(R.id.logField)).setText("");
                LogUtil.logMessage(rss.getString(R.string.output) + f);

                process(Uri.fromFile(f));
            } catch (IOException e) {
                showError(e);
            }
        }
    }

    private String getNameFromNonSplitApks() {
        String realName = null;
        for(Uri uri : uris) {
            final String fileName = getOriginalFileName(this, uri);
            if (fileName.equals("base.apk")) {
                try {
                    realName = Objects.requireNonNull(getPackageManager().getPackageArchiveInfo(Objects.requireNonNull(FileUtils.getPath(uri, this)), 0)).packageName;
                    break;
                } catch (NullPointerException | IOException ignored) {}
            } else if (!fileName.startsWith("config") && !fileName.startsWith("split")) {
                // this should hopefully be base.apk renamed to the package name
                realName = fileName;
                break;
            }
        }
        return (TextUtils.isEmpty(realName) ? "unknown" : realName.replace(".apk", "")) + "_antisplit.apk";
    }
}