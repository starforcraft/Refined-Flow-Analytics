package com.ultramega.refinedflowanalytics.util;

public final class TickScheduler {
    private int counter = 0;

    private final int interval;

    public TickScheduler(final int interval) {
        this.interval = interval;
    }

    /**
     * Run every tick - returns true once every `interval` ticks. Runs in first tick after initialization.
     */
    public boolean shouldRun() {
        try {
            return this.counter == 0;
        } finally {
            if (++this.counter >= this.interval) {
                this.counter = 0;
            }
        }
    }
}
