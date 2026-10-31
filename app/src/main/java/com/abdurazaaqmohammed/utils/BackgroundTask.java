package com.abdurazaaqmohammed.utils;

import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundTask {

    public interface Work {
        void run() throws Exception;
    }

    public interface ErrorHandler {
        void onError(Throwable error);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "antisplit-worker");
        thread.setDaemon(true);
        return thread;
    });

    private BackgroundTask() {
    }

    public static void submit(Work work) {
        EXECUTOR.execute(() -> {
            try {
                work.run();
            } catch (Throwable ignored) {
            }
        });
    }

    public static void submit(Work work, Handler handler, Runnable onSuccess, ErrorHandler onError) {
        EXECUTOR.execute(() -> {
            try {
                work.run();
                if (onSuccess != null && handler != null)
                    handler.post(onSuccess);
            } catch (Throwable error) {
                if (onError != null && handler != null)
                    handler.post(() -> onError.onError(error));
            }
        });
    }

    public static void submit(Work work, Handler handler, ErrorHandler onError) {
        submit(work, handler, null, onError);
    }
}
