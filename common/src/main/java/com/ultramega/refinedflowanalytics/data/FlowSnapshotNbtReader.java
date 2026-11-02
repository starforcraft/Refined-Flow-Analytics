package com.ultramega.refinedflowanalytics.data;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;

/** Reads version 3 in either compound-field order, including files with the dictionary after both histories. */
final class FlowSnapshotNbtReader {
    private static final int BUFFER_SIZE = 64 * 1024;

    private final Map<String, ResourceChangeKey> keys = new HashMap<>();
    private final NbtAccounter accounter = NbtAccounter.unlimitedHeap();
    private final FlowSnapshotData.Restore restore;

    private FlowSnapshotNbtReader(final CompoundTag metadata) {
        final CompoundTag dictionary = metadata.getCompound("items_map");
        for (final String id : dictionary.getAllKeys()) {
            if (!isResourceId(id)) {
                continue;
            }
            ResourceCodecs.CODEC.parse(NbtOps.INSTANCE, dictionary.get(id)).result().ifPresent(key -> {
                this.keys.put(id, FlowSnapshotData.changeKey(key, (short) 1));
                this.keys.put("-" + id, FlowSnapshotData.changeKey(key, (short) -1));
            });
        }
        this.restore = new FlowSnapshotData.Restore(metadata, this.deltas(metadata.getCompound("pending")), this.deltas(metadata.getCompound("tick_pending")));
    }

