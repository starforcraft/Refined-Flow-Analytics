package com.ultramega.refinedflowanalytics.network;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public sealed interface MenuState {
    StreamCodec<RegistryFriendlyByteBuf, MenuState> STREAM_CODEC = Kind.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().dispatch(MenuState::kind, Kind::codec);

    Kind kind();

    enum Kind {
        SNAPSHOT_REQUEST,
        DETAILED_REQUEST,
        SNAPSHOT,
        DETAILED_SNAPSHOT;

        private static final Kind[] VALUES = values();
        private static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(id -> {
            if (id < 0 || id >= VALUES.length) {
                throw new DecoderException("Unknown menu state type: " + id);
            }
            return VALUES[id];
        }, Kind::ordinal);

        private StreamCodec<? super RegistryFriendlyByteBuf, ? extends MenuState> codec() {
            return switch (this) {
                case SNAPSHOT_REQUEST -> SnapshotRequest.STREAM_CODEC;
                case DETAILED_REQUEST -> DetailedRequest.STREAM_CODEC;
                case SNAPSHOT -> Snapshot.STREAM_CODEC;
                case DETAILED_SNAPSHOT -> DetailedSnapshot.STREAM_CODEC;
            };
        }
    }

    record SnapshotRequest(int granularity, boolean allStored) implements MenuState {
        public static final StreamCodec<ByteBuf, SnapshotRequest> STREAM_CODEC = StreamCodec.composite(
            Codecs.GRANULARITY, SnapshotRequest::granularity,
            ByteBufCodecs.BOOL, SnapshotRequest::allStored,
            SnapshotRequest::new
        );

        public SnapshotRequest {
            Codecs.validateGranularity(granularity);
        }

        @Override
        public Kind kind() {
            return Kind.SNAPSHOT_REQUEST;
        }
    }

    record DetailedRequest(PlatformResourceKey resource, int granularity) implements MenuState {
        public static final StreamCodec<RegistryFriendlyByteBuf, DetailedRequest> STREAM_CODEC = StreamCodec.composite(
            Codecs.RESOURCE, DetailedRequest::resource,
            Codecs.GRANULARITY, DetailedRequest::granularity,
            DetailedRequest::new
        );

        public DetailedRequest {
            Objects.requireNonNull(resource);
            Codecs.validateGranularity(granularity);
        }

        @Override
        public Kind kind() {
            return Kind.DETAILED_REQUEST;
        }
    }

    record Snapshot(int granularity,
                    boolean allStored,
                    Map<PlatformResourceKey, Map<Short, Long>> data) implements MenuState {
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> STREAM_CODEC = StreamCodec.composite(
            Codecs.GRANULARITY, Snapshot::granularity,
            ByteBufCodecs.BOOL, Snapshot::allStored,
            Codecs.SNAPSHOT_DATA, Snapshot::data,
            Snapshot::new
        );

        public Snapshot {
            Codecs.validateGranularity(granularity);
            Objects.requireNonNull(data);
        }

        @Override
        public Kind kind() {
            return Kind.SNAPSHOT;
        }
    }

    record DetailedSnapshot(Map<ResourceChangeGranularityKey, long[]> data) implements MenuState {
        public static final StreamCodec<RegistryFriendlyByteBuf, DetailedSnapshot> STREAM_CODEC = Codecs.DETAILED_DATA.map(DetailedSnapshot::new, DetailedSnapshot::data);

        public DetailedSnapshot {
            Objects.requireNonNull(data);
        }

        @Override
        public Kind kind() {
            return Kind.DETAILED_SNAPSHOT;
        }
    }

    final class Codecs {
        // A detail response contains at most 209 historical samples per direction today
        static final int MAX_SAMPLES = 256;
        private static final int MAX_RESOURCES = 65_536;

        private static final StreamCodec<RegistryFriendlyByteBuf, PlatformResourceKey> RESOURCE =
            NeoForgeStreamCodecs.lazy(() -> ResourceCodecs.STREAM_CODEC);
        private static final StreamCodec<ByteBuf, Integer> GRANULARITY =
            ByteBufCodecs.VAR_INT.map(Codecs::validateGranularity, Codecs::validateGranularity);
        private static final StreamCodec<ByteBuf, Map<Short, Long>> CHANGES =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.SHORT, ByteBufCodecs.VAR_LONG, 2);
        private static final StreamCodec<RegistryFriendlyByteBuf, Map<PlatformResourceKey, Map<Short, Long>>> SNAPSHOT_DATA =
            ByteBufCodecs.map(LinkedHashMap::new, RESOURCE, CHANGES, MAX_RESOURCES);
        static final StreamCodec<ByteBuf, long[]> SAMPLES = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(MAX_SAMPLES)).map(
            values -> values.stream().mapToLong(Long::longValue).toArray(),
            values -> Arrays.stream(values).boxed().toList()
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, ResourceChangeGranularityKey> DETAIL_KEY = StreamCodec.composite(
            RESOURCE, ResourceChangeGranularityKey::resourceKey,
            ByteBufCodecs.SHORT, ResourceChangeGranularityKey::sign,
            GRANULARITY, ResourceChangeGranularityKey::granularity,
            ResourceChangeGranularityKey::new
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceChangeGranularityKey, long[]>> DETAILED_DATA =
            ByteBufCodecs.map(LinkedHashMap::new, DETAIL_KEY, SAMPLES, 4); // Inflow, outflow, stored amount & recorded duration

        private Codecs() {
        }

        private static int validateGranularity(final int ticks) {
            for (final Granularity granularity : Granularity.values()) {
                if (granularity.getTickAmount() == ticks) {
                    return ticks;
                }
            }
            throw new IllegalArgumentException("Unsupported snapshot granularity: " + ticks);
        }
    }
}
