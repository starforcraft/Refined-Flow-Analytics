package com.ultramega.refinedflowanalytics.api;

/**
 * Marks synchronous inventory discovery/removal when a storage joins or leaves a composite.
 * Therefore, actual inserts/extracts/external inventory updates remain observable.
 */
public final class StorageSourceChangeContext {
    private static final ThreadLocal<Boolean> SOURCE_CHANGE = new ThreadLocal<>();

    private StorageSourceChangeContext() {
    }

    public static boolean isSourceChange() {
        return Boolean.TRUE.equals(SOURCE_CHANGE.get());
    }

    public static void run(final Runnable action) {
        final Boolean previous = SOURCE_CHANGE.get();
        SOURCE_CHANGE.set(true);
        try {
            action.run();
        } finally {
            if (previous == null) {
                SOURCE_CHANGE.remove();
            } else {
                SOURCE_CHANGE.set(previous);
            }
        }
    }
}