    private static boolean isResourceId(final String id) {
        try {
            final int index = Integer.parseInt(id);
            return index > 0 && id.equals(Integer.toString(index));
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    static FlowSnapshotData.Timing readTiming(final Path file) throws IOException {
        final CollectFields fields = new CollectFields(
            new FieldSelector("data", IntTag.TYPE, "interval_count"), new FieldSelector("data", IntTag.TYPE, "pending_ticks"));
        return FlowSnapshotData.Timing.from(readMetadata(file, fields));
    }

    static FlowSnapshotData read(final Path file) throws IOException {
        // Only collect metadata. Vanilla's normal SavedData loader also materializes every historical delta as a tag.
        final CollectFields metadata = new CollectFields(
            new FieldSelector("data", IntTag.TYPE, "format_version"),
            new FieldSelector("data", IntTag.TYPE, "interval_count"),
            new FieldSelector("data", IntTag.TYPE, "tick_count"),
            new FieldSelector("data", IntTag.TYPE, "pending_ticks"),
            new FieldSelector("data", CompoundTag.TYPE, "items_map"),
            new FieldSelector("data", CompoundTag.TYPE, "pending"),
            new FieldSelector("data", CompoundTag.TYPE, "tick_pending"));
        final FlowSnapshotNbtReader reader = new FlowSnapshotNbtReader(readMetadata(file, metadata));
        try (var input = open(file)) {
            if (input.readUnsignedByte() != Tag.TAG_COMPOUND) {
                throw new IOException("Invalid flow history root");
            }
            input.readUTF();
            reader.readRoot(input);
            if (input.read() != -1) {
                throw new IOException("Trailing data after flow history root");
            }
        }
        return reader.restore.finish();
    }

    static FlowSnapshotData read(final CompoundTag data) {
        final FlowSnapshotNbtReader reader = new FlowSnapshotNbtReader(data);
        reader.readList(data.getList("snapshots", Tag.TAG_COMPOUND), reader.restore.seconds);
        reader.readList(data.getList("tick_snapshots", Tag.TAG_COMPOUND), reader.restore.ticks);
        return reader.restore.finish();
    }

    private static CompoundTag readMetadata(final Path file, final CollectFields fields) throws IOException {
        try (var input = open(file)) {
            NbtIo.parse(input, fields, NbtAccounter.unlimitedHeap());
        }
        if (!(fields.getResult() instanceof CompoundTag root)) {
            throw new IOException("Missing flow history root");
        }
        return root.getCompound("data");
    }

    private static DataInputStream open(final Path file) throws IOException {
        final BufferedInputStream raw = new BufferedInputStream(Files.newInputStream(file), BUFFER_SIZE);
        try {
            raw.mark(2);
            final boolean compressed = raw.read() == 0x1f && raw.read() == 0x8b;
            raw.reset();
            return new DataInputStream(compressed ? new BufferedInputStream(new GZIPInputStream(raw, BUFFER_SIZE), BUFFER_SIZE) : raw);
        } catch (final IOException e) {
            raw.close();
            throw e;
        }
    }

    private void readRoot(final DataInput input) throws IOException {
        int type;
        while ((type = input.readUnsignedByte()) != Tag.TAG_END) {
            final String name = input.readUTF();
            if (type == Tag.TAG_COMPOUND && "data".equals(name)) {
                this.readData(input);
            } else {
                TagTypes.getType(type).skip(input, this.accounter);
            }
        }
    }

    private void readData(final DataInput input) throws IOException {
        int type;
        while ((type = input.readUnsignedByte()) != Tag.TAG_END) {
            final String name = input.readUTF();
            if (type == Tag.TAG_LIST && "snapshots".equals(name)) {
                this.readList(input, this.restore.seconds);
            } else if (type == Tag.TAG_LIST && "tick_snapshots".equals(name)) {
                this.readList(input, this.restore.ticks);
            } else {
                TagTypes.getType(type).skip(input, this.accounter);
            }
        }
    }

    private void readList(final DataInput input, final FlowSnapshotData.HistoryCursor cursor) throws IOException {
        final int type = input.readUnsignedByte();
        final int size = input.readInt();
        if (size < 0 || (size > 0 && type != Tag.TAG_COMPOUND)) {
            throw new IOException("Invalid flow history snapshot list");
        }
        for (int i = 0; i < size; i++) {
            this.readSnapshot(input, cursor);
        }
    }

    private void readList(final ListTag list, final FlowSnapshotData.HistoryCursor cursor) {
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final int offset = entry.getInt("offset");
            if (cursor.retains(offset)) {
                cursor.append(offset, this.deltas(entry.getCompound("deltas")));
            }
        }
    }

    private void readSnapshot(final DataInput input, final FlowSnapshotData.HistoryCursor cursor) throws IOException {
        int offset = -1;
        Map<ResourceChangeKey, Long> changes = Map.of();
        int type;
        while ((type = input.readUnsignedByte()) != Tag.TAG_END) {
            final String name = input.readUTF();
            if (type == Tag.TAG_INT && "offset".equals(name)) {
                offset = input.readInt();
            } else if (type == Tag.TAG_COMPOUND && "deltas".equals(name) && (offset == -1 || cursor.retains(offset))) {
                changes = this.deltas(input);
            } else {
                TagTypes.getType(type).skip(input, this.accounter);
            }
        }
        if (offset < 0) {
            throw new IOException("Missing or negative flow history snapshot offset");
        }
        if (cursor.retains(offset)) {
            cursor.append(offset, changes);
        }
    }

    private Map<ResourceChangeKey, Long> deltas(final DataInput input) throws IOException {
        final Object2LongOpenHashMap<ResourceChangeKey> changes = new Object2LongOpenHashMap<>(2);
        int type;
        while ((type = input.readUnsignedByte()) != Tag.TAG_END) {
            final ResourceChangeKey key = this.keys.get(input.readUTF());
            if (type == Tag.TAG_LONG) {
                final long amount = input.readLong();
                if (key != null && amount != 0) {
                    changes.put(key, amount);
                }
            } else if (key != null && type >= Tag.TAG_BYTE && type <= Tag.TAG_DOUBLE) {
                final long amount = ((NumericTag) TagTypes.getType(type).load(input, this.accounter)).getAsLong();
                if (amount != 0) {
                    changes.put(key, amount);
                }
            } else {
                TagTypes.getType(type).skip(input, this.accounter);
            }
        }
        return changes;
    }

    private Map<ResourceChangeKey, Long> deltas(final CompoundTag tag) {
        final Object2LongOpenHashMap<ResourceChangeKey> changes = new Object2LongOpenHashMap<>(tag.size());
        for (final String name : tag.getAllKeys()) {
            final ResourceChangeKey key = this.keys.get(name);
            final long amount = tag.getLong(name);
            if (key != null && amount != 0) {
                changes.put(key, amount);
            }
        }
        return changes;
    }
}
