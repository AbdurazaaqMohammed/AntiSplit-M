package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.reandroid.apk.APKLogger;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyAPKLogger implements APKLogger {

    private static final int MAX_LOG_CHARS = 20000;

    private final Handler handler;
    private final Resources resources;
    private final AppSettings settings;
    private final TextView logField;
    private final NestedScrollView scrollView;
    private final StringBuilder buffer = new StringBuilder();
    private final AtomicBoolean updatePending = new AtomicBoolean(false);

    public MyAPKLogger(Handler handler, Resources resources, AppSettings settings,
            TextView logField, NestedScrollView scrollView) {
        this.handler = handler;
        this.resources = resources;
        this.settings = settings;
        this.logField = logField;
        this.scrollView = scrollView;
    }

    @Override
    public void logMessage(String message) {
        if (message == null || !settings.logEnabled)
            return;
        synchronized (buffer) {
            buffer.append(message).append('\n');
            if (buffer.length() > MAX_LOG_CHARS)
                buffer.delete(0, buffer.length() - MAX_LOG_CHARS);
        }
        scheduleUpdate();
    }

    public void logMessage(int resourceId) {
        logMessage(resources.getString(resourceId));
    }

    @Override
    public void logError(String message, Throwable throwable) {
        logMessage(message);
        if (throwable != null)
            logMessage(throwable.toString());
    }

    @Override
    public void logVerbose(String message) {
        logMessage(message);
    }

    public void clear() {
        synchronized (buffer) {
            buffer.setLength(0);
        }
    }

    public CharSequence getText() {
        synchronized (buffer) {
            return buffer.toString();
        }
    }

    private void scheduleUpdate() {
        if (!updatePending.compareAndSet(false, true))
            return;
        handler.post(() -> {
            updatePending.set(false);
            String text;
            synchronized (buffer) {
                text = buffer.toString();
            }
            if (!text.contentEquals(logField.getText()))
                logField.setText(text);
            scrollView.fullScroll(View.FOCUS_DOWN);
        });
    }
}
