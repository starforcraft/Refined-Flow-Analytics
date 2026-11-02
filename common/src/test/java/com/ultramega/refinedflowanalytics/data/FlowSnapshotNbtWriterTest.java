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
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

class FlowSnapshotNbtWriterTest {
    @TempDir
    private Path directory;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final PlatformRegistryImpl<ResourceType> types = new PlatformRegistryImpl<>();
        types.register(ResourceLocation.fromNamespaceAndPath("refinedstorage", "item"), ResourceTypes.ITEM);
        types.register(ResourceLocation.fromNamespaceAndPath("refinedstorage", "fluid"), ResourceTypes.FLUID);
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
        final FlowSnapshotData loaded = FlowSnapshotData.load(streamed, null);
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
        final FlowSnapshotData loaded = FlowSnapshotData.load(this.saveAndCompare(data), null);
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

    private CompoundTag saveAndCompare(final FlowSnapshotData data) throws IOException {
        final CompoundTag expected = data.save(new CompoundTag(), null);
        final Path file = this.directory.resolve("history.dat");
        data.save(file.toFile(), null);
        FlowSnapshotData.flushPendingSaves();
        final CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        assertEquals(expected, root.getCompound("data"));
        assertEquals(SharedConstants.getCurrentVersion().getDataVersion().getVersion(), root.getInt("DataVersion"));
        assertFalse(data.isDirty());
        try (var files = Files.list(this.directory)) {
            assertEquals(1, files.count());
        }
        return root.getCompound("data");
    }
}
