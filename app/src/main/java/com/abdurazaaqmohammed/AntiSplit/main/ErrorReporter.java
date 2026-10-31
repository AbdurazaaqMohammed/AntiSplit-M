package com.abdurazaaqmohammed.AntiSplit.main;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.utils.AppVersion;
import com.abdurazaaqmohammed.utils.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ErrorReporter {

    private static final String ISSUE_URL =
            "https://github.com/AbdurazaaqMohammed/AntiSplit-M/issues/new?title=Crash%20Report&body=";
    private static final int MAX_STACK_TRACE_LINES = 30;
    private static final int MAX_ISSUE_LENGTH = 6000;

    private final Activity activity;
    private final Handler handler;
    private final Resources resources;
    private final TextView logField;
    private final TextView errorField;

    public ErrorReporter(Activity activity, Handler handler, Resources resources,
            TextView logField, TextView errorField) {
        this.activity = activity;
        this.handler = handler;
        this.resources = resources;
        this.logField = logField;
        this.errorField = errorField;
    }

    public void show(Throwable error, Uri selectedUri) {
        if (error instanceof OutOfMemoryError) {
            showOutOfMemory();
            return;
        }
        if (error == null || activity.isFinishing() || activity.isDestroyed())
            return;

        boolean hasMessage = !TextUtils.isEmpty(error.getMessage());
        String title = hasMessage ? error.getMessage() : error.getClass().getSimpleName();

        StringBuilder details = new StringBuilder();
        if (hasMessage) {
            details.append(title);
            if (isZipLike(error, selectedUri))
                appendZipHint(details);
        }
        StackTraceElement[] stackTrace = error.getStackTrace();
        int lineCount = Math.min(stackTrace.length, MAX_STACK_TRACE_LINES);
        for (int i = 0; i < lineCount; i++)
            details.append(stackTrace[i]).append('\n');

        final String dialogText = details.toString();
        final StringBuilder issueText = new StringBuilder(dialogText);
        handler.post(() -> showDialog(title, dialogText, issueText));
    }

    public void show(CharSequence message) {
        handler.post(() -> {
            if (activity.isFinishing() || activity.isDestroyed())
                return;
            errorField.setVisibility(View.VISIBLE);
            errorField.setText(message);
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        });
    }

    public void show(int messageRes) {
        show(resources.getString(messageRes));
    }

    public void clear() {
        logField.setText("");
        errorField.setText("");
        errorField.setVisibility(View.GONE);
    }

    public void copyLog() {
        copyText(logField.getText() + "\n" + errorField.getText());
    }

    private void showDialog(String title, String dialogText, StringBuilder issueText) {
        if (activity.isFinishing() || activity.isDestroyed())
            return;

        issueText.append('\n')
                .append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
                .append(resources.getString(R.string.app_name)).append(' ')
                .append(AppVersion.name(activity)).append('\n')
                .append("Storage permission granted: ")
                .append(!FileUtils.doesNotHaveStoragePerm(activity)).append('\n')
                .append(logField.getText());

        String body = issueText.toString();
        if (body.length() > MAX_ISSUE_LENGTH)
            body = body.substring(0, MAX_ISSUE_LENGTH);
        final CharSequence encodedBody = Uri.encode(body);

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_button_layout, null);
        ((TextView) dialogView.findViewById(R.id.errorD)).setText(dialogText);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.copy_log, (d, which) -> {
                    copyText(issueText);
                    d.dismiss();
                })
                .setNegativeButton(R.string.create_issue, (d, which) -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(ISSUE_URL + encodedBody)));
                    d.dismiss();
                })
                .setNeutralButton(R.string.cancel, (d, which) -> d.dismiss())
                .create();
        DialogStyler.show(activity, dialog);
    }

    private void showOutOfMemory() {
        handler.post(() -> {
            if (activity.isFinishing() || activity.isDestroyed())
                return;
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.out_of_memory_title)
                    .setMessage(R.string.out_of_memory_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private boolean isZipLike(Throwable error, Uri selectedUri) {
        String message = error.getMessage();
        if (message != null && message.contains("Failed to find end record"))
            return true;
        if (selectedUri == null)
            return false;
        try {
            String mimeType = activity.getContentResolver().getType(selectedUri);
            return TextUtils.isEmpty(mimeType)
                    || (!"application/zip".equals(mimeType)
                    && !"application/octet-stream".equals(mimeType)
                    && !"application/vnd.apkm".equals(mimeType));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void appendZipHint(StringBuilder target) {
        target.append(" - ")
                .append(resources.getString(R.string.check_file_valid))
                .append(resources.getString(R.string.select_from_installed_apps))
                .append('\n');
    }

    private void copyText(CharSequence text) {
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (clipboard == null)
            return;
        clipboard.setPrimaryClip(ClipData.newPlainText("log", text));
        Toast.makeText(activity, R.string.copied_log, Toast.LENGTH_SHORT).show();
    }
}
