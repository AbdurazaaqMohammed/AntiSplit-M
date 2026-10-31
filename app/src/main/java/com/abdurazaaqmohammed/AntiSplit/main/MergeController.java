package com.abdurazaaqmohammed.AntiSplit.main;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.abdurazaaqmohammed.utils.BackgroundTask;
import com.abdurazaaqmohammed.utils.DeviceSpecsUtil;
import com.abdurazaaqmohammed.utils.UriNames;
import com.reandroid.Merger;
import com.reandroid.apk.ApkBundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public final class MergeController {

    public interface Listener {
        void onMergeStarted();

        void onMerged(MergedApk apk);

        void onMergeFailed(Throwable error);

        void onBatchFinished();
    }

    public static final class MergedApk {
        public final File file;
        public final Uri installUri;
        public final String outputName;
        public final List<Uri> sourceUris;

        MergedApk(File file, Uri installUri, String outputName, List<Uri> sourceUris) {
            this.file = file;
            this.installUri = installUri;
            this.outputName = outputName;
            this.sourceUris = sourceUris;
        }
    }

    private enum Mode {
        ARCHIVE,
        APK_FILES,
        INSTALLED
    }

    private final Activity activity;
    private final AppSettings settings;
    private final DeviceSpecsUtil deviceSpecs;
    private final MyAPKLogger logger;
    private final OutputNaming naming;
    private final Handler handler;
    private final Listener listener;

    private volatile boolean running;
    private int workingDirectoryCount;
    private volatile Uri currentSource;

    public MergeController(Activity activity, AppSettings settings,
            DeviceSpecsUtil deviceSpecs, MyAPKLogger logger, OutputNaming naming,
            Handler handler, Listener listener) {
        this.activity = activity;
        this.settings = settings;
        this.deviceSpecs = deviceSpecs;
        this.logger = logger;
        this.naming = naming;
        this.handler = handler;
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public Uri getCurrentSource() {
        return currentSource;
    }

    public void mergeArchive(Uri archive, List<String> excludedSplits) {
        submit(Mode.ARCHIVE, Collections.singletonList(archive), excludedSplits, null);
    }

    public void mergeArchives(List<Uri> archives) {
        submit(Mode.ARCHIVE, archives, null, null);
    }

    public void mergeApkFiles(List<Uri> apkFiles) {
        submit(Mode.APK_FILES, apkFiles, null, null);
    }

    public void mergeInstalledApp(String packageName) {
        submit(Mode.INSTALLED, Collections.<Uri>emptyList(), null, packageName);
    }

    private void submit(final Mode mode, final List<Uri> sources,
            final List<String> excludedSplits, final String packageName) {
        if (running)
            return;
        running = true;
        currentSource = sources.isEmpty() ? null : sources.get(0);
        handler.post(listener::onMergeStarted);

        BackgroundTask.submit(
                () -> runBatch(mode, sources, excludedSplits, packageName),
                handler,
                listener::onBatchFinished,
                error -> {
                    listener.onMergeFailed(error);
                    listener.onBatchFinished();
                });
    }

    private void runBatch(Mode mode, List<Uri> sources, List<String> excludedSplits,
            String packageName) throws Exception {
        try {
            if (mode == Mode.INSTALLED)
                mergeInstalledAppInternal(packageName);
            else if (mode == Mode.APK_FILES)
                mergeApkFilesInternal(sources);
            else
                for (Uri archive : sources) {
                    currentSource = archive;
                    mergeArchiveInternal(archive, excludedSplits);
                }
        } finally {
            running = false;
        }
    }

    private void mergeArchiveInternal(Uri archive, List<String> excludedSplits) throws Exception {
        List<String> splitsToExclude = excludedSplits;
        if (splitsToExclude == null && settings.selectSplitsForDevice)
            splitsToExclude = deviceSpecs.getSplitsForDevice(archive);

        Merger merger = new Merger(newWorkingDirectory(), activity,
                activity.getResources(), logger, settings.compressionLevel);
        File merged = merger.run(archive, splitsToExclude, settings.signApk, settings.force);
        if (merged == null)
            return;
        postMerged(new MergedApk(merged, merger.getSignedApkUri(),
                naming.forArchive(archive), Collections.singletonList(archive)));
    }

    private void mergeApkFilesInternal(List<Uri> apkFiles) throws Exception {
        if (apkFiles == null || apkFiles.isEmpty())
            throw new IOException("No APK files selected");

        File workingDirectory = newWorkingDirectory();
        copyApkFiles(apkFiles, workingDirectory);

        Merger merger = new Merger(workingDirectory, activity,
                activity.getResources(), logger, settings.compressionLevel);
        File merged = merger.run(null, null, settings.signApk, settings.force);
        if (merged == null)
            return;
        postMerged(new MergedApk(merged, merger.getSignedApkUri(),
                naming.forApkFiles(apkFiles), apkFiles));
    }

    private void mergeInstalledAppInternal(String packageName) throws Exception {
        if (TextUtils.isEmpty(packageName))
            throw new IOException("No package selected");

        PackageInfo info = activity.getPackageManager().getPackageInfo(packageName, 0);
        File sourceDirectory = new File(info.applicationInfo.sourceDir).getParentFile();
        if (sourceDirectory == null)
            throw new IOException("Unable to locate installed package files");

        Merger merger = new Merger(newWorkingDirectory(), activity,
                activity.getResources(), logger, settings.compressionLevel);
        try (ApkBundle bundle = new ApkBundle(settings.compressionLevel)) {
            bundle.loadApkDirectory(sourceDirectory);
            File merged = merger.run(bundle, settings.signApk, settings.force);
            if (merged == null)
                return;
            postMerged(new MergedApk(merged, merger.getSignedApkUri(),
                    naming.forInstalledApp(packageName), Collections.<Uri>emptyList()));
        }
    }

    private void copyApkFiles(List<Uri> apkFiles, File directory) throws IOException {
        for (int i = 0; i < apkFiles.size(); i++) {
            Uri uri = apkFiles.get(i);
            String fileName = UriNames.cacheName(activity, uri, "split_" + i + ".apk");
            File target = new File(directory, fileName);
            if (target.exists())
                target = new File(directory, i + "_" + fileName);
            try (InputStream input = com.starry.FileUtils.getInputStream(uri, activity)) {
                if (input == null)
                    throw new IOException("Unable to open " + uri);
                com.abdurazaaqmohammed.utils.FileUtils.copyFile(input, target);
            }
        }
    }

    private File newWorkingDirectory() throws IOException {
        File directory = new File(activity.getCacheDir(),
                "merge_" + System.currentTimeMillis() + '_' + (workingDirectoryCount++));
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Unable to create working directory");
        return directory;
    }

    private void postMerged(final MergedApk apk) {
        handler.post(() -> listener.onMerged(apk));
    }
}
