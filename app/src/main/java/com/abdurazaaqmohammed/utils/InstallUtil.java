package com.abdurazaaqmohammed.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.abdurazaaqmohammed.AntiSplit.R;

public class InstallUtil {
    public static void installApk(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(uri);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException firstError) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (Exception secondError) {
                Toast.makeText(context, R.string.install_failed, Toast.LENGTH_LONG).show();
            }
        }
    }
}
