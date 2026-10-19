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
import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateUtil {
    public static void checkForUpdates(boolean toast, MainActivity context) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = getHttpURLConnection();

                try (InputStream inputStream = conn.getInputStream();
                     InputStreamReader in = new InputStreamReader(inputStream);
                     BufferedReader reader = new BufferedReader(in)) {
                    String line;
                    String latestVersion = "";
                    String changelog = "";
                    String dl = "";
                    boolean rightBranch = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("browser_download_url")) {
                            String[] parts = line.split("\"");
                            if (parts.length < 4)
                                continue;
                            dl = parts[3];
                            String[] urlParts = dl.split("/");
                            if (urlParts.length < 8)
                                continue;
                            latestVersion = urlParts[7];
                            rightBranch = !latestVersion.isEmpty() && latestVersion.charAt(0) == '2';
                        } else if (line.contains("body") && rightBranch) {
                            String[] parts = line.split("\"");
                            if (parts.length >= 4)
                                changelog = parts[3];
                            break;
                        }
                    }
                    if (TextUtils.isEmpty(latestVersion) || TextUtils.isEmpty(dl))
                        throw new IOException("Unable to read update information");

                    String currentVer;
                    try {
                        currentVer = context.getPackageManager()
                                .getPackageInfo(context.getPackageName(), 0).versionName;
                    } catch (Exception e) {
                        currentVer = null;
                    }
                    boolean newVer = false;
                    char[] curr = TextUtils.isEmpty(currentVer) ? new char[]{'2', '3', '0'}
                            : currentVer.replace(".", "").toCharArray();
                    char[] latest = latestVersion.replace(".", "").toCharArray();

                    int maxLength = Math.max(curr.length, latest.length);
                    for (int i = 0; i < maxLength; i++) {
                        char currChar = i < curr.length ? curr[i] : '0';
                        char latestChar = i < latest.length ? latest[i] : '0';

                        if (latestChar > currChar) {
                            newVer = true;
                            break;
                        } else if (latestChar < currChar) {
                            break;
                        }
                    }
                    Resources rss = context.getRss();

                    if (newVer) {
                        if (!toast && !TextUtils.isEmpty(context.lastVerChecked)
                                && context.lastVerChecked.equals(latestVersion))
                            return;
                        String ending = ".apk";
                        String filename = "AntiSplit-M.v" + latestVersion + ending;
                        String link = dl.endsWith(ending) ? dl : dl + File.separator + filename;
                        String linebreak = "<br />";
                        CharSequence message = Html.fromHtml(rss.getString(R.string.new_ver)
                                + " (" + latestVersion + ")" + linebreak + linebreak
                                + rss.getString(R.string.changelog) + linebreak
                                + changelog.replace("\\r\\n", linebreak));

                        String finalLatestVersion = latestVersion;
                        context.getHandler().post(() -> {
                            AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                                    .setTitle(rss.getString(R.string.update))
                                    .setMessage(message)
                                    .setPositiveButton(rss.getString(R.string.dl), (dialog, which) -> {
                                        if (context.checkUpdateAfterStoragePermission == null)
                                            context.checkUpdateAfterStoragePermission = () -> {
                                                DownloadManager.Request request = new DownloadManager.Request(
                                                        Uri.parse(link))
                                                        .setTitle(filename).setDescription(filename)
                                                        .setMimeType("application/vnd.android.package-archive")
                                                        .setDestinationInExternalPublicDir(
                                                                Environment.DIRECTORY_DOWNLOADS, filename)
                                                        .setNotificationVisibility(
                                                                DownloadManager.Request.VISIBILITY_VISIBLE
                                                                        | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                                context.downloadId = ((DownloadManager) context
                                                        .getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                                            };
                                        if (Build.VERSION.SDK_INT < 29
                                                && com.abdurazaaqmohammed.utils.FileUtils.doesNotHaveStoragePerm(context))
                                            context.checkStoragePerm(10);
                                        else
                                            context.checkUpdateAfterStoragePermission.run();
                                    })
                                    .setNegativeButton(rss.getString(R.string.github_release), (dialog, which) ->
                                            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                                                    "https://github.com/AbdurazaaqMohammed/AntiSplit-M/releases/latest"))))
                                    .setNeutralButton(rss.getString(R.string.cancel), null).create();
                            alertDialog.setOnDismissListener(dialog -> context.lastVerChecked = finalLatestVersion);
                            alertDialog.show();
                        });
                    } else if (toast)
                        context.getHandler().post(() -> Toast.makeText(context,
                                rss.getString(R.string.no_update_found), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (toast)
                    context.runOnUiThread(() -> Toast.makeText(context,
                            R.string.update_check_failed, Toast.LENGTH_SHORT).show());
            }
        }, "update-check").start();
    }

    @NonNull
    private static HttpURLConnection getHttpURLConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://api.github.com/repos/AbdurazaaqMohammed/AntiSplit-M/releases").openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        return conn;
    }
}
