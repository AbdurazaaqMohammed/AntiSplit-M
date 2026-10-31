package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.FileUtils;
import com.abdurazaaqmohammed.utils.UpdateUtil;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

public final class SettingsSheet {

    private final MainActivity activity;
    private final AppSettings settings;
    private final Resources resources;

    public SettingsSheet(MainActivity activity, AppSettings settings) {
        this.activity = activity;
        this.settings = settings;
        this.resources = activity.getResources();
    }

    public void show() {
        ScrollView content = (ScrollView) activity.getLayoutInflater()
                .inflate(R.layout.settings_dialog, null);

        setupTheme(content);
        setupSuffix(content);
        setupSwitches(content);
        setupSaveMode(content);
        setupCompressionLevel(content);
        setupLanguage(content);
        content.findViewById(R.id.abt).setOnClickListener(v -> {
            View aboutView = LayoutInflater.from(activity).inflate(R.layout.dialog_about, null);

            TextView versionText = aboutView.findViewById(R.id.aboutVersion);
            try {
                versionText.setText(activity.getString(R.string.about_version, activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName));
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            aboutView.findViewById(R.id.aboutGithub).setOnClickListener(v2 -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AbdurazaaqMohammed/AntiSplit-M"))));
            aboutView.findViewById(R.id.donate).setOnClickListener(v2 -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/AbdurazaaqMohammed"))));

            List<AboutLibrary> libraries = Arrays.asList(
                    new AboutLibrary("APKEditor", "REAndroid", "https://github.com/REAndroid/APKEditor", "Apache-2.0", "Apache-2.0.txt"),
                    new AboutLibrary("apksig for Android", "Muntashir Al-Islam", "https://github.com/MuntashirAkon/apksig-android", "Apache-2.0", "Apache-2.0.txt"),
                    new AboutLibrary("aXML", "APK Explorer & Editor", "https://github.com/apk-editor/aXML", "GPL-3.0", "GPL-3.0+.txt"),
                    new AboutLibrary("Material Design Icons", "Google", "https://github.com/google/material-design-icons", "Apache-2.0", "Apache-2.0.txt"),
                    new AboutLibrary("AndroidX", "Google", "https://github.com/androidx/androidx", "Apache-2.0", "Apache-2.0.txt"),
                    new AboutLibrary("Material Components", "Google", "https://github.com/material-components/material-components-android", "Apache-2.0", "Apache-2.0.txt"),
                    new AboutLibrary("smali/baksmali", "JesusFreke and the Android Open Source Project", "https://github.com/JesusFreke/smali", "BSD-3-Clause", "BSD-3-Clause.txt")
            );

            LinearLayout libsContainer = aboutView.findViewById(R.id.aboutLibs);
            for (AboutLibrary lib : libraries) {
                View row = LayoutInflater.from(activity).inflate(R.layout.dialog_about_item, libsContainer, false);
                ((TextView) row.findViewById(R.id.aboutLibName)).setText(lib.name);
                ((TextView) row.findViewById(R.id.aboutLibAuthor)).setText(activity.getString(R.string.by_x, lib.author));
                TextView licenseText = row.findViewById(R.id.aboutLibLicense);
                licenseText.setText(lib.licenseName);
                if (licenseText.getBackground() instanceof GradientDrawable) {
                    ((GradientDrawable) licenseText.getBackground()).setColor(
                            MaterialColors.getColor(activity, com.google.android.material.R.attr.colorSecondaryContainer, Color.TRANSPARENT));
                }
                row.setOnClickListener(v2 -> showLibraryDialog(lib));
                libsContainer.addView(row);
            }

            new MaterialAlertDialogBuilder(activity)
                    .setPositiveButton(activity.getString(R.string.close), null)
                    .setView(aboutView)
                    .show();
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.settings)
                .setView(content)
                .setPositiveButton(R.string.close, (d, which) -> d.dismiss())
                .create();
        DialogStyler.show(activity, dialog);
    }

    private void showLibraryDialog(AboutLibrary lib) {
        StringBuilder message = new StringBuilder(lib.licenseName).append('\n');
        try (FileReader fr = new FileReader(FileUtils.copyFileFromAssetsAndGetFile(lib.licenseFile, activity));
                BufferedReader br = new BufferedReader(fr)) {
            String line;
            while((line = br.readLine()) != null) {
                message.append('\n').append(line);
            }
            //license.replaceFirst("[<\\[](?:yyyy|year)[]>]\\s+[\\[<]name of (?:author|copyright owner)[>\\]]", copyright[which])).
        } catch (Exception ignored) {
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(lib.name)
                .setMessage(message)
                .setPositiveButton(activity.getString(R.string.github), (dialog, which) -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lib.url))))
                .setNegativeButton(activity.getString(R.string.close), null)
                .show();
    }

    public static final class AboutLibrary {
        private final String name;
        private final String author;
        private final String url;
        private final String licenseName;
        private final String licenseFile;

        public AboutLibrary(String name, String author, String url, String licenseName,
                            String licenseFile) {
            this.name = name;
            this.author = author;
            this.url = url;
            this.licenseName = licenseName;
            this.licenseFile = licenseFile;
        }

        public String name() {
            return name;
        }

    }
    private void setupTheme(ScrollView content) {
        MaterialButtonToggleGroup themeButtons = content.findViewById(R.id.themeToggleGroup);
        themeButtons.check(settings.systemTheme ? R.id.systemThemeButton
                : settings.theme == R.style.Theme_MyApp_Light ? R.id.lightThemeButton
                : settings.theme == R.style.Theme_MyApp_Dark ? R.id.darkThemeButton
                : R.id.blackThemeButton);

        themeButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;
            boolean systemTheme = false;
            int theme;
            if (checkedId == R.id.lightThemeButton) {
                theme = R.style.Theme_MyApp_Light;
            } else if (checkedId == R.id.darkThemeButton) {
                theme = R.style.Theme_MyApp_Dark;
            } else if (checkedId == R.id.blackThemeButton) {
                theme = R.style.Theme_MyApp_Black;
            } else {
                systemTheme = true;
                theme = AppSettings.systemTheme(activity);
            }
            if (systemTheme == settings.systemTheme && theme == settings.theme)
                return;
            settings.systemTheme = systemTheme;
            settings.theme = theme;
            activity.recreateForSettingsChange(theme);
        });
    }

