package com.ultramega.refinedflowanalytics.network;

import com.ultramega.refinedflowanalytics.api.StorageSourceChangeContext;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.NetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;

public final class FlowHistoryNetworkComponent implements NetworkComponent {
    public static final int MONITOR_SAMPLES = 200;

    private static final Set<FlowHistoryNetworkComponent> ACTIVE = new HashSet<>();
    private static final Map<UUID, FlowHistoryNetworkComponent> OWNERS = new HashMap<>();

    private final Network network;
    private final Set<FlowHistoryNode> nodes = new HashSet<>();
    private final RootStorageListener listener = this::changed;
    private final Map<Query, FlowSnapshotData.Samples> queries = new HashMap<>();
    private final Map<RollingQuery, FlowSnapshotData.FlowTotals> rollingQueries = new HashMap<>();

    @Nullable
    private FlowSnapshotData data;
    @Nullable
    private UUID historyId;
    private boolean resolve = true;
    private boolean listening;
    private boolean recording;

    public FlowHistoryNetworkComponent(final Network network) {
        this.network = network;
    }

    @Override
    public void onContainerAdded(final NetworkNodeContainer container) {
        if (container.getNode() instanceof FlowHistoryNode node) {
            this.nodes.add(node);
            this.resolve = true;
            ACTIVE.add(this);
        }
    }

    @Override
    public void onContainerRemoved(final NetworkNodeContainer container) {
        if (container.getNode() instanceof FlowHistoryNode node) {
            this.nodes.remove(node);
            if (this.nodes.isEmpty()) {
                ACTIVE.remove(this);
                this.stopListening();
            }
        }
    }

    @Override
    public void onNetworkRemoved() {
        ACTIVE.remove(this);
        this.stopListening();
        if (this.historyId != null) {
            OWNERS.remove(this.historyId, this);
        }
        this.nodes.clear();
        this.queries.clear();
        this.rollingQueries.clear();
    }

    @Override
    public void onNetworkMergedWith(final Network newMainNetwork) {
        newMainNetwork.getComponent(FlowHistoryNetworkComponent.class).resolve = true;
        this.onNetworkRemoved();
    }

    @Override
    public void onNetworkSplit(final Set<Network> networks) {
        networks.forEach(branch -> branch.getComponent(FlowHistoryNetworkComponent.class).resolve = true);
    }

    public static void tickAll() {
        final var components = new ArrayList<>(ACTIVE);
        components.forEach(FlowHistoryNetworkComponent::resolveHistory);
        components.forEach(FlowHistoryNetworkComponent::tick);
    }

    public static void clear() {
        new ArrayList<>(ACTIVE).forEach(FlowHistoryNetworkComponent::stopListening);
        ACTIVE.clear();
        OWNERS.clear();
    }

    public Optional<FlowSnapshotData> getData() {
        this.resolveHistory();
        return Optional.ofNullable(this.data);
    }

    public long getRevision() {
        return this.getData().map(FlowSnapshotData::getRevision).orElse(-1L);
    }

    public FlowSnapshotData.Samples getSamples(final PlatformResourceKey resource,
                                               final boolean fuzzy,
                                               final UnaryOperator<ResourceKey> normalizer,
                                               final int granularity) {
        this.resolveHistory();
        if (this.data == null) {
            return new FlowSnapshotData.Samples(new long[0], new long[0]);
        }
        return this.queries.computeIfAbsent(new Query(resource, fuzzy, granularity), key -> {
            final ResourceKey normalized = normalizer.apply(resource);
            return this.data.getSamples(candidate -> normalized.equals(normalizer.apply(candidate)), granularity, MONITOR_SAMPLES);
        });
    }

    public FlowSnapshotData.FlowTotals getRollingFlow(final PlatformResourceKey resource,
                                                      final boolean fuzzy,
                                                      final UnaryOperator<ResourceKey> normalizer,
                                                      final int windowTicks) {
        this.resolveHistory();
        if (this.data == null) {
            return new FlowSnapshotData.FlowTotals(0, 0, 0);
        }
        return this.rollingQueries.computeIfAbsent(new RollingQuery(resource, fuzzy, windowTicks), key -> {
            final ResourceKey normalized = normalizer.apply(resource);
            return this.data.getRollingFlow(candidate -> normalized.equals(normalizer.apply(candidate)), windowTicks);
        });
    }

    private void resolveHistory() {
        if (!this.resolve || this.nodes.isEmpty()) {
            return;
        }
        final ServerLevel level = this.nodes.stream().map(FlowHistoryNode::getServerLevel)
            .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (level == null) {
            return;
        }

        UUID selectedId = this.historyId;
        FlowSnapshotData selected = this.data;
        // If multiple recorders or reunited branches describe overlapping pasts: choose one, never add them
        for (final FlowHistoryNode node : this.nodes) {
            final UUID id = node.getHistoryId();
            if (id != null && !id.equals(selectedId)) {
                final FlowSnapshotData candidate = FlowSnapshotData.get(level, id);
                if (selected == null || candidate.getRecordedIntervals() > selected.getRecordedIntervals()
                    || (candidate.getRecordedIntervals() == selected.getRecordedIntervals()
                    && selectedId != null && id.compareTo(selectedId) < 0)) {
                    selected = candidate;
                    selectedId = id;
                }
            }
        }

        final FlowHistoryNetworkComponent owner = selectedId == null ? null : OWNERS.get(selectedId);
        if (selectedId == null || (owner != null && owner != this)) {
            selectedId = UUID.randomUUID();
            final FlowSnapshotData fork = FlowSnapshotData.get(level, selectedId);
            if (selected != null) {
                selected.copyTo(fork);
            }
            selected = fork;
        }

        if (this.historyId != null) {
            OWNERS.remove(this.historyId, this);
        }
        final UUID resolvedId = selectedId;
        this.historyId = resolvedId;
        this.data = selected;
        OWNERS.put(resolvedId, this);
        this.nodes.forEach(node -> node.setHistoryId(resolvedId));
        this.resolve = false;
        this.queries.clear();
        this.rollingQueries.clear();
        if (!this.listening) {
            this.network.getComponent(StorageNetworkComponent.class).addListener(this.listener);
            this.listening = true;
        }
        this.recording = this.nodes.stream().anyMatch(FlowHistoryNode::isActive);
    }

    private void changed(final MutableResourceList.OperationResult change) {
        if (this.recording && this.data != null && change.change() != 0
            && !StorageSourceChangeContext.isSourceChange() && change.resource() instanceof PlatformResourceKey resource) {
            this.data.recordChange(resource, change.change());
        }
    }

    private void tick() {
        this.recording = this.nodes.stream().anyMatch(FlowHistoryNode::isActive);
        if (this.recording && this.data != null) {
            this.queries.keySet().removeIf(query -> query.granularity() == Granularity.TICK.getTickAmount());
            if (this.data.tick()) {
                this.queries.clear();
            }
            // All detectors querying the same resource share one rolling sum per recorded tick
            this.rollingQueries.clear();
        }
    }

    private void stopListening() {
        if (this.listening) {
            this.network.getComponent(StorageNetworkComponent.class).removeListener(this.listener);
            this.listening = false;
        }
        this.recording = false;
    }

    private record Query(PlatformResourceKey resource, boolean fuzzy, int granularity) {
    }

    private record RollingQuery(PlatformResourceKey resource, boolean fuzzy, int windowTicks) {
    }
}
