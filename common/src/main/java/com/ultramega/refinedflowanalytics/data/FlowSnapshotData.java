package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class FlowSnapshotData extends SavedData {
    private static final int FORMAT_VERSION = 3;
    private static final int DATA_GRANULARITY = 20;
    // Seven days of active recording, measured in snapshots rather than ticks
    private static final int MAX_COLLECTION_SNAPSHOTS = 20 * 60 * 60 * 24 * 7 / DATA_GRANULARITY;
    private static final int MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET = 209;

    private final SparseSnapshotHistory<ResourceChangeKey> history = new SparseSnapshotHistory<>(MAX_COLLECTION_SNAPSHOTS);
    private final SparseSnapshotHistory<ResourceChangeKey> tickHistory = new SparseSnapshotHistory<>(MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
    private final Map<ResourceChangeKey, Long> pending = new HashMap<>();
    private final Map<ResourceChangeKey, Long> tickPending = new HashMap<>();
    private int pendingTicks;
    private long revision;
    private long tickRevision;

    public static FlowSnapshotData load(final CompoundTag tag) {
        final FlowSnapshotData data = new FlowSnapshotData();
        final int version = tag.getIntOr("format_version", 0);
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported flow history format: " + version);
        }
        final ListTag snapshots = tag.getListOrEmpty("snapshots");
        final CompoundTag dictionary = tag.getCompoundOrEmpty("items_map");
        final Map<Integer, Optional<PlatformResourceKey>> resolvedKeys = new HashMap<>();
        final int intervals = tag.getIntOr("interval_count", 0);
        if (intervals < 0) {
            throw new IllegalArgumentException("Negative flow history interval count");
        }
        final int retainedStart = Math.max(0, intervals - MAX_COLLECTION_SNAPSHOTS);
        int cursor = retainedStart;
        for (int i = 0; i < snapshots.size(); i++) {
            final CompoundTag snapshot = snapshots.getCompoundOrEmpty(i);
            final int offset = snapshot.getIntOr("offset", 0);
            if (offset < retainedStart) {
                continue;
            }
            if (offset < cursor || offset >= intervals) {
                throw new IllegalArgumentException("Invalid flow history snapshot offset: " + offset);
            }
            data.history.advanceEmpty(offset - cursor);
            data.history.record(readDeltas(snapshot.getCompoundOrEmpty("deltas"), dictionary, resolvedKeys));
            cursor = offset + 1;
        }
        data.history.advanceEmpty(intervals - cursor);
        data.pending.putAll(readDeltas(tag.getCompoundOrEmpty("pending"), dictionary, resolvedKeys));
        data.pendingTicks = Math.clamp(tag.getIntOr("pending_ticks", 0), 0, DATA_GRANULARITY - 1);
        final int tickCount = Math.clamp(tag.getIntOr("tick_count", 0), 0, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
        final ListTag ticks = tag.getListOrEmpty("tick_snapshots");
        int tickCursor = 0;
        for (int i = 0; i < ticks.size(); i++) {
            final CompoundTag snapshot = ticks.getCompoundOrEmpty(i);
            final int offset = snapshot.getIntOr("offset", 0);
            if (offset < tickCursor || offset >= tickCount) {
                throw new IllegalArgumentException("Invalid tick history snapshot offset: " + offset);
            }
            data.tickHistory.advanceEmpty(offset - tickCursor);
            data.tickHistory.record(readDeltas(snapshot.getCompoundOrEmpty("deltas"), dictionary, resolvedKeys));
            tickCursor = offset + 1;
        }
        data.tickHistory.advanceEmpty(tickCount - tickCursor);
        data.tickPending.putAll(readDeltas(tag.getCompoundOrEmpty("tick_pending"), dictionary, resolvedKeys));
        if (retainedStart > 0) {
            data.setDirty();
        }
        return data;
    }

    private static Map<ResourceChangeKey, Long> readDeltas(final CompoundTag snapshot,
                                                         final CompoundTag dictionary,
                                                         final Map<Integer, Optional<PlatformResourceKey>> resolvedKeys) {
        final Map<ResourceChangeKey, Long> deltas = new HashMap<>();
        for (final String signedId : snapshot.keySet()) {
            final long value = snapshot.getLongOr(signedId, 0);
            if (value == 0) {
                continue;
            }
            final int id = Integer.parseInt(signedId);
            if (id == 0 || id == Integer.MIN_VALUE) {
                continue;
            }
            final Optional<PlatformResourceKey> resource = resolvedKeys.computeIfAbsent(Math.abs(id), key -> {
                final String name = Integer.toString(key);
                return dictionary.contains(name)
                    ? ResourceCodecs.CODEC.parse(NbtOps.INSTANCE, dictionary.get(name)).result()
                    : Optional.empty();
            });
            resource.ifPresent(key -> deltas.put(new ResourceChangeKey(key, id > 0 ? (short) +1 : (short) -1), value));
        }
        return deltas;
    }

    public static FlowSnapshotData get(final ServerLevel level, final UUID networkId) {
        final Codec<FlowSnapshotData> codec = CompoundTag.CODEC.xmap(FlowSnapshotData::load, FlowSnapshotData::save);
        final SavedDataType<FlowSnapshotData> type = new SavedDataType<>(
            createFlowAnalyticsIdentifier("network_history/" + networkId), FlowSnapshotData::new, codec, null);
        return level.getServer().overworld().getDataStorage().computeIfAbsent(type);
    }

    public CompoundTag save() {
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

    public void recordSnapshot(final Map<ResourceChangeKey, Long> deltaMap) {
        if (this.history.record(deltaMap)) {
            this.revision++;
            this.setDirty();
        }
    }

    public void trimToLast(final int max) {
        if (this.history.trimToLast(max)) {
            this.revision++;
            this.setDirty();
        }
    }

    public List<Map<ResourceChangeKey, Long>> getSnapshots() {
        return this.history.asList();
    }

    public int getRecordedIntervals() {
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
            this.pending.merge(new ResourceChangeKey(resource, amount > 0 ? (short) 1 : (short) -1), amount, Long::sum);
            this.tickPending.merge(new ResourceChangeKey(resource, amount > 0 ? (short) 1 : (short) -1), amount, Long::sum);
            this.setDirty();
        }
    }

    public boolean tick() {
        this.setDirty();
        if (this.tickHistory.record(this.tickPending)) {
            this.tickRevision++;
        }
        this.tickPending.clear();
        if (++this.pendingTicks < DATA_GRANULARITY) {
            return false;
        }
        this.pendingTicks = 0;
        this.recordSnapshot(this.pending);
        this.pending.clear();
        return true;
    }

    public void copyTo(final FlowSnapshotData target) {
        copyHistory(this.history, target.history);
        copyHistory(this.tickHistory, target.tickHistory);
        target.tickPending.clear();
        target.tickPending.putAll(this.tickPending);
        target.tickRevision++;
        target.pending.clear();
        target.pending.putAll(this.pending);
        target.pendingTicks = this.pendingTicks;
        target.revision++;
        target.setDirty();
    }

    private static void copyHistory(final SparseSnapshotHistory<ResourceChangeKey> source,
                                    final SparseSnapshotHistory<ResourceChangeKey> target) {
        target.trimToLast(0);
        final int[] cursor = {0};
        source.forEachStored((offset, changes) -> {
            target.advanceEmpty(offset - cursor[0]);
            target.record(changes);
            cursor[0] = offset + 1;
        });
        target.advanceEmpty(source.size() - cursor[0]);
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

    public record Samples(long[] inflow, long[] outflow) {
    }

    public record FlowTotals(long inflow, long outflow, int recordedTicks) {
    }
}