    private void setupSuffix(ScrollView content) {
        TextInputEditText suffixInput = content.findViewById(R.id.suffixInput);
        suffixInput.setText(settings.suffix);
        suffixInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                settings.suffix = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupSwitches(ScrollView content) {
        CompoundButton updateSwitch = content.findViewById(R.id.updateToggle);
        Button checkUpdateNow = content.findViewById(R.id.checkUpdateNow);
        updateSwitch.setChecked(settings.checkForUpdates);
        checkUpdateNow.setVisibility(settings.checkForUpdates ? View.GONE : View.VISIBLE);
        updateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.checkForUpdates = isChecked;
            checkUpdateNow.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
        checkUpdateNow.setOnClickListener(v -> UpdateUtil.checkForUpdates(true, activity));

        CompoundButton logSwitch = content.findViewById(R.id.logToggle);
        logSwitch.setChecked(settings.logEnabled);
        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                settings.logEnabled = isChecked);

        CompoundButton signSwitch = content.findViewById(R.id.signToggle);
        signSwitch.setChecked(settings.signApk);
        signSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                settings.signApk = isChecked);

        CompoundButton forceSwitch = content.findViewById(R.id.forceToggle);
        forceSwitch.setChecked(settings.force);
        forceSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                settings.force = isChecked);

        CompoundButton showDialogSwitch = content.findViewById(R.id.showDialogToggle);
        CompoundButton selectForDeviceSwitch =
                content.findViewById(R.id.selectSplitsForDeviceToggle);

        showDialogSwitch.setChecked(settings.showDialog);
        showDialogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.showDialog = isChecked;
            if (isChecked) {
                settings.selectSplitsForDevice = false;
                selectForDeviceSwitch.setChecked(false);
            }
        });

        selectForDeviceSwitch.setChecked(settings.selectSplitsForDevice);
        selectForDeviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.selectSplitsForDevice = isChecked;
            if (isChecked) {
                settings.showDialog = false;
                showDialogSwitch.setChecked(false);
            }
        });
    }

    private void setupSaveMode(ScrollView content) {
        AutoCompleteTextView saveModeView = content.findViewById(R.id.auto_complete_tv);
        String[] saveModes = {
                resources.getString(R.string.ask),
                resources.getString(R.string.same),
                resources.getString(R.string.pick_folder)
        };
        saveModeView.setText(saveModes[Math.min(settings.saveMode, saveModes.length - 1)], false);
        saveModeView.setAdapter(new ArrayAdapter<>(activity, R.layout.dropdownitem, saveModes));
        saveModeView.setOnItemClickListener((parent, view, position, id) -> {
            settings.saveMode = position;
            if (position != AppSettings.SAVE_ASK
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                activity.checkStoragePerm(MainActivity.REQUEST_STORAGE_PERMISSION);
            if (position == AppSettings.SAVE_TO_FOLDER)
                showOutputFolderDialog();
        });
    }

    private void showOutputFolderDialog() {
        TextInputLayout inputLayout = new TextInputLayout(activity);
        inputLayout.setHint(resources.getString(R.string.pick_folder));
        TextInputEditText folderInput = new TextInputEditText(inputLayout.getContext());
        folderInput.setText(settings.outputFolder);
        folderInput.setSingleLine(true);
        inputLayout.addView(folderInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.pick_folder)
                .setView(inputLayout)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    CharSequence folderText = folderInput.getText();
                    if (TextUtils.isEmpty(folderText)) {
                        activity.reportError(resources.getString(R.string.no_filepath) + folderText);
                        return;
                    }
                    String folderPath = folderText.toString().trim();
                    File folder = new File(folderPath);
                    if (folder.exists() || folder.mkdirs()) {
                        settings.outputFolder = folderPath;
                    } else {
                        activity.reportError(resources.getString(R.string.no_filepath) + folderText);
                        settings.saveMode = AppSettings.SAVE_ASK;
                    }
                })
                .create();
        DialogStyler.show(activity, dialog, false);
    }

    private void setupCompressionLevel(ScrollView content) {
        AutoCompleteTextView compressionView = content.findViewById(R.id.auto_complete_tv2);
        String[] levels = {
                resources.getString(R.string.default_c),
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        };
        int selectedIndex = Math.max(0,
                Math.min(settings.compressionLevel + 1, levels.length - 1));
        compressionView.setText(levels[selectedIndex], false);
        compressionView.setAdapter(new ArrayAdapter<>(activity, R.layout.dropdownitem, levels));
        compressionView.setOnItemClickListener(
                (parent, view, position, id) -> settings.compressionLevel = position - 1);
    }

    private void setupLanguage(ScrollView content) {
        content.findViewById(R.id.langPicker).setOnClickListener(v -> {
            final String[] languageCodes = resources.getStringArray(R.array.langs);
            String[] languageNames = resources.getStringArray(R.array.langs_display);
            int selectedIndex = -1;
            for (int i = 0; i < languageCodes.length; i++) {
                if (languageCodes[i].equals(settings.language)) {
                    selectedIndex = i;
                    break;
                }
            }

            AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.lang)
                    .setSingleChoiceItems(languageNames, selectedIndex, (d, which) -> {
                        settings.language = languageCodes[which];
                        d.dismiss();
                        activity.recreateForLanguageChange();
                    })
                    .create();
            DialogStyler.show(activity, dialog);
        });
    }
}
