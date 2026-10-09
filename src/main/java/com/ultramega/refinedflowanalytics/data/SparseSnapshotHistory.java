package com.ultramega.refinedflowanalytics.data;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * A bounded timeline of fixed-duration intervals. Missing entries are zero-flow intervals.
 */
final class SparseSnapshotHistory<K> {
    private final int capacity;
    private final Deque<Snapshot<K>> snapshots = new ArrayDeque<>();
    private long nextSequence;
    private int intervalCount;

    SparseSnapshotHistory(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("History capacity must be positive");
        }
        this.capacity = capacity;
    }

    boolean record(final Map<K, Long> deltas) {
        if (deltas.isEmpty()) {
            return this.advanceEmpty(1);
        }
        final Map<K, Long> nonzero = new HashMap<>();
        deltas.forEach((key, value) -> {
            if (value != 0) {
                nonzero.put(key, value);
            }
        });
        if (nonzero.isEmpty()) {
            return this.advanceEmpty(1);
        }
        this.snapshots.addLast(new Snapshot<>(this.nextSequence, Map.copyOf(nonzero)));
        this.nextSequence++;
        this.intervalCount = (int) Math.min(this.capacity, (long) this.intervalCount + 1);
        this.removeExpired();
        return true;
    }

    boolean advanceEmpty(final int intervals) {
        if (intervals < 0) {
            throw new IllegalArgumentException("Interval count must not be negative");
        }
        // A full window of zeros stays identical when another idle interval elapses.
        final boolean changed = intervals > 0 && (this.intervalCount < this.capacity || !this.snapshots.isEmpty());
        this.nextSequence += intervals;
        this.intervalCount = (int) Math.min(this.capacity, (long) this.intervalCount + intervals);
        this.removeExpired();
        return changed;
    }

    boolean trimToLast(final int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Retention must not be negative");
        }
        if (this.intervalCount <= max) {
            return false;
        }
        this.intervalCount = max;
        this.removeExpired();
        return true;
    }

    private void removeExpired() {
        final long oldest = this.nextSequence - this.intervalCount;
        while (!this.snapshots.isEmpty() && this.snapshots.getFirst().sequence() < oldest) {
            this.snapshots.removeFirst();
        }
    }

    int size() {
        return this.intervalCount;
    }

    int frameCount(final int intervalsPerFrame, final int limit) {
        if (intervalsPerFrame <= 0 || limit <= 0) {
            throw new IllegalArgumentException("Frame size and limit must be positive");
        }
        return (int) Math.min(limit, ((long) this.intervalCount + intervalsPerFrame - 1) / intervalsPerFrame);
    }

    void forEachRecent(final int intervalsPerFrame, final int limit, final BiConsumer<Integer, Map<K, Long>> consumer) {
        final int frames = this.frameCount(intervalsPerFrame, limit);
        final var iterator = this.snapshots.descendingIterator();
        while (iterator.hasNext()) {
            final Snapshot<K> snapshot = iterator.next();
            final long fromNewest = (this.nextSequence - 1 - snapshot.sequence()) / intervalsPerFrame;
            if (fromNewest >= frames) {
                break;
            }
            consumer.accept(frames - 1 - (int) fromNewest, snapshot.deltas());
        }
    }

    long sumRecent(final int intervals, final Predicate<K> matches) {
        final long[] total = {0};
        this.forEachRecent(intervals, 1, (index, changes) -> changes.forEach((key, amount) -> {
            if (matches.test(key)) {
                total[0] += amount;
            }
        }));
        return total[0];
    }

    void forEachStored(final BiConsumer<Integer, Map<K, Long>> consumer) {
        final long oldest = this.nextSequence - this.intervalCount;
        for (final Snapshot<K> snapshot : this.snapshots) {
            consumer.accept((int) (snapshot.sequence() - oldest), snapshot.deltas());
        }
    }

    /**
     * An immutable diagnostic view: zero intervals are expanded only when accessed.
     */
    List<Map<K, Long>> asList() {
        final List<Snapshot<K>> retained = List.copyOf(this.snapshots);
        final int size = this.intervalCount;
        final long oldest = this.nextSequence - size;
        return new AbstractList<>() {
            @Override
            public Map<K, Long> get(final int index) {
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException(index);
                }
                final long sequence = oldest + index;
                int low = 0;
                int high = retained.size() - 1;
                while (low <= high) {
                    final int middle = (low + high) >>> 1;
                    final Snapshot<K> snapshot = retained.get(middle);
                    if (snapshot.sequence() == sequence) {
                        return snapshot.deltas();
                    }
                    if (snapshot.sequence() < sequence) {
                        low = middle + 1;
                    } else {
                        high = middle - 1;
                    }
                }
                return Map.of();
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    private record Snapshot<K>(long sequence, Map<K, Long> deltas) {
    }
}
