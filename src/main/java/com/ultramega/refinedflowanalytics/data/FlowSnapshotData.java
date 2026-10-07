package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class FlowSnapshotData extends SavedData {
    private static final int FORMAT_VERSION = 3;
    private static final int DATA_GRANULARITY = 20;
    // Seven days of active recording, measured in snapshots rather than ticks
    private static final int MAX_COLLECTION_SNAPSHOTS = 20 * 60 * 60 * 24 * 7 / DATA_GRANULARITY;
    private static final int MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET = 209;

    private final SparseSnapshotHistory<ResourceChangeKey> history = new SparseSnapshotHistory<>(MAX_COLLECTION_SNAPSHOTS);
    private final Map<ResourceChangeKey, Long> pending = new HashMap<>();
    private int pendingTicks;
    private long revision;

    public static FlowSnapshotData load(final CompoundTag tag, final HolderLookup.Provider provider) {
        final FlowSnapshotData data = new FlowSnapshotData();
        final int version = tag.getInt("format_version");
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported flow history format: " + version);
        }
        final ListTag snapshots = tag.getList("snapshots", Tag.TAG_COMPOUND);
        final CompoundTag dictionary = tag.getCompound("items_map");
        final Map<Integer, Optional<PlatformResourceKey>> resolvedKeys = new HashMap<>();
        final int intervals = tag.getInt("interval_count");
        if (intervals < 0) {
            throw new IllegalArgumentException("Negative flow history interval count");
        }
        final int retainedStart = Math.max(0, intervals - MAX_COLLECTION_SNAPSHOTS);
        int cursor = retainedStart;
        for (int i = 0; i < snapshots.size(); i++) {
            final CompoundTag snapshot = snapshots.getCompound(i);
            final int offset = snapshot.getInt("offset");
            if (offset < retainedStart) {
                continue;
            }
            if (offset < cursor || offset >= intervals) {
                throw new IllegalArgumentException("Invalid flow history snapshot offset: " + offset);
            }
            data.history.advanceEmpty(offset - cursor);
            data.history.record(readDeltas(snapshot.getCompound("deltas"), dictionary, resolvedKeys));
            cursor = offset + 1;
        }
        data.history.advanceEmpty(intervals - cursor);
        data.pending.putAll(readDeltas(tag.getCompound("pending"), dictionary, resolvedKeys));
        data.pendingTicks = Math.clamp(tag.getInt("pending_ticks"), 0, DATA_GRANULARITY - 1);
        if (retainedStart > 0) {
            data.setDirty();
        }
        return data;
    }

    private static Map<ResourceChangeKey, Long> readDeltas(final CompoundTag snapshot,
                                                         final CompoundTag dictionary,
                                                         final Map<Integer, Optional<PlatformResourceKey>> resolvedKeys) {
        final Map<ResourceChangeKey, Long> deltas = new HashMap<>();
        for (final String signedId : snapshot.getAllKeys()) {
            final long value = snapshot.getLong(signedId);
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
        final SavedData.Factory<FlowSnapshotData> factory = new SavedData.Factory<>(FlowSnapshotData::new, FlowSnapshotData::load);
        final String name = MOD_ID + "_network_history/" + networkId;
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory, name);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider provider) {
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
        final CompoundTag dictionary = new CompoundTag();
        resourceIds.forEach((key, id) -> ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, key).result()
            .ifPresent(encoded -> dictionary.put(Integer.toString(id), encoded)));
        tag.putInt("format_version", FORMAT_VERSION);
        tag.putInt("interval_count", this.history.size());
        tag.put("items_map", dictionary);
        tag.put("snapshots", snapshots);
        tag.put("pending", pendingTag);
        tag.putInt("pending_ticks", this.pendingTicks);
        return tag;
    }

    @Override
    public void save(final File file, final HolderLookup.Provider provider) {
        final Path dir = Path.of(file.getParent());
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        super.save(file, provider);
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

    public void recordChange(final PlatformResourceKey resource, final long amount) {
        if (amount != 0) {
            this.pending.merge(new ResourceChangeKey(resource, amount > 0 ? (short) 1 : (short) -1), amount, Long::sum);
            this.setDirty();
        }
    }

    public boolean tick() {
        this.setDirty();
        if (++this.pendingTicks < DATA_GRANULARITY) {
            return false;
        }
        this.pendingTicks = 0;
        this.recordSnapshot(this.pending);
        this.pending.clear();
        return true;
    }

    public void copyTo(final FlowSnapshotData target) {
        target.history.trimToLast(0);
        final int[] cursor = {0};
        this.history.forEachStored((offset, changes) -> {
            target.history.advanceEmpty(offset - cursor[0]);
            target.history.record(changes);
            cursor[0] = offset + 1;
        });
        target.history.advanceEmpty(this.history.size() - cursor[0]);
        target.pending.clear();
        target.pending.putAll(this.pending);
        target.pendingTicks = this.pendingTicks;
        target.revision++;
        target.setDirty();
    }

    public Samples getSamples(final Predicate<PlatformResourceKey> matches, final int granularity, final int limit) {
        final int intervals = this.intervalsPerFrame(granularity);
        final int frames = this.history.frameCount(intervals, limit);
        final long[] inflow = new long[frames];
        final long[] outflow = new long[frames];
        this.history.forEachRecent(intervals, limit, (index, changes) -> changes.forEach((key, amount) -> {
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

    private int intervalsPerFrame(final int desiredGranularity) {
        if (desiredGranularity < DATA_GRANULARITY || desiredGranularity % DATA_GRANULARITY != 0) {
            throw new IllegalArgumentException("Flow granularity must be a positive multiple of " + DATA_GRANULARITY);
        }
        return desiredGranularity / DATA_GRANULARITY;
    }

    public Map<ResourceChangeGranularityKey, long[]> getGenerationDetails(final PlatformResourceKey itemKey,
                                                                          final int desiredGranularity) {
        final int intervals = this.intervalsPerFrame(desiredGranularity);
        final int frames = this.history.frameCount(intervals, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET);
        final long[] inflow = new long[frames];
        final long[] outflow = new long[frames];
        final ResourceChangeKey inflowKey = new ResourceChangeKey(itemKey, (short) +1);
        final ResourceChangeKey outflowKey = new ResourceChangeKey(itemKey, (short) -1);
        // Only look up this resource; gaps are already represented by zero-filled arrays
        this.history.forEachRecent(intervals, MAX_SNAPSHOTS_FOR_CLIENTBOUND_PACKET, (index, changes) -> {
            inflow[index] += changes.getOrDefault(inflowKey, 0L);
            outflow[index] += changes.getOrDefault(outflowKey, 0L);
        });
        for (int i = 0; i < outflow.length; i++) {
            outflow[i] = Math.abs(outflow[i]);
        }
        final Map<ResourceChangeGranularityKey, long[]> result = new HashMap<>();
        result.put(new ResourceChangeGranularityKey(itemKey, (short) +1, desiredGranularity), inflow);
        result.put(new ResourceChangeGranularityKey(itemKey, (short) -1, desiredGranularity), outflow);
        return result;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(final int desiredGranularity) {
        final Map<ResourceChangeKey, Long> result = new HashMap<>();
        // The resource list needs just the newest frame, not the complete graph window
        this.history.forEachRecent(this.intervalsPerFrame(desiredGranularity), 1,
            (index, changes) -> changes.forEach((key, amount) -> result.merge(key, amount, Long::sum)));
        return result;
    }

    public record Samples(long[] inflow, long[] outflow) {
    }
}
