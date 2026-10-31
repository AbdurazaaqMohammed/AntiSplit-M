package com.abdurazaaqmohammed.utils;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.AntiSplit.main.DialogStyler;
import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateUtil {

    private static final String RELEASES_API =
            "https://api.github.com/repos/AbdurazaaqMohammed/AntiSplit-M/releases";
    private static final String RELEASES_PAGE =
            "https://github.com/AbdurazaaqMohammed/AntiSplit-M/releases/latest";
    private static final String APK_EXTENSION = ".apk";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "update-check");
        thread.setDaemon(true);
        return thread;
    });

    private UpdateUtil() {
    }

    public static void checkForUpdates(boolean manualCheck, final MainActivity activity) {
        EXECUTOR.execute(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                if (!isNewerVersion(AppVersion.name(activity), release.version)) {
                    if (manualCheck)
                        activity.runOnUiThread(() -> Toast.makeText(activity,
                                R.string.no_update_found, Toast.LENGTH_SHORT).show());
                    return;
                }
                if (!manualCheck
                        && release.version.equals(activity.getSettings().lastVersionChecked))
                    return;

                Resources resources = activity.getResources();
                activity.getHandler().post(() -> showUpdateDialog(activity, release, resources));
            } catch (Exception e) {
                if (manualCheck)
                    activity.runOnUiThread(() -> Toast.makeText(activity,
                            R.string.update_check_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static void showUpdateDialog(MainActivity activity, ReleaseInfo release,
            Resources resources) {
        if (activity.isFinishing() || activity.isDestroyed())
            return;

        String lineBreak = "<br />";
        CharSequence message = Html.fromHtml(resources.getString(R.string.new_ver)
                + " (" + release.version + ")" + lineBreak + lineBreak
                + resources.getString(R.string.changelog) + lineBreak
                + release.changelog.replace("\\r\\n", lineBreak).replace("\\n", lineBreak));

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update)
                .setMessage(message)
                .setPositiveButton(R.string.dl, (d, which) -> downloadUpdate(activity, release))
                .setNegativeButton(R.string.github_release, (d, which) ->
                        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(RELEASES_PAGE))))
                .setNeutralButton(R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(d ->
                activity.getSettings().lastVersionChecked = release.version);
        DialogStyler.show(activity, dialog);
    }

    private static void downloadUpdate(final MainActivity activity, ReleaseInfo release) {
        final String fileName = "AntiSplit-M.v" + release.version + APK_EXTENSION;
        Runnable enqueue = () -> {
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(release.downloadUrl))
                    .setTitle(fileName)
                    .setDescription(fileName)
                    .setMimeType("application/vnd.android.package-archive")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE
                            | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager =
                    (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager == null)
                return;
            activity.downloadId = downloadManager.enqueue(request);
        };

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && FileUtils.doesNotHaveStoragePerm(activity)) {
            activity.checkUpdateAfterStoragePermission = enqueue;
            activity.checkStoragePerm(10);
        } else {
            enqueue.run();
        }
    }

    @NonNull
    private static ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(RELEASES_API).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0");
        connection.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        String latestVersion = "";
        String changelog = "";
        String downloadUrl = "";

        try (InputStream inputStream = connection.getInputStream();
             InputStreamReader in = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(in)) {
            String line;
            boolean matchingRelease = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("browser_download_url")) {
                    String[] parts = line.split("\"");
                    if (parts.length < 4)
                        continue;
                    String url = parts[3];
                    String[] urlParts = url.split("/");
                    if (!url.endsWith(APK_EXTENSION) || urlParts.length < 8)
                        continue;
                    String version = urlParts[7];
                    if (version.isEmpty() || version.charAt(0) != '2')
                        continue;
                    downloadUrl = url;
                    latestVersion = version;
                    matchingRelease = true;
                } else if (line.contains("body") && matchingRelease) {
                    String[] parts = line.split("\"");
                    if (parts.length >= 4)
                        changelog = parts[3];
                    break;
                }
            }
        } finally {
            connection.disconnect();
        }

        if (TextUtils.isEmpty(latestVersion) || TextUtils.isEmpty(downloadUrl))
            throw new IOException("Unable to read update information");
        return new ReleaseInfo(latestVersion, downloadUrl, changelog);
    }

    static boolean isNewerVersion(String currentVersion, String latestVersion) {
        if (TextUtils.isEmpty(latestVersion))
            return false;
        if (TextUtils.isEmpty(currentVersion))
            return true;

        String[] currentParts = currentVersion.split("[^0-9]+");
        String[] latestParts = latestVersion.split("[^0-9]+");
        int count = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < count; i++) {
            int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            int latest = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            if (latest != current)
                return latest > current;
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        if (TextUtils.isEmpty(part))
            return 0;
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class ReleaseInfo {
        final String version;
        final String downloadUrl;
        final String changelog;

        ReleaseInfo(String version, String downloadUrl, String changelog) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
        }
    }
}
