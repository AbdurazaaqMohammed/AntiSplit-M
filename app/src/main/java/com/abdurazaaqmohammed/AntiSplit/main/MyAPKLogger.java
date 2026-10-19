package com.abdurazaaqmohammed.AntiSplit.main;

import android.view.View;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.reandroid.apk.APKLogger;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyAPKLogger implements APKLogger {
    private static final int MAX_LOG_CHARS = 12000;
    private final MainActivity mainActivity;
    private final TextView logField;
    private final NestedScrollView scrollView;
    private final StringBuilder buffer = new StringBuilder();
    private final AtomicBoolean updatePending = new AtomicBoolean(false);

    MyAPKLogger(MainActivity mainActivity, TextView logField, NestedScrollView scrollView) {
        this.mainActivity = mainActivity;
        this.logField = logField;
        this.scrollView = scrollView;
    }

    @Override
    public void logMessage(String s) {
        if (s == null || !mainActivity.logEnabled)
            return;
        synchronized (buffer) {
            buffer.append(s).append('\n');
            if (buffer.length() > MAX_LOG_CHARS)
                buffer.delete(0, buffer.length() - MAX_LOG_CHARS);
        }
        scheduleUpdate();
    }

    private void scheduleUpdate() {
        if (!updatePending.compareAndSet(false, true))
            return;
        mainActivity.getHandler().post(() -> {
            updatePending.set(false);
            if (mainActivity.isFinishing() || mainActivity.isDestroyed())
                return;
            boolean wasAtBottom = !scrollView.canScrollVertically(1);
            String text;
            synchronized (buffer) {
                text = buffer.toString();
            }
            if (!text.contentEquals(logField.getText()))
                logField.setText(text);
            if (wasAtBottom)
                scrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    @Override
    public void logError(String s, Throwable throwable) {
        logMessage(s);
        if (throwable != null)
            mainActivity.showError(throwable);
    }

    @Override
    public void logVerbose(String s) {
        logMessage(s);
    }

    public void logMessage(int resourceId) {
        if (mainActivity.logEnabled)
            logMessage(mainActivity.getRss().getString(resourceId));
    }

    public void clear() {
        synchronized (buffer) {
            buffer.setLength(0);
        }
    }
}
