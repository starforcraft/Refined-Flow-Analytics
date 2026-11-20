package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApiProxy;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.support.registry.PlatformRegistryImpl;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceTypes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSnapshotNbtWriterTest {
    @TempDir
    private Path directory;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final PlatformRegistryImpl<ResourceType> types = new PlatformRegistryImpl<>();
        types.register(Identifier.fromNamespaceAndPath("refinedstorage", "item"), ResourceTypes.ITEM);
        types.register(Identifier.fromNamespaceAndPath("refinedstorage", "fluid"), ResourceTypes.FLUID);
        final RefinedStorageApi api = (RefinedStorageApi) Proxy.newProxyInstance(RefinedStorageApi.class.getClassLoader(),
            new Class<?>[]{RefinedStorageApi.class}, (proxy, method, args) -> {
                if ("getResourceTypeRegistry".equals(method.getName())) {
                    return types;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        ((RefinedStorageApiProxy) RefinedStorageApi.INSTANCE).setDelegate(api);
    }

    @Test
    void streamedFileMatchesExistingSerializerForItemsFluidsComponentsAndPendingChanges() throws IOException {
        final FlowSnapshotData data = new FlowSnapshotData();
        final ItemResource iron = new ItemResource(Items.IRON_INGOT);
        final FluidResource water = new FluidResource(Fluids.WATER);
        final ItemResource namedSword = new ItemResource(Items.DIAMOND_SWORD, DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_NAME, Component.literal("Flow ä☃"))
            .build());
        data.recordChange(iron, Long.MAX_VALUE);
        data.recordChange(iron, -64);
        data.recordChange(water, -1000);
        for (int i = 0; i < 60; i++) {
            data.tick();
        }
        data.recordChange(namedSword, 1);
        data.tick();
        data.recordChange(water, 500);
        final CompoundTag streamed = this.saveAndCompare(data);
        final FlowSnapshotData loaded = FlowSnapshotData.load(streamed);
        assertEquals(data.getSnapshots(), loaded.getSnapshots());
        assertArrayEquals(data.getSamples(key -> true, 1, 209).inflow(), loaded.getSamples(key -> true, 1, 209).inflow());
        assertArrayEquals(data.getSamples(key -> true, 1, 209).outflow(), loaded.getSamples(key -> true, 1, 209).outflow());
        for (int i = 0; i < 20; i++) {
            data.tick();
            loaded.tick();
        }
        assertEquals(data.getSnapshots(), loaded.getSnapshots());
    }

    @Test
    void streamedListsPreserveIdleOffsetsAfterRetention() throws IOException {
        final FlowSnapshotData data = new FlowSnapshotData();
        final ResourceChangeKey key = new ResourceChangeKey(new ItemResource(Items.IRON_INGOT), (short) 1);
        data.recordSnapshot(Map.of(key, 1L));
        data.recordSnapshot(Map.of());
        data.recordSnapshot(Map.of(key, 2L));
        data.recordSnapshot(Map.of());
        data.recordSnapshot(Map.of(key, 3L));
        data.trimToLast(3);
        final FlowSnapshotData loaded = FlowSnapshotData.load(this.saveAndCompare(data));
        assertEquals(data.getSnapshots(), loaded.getSnapshots());
    }

    @Test
    void emptyHistoriesAndDictionaryMatchTheExistingFormat() throws IOException {
        final FlowSnapshotData data = new FlowSnapshotData();
        data.setDirty();
        this.saveAndCompare(data);
    }

    @Test
    void streamingPropagatesIoFailuresFromInsideAHistory() {
        final SparseSnapshotHistory<ResourceChangeKey> history = new SparseSnapshotHistory<>(1);
        history.record(Map.of(new ResourceChangeKey(new ItemResource(Items.IRON_INGOT), (short) 1), 64L));
        final IOException failure = new IOException("Disk full");
        final DataOutputStream output = new DataOutputStream(new OutputStream() {
            private int written;

            @Override
            public void write(final int value) throws IOException {
                if (++this.written >= 110) {
                    throw failure;
                }
            }
        });
        assertEquals(failure, assertThrows(IOException.class, () -> new FlowSnapshotNbtWriter(output)
            .write(3, history, new SparseSnapshotHistory<>(1), Map.of(), Map.of(), 0)));
    }

    @Test
    void readsPlainLegacyFileIncludingUnknownIdsAndExpiredIntervals() throws IOException {
        final ItemResource iron = new ItemResource(Items.IRON_INGOT);
        final FlowSnapshotData before = new FlowSnapshotData();
        before.recordChange(iron, 1234);
        before.recordChange(iron, -5678);
        for (int i = 0; i < 20; i++) {
            before.tick();
        }
        final CompoundTag data = before.save();
        final var snapshots = data.getListOrEmpty("snapshots");
        final CompoundTag retained = snapshots.getCompoundOrEmpty(0).copy();
        final int capacity = 7 * 24 * 60 * 60;
        retained.putInt("offset", capacity + 4);
        retained.getCompoundOrEmpty("deltas").putLong("999999", 99L);
        final CompoundTag dictionary = data.getCompoundOrEmpty("items_map");
        dictionary.put("0", dictionary.get("1").copy());
        retained.getCompoundOrEmpty("deltas").putLong("0", 99L);
        snapshots.add(retained);
        data.putInt("interval_count", capacity + 5);
        final CompoundTag root = new CompoundTag();
        root.put("data", data);
        final Path file = this.directory.resolve("legacy.dat");
        NbtIo.write(root, file);
        final FlowSnapshotData actual = FlowSnapshotNbtReader.read(file);
        assertEquals(capacity, actual.getRecordedIntervals());
        assertTrue(actual.isDirty());
        assertEquals(Map.of(), actual.getSnapshots().getFirst());
        assertEquals(before.getSnapshots().getFirst(), actual.getSnapshots().getLast());
        assertEquals(before.getSnapshots().getFirst(), actual.getLastSnapshotAggregated(20));
    }

    @Test
    void recordsForksAndSavesWhileAnExistingHistoryIsStillLoading() throws IOException {
        final ItemResource iron = new ItemResource(Items.IRON_INGOT);
        final FluidResource water = new FluidResource(Fluids.WATER);
        final Path file = this.directory.resolve("original.dat");
        final Path forkFile = this.directory.resolve("fork.dat");
        final FlowSnapshotData reference = new FlowSnapshotData();
        for (int tick = 0; tick < 37; tick++) {
            reference.recordChange(iron, 101);
            reference.tick();
        }
        reference.recordChange(water, -17);
        reference.save(file);
        FlowSnapshotData.flushPendingSaves();

        final List<Runnable> reads = new ArrayList<>();
        final FlowSnapshotData live = FlowSnapshotData.open(file, reads::add);
        assertEquals(1, reads.size());
        assertFalse(live.pollLoad());
        for (int tick = 0; tick < 250; tick++) {
            if (tick % 6 == 0) {
                reference.recordChange(water, 300);
                live.recordChange(water, 300);
            }
            reference.tick();
            live.tick();
        }
        assertEquals(reference.getRecordedIntervals(), live.getRecordedIntervals());
        reference.recordChange(iron, -55);
        live.recordChange(iron, -55);
        final FlowSnapshotData referenceFork = new FlowSnapshotData();
        final FlowSnapshotData fork = new FlowSnapshotData();
        reference.copyTo(referenceFork);
        live.copyTo(fork);
        referenceFork.recordChange(water, 47);
        fork.recordChange(water, 47);
        referenceFork.tick();
        fork.tick();
        live.save(file);
        fork.save(forkFile);
        // This change is later than the save. Both the queued write and the fork must keep their capture.
        live.recordChange(iron, 222);
        reads.getFirst().run();
        FlowSnapshotData.flushPendingSaves();
        assertTrue(live.pollLoad());
        assertTrue(fork.pollLoad());
        final FlowSnapshotData onDisk = FlowSnapshotNbtReader.read(file);
        final FlowSnapshotData forkOnDisk = FlowSnapshotNbtReader.read(forkFile);
        final FlowSnapshotData atSave = new FlowSnapshotData();
        reference.copyTo(atSave);
        this.assertSameHistory(atSave, onDisk);
        reference.recordChange(iron, 222);
        this.assertSameHistory(reference, live);
        final FlowSnapshotData atForkSave = new FlowSnapshotData();
        referenceFork.copyTo(atForkSave);
        this.assertSameHistory(atForkSave, forkOnDisk);
        this.assertSameHistory(referenceFork, fork);
    }

    @Test
    void unreadableHistoryIsNeverOverwrittenByASaveQueuedDuringLoad() throws IOException {
        final byte[] truncated = {0x1f, (byte) 0x8b, 8};
        final Path file = this.directory.resolve("broken.dat");
        Files.write(file, truncated);
        final List<Runnable> reads = new ArrayList<>();
        final FlowSnapshotData live = FlowSnapshotData.open(file, reads::add);
        live.tick();
        live.save(file);
        reads.getFirst().run();
        FlowSnapshotData.flushPendingSaves();
        live.tick();
        live.save(file);
        FlowSnapshotData.flushPendingSaves();
        assertArrayEquals(truncated, Files.readAllBytes(file));
    }

    private void assertSameHistory(final FlowSnapshotData expected, final FlowSnapshotData actual) {
        assertEquals(expected.getSnapshots(), actual.getSnapshots());
        assertArrayEquals(expected.getSamples(key -> true, 1, 209).inflow(), actual.getSamples(key -> true, 1, 209).inflow());
        assertArrayEquals(expected.getSamples(key -> true, 1, 209).outflow(), actual.getSamples(key -> true, 1, 209).outflow());
        // Complete the partially recorded second and compare pending tick as well as second-level flow.
        for (int tick = 0; tick < 20; tick++) {
            expected.tick();
            actual.tick();
        }
        assertEquals(expected.getSnapshots(), actual.getSnapshots());
        assertEquals(expected.getRollingFlow(key -> true, 20), actual.getRollingFlow(key -> true, 20));
    }

    private CompoundTag saveAndCompare(final FlowSnapshotData data) throws IOException {
        final CompoundTag expected = data.save();
        final Path file = this.directory.resolve("history.dat");
        data.save(file);
        FlowSnapshotData.flushPendingSaves();
        final CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        assertEquals(expected, root.getCompoundOrEmpty("data"));
        this.assertSameHistory(FlowSnapshotData.load(expected), FlowSnapshotNbtReader.read(file));
        assertEquals(SharedConstants.getCurrentVersion().dataVersion().version(), root.getIntOr("DataVersion", 0));
        assertFalse(data.isDirty());
        try (var files = Files.list(this.directory)) {
            assertEquals(1, files.count());
        }
        return root.getCompoundOrEmpty("data");
    }
}
