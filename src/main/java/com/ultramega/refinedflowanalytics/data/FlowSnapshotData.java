package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.io.File;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class FlowSnapshotData extends SavedData {
    public final int dataGranularity = 20; // TODO: move to config
    private final List<Map<ResourceChangeKey, Long>> snapshots = new ArrayList<>();
    private final int maxCollectionSnapshots = 20 * 60 * 60 * 24 * 7; // TODO: move to config
    private final int maxSnapshotsForClientboundPacket = 209; // moving that to config might be quite tricky

    private final Map<PlatformResourceKey, Integer> itemResolutionMap = new LinkedHashMap<>();

    private final AtomicInteger itemIdCounter = new AtomicInteger(1);

    public static FlowSnapshotData load(final CompoundTag tag, final HolderLookup.Provider provider) {
        final FlowSnapshotData data = new FlowSnapshotData();
        final ListTag snapshotsTag = tag.getList("snapshots", Tag.TAG_COMPOUND);
        final CompoundTag itemsMapTag = tag.getCompound("items_map");
        for (final String id : itemsMapTag.getAllKeys()) {
            final int resourceId = Integer.parseInt(id);
            ResourceCodecs.CODEC.parse(NbtOps.INSTANCE, itemsMapTag.get(id)).result()
                .ifPresent(key -> data.itemResolutionMap.put(key, resourceId));
            data.itemIdCounter.updateAndGet(next -> Math.max(next, resourceId + 1));
        }
        snapshotsTag.stream()
            .map(CompoundTag.class::cast)
            .map(t -> t.getAllKeys().stream()
                // snapshot scope
                .flatMap(itemId ->
                    // item scope
                    ResourceCodecs.CODEC
                        .decode(NbtOps.INSTANCE, itemsMapTag.get(String.valueOf(
                            Math.abs(Integer.parseInt(itemId)))))
                        .result().stream()
                        .map(p -> new ResourceChangeKey(p.getFirst(),
                            t.getLong(itemId) > 0 ? (short) +1
                                : (short) -1))
                        .map(k -> Map.entry(k, t.getLong(itemId))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .forEach(data.snapshots::add);
        return data;
    }

    public static FlowSnapshotData get(final ServerLevel level, final int networkId) {
        final SavedData.Factory<FlowSnapshotData> factory = new SavedData.Factory<>(
            FlowSnapshotData::new, FlowSnapshotData::load);
        final String name = MOD_ID + "_snapshots/" + networkId;
        return level.getDataStorage().computeIfAbsent(factory, name);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider provider) {
        // write items map
        final CompoundTag itemsResolutionMapTag = new CompoundTag();

        this.itemResolutionMap.forEach((key, id) -> ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, key).result()
            .ifPresent(k -> itemsResolutionMapTag.put(String.valueOf(id), k)));
        tag.put("items_map", itemsResolutionMapTag);

        // write snapshots
        final ListTag snapshotsTag = new ListTag();
        for (final Map<ResourceChangeKey, Long> snap : this.snapshots) {
            final CompoundTag snapshotTag = new CompoundTag();
            for (final var entry : snap.entrySet()) {
                final int itemId = this.itemResolutionMap.computeIfAbsent(
                    entry.getKey().resourceKey(),
                    k -> this.itemIdCounter.getAndIncrement());
                final int signedId = itemId * (entry.getKey().sign() > 0 ? 1 : -1);
                snapshotTag.putLong(Integer.toString(signedId), entry.getValue());
            }
            snapshotsTag.add(snapshotTag);
        }
        tag.put("snapshots", snapshotsTag);
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

    /**
     * Record one snapshot of deltas
     */
    public void recordSnapshot(final Map<ResourceChangeKey, Long> deltaMap) {
        // add new items to resolution map
        for (final ResourceChangeKey key : deltaMap.keySet()) {
            final PlatformResourceKey resKey = key.resourceKey();
            this.itemResolutionMap.computeIfAbsent(resKey, k -> this.itemIdCounter.getAndIncrement());
        }

        this.snapshots.add(new HashMap<>(deltaMap));
        this.trimToLast(this.maxCollectionSnapshots);
        this.setDirty();
    }

    /**
     * Keep only the last N snapshots
     */
    public void trimToLast(final int max) {
        final int extra = this.snapshots.size() - max;
        if (extra > 0) {
            this.snapshots.subList(0, extra).clear();
        }
    }

    public List<Map<ResourceChangeKey, Long>> getSnapshots() {
        return Collections.unmodifiableList(this.snapshots);
    }

    private List<Map<ResourceChangeKey, Long>> aggregateSnapshotsToGranularity(final int desiredGranularity) {
        final int granularity = desiredGranularity / this.dataGranularity;
        /*
         * First, we're splitting the list into frames from the end of the list
         * (.reversed()).
         * Then we're summing them all by key.
         * Then we're reversing it back again.
         *
         * The whole double-reverse concept is there to take only the newest frames.
         */
        return IntStream.range(0, (int) Math.ceil((double) this.snapshots.size() / granularity))
            // splitting into frames
            .mapToObj(
                i -> this.snapshots.reversed().subList(
                    i * granularity,
                    Math.min((i + 1) * granularity, this.snapshots.size())))
            .map(frame -> (Map<ResourceChangeKey, Long>) frame.stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Long::sum,
                    LinkedHashMap::new)))
            .limit(this.maxSnapshotsForClientboundPacket)
            .toList()
            .reversed();
    }

    public Map<ResourceChangeGranularityKey, long[]> getGenerationDetails(final PlatformResourceKey itemKey,
                                                                          final int desiredGranularity) {
        LongBuffer incbuf = LongBuffer.allocate(this.maxSnapshotsForClientboundPacket);
        LongBuffer decbuf = LongBuffer.allocate(this.maxSnapshotsForClientboundPacket);
        final List<Map<ResourceChangeKey, Long>> aggregatedSnapshots = this.aggregateSnapshotsToGranularity(
            desiredGranularity);
        for (final Map<ResourceChangeKey, Long> snapshot : aggregatedSnapshots) {
            if (incbuf.hasRemaining()) {
                incbuf.put(snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) +1), 0L));
                decbuf.put(Math.abs(
                    snapshot.getOrDefault(new ResourceChangeKey(itemKey, (short) -1), 0L)));
            }
        }
        if (incbuf.hasRemaining()) {
            incbuf = LongBuffer
                .allocate(incbuf.position())
                .put(incbuf.flip().rewind());
            decbuf = LongBuffer
                .allocate(incbuf.position())
                .put(decbuf.flip().rewind());
        }
        final Map<ResourceChangeGranularityKey, long[]> generationDetails = new HashMap<>();
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) +1, desiredGranularity),
            incbuf.array());
        generationDetails.put(new ResourceChangeGranularityKey(itemKey, (short) -1, desiredGranularity),
            decbuf.array());
        return generationDetails;
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(final int desiredGranularity) {
        final List<Map<ResourceChangeKey, Long>> aggregatedSnapshots = this.aggregateSnapshotsToGranularity(
            desiredGranularity);
        if (aggregatedSnapshots.isEmpty()) {
            return new HashMap<>();
        }
        return aggregatedSnapshots.getLast();
    }
}
