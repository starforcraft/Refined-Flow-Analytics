package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.api.StorageSourceChangeContext;
import com.ultramega.refinedflowanalytics.container.FlowScopeMonitorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowScopeMonitorProperties;
import com.ultramega.refinedflowanalytics.data.FlowMonitorHistory;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorFlowText;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.AbstractDirectionalBlock;
import com.refinedmods.refinedstorage.common.support.FilterWithFuzzyMode;
import com.refinedmods.refinedstorage.common.support.containermenu.NetworkNodeExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl;
import com.refinedmods.refinedstorage.common.util.PlatformUtil;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowScopeMonitorBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowScopeMonitorBlockEntity.MonitorNetworkNode>
    implements NetworkNodeExtendedMenuProvider<ResourceContainerData> {
    public static final String ITEM_VISIBILITY_TAG = "item_visibility";
    public static final String FLOW_TEXT_TAG = "flow_text";
    public static final String GRANULARITY_TAG = "granularity";
    public static final String LINE_STYLE_TAG = "line_style";

    private static final String FLOW_IN_TAG = "flow_in";
    private static final String FLOW_OUT_TAG = "flow_out";
    private static final String FLOW_ACTIVE_TAG = "flow_active";
    private static final String FLOW_DISPLAY_TAG = "flow_display";

    private final FlowMonitorHistory history = new FlowMonitorHistory();
    private final FilterWithFuzzyMode filter;
    private MonitorItemVisibility itemVisibility = MonitorItemVisibility.SHOW;
    private MonitorFlowText flowText = MonitorFlowText.NET;
    private Granularity granularity = Granularity.SECOND;
    private LineStyle lineStyle = LineStyle.EXACT;
    private long pendingInflow;
    private long pendingOutflow;
    private int recordingTicks;
    private boolean loadingData;
    private boolean historyInitialized;
    private boolean displayActive;
    private long displayRevision;
    private long[] displayInflow = new long[0];
    private long[] displayOutflow = new long[0];

    public FlowScopeMonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ModBlockEntities.FLOW_MONITOR.get(), pos, state, new MonitorNetworkNode());
        this.filter = FilterWithFuzzyMode.create(ResourceContainerImpl.createForFilter(1), this::filterChanged);
        this.mainNetworkNode.changeListener = this::recordChange;
        this.mainNetworkNode.networkChanged = this::resetPending;
    }

    private void recordChange(final MutableResourceList.OperationResult change) {
        if (!this.mainNetworkNode.isActive() || StorageSourceChangeContext.isSourceChange() || change.change() == 0) {
            return;
        }
        final PlatformResourceKey selected = this.getConfiguredResource();
        final var normalizer = this.filter.createNormalizer();
        if (selected == null || !normalizer.apply(selected).equals(normalizer.apply(change.resource()))) {
            return;
        }
        // Seed before accepting the first live delta so it cannot also appear in the imported history.
        this.initializeHistory();
        if (change.change() > 0) {
            this.pendingInflow += change.change();
        } else {
            this.pendingOutflow -= change.change();
        }
    }

    private void resetPending() {
        this.pendingInflow = 0;
        this.pendingOutflow = 0;
        this.recordingTicks = 0;
    }

    private void filterChanged() {
        if (this.loadingData) {
            return;
        }
        this.history.clear();
        this.historyInitialized = false;
        this.resetPending();
        this.initializeHistory();
        this.setChanged();
        this.updateDisplay(true);
    }

    @Override
    public void doWork() {
        super.doWork();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final boolean active = this.mainNetworkNode.isActive() && this.mainNetworkNode.getNetwork() != null;
        if (!active || this.getConfiguredResource() == null) {
            this.resetPending();
            if (this.displayActive != active) {
                this.updateDisplay(true);
            }
            return;
        }
        this.initializeHistory();
        if (++this.recordingTicks >= 20) {
            this.history.append(this.pendingInflow, this.pendingOutflow);
            this.resetPending();
            this.setChanged();
            this.updateDisplay(false);
        } else if (this.displayActive != active) {
            this.updateDisplay(true);
        }
    }

    private void initializeHistory() {
        final Network network = this.mainNetworkNode.getNetwork();
        final PlatformResourceKey selected = this.getConfiguredResource();
        if (this.historyInitialized || this.loadingData || this.level == null || this.level.isClientSide
            || network == null || selected == null) {
            return;
        }
        FlowSnapshotData source = null;
        for (final var container : network.getComponent(GraphNetworkComponent.class).getContainers()) {
            if (container.getNode() instanceof FlowScopeBlockEntity.FlowScopeNetworkNode node) {
                final FlowSnapshotData candidate = node.getSnapshotData();
                if (candidate != null && (source == null || candidate.getRecordedIntervals() > source.getRecordedIntervals())) {
                    source = candidate;
                }
            }
        }
        if (source != null) {
            final var normalizer = this.filter.createNormalizer();
            final var normalized = normalizer.apply(selected);
            source.copyToMonitor(this.history, resource -> normalized.equals(normalizer.apply(resource)));
        }
        this.historyInitialized = true;
        this.setChanged();
        this.updateDisplay(true);
    }

    private void updateDisplay(final boolean force) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final int seconds = this.granularity.getTickAmount() / 20;
        final long[] inflow = this.history.getInflow(seconds);
        final long[] outflow = this.history.getOutflow(seconds);
        final boolean active = this.mainNetworkNode.isActive() && this.mainNetworkNode.getNetwork() != null;
        if (force || active != this.displayActive || !Arrays.equals(inflow, this.displayInflow) || !Arrays.equals(outflow, this.displayOutflow)) {
            this.displayInflow = inflow;
            this.displayOutflow = outflow;
            this.displayActive = active;
            this.displayRevision++;
            PlatformUtil.sendBlockUpdateToClient(this.level, this.worldPosition);
        }
    }

    public MonitorItemVisibility getItemVisibility() {
        return this.itemVisibility;
    }

    public void setItemVisibility(final MonitorItemVisibility value) {
        if (this.itemVisibility != value) {
            this.itemVisibility = Objects.requireNonNull(value);
            this.displaySettingsChanged();
        }
    }

    public MonitorFlowText getFlowText() {
        return this.flowText;
    }

    public void setFlowText(final MonitorFlowText value) {
        if (this.flowText != value) {
            this.flowText = Objects.requireNonNull(value);
            this.displaySettingsChanged();
        }
    }

    public Granularity getGranularity() {
        return this.granularity;
    }

    public void setGranularity(final Granularity value) {
        if (this.granularity != value) {
            this.granularity = Objects.requireNonNull(value);
            this.displaySettingsChanged();
        }
    }

    public LineStyle getLineStyle() {
        return this.lineStyle;
    }

    public void setLineStyle(final LineStyle value) {
        if (this.lineStyle != value) {
            this.lineStyle = Objects.requireNonNull(value);
            this.displaySettingsChanged();
        }
    }

    private void displaySettingsChanged() {
        this.setChanged();
        this.updateDisplay(true);
    }

    public boolean isFuzzyMode() {
        return this.filter.isFuzzyMode();
    }

    public void setFuzzyMode(final boolean fuzzy) {
        if (fuzzy != this.filter.isFuzzyMode()) {
            this.filter.setFuzzyMode(fuzzy);
        }
    }

    @Nullable
    public PlatformResourceKey getConfiguredResource() {
        return this.filter.getFilterContainer().getResource(0);
    }

    public boolean isDisplayActive() {
        return this.displayActive;
    }

    public long getDisplayRevision() {
        return this.displayRevision;
    }

    public long[] getDisplayInflow() {
        return this.displayInflow.clone();
    }

    public long[] getDisplayOutflow() {
        return this.displayOutflow.clone();
    }

    @Override
    public void writeConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.writeConfiguration(tag, provider);
        this.filter.save(tag, provider);
        tag.putString(ITEM_VISIBILITY_TAG, this.itemVisibility.name());
        tag.putString(FLOW_TEXT_TAG, this.flowText.name());
        tag.putString(GRANULARITY_TAG, this.granularity.name());
        tag.putString(LINE_STYLE_TAG, this.lineStyle.name());
    }

    @Override
    public void readConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        final PlatformResourceKey previous = this.getConfiguredResource();
        final boolean fuzzy = this.isFuzzyMode();
        final boolean wasLoading = this.loadingData;
        this.loadingData = true;
        try {
            super.readConfiguration(tag, provider);
            this.filter.load(tag, provider);
            this.itemVisibility = FlowScopeMonitorProperties.read(tag.getString(ITEM_VISIBILITY_TAG), MonitorItemVisibility.values(), MonitorItemVisibility.SHOW);
            this.flowText = FlowScopeMonitorProperties.read(tag.getString(FLOW_TEXT_TAG), MonitorFlowText.values(), MonitorFlowText.NET);
            this.granularity = FlowScopeMonitorProperties.read(tag.getString(GRANULARITY_TAG), Granularity.values(), Granularity.SECOND);
            this.lineStyle = FlowScopeMonitorProperties.read(tag.getString(LINE_STYLE_TAG), LineStyle.values(), LineStyle.EXACT);
        } finally {
            this.loadingData = wasLoading;
        }
        if (!wasLoading && (!Objects.equals(previous, this.getConfiguredResource()) || fuzzy != this.isFuzzyMode())) {
            this.filterChanged();
        } else if (!wasLoading) {
            this.displaySettingsChanged();
        }
    }

    @Override
    public void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("flow_history_initialized", this.historyInitialized);
        for (final int seconds : FlowMonitorHistory.getIntervals()) {
            tag.putLongArray("flow_history_" + seconds, this.history.saveBucket(seconds));
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        this.loadingData = true;
        try {
            super.loadAdditional(tag, provider);
            if (tag.getBoolean(FLOW_DISPLAY_TAG)) {
                final long[] inflow = tag.getLongArray(FLOW_IN_TAG);
                final long[] outflow = tag.getLongArray(FLOW_OUT_TAG);
                final int length = Math.min(FlowMonitorHistory.CAPACITY, Math.min(inflow.length, outflow.length));
                this.displayInflow = Arrays.copyOf(inflow, length);
                this.displayOutflow = Arrays.copyOf(outflow, length);
            } else {
                for (final int seconds : FlowMonitorHistory.getIntervals()) {
                    this.history.loadBucket(seconds, tag.getLongArray("flow_history_" + seconds));
                }
                this.historyInitialized = tag.getBoolean("flow_history_initialized") || this.history.getInflow(1).length > 0;
                this.displayInflow = this.history.getInflow(this.granularity.getTickAmount() / 20);
                this.displayOutflow = this.history.getOutflow(this.granularity.getTickAmount() / 20);
            }
            this.displayActive = tag.getBoolean(FLOW_ACTIVE_TAG);
            this.displayRevision++;
            this.resetPending();
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        this.writeConfiguration(tag, provider);
        tag.putLongArray(FLOW_IN_TAG, this.displayInflow);
        tag.putLongArray(FLOW_OUT_TAG, this.displayOutflow);
        tag.putBoolean(FLOW_ACTIVE_TAG, this.displayActive);
        tag.putBoolean(FLOW_DISPLAY_TAG, true);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ResourceContainerData getMenuData() {
        return ResourceContainerData.of(this.filter.getFilterContainer());
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, ResourceContainerData> getMenuCodec() {
        return ResourceContainerData.STREAM_CODEC;
    }

    @Override
    public Component getName() {
        return this.overrideName(createFlowAnalyticsTranslation("block", "flow_scope_monitor"));
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new FlowScopeMonitorContainerMenu(id, player, this, this.filter.getFilterContainer());
    }

    @Override
    protected boolean doesBlockStateChangeWarrantNetworkNodeUpdate(final BlockState oldState, final BlockState newState) {
        return AbstractDirectionalBlock.didDirectionChange(oldState, newState);
    }

    public static class MonitorNetworkNode extends SimpleNetworkNode {
        private Consumer<MutableResourceList.OperationResult> changeListener = change -> { };
        private Runnable networkChanged = () -> { };
        private final RootStorageListener storageListener = change -> this.changeListener.accept(change);

        public MonitorNetworkNode() {
            super(Platform.INSTANCE.getConfig().getStorageMonitor().getEnergyUsage());
        }

        @Override
        public void setNetwork(@Nullable final Network network) {
            final Network previous = this.getNetwork();
            if (previous == network) {
                return;
            }
            if (previous != null) {
                previous.getComponent(StorageNetworkComponent.class).removeListener(this.storageListener);
            }
            this.networkChanged.run();
            super.setNetwork(network);
            if (network != null) {
                network.getComponent(StorageNetworkComponent.class).addListener(this.storageListener);
            }
        }
    }
}
