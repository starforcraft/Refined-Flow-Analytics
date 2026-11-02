package com.ultramega.refinedflowanalytics.data;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.annotation.Nullable;

final class AsyncFlowSave {
    private static final Executor IO = Executors.newSingleThreadExecutor(task -> {
        final Thread thread = new Thread(task, "Refined Flow Analytics IO");
        thread.setDaemon(true);
        return thread;
    });

    private final Executor executor;
    @Nullable
    private Request pending;
    private boolean running;

    AsyncFlowSave() {
        this(IO);
    }

    AsyncFlowSave(final Executor executor) {
        this.executor = executor;
    }

    synchronized void submit(final Save action, final Consumer<Exception> onFailure) {
        this.pending = new Request(action, onFailure);
        if (!this.running) {
            this.running = true;
            try {
                this.executor.execute(this::drain);
            } catch (final RuntimeException e) {
                this.running = false;
                this.pending = null;
                onFailure.accept(e);
            }
        }
    }

    private void drain() {
        while (true) {
            final Request request;
            synchronized (this) {
                if (this.pending == null) {
                    this.running = false;
                    return;
                }
                request = this.pending;
                this.pending = null;
            }
            try {
                request.action().run();
            } catch (final Exception e) {
                request.onFailure().accept(e);
            }
        }
    }

    static void flush() {
        CompletableFuture.runAsync(() -> { }, IO).join();
    }

    @FunctionalInterface
    interface Save {
        void run() throws IOException;
    }

    private record Request(Save action, Consumer<Exception> onFailure) {
    }
}
