package com.ultramega.refinedflowanalytics.data;

import java.util.Arrays;

/** Bounded one-second, minute, hour and day histories for a monitor's selected filter */
public final class FlowMonitorHistory {
    public static final int CAPACITY = 200;
    private static final int[] INTERVALS = {1, 60, 3600, 86400};
    private final Bucket[] buckets = Arrays.stream(INTERVALS).mapToObj(Bucket::new).toArray(Bucket[]::new);

    public static int[] getIntervals() {
        return INTERVALS.clone();
    }

    public void append(final long produced, final long consumed) {
        for (final Bucket bucket : this.buckets) {
            bucket.accept(Math.max(0, produced), Math.max(0, consumed));
        }
    }

    public void clear() {
        for (final Bucket bucket : this.buckets) {
            bucket.clear();
        }
    }

    public void loadFrames(final int seconds, final long[] inflow, final long[] outflow) {
        if (inflow.length != outflow.length) {
            throw new IllegalArgumentException("Flow histories must have the same number of frames");
        }
        final Bucket bucket = this.bucket(seconds);
        bucket.clear();
        for (int i = Math.max(0, inflow.length - CAPACITY); i < inflow.length; i++) {
            bucket.store(Math.max(0, inflow[i]), Math.max(0, outflow[i]));
        }
    }

    public long[] getInflow(final int seconds) {
        return this.bucket(seconds).values(true);
    }

    public long[] getOutflow(final int seconds) {
        return this.bucket(seconds).values(false);
    }

    public long[] saveBucket(final int seconds) {
        final Bucket bucket = this.bucket(seconds);
        final long[] result = new long[3 + bucket.size * 2];
        result[0] = bucket.elapsed;
        result[1] = bucket.pendingIn;
        result[2] = bucket.pendingOut;
        for (int i = 0; i < bucket.size; i++) {
            final int index = (bucket.next - bucket.size + CAPACITY + i) % CAPACITY;
            result[3 + i * 2] = bucket.inflow[index];
            result[4 + i * 2] = bucket.outflow[index];
        }
        return result;
    }

    public void loadBucket(final int seconds, final long[] saved) {
        final Bucket bucket = this.bucket(seconds);
        bucket.clear();
        if (saved.length < 3 || (saved.length - 3) % 2 != 0) {
            return;
        }
        final int count = (saved.length - 3) / 2;
        for (int i = Math.max(0, count - CAPACITY); i < count; i++) {
            bucket.store(Math.max(0, saved[3 + i * 2]), Math.max(0, saved[4 + i * 2]));
        }
        if (saved[0] > 0 && saved[0] < seconds) {
            bucket.elapsed = (int) saved[0];
            bucket.pendingIn = Math.max(0, saved[1]);
            bucket.pendingOut = Math.max(0, saved[2]);
        }
    }

    private Bucket bucket(final int seconds) {
        for (final Bucket bucket : this.buckets) {
            if (bucket.seconds == seconds) {
                return bucket;
            }
        }
        throw new IllegalArgumentException("Unsupported monitor interval: " + seconds);
    }

    private static final class Bucket {
        private final int seconds;
        private final long[] inflow = new long[CAPACITY];
        private final long[] outflow = new long[CAPACITY];
        private int next;
        private int size;
        private int elapsed;
        private long pendingIn;
        private long pendingOut;

        private Bucket(final int seconds) {
            this.seconds = seconds;
        }

        private void accept(final long produced, final long consumed) {
            this.pendingIn += produced;
            this.pendingOut += consumed;
            if (++this.elapsed == this.seconds) {
                this.store(this.pendingIn, this.pendingOut);
                this.elapsed = 0;
                this.pendingIn = 0;
                this.pendingOut = 0;
            }
        }

        private void store(final long produced, final long consumed) {
            this.inflow[this.next] = produced;
            this.outflow[this.next] = consumed;
            this.next = (this.next + 1) % CAPACITY;
            this.size = Math.min(CAPACITY, this.size + 1);
        }

        private long[] values(final boolean production) {
            final boolean partial = this.elapsed > 0;
            final int complete = Math.min(this.size, CAPACITY - (partial ? 1 : 0));
            final long[] result = new long[complete + (partial ? 1 : 0)];
            final long[] source = production ? this.inflow : this.outflow;
            for (int i = 0; i < complete; i++) {
                result[i] = source[(this.next - complete + CAPACITY + i) % CAPACITY];
            }
            if (partial) {
                result[complete] = production ? this.pendingIn : this.pendingOut;
            }
            return result;
        }

        private void clear() {
            this.next = 0;
            this.size = 0;
            this.elapsed = 0;
            this.pendingIn = 0;
            this.pendingOut = 0;
            Arrays.fill(this.inflow, 0);
            Arrays.fill(this.outflow, 0);
        }
    }
}
