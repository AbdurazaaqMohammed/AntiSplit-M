package com.abdurazaaqmohammed.AntiSplit.main;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;

public final class DialogStyler {

    private static final float WIDTH_FRACTION = 0.92f;
    private static final int MAX_WIDTH_DP = 560;
    private static final float HEIGHT_FRACTION = 0.85f;

    private DialogStyler() {
    }

    public static void show(Activity activity, AlertDialog dialog) {
        show(activity, dialog, true);
    }

    public static void show(Activity activity, AlertDialog dialog, boolean fixedHeight) {
        activity.runOnUiThread(() -> size(activity, dialog, fixedHeight));
    }

    private static void size(Activity activity, AlertDialog dialog, boolean fixedHeight) {
        if (activity.isFinishing() || activity.isDestroyed())
            return;
        try {
            if (!dialog.isShowing())
                dialog.show();
        } catch (Exception e) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null)
            return;

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = Math.min((int) (metrics.widthPixels * WIDTH_FRACTION),
                (int) (MAX_WIDTH_DP * metrics.density));
        int height = fixedHeight
                ? (int) (metrics.heightPixels * HEIGHT_FRACTION)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setLayout(width, height);
    }
}
