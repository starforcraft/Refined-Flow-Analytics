package com.ultramega.refinedflowanalytics.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncFlowSaveTest {
    @Test
    void replacesQueuedSnapshotsInsteadOfGrowingTheExecutorQueue() {
        final List<Runnable> tasks = new ArrayList<>();
        final List<Integer> writes = new ArrayList<>();
        final AsyncFlowSave writer = new AsyncFlowSave(tasks::add);
        final AtomicReference<Exception> failure = new AtomicReference<>();
        for (int i = 0; i < 1000; i++) {
            final int value = i;
            writer.submit(() -> writes.add(value), failure::set);
        }
        assertEquals(1, tasks.size());
        assertTrue(writes.isEmpty());
        tasks.getFirst().run();
        assertEquals(List.of(999), writes);
        assertNull(failure.get());
    }

    @Test
    void savesOnlyTheNewestReplacementAfterASlowWrite() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final List<Integer> writes = new ArrayList<>();
        final AtomicReference<Exception> failure = new AtomicReference<>();
        try (var executor = Executors.newSingleThreadExecutor()) {
            final AsyncFlowSave writer = new AsyncFlowSave(executor);
            writer.submit(() -> {
                started.countDown();
                await(release);
                writes.add(0);
            }, failure::set);
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                for (int i = 1; i < 1000; i++) {
                    final int value = i;
                    writer.submit(() -> writes.add(value), failure::set);
                }
                writer.submit(() -> {
                    writes.add(1000);
                    finished.countDown();
                }, failure::set);
            } finally {
                release.countDown();
            }
            assertTrue(finished.await(5, TimeUnit.SECONDS));
        }
        assertEquals(List.of(0, 1000), writes);
        assertNull(failure.get());
    }

    @Test
    void failedWriteDoesNotPreventRetrying() {
        final List<Runnable> tasks = new ArrayList<>();
        final AsyncFlowSave writer = new AsyncFlowSave(tasks::add);
        final AtomicReference<Exception> failure = new AtomicReference<>();
        final List<String> writes = new ArrayList<>();
        writer.submit(() -> {
            throw new IOException("Disk full");
        }, failure::set);
        tasks.removeFirst().run();
        assertEquals("Disk full", failure.get().getMessage());
        writer.submit(() -> writes.add("retried"), failure::set);
        tasks.removeFirst().run();
        assertEquals(List.of("retried"), writes);
    }

    private static void await(final CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting for test write");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
