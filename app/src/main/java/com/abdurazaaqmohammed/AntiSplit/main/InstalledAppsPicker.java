package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.BackgroundTask;
import com.abdurazaaqmohammed.utils.CompareUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InstalledAppsPicker {

    public interface Listener {
        void onAppSelected(String packageName);
    }

    private final MainActivity activity;
    private final AppSettings settings;
    private final Listener listener;

    private List<AppInfo> apps;

    public InstalledAppsPicker(MainActivity activity, AppSettings settings, Listener listener) {
        this.activity = activity;
        this.settings = settings;
        this.listener = listener;
    }

    public void show() {
        activity.clearLog();
        activity.setProgressVisible(true);
        BackgroundTask.submit(() -> {
            final List<AppInfo> loadedApps = loadApps();
            activity.getHandler().post(() -> {
                activity.setProgressVisible(false);
                apps = loadedApps;
                showDialog();
            });
        }, activity.getHandler(), null, error -> {
            activity.setProgressVisible(false);
            activity.reportError(error);
        });
    }

    private List<AppInfo> loadApps() {
        List<AppInfo> installedApps = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return installedApps;

        PackageManager packageManager = activity.getPackageManager();
        for (PackageInfo packageInfo : packageManager.getInstalledPackages(0)) {
            try {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (appInfo == null)
                    appInfo = packageManager.getApplicationInfo(packageInfo.packageName, 0);
                if (appInfo.splitSourceDirs != null && appInfo.splitSourceDirs.length > 0)
                    installedApps.add(new AppInfo(
                            String.valueOf(packageManager.getApplicationLabel(appInfo)),
                            packageInfo.packageName,
                            packageInfo.lastUpdateTime,
                            packageInfo.firstInstallTime));
            } catch (Exception ignored) {
            }
        }
        sort(installedApps);
        return installedApps;
    }

    private void sort(List<AppInfo> appList) {
        if (settings.sortMode == 0) {
            Collections.sort(appList, CompareUtils::compareAppInfoByName);
            return;
        }
        final boolean byUpdateTime = settings.sortMode == 1;
        Collections.sort(appList, (first, second) -> Long.compare(
                byUpdateTime ? second.lastUpdated : second.firstInstall,
                byUpdateTime ? first.lastUpdated : first.firstInstall));
    }

    private void showDialog() {
        if (activity.isFinishing() || activity.isDestroyed())
            return;

        final AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.select_from_installed_apps)
                .setNegativeButton(R.string.cancel, null)
                .create();

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_search, null);
        ListView listView = content.findViewById(R.id.list_view);
        final AppListArrayAdapter adapter =
                new AppListArrayAdapter(activity, apps, true);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            listener.onAppSelected(adapter.getItem(position).packageName);
        });

        EditText searchBar = content.findViewById(R.id.search_bar);
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

        content.findViewById(R.id.filter_button).setOnClickListener(anchor ->
                showSortMenu(anchor, adapter));

        dialog.setView(content);
        DialogStyler.show(activity, dialog);
    }

    private void showSortMenu(View anchor, AppListArrayAdapter adapter) {
        PopupMenu popupMenu = new PopupMenu(activity, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort_name)
                settings.sortMode = 0;
            else if (itemId == R.id.date_updated)
                settings.sortMode = 1;
            else
                settings.sortMode = 2;
            sort(apps);
            adapter.setItems(apps);
            return true;
        });
        popupMenu.show();
    }
}
