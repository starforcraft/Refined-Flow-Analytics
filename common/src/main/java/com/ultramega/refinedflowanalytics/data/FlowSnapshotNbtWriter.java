package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/** Writes the existing NBT format without materializing a tag tree for the complete history */
final class FlowSnapshotNbtWriter {
    private final DataOutput output;
    private final Map<PlatformResourceKey, ResourceNames> resources = new LinkedHashMap<>();

    FlowSnapshotNbtWriter(final DataOutput output) {
        this.output = output;
    }

    void write(final int formatVersion,
               final SparseSnapshotHistory<ResourceChangeKey> history,
               final SparseSnapshotHistory<ResourceChangeKey> tickHistory,
               final Map<ResourceChangeKey, Long> pending,
               final Map<ResourceChangeKey, Long> tickPending,
               final int pendingTicks) throws IOException {
        this.startCompound("");
        final CompoundTag version = new CompoundTag();
        NbtUtils.addCurrentDataVersion(version);
        this.writeInt("DataVersion", version.getInt("DataVersion"));
        this.startCompound("data");
        this.writeInt("format_version", formatVersion);
        this.writeInt("interval_count", history.size());
        this.writeHistory("snapshots", history);
        this.writeDeltas("pending", pending);
        this.writeInt("pending_ticks", pendingTicks);
        this.writeInt("tick_count", tickHistory.size());
        this.writeHistory("tick_snapshots", tickHistory);
        this.writeDeltas("tick_pending", tickPending);

        this.writeDictionary();
        this.output.writeByte(Tag.TAG_END);
        this.output.writeByte(Tag.TAG_END);
    }

    private void writeHistory(final String name, final SparseSnapshotHistory<ResourceChangeKey> history) throws IOException {
        this.output.writeByte(Tag.TAG_LIST);
        this.output.writeUTF(name);
        this.output.writeByte(history.storedSize() == 0 ? Tag.TAG_END : Tag.TAG_COMPOUND);
        this.output.writeInt(history.storedSize());
        history.forEachStoredChecked((offset, changes) -> {
            this.writeInt("offset", offset);
            this.writeDeltas("deltas", changes);
            this.output.writeByte(Tag.TAG_END);
        });
    }

    private void writeDeltas(final String name, final Map<ResourceChangeKey, Long> changes) throws IOException {
        this.startCompound(name);
        for (final var entry : changes.entrySet()) {
            final ResourceChangeKey key = entry.getKey();
            ResourceNames names = this.resources.get(key.resourceKey());
            if (names == null) {
                final String id = Integer.toString(this.resources.size() + 1);
                names = new ResourceNames(id, "-" + id);
                this.resources.put(key.resourceKey(), names);
            }
            this.output.writeByte(Tag.TAG_LONG);
            this.output.writeUTF(key.sign() > 0 ? names.positive() : names.negative());
            this.output.writeLong(entry.getValue());
        }
        this.output.writeByte(Tag.TAG_END);
    }

    private void writeDictionary() throws IOException {
        this.startCompound("items_map");
        for (final var entry : this.resources.entrySet()) {
            final Tag encoded = ResourceCodecs.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).result().orElse(null);
            if (encoded != null) {
                this.output.writeByte(encoded.getId());
                this.output.writeUTF(entry.getValue().positive());
                encoded.write(this.output);
            }
        }
        this.output.writeByte(Tag.TAG_END);
    }

    private void startCompound(final String name) throws IOException {
        this.output.writeByte(Tag.TAG_COMPOUND);
        this.output.writeUTF(name);
    }

    private void writeInt(final String name, final int value) throws IOException {
        this.output.writeByte(Tag.TAG_INT);
        this.output.writeUTF(name);
        this.output.writeInt(value);
    }

    private record ResourceNames(String positive, String negative) {
    }
}
