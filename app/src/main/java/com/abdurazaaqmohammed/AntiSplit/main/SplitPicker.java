package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.BackgroundTask;
import com.abdurazaaqmohammed.utils.CompareUtils;
import com.abdurazaaqmohammed.utils.DeviceSpecsUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SplitPicker {

    public interface Listener {
        void onSplitsSelected(Uri archiveUri, List<String> excludedSplits);
    }

    private static final int HEADER_COUNT = 5;
    private static final int INDEX_SELECT_ALL = 0;
    private static final int INDEX_DEVICE_ALL = 1;
    private static final int INDEX_DEVICE_ARCHITECTURE = 2;
    private static final int INDEX_DEVICE_DENSITY = 3;
    private static final int INDEX_DEVICE_LANGUAGE = 4;

    private final MainActivity activity;
    private final DeviceSpecsUtil deviceSpecs;
    private final MyAPKLogger logger;
    private final Resources resources;
    private final Uri archiveUri;
    private final Listener listener;

    private String[] itemNames;
    private boolean[] checkedItems;
    private int splitCount;

    public SplitPicker(MainActivity activity, DeviceSpecsUtil deviceSpecs, MyAPKLogger logger,
            Uri archiveUri, Listener listener) {
        this.activity = activity;
        this.deviceSpecs = deviceSpecs;
        this.logger = logger;
        this.resources = activity.getResources();
        this.archiveUri = archiveUri;
        this.listener = listener;
    }

    public void show() {
        activity.setProgressVisible(true);
        logger.logMessage(R.string.searching);
        BackgroundTask.submit(this::loadSplits, activity.getHandler(),
                this::showDialog, error -> {
                    activity.setProgressVisible(false);
                    activity.reportError(error);
                });
    }

    private void loadSplits() throws Exception {
        List<String> splits = deviceSpecs.getListOfSplits(archiveUri);
        Collections.sort(splits, CompareUtils::compareByName);
        splitCount = splits.size();

        itemNames = new String[HEADER_COUNT + splitCount];
        checkedItems = new boolean[HEADER_COUNT + splitCount];
        itemNames[INDEX_SELECT_ALL] = resources.getString(R.string.all);
        itemNames[INDEX_DEVICE_ALL] = resources.getString(R.string.for_device);
        itemNames[INDEX_DEVICE_ARCHITECTURE] = resources.getString(R.string.arch_for_device);
        itemNames[INDEX_DEVICE_DENSITY] = resources.getString(R.string.dpi_for_device);
        itemNames[INDEX_DEVICE_LANGUAGE] = resources.getString(R.string.lang_for_device);
        for (int i = 0; i < splitCount; i++)
            itemNames[HEADER_COUNT + i] = splits.get(i);
    }

    private void showDialog() {
        if (activity.isFinishing() || activity.isDestroyed())
            return;

        activity.setProgressVisible(false);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.select_splits)
                .setMultiChoiceItems(itemNames, checkedItems, this::onItemChecked)
                .setPositiveButton(android.R.string.ok, (d, which) -> onConfirmed())
                .setNegativeButton(R.string.cancel, null)
                .create();
        DialogStyler.show(activity, dialog);
        if (dialog.isShowing())
            checkBaseApk(dialog);
    }

    private void onItemChecked(DialogInterface dialog, int which, boolean isChecked) {
        ListView listView = ((AlertDialog) dialog).getListView();
        if (which == INDEX_SELECT_ALL) {
            setAllSplitsChecked(listView, isChecked);
            return;
        }
        if (which == INDEX_DEVICE_ALL) {
            selectForDevice(listView, isChecked);
            return;
        }
        if (which == INDEX_DEVICE_ARCHITECTURE) {
            applySelection(listView, isChecked, SelectionType.ARCHITECTURE);
            return;
        }
        if (which == INDEX_DEVICE_DENSITY) {
            applySelection(listView, isChecked, SelectionType.DENSITY);
            ensureDensitySelected(listView);
            return;
        }
        if (which == INDEX_DEVICE_LANGUAGE) {
            applySelection(listView, isChecked, SelectionType.LANGUAGE);
            return;
        }

        checkedItems[which] = isChecked;
        if (!isChecked)
            listView.setItemChecked(INDEX_SELECT_ALL, checkedItems[INDEX_SELECT_ALL] = false);
        else
            listView.setItemChecked(INDEX_SELECT_ALL, checkedItems[INDEX_SELECT_ALL] = allSplitsChecked());
    }

    private void setAllSplitsChecked(ListView listView, boolean isChecked) {
        for (int i = HEADER_COUNT; i < checkedItems.length; i++)
            listView.setItemChecked(i, checkedItems[i] = isChecked);
    }

    private void selectForDevice(ListView listView, boolean isChecked) {
        for (int i = HEADER_COUNT; i < checkedItems.length; i++)
            listView.setItemChecked(i,
                    checkedItems[i] = isChecked && deviceSpecs.shouldIncludeSplit(itemNames[i]));
        if (isChecked)
            ensureDensitySelected(listView);
    }

    private enum SelectionType {
        ARCHITECTURE,
        DENSITY,
        LANGUAGE
    }

    private void applySelection(ListView listView, boolean isChecked, SelectionType type) {
        for (int i = HEADER_COUNT; i < checkedItems.length; i++) {
            if (!matches(type, itemNames[i]))
                continue;
            listView.setItemChecked(i, checkedItems[i] = isChecked);
        }
        if (isChecked)
            syncSelectAll(listView);
    }

    private boolean matches(SelectionType type, String itemName) {
        if (type == SelectionType.ARCHITECTURE)
            return deviceSpecs.shouldIncludeArchitecture(itemName);
        if (type == SelectionType.DENSITY)
            return deviceSpecs.shouldIncludeDensity(itemName);
        return deviceSpecs.shouldIncludeLanguage(itemName);
    }

    private void ensureDensitySelected(ListView listView) {
        for (int i = HEADER_COUNT; i < checkedItems.length; i++) {
            if (checkedItems[i] && DeviceSpecsUtil.isDensity(itemNames[i]))
                return;
        }
        for (int i = HEADER_COUNT; i < checkedItems.length; i++) {
            if (DeviceSpecsUtil.hasDensity(itemNames[i], "hdpi")) {
                listView.setItemChecked(i, checkedItems[i] = true);
                return;
            }
        }
    }

    private void syncSelectAll(ListView listView) {
        listView.setItemChecked(INDEX_SELECT_ALL, checkedItems[INDEX_SELECT_ALL] = allSplitsChecked());
    }

    private boolean allSplitsChecked() {
        for (int i = HEADER_COUNT; i < checkedItems.length; i++) {
            if (!checkedItems[i])
                return false;
        }
        return splitCount > 0;
    }

    private void checkBaseApk(AlertDialog dialog) {
        ListView listView = dialog.getListView();
        for (int i = HEADER_COUNT; i < itemNames.length; i++) {
            if (DeviceSpecsUtil.isBaseApk(itemNames[i])) {
                listView.setItemChecked(i, checkedItems[i] = true);
                listView.setItemChecked(INDEX_SELECT_ALL,
                        checkedItems[INDEX_SELECT_ALL] = allSplitsChecked());
                return;
            }
        }
    }

    private void onConfirmed() {
        List<String> excludedSplits = new ArrayList<>();
        for (int i = HEADER_COUNT; i < checkedItems.length; i++) {
            if (!checkedItems[i])
                excludedSplits.add(itemNames[i]);
        }
        if (excludedSplits.size() == splitCount) {
            activity.reportError(R.string.nothing);
            return;
        }
        listener.onSplitsSelected(archiveUri, excludedSplits);
    }
}
