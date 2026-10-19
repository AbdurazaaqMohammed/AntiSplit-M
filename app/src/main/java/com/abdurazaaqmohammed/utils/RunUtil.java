package com.abdurazaaqmohammed.utils;

import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunUtil {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "antisplit-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Handler handler;
    private final MainActivity context;
    private final CharSequence msg;

    public RunUtil(Handler handler, MainActivity context, CharSequence msg) {
        this.handler = handler;
        this.context = context;
        this.msg = msg;
    }

    public static void runInBackground(Runnable runnable) {
        EXECUTOR.execute(runnable);
    }

    public void runInBackground(Callable<Boolean> callable) {
        EXECUTOR.execute(() -> {
            try {
                Boolean success = callable.call();
                if (success && handler != null && !TextUtils.isEmpty(msg))
                    handler.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
            } catch (Throwable e) {
                context.showError(e);
            }
        });
    }

    public void runInBackground(Callable<Boolean> callable, Runnable doAfter, boolean onlyIfSuccessful) {
        EXECUTOR.execute(() -> {
            try {
                if (callable.call()) {
                    if (handler != null)
                        handler.post(doAfter);
                } else if (!onlyIfSuccessful && handler != null) {
                    handler.post(doAfter);
                }
            } catch (Throwable e) {
                if (!onlyIfSuccessful && handler != null)
                    handler.post(doAfter);
                context.showError(e);
            }
        });
    }
}
