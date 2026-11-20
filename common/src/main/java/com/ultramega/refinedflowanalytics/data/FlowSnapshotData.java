package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Suppliers;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FlowSnapshotData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Executor READER = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("Refined Flow Analytics reader").factory());
    private static final Interner<ResourceChangeKey> CHANGE_KEYS = Interners.newWeakInterner();
    private static final int FORMAT_VERSION = 3;
    private static final int DATA_GRANULARITY = 20;
    // Seven days of active recording, measured in snapshots rather than ticks
    private static final int MAX_COLLECTION_SNAPSHOTS = 20 * 60 * 60 * 24 * 7 / DATA_GRANULARITY;
    private static final int MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET = 209;

    private final SparseSnapshotHistory<ResourceChangeKey> history = new SparseSnapshotHistory<>(MAX_COLLECTION_SNAPSHOTS);
    private final SparseSnapshotHistory<ResourceChangeKey> tickHistory = new SparseSnapshotHistory<>(MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
    private final Map<ResourceChangeKey, Long> pending = new HashMap<>();
    private final Map<ResourceChangeKey, Long> tickPending = new HashMap<>();
    private final AsyncFlowSave writer = new AsyncFlowSave();
    private volatile boolean saveFailed;
    @Nullable
    private Path unreadableFile;
    @Nullable
    private Loading loading;
    private int pendingTicks;
    private long revision;
    private long tickRevision;

    public static FlowSnapshotData load(final CompoundTag tag) {
        return FlowSnapshotNbtReader.read(tag);
    }

    public static FlowSnapshotData get(final ServerLevel level, final UUID networkId) {
        final var storage = level.getServer().overworld().getDataStorage();
        return ((FlowHistoryStorage) storage).refinedflowanalytics$getHistory(networkId);
    }

    public static FlowSnapshotData open(final Path path) {
        return open(path, READER);
    }

    static FlowSnapshotData open(final Path path, final Executor reader) {
        final FlowSnapshotData data = new FlowSnapshotData();
        final Path file = path.toAbsolutePath();
        if (!Files.notExists(file)) {
            final var original = CompletableFuture.supplyAsync(() -> readFile(file), reader);
            // Only network merges need the exact duration before the background restore finishes.
            final Supplier<Timing> timing = Suppliers.memoize(() -> {
                try {
                    return FlowSnapshotNbtReader.readTiming(file);
                } catch (final IOException | RuntimeException e) {
                    return new Timing(0, 0);
                }
            });
            data.loading = new Loading(original, timing, new ArrayList<>());
        }
        return data;
    }

    private static FlowSnapshotData readFile(final Path file) {
        try {
            return FlowSnapshotNbtReader.read(file);
        } catch (final NoSuchFileException e) {
            return new FlowSnapshotData();
        } catch (final IOException | RuntimeException e) {
            LOGGER.error("Could not read flow history {}. Keeping the original file and recording in memory for this session.", file, e);
            final FlowSnapshotData data = new FlowSnapshotData();
            data.unreadableFile = file;
            return data;
        }
    }

    public boolean pollLoad() {
        if (this.loading != null && this.loading.original.isDone()) {
            this.finishLoading();
            return true;
        }
        return false;
    }

    private void finishLoading() {
        final Loading load = this.loading;
        if (load == null) {
            return;
        }
        final FlowSnapshotData original = load.original.join();
        final boolean dirty = this.isDirty() || original.isDirty();
        final Map<ResourceChangeKey, Long> currentTick = new HashMap<>(this.tickPending);
        this.loading = null;
        // The baseline is shared with saves and network forks. Replay each branch's suffix into its own history.
        original.copyStateTo(this);
        for (final var changes : load.ticks) {
            changes.forEach(this::addChange);
            this.tick();
        }
        currentTick.forEach(this::addChange);
        this.revision++;
        this.tickRevision++;
        this.setDirty(dirty);
    }

    public CompoundTag save() {
        this.finishLoading();
        final CompoundTag tag = new CompoundTag();
        final Map<PlatformResourceKey, Integer> resourceIds = new LinkedHashMap<>();
        final ListTag snapshots = new ListTag();
        this.history.forEachStored((offset, changes) -> {
            final CompoundTag deltas = new CompoundTag();
            changes.forEach((key, value) -> {
                final int id = resourceIds.computeIfAbsent(key.resourceKey(), resource -> resourceIds.size() + 1);
                deltas.putLong(Integer.toString(key.sign() > 0 ? id : -id), value);
            });
            final CompoundTag snapshot = new CompoundTag();
            snapshot.putInt("offset", offset);
            snapshot.put("deltas", deltas);
            snapshots.add(snapshot);
        });
        final CompoundTag pendingTag = new CompoundTag();
        this.pending.forEach((key, value) -> {
            final int id = resourceIds.computeIfAbsent(key.resourceKey(), resource -> resourceIds.size() + 1);
            pendingTag.putLong(Integer.toString(key.sign() > 0 ? id : -id), value);
        });
        final ListTag tickSnapshots = new ListTag();
        this.tickHistory.forEachStored((offset, changes) -> {
            final CompoundTag snapshot = new CompoundTag();
            final CompoundTag deltas = new CompoundTag();
            changes.forEach((key, value) -> {
                final int id = resourceIds.computeIfAbsent(key.resourceKey(), resource -> resourceIds.size() + 1);
                deltas.putLong(Integer.toString(key.sign() > 0 ? id : -id), value);
            });
            snapshot.putInt("offset", offset);
            snapshot.put("deltas", deltas);
            tickSnapshots.add(snapshot);
        });
        final CompoundTag tickPendingTag = new CompoundTag();
        this.tickPending.forEach((key, value) -> {
            final int id = resourceIds.computeIfAbsent(key.resourceKey(), resource -> resourceIds.size() + 1);
            tickPendingTag.putLong(Integer.toString(key.sign() > 0 ? id : -id), value);
        });
        final CompoundTag dictionary = new CompoundTag();
        resourceIds.forEach((key, id) -> ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, key).result()
            .ifPresent(encoded -> dictionary.put(Integer.toString(id), encoded)));
        tag.putInt("format_version", FORMAT_VERSION);
        tag.putInt("interval_count", this.history.size());
        tag.put("items_map", dictionary);
        tag.put("snapshots", snapshots);
        tag.put("pending", pendingTag);
        tag.putInt("pending_ticks", this.pendingTicks);
        tag.putInt("tick_count", this.tickHistory.size());
        tag.put("tick_snapshots", tickSnapshots);
        tag.put("tick_pending", tickPendingTag);
        return tag;
    }

    public void save(final Path path) {
        final Path target = path.toAbsolutePath();
        if (!this.isDirty() || target.equals(this.unreadableFile)) {
            return;
        }

        final FlowSnapshotData snapshot = new FlowSnapshotData();
        this.copyStateTo(snapshot);
        this.saveFailed = false;
        this.setDirty(false);
        this.writer.submit(() -> {
            // A world can be saved or closed while its history is still loading.
            snapshot.finishLoading();
            if (!target.equals(snapshot.unreadableFile)) {
                snapshot.writeSnapshot(target);
            }
        }, e -> {
            // Do not clear the live dirty flag from the IO thread: ticks may have changed it since capture
            this.saveFailed = true;
            LOGGER.error("Could not save flow history to {}", target, e);
        });
    }

    private void writeSnapshot(final Path target) throws IOException {
        Files.createDirectories(target.getParent());
        final Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (var raw = Files.newOutputStream(temporary);
                 var compressed = new GZIPOutputStream(raw, 64 * 1024);
                 var output = new DataOutputStream(new BufferedOutputStream(compressed, 64 * 1024))) {
                new FlowSnapshotNbtWriter(output).write(FORMAT_VERSION, this.history, this.tickHistory, this.pending, this.tickPending, this.pendingTicks);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || this.saveFailed;
    }

    public static void flushPendingSaves() {
        CompletableFuture.runAsync(() -> { }, READER).join();
        AsyncFlowSave.flush();
    }

    public void recordSnapshot(final Map<ResourceChangeKey, Long> deltaMap) {
        this.finishLoading();
        this.recordInterval(deltaMap);
    }

    private void recordInterval(final Map<ResourceChangeKey, Long> deltaMap) {
        if (this.history.record(deltaMap)) {
            this.revision++;
            this.setDirty();
        }
    }

    public void trimToLast(final int max) {
        this.finishLoading();
        if (this.history.trimToLast(max)) {
            this.revision++;
            this.setDirty();
        }
    }

    public List<Map<ResourceChangeKey, Long>> getSnapshots() {
        this.finishLoading();
        return this.history.asList();
    }

    public int getRecordedIntervals() {
        this.pollLoad();
        if (this.loading != null) {
            final Timing timing = this.loading.timing.get();
            if (!this.pollLoad()) {
                final long completed = ((long) timing.pendingTicks + this.loading.ticks.size()) / DATA_GRANULARITY;
                return (int) Math.min(MAX_COLLECTION_SNAPSHOTS, timing.intervals + completed);
            }
        }
        return this.history.size();
    }

    public long getRevision() {
        return this.revision;
    }

    public long getRevision(final int granularity) {
        return granularity == Granularity.TICK.getTickAmount() ? this.tickRevision : this.revision;
    }

    public void recordChange(final PlatformResourceKey resource, final long amount) {
        if (amount != 0) {
            final ResourceChangeKey key = changeKey(resource, amount > 0 ? (short) 1 : (short) -1);
            this.addChange(key, amount);
            this.setDirty();
        }
    }

    static ResourceChangeKey changeKey(final PlatformResourceKey resource, final short sign) {
        return CHANGE_KEYS.intern(new ResourceChangeKey(resource, sign));
    }

    private void addChange(final ResourceChangeKey key, final long amount) {
        this.pending.merge(key, amount, Long::sum);
        this.tickPending.merge(key, amount, Long::sum);
    }

    public boolean tick() {
        final boolean restored = this.pollLoad();
        if (this.loading != null) {
            this.loading.ticks.add(Map.copyOf(this.tickPending));
        }
        this.setDirty();
        if (this.tickHistory.record(this.tickPending)) {
            this.tickRevision++;
        }
        this.tickPending.clear();
        if (++this.pendingTicks < DATA_GRANULARITY) {
            return restored;
        }
        this.pendingTicks = 0;
        this.recordInterval(this.pending);
        this.pending.clear();
        return true;
    }

    public void copyTo(final FlowSnapshotData target) {
        this.copyStateTo(target);
        target.tickRevision++;
        target.revision++;
        target.setDirty();
    }

    private void copyStateTo(final FlowSnapshotData target) {
        this.history.copyTo(target.history);
        this.tickHistory.copyTo(target.tickHistory);
        target.tickPending.clear();
        target.tickPending.putAll(this.tickPending);
        target.pending.clear();
        target.pending.putAll(this.pending);
        target.pendingTicks = this.pendingTicks;
        target.loading = this.loading == null ? null : this.loading.copy();
        target.unreadableFile = this.unreadableFile;
    }

    public Samples getSamples(final Predicate<PlatformResourceKey> matches, final int granularity, final int limit) {
        final int intervals = this.intervalsPerFrame(granularity);
        final SparseSnapshotHistory<ResourceChangeKey> source = this.historyFor(granularity);
        final int frames = source.frameCount(intervals, limit);
        final long[] inflow = new long[frames];
        final long[] outflow = new long[frames];
        source.forEachRecent(intervals, limit, (index, changes) -> changes.forEach((key, amount) -> {
            if (matches.test(key.resourceKey())) {
                if (key.sign() > 0) {
                    inflow[index] += Math.abs(amount);
                } else {
                    outflow[index] += Math.abs(amount);
                }
            }
        }));
        return new Samples(inflow, outflow);
    }

    /** Sum the last completed ticks, including idle gaps, without constructing graph sample arrays */
    public FlowTotals getRollingFlow(final Predicate<PlatformResourceKey> matches, final int windowTicks) {
        final long inflow = this.tickHistory.sumRecent(windowTicks, key -> key.sign() > 0 && matches.test(key.resourceKey()));
        final long outflow = -this.tickHistory.sumRecent(windowTicks, key -> key.sign() < 0 && matches.test(key.resourceKey()));
        return new FlowTotals(inflow, outflow, Math.min(windowTicks, this.tickHistory.size()));
    }

    private int intervalsPerFrame(final int desiredGranularity) {
        if (desiredGranularity == Granularity.TICK.getTickAmount()) {
            return 1;
        }
        if (desiredGranularity < DATA_GRANULARITY || desiredGranularity % DATA_GRANULARITY != 0) {
            throw new IllegalArgumentException("Flow granularity must be a positive multiple of " + DATA_GRANULARITY);
        }
        return desiredGranularity / DATA_GRANULARITY;
    }

    private SparseSnapshotHistory<ResourceChangeKey> historyFor(final int granularity) {
        return granularity == Granularity.TICK.getTickAmount() ? this.tickHistory : this.history;
    }

    public Map<ResourceChangeGranularityKey, long[]> getGenerationDetails(final PlatformResourceKey itemKey,
                                                                          final int desiredGranularity) {
        final int intervals = this.intervalsPerFrame(desiredGranularity);
        final SparseSnapshotHistory<ResourceChangeKey> source = this.historyFor(desiredGranularity);
        final int frames = source.frameCount(intervals, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
        final long[] inflow = new long[frames];
        final long[] outflow = new long[frames];
        final ResourceChangeKey inflowKey = new ResourceChangeKey(itemKey, (short) +1);
        final ResourceChangeKey outflowKey = new ResourceChangeKey(itemKey, (short) -1);
        // Only look up this resource; gaps are already represented by zero-filled arrays
        source.forEachRecent(intervals, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET, (index, changes) -> {
            inflow[index] += changes.getOrDefault(inflowKey, 0L);
            outflow[index] += changes.getOrDefault(outflowKey, 0L);
        });
        for (int i = 0; i < outflow.length; i++) {
            outflow[i] = Math.abs(outflow[i]);
        }
        final Map<ResourceChangeGranularityKey, long[]> result = new HashMap<>();
        result.put(new ResourceChangeGranularityKey(itemKey, (short) +1, desiredGranularity), inflow);
        result.put(new ResourceChangeGranularityKey(itemKey, (short) -1, desiredGranularity), outflow);
        final long recordedTicks = Math.min(source.size(), (long) frames * intervals) * (desiredGranularity == 1 ? 1 : DATA_GRANULARITY);
        result.put(new ResourceChangeGranularityKey(itemKey, FlowEstimate.DURATION_SIGN, desiredGranularity), new long[]{recordedTicks});
        return result;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(final int desiredGranularity) {
        final Map<ResourceChangeKey, Long> result = new HashMap<>();
        // The resource list needs just the newest frame, not the complete graph window
        this.historyFor(desiredGranularity).forEachRecent(this.intervalsPerFrame(desiredGranularity), 1,
            (index, changes) -> changes.forEach((key, amount) -> result.merge(key, amount, Long::sum)));
        return result;
    }

    static final class Restore {
        final FlowSnapshotData data = new FlowSnapshotData();
        final HistoryCursor seconds;
        final HistoryCursor ticks;

        Restore(final CompoundTag metadata, final Map<ResourceChangeKey, Long> pending, final Map<ResourceChangeKey, Long> tickPending) {
            final int version = metadata.getIntOr("format_version", 0);
            if (version != FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported flow history format: " + version);
            }
            final int intervals = metadata.getIntOr("interval_count", 0);
            this.seconds = new HistoryCursor(this.data.history, intervals, MAX_COLLECTION_SNAPSHOTS);
            final int tickCount = Math.clamp(metadata.getIntOr("tick_count", 0), 0, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
            this.ticks = new HistoryCursor(this.data.tickHistory, tickCount, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
            this.data.pending.putAll(pending);
            this.data.tickPending.putAll(tickPending);
            this.data.pendingTicks = Math.clamp(metadata.getIntOr("pending_ticks", 0), 0, DATA_GRANULARITY - 1);
            if (intervals > MAX_COLLECTION_SNAPSHOTS) {
                this.data.setDirty();
            }
        }

        FlowSnapshotData finish() {
            this.seconds.finish();
            this.ticks.finish();
            return this.data;
        }
    }

    static final class HistoryCursor {
        private final SparseSnapshotHistory<ResourceChangeKey> history;
        private final int count;
        private final int retainedStart;
        private int cursor;

        private HistoryCursor(final SparseSnapshotHistory<ResourceChangeKey> history, final int count, final int capacity) {
            if (count < 0) {
                throw new IllegalArgumentException("Negative flow history interval count");
            }
            this.history = history;
            this.count = count;
            this.retainedStart = Math.max(0, count - capacity);
            this.cursor = this.retainedStart;
        }

        boolean retains(final int offset) {
            return offset >= this.retainedStart;
        }

        void append(final int offset, final Map<ResourceChangeKey, Long> deltas) {
            if (offset < this.cursor || offset >= this.count) {
                throw new IllegalArgumentException("Invalid flow history snapshot offset: " + offset);
            }
            this.history.advanceEmpty(offset - this.cursor);
            this.history.recordOwned(deltas);
            this.cursor = offset + 1;
        }

        private void finish() {
            this.history.advanceEmpty(this.count - this.cursor);
        }
    }

    private record Loading(CompletableFuture<FlowSnapshotData> original, Supplier<Timing> timing, List<Map<ResourceChangeKey, Long>> ticks) {
        private Loading copy() {
            return new Loading(this.original, this.timing, new ArrayList<>(this.ticks));
        }
    }

    record Timing(int intervals, int pendingTicks) {
        static Timing from(final CompoundTag metadata) {
            return new Timing(Math.clamp(metadata.getIntOr("interval_count", 0), 0, MAX_COLLECTION_SNAPSHOTS),
                Math.clamp(metadata.getIntOr("pending_ticks", 0), 0, DATA_GRANULARITY - 1));
        }
    }

    public record Samples(long[] inflow, long[] outflow) {
    }

    public record FlowTotals(long inflow, long outflow, int recordedTicks) {
    }
}
