package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.block.network.FlowMonitorNetworkNode;
import com.ultramega.refinedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowMonitorProperties;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

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
import java.util.stream.LongStream;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowMonitorBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowMonitorNetworkNode>
    implements NetworkNodeExtendedMenuProvider<ResourceContainerData> {
    public static final String ITEM_VISIBILITY_TAG = "item_visibility";
    public static final String FLOW_TEXT_TAG = "flow_text";
    public static final String GRANULARITY_TAG = "granularity";
    public static final String LINE_STYLE_TAG = "line_style";

    private static final Codec<long[]> SAMPLES_CODEC = Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);

    private static final String FLOW_IN_TAG = "flow_in";
    private static final String FLOW_OUT_TAG = "flow_out";
    private static final String FLOW_ACTIVE_TAG = "flow_active";
    private static final String FLOW_DISPLAY_TAG = "flow_display";

    private final FilterWithFuzzyMode filter;
    private MonitorItemVisibility itemVisibility = MonitorItemVisibility.SHOW;
    private FlowDirection flowText = FlowDirection.NET;
    private Granularity granularity = Granularity.SECOND;
    private LineStyle lineStyle = LineStyle.EXACT;
    private boolean loadingData;
    @Nullable
    private FlowSnapshotData displaySource;
    private long sourceRevision = -1;
    private boolean displayActive;
    private long displayRevision;
    private long[] displayInflow = new long[0];
    private long[] displayOutflow = new long[0];

    public FlowMonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ModBlockEntities.getFlowMonitor(), pos, state, new FlowMonitorNetworkNode());
        this.filter = FilterWithFuzzyMode.create(ResourceContainerImpl.createForFilter(1), this::filterChanged);
        this.mainNetworkNode.setOwner(this::getLevel, this::setChanged);
    }

    private void filterChanged() {
        if (this.loadingData) {
            return;
        }
        this.setChanged();
        this.updateDisplay(true);
    }

    @Override
    public void doWork() {
        super.doWork();
        this.updateDisplay(false);
    }

    private void updateDisplay(final boolean force) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        final FlowHistoryNetworkComponent component = this.mainNetworkNode.getHistoryComponent().orElse(null);
        final FlowSnapshotData source = component == null ? null : component.getData().orElse(null);
        final long revision = source == null ? -1 : source.getRevision(this.granularity.getTickAmount());
        final boolean active = this.mainNetworkNode.isActive() && this.mainNetworkNode.getNetwork() != null;
        if (!force && active == this.displayActive && source == this.displaySource && revision == this.sourceRevision) {
            return;
        }

        final PlatformResourceKey selected = this.getConfiguredResource();
        final FlowSnapshotData.Samples samples = component == null || selected == null
            ? new FlowSnapshotData.Samples(new long[0], new long[0])
            : component.getSamples(selected, this.isFuzzyMode(), this.filter.createNormalizer(), this.granularity.getTickAmount());
        final long[] inflow = samples.inflow();
        final long[] outflow = samples.outflow();
        this.displaySource = source;
        this.sourceRevision = revision;
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

    public FlowDirection getFlowText() {
        return this.flowText;
    }

    public void setFlowText(final FlowDirection value) {
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
    public void writeConfiguration(final ValueOutput output) {
        super.writeConfiguration(output);
        this.filter.store(output);
        output.putString(ITEM_VISIBILITY_TAG, this.itemVisibility.name());
        output.putString(FLOW_TEXT_TAG, this.flowText.name());
        output.putString(GRANULARITY_TAG, this.granularity.name());
        output.putString(LINE_STYLE_TAG, this.lineStyle.name());
    }

    @Override
    public void readConfiguration(final ValueInput input) {
        final PlatformResourceKey previous = this.getConfiguredResource();
        final boolean fuzzy = this.isFuzzyMode();
        final boolean wasLoading = this.loadingData;
        this.loadingData = true;
        try {
            super.readConfiguration(input);
            this.filter.read(input);
            this.itemVisibility = FlowMonitorProperties.read(input.getStringOr(ITEM_VISIBILITY_TAG, ""), MonitorItemVisibility.values(), MonitorItemVisibility.SHOW);
            this.flowText = FlowMonitorProperties.read(input.getStringOr(FLOW_TEXT_TAG, ""), FlowDirection.values(), FlowDirection.NET);
            this.granularity = FlowMonitorProperties.read(input.getStringOr(GRANULARITY_TAG, ""), Granularity.values(), Granularity.SECOND);
            this.lineStyle = FlowMonitorProperties.read(input.getStringOr(LINE_STYLE_TAG, ""), LineStyle.values(), LineStyle.EXACT);
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
    public void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.mainNetworkNode.saveHistoryId(output);
    }

    @Override
    public void loadAdditional(final ValueInput input) {
        this.loadingData = true;
        try {
            super.loadAdditional(input);
            if (input.getBooleanOr(FLOW_DISPLAY_TAG, false)) {
                final long[] inflow = input.read(FLOW_IN_TAG, SAMPLES_CODEC).orElseGet(() -> new long[0]);
                final long[] outflow = input.read(FLOW_OUT_TAG, SAMPLES_CODEC).orElseGet(() -> new long[0]);
                final int length = Math.min(FlowHistoryNetworkComponent.MONITOR_SAMPLES, Math.min(inflow.length, outflow.length));
                this.displayInflow = Arrays.copyOf(inflow, length);
                this.displayOutflow = Arrays.copyOf(outflow, length);
            } else {
                this.mainNetworkNode.loadHistoryId(input);
                this.displaySource = null;
                this.sourceRevision = -1;
                this.displayInflow = new long[0];
                this.displayOutflow = new long[0];
            }
            this.displayActive = input.getBooleanOr(FLOW_ACTIVE_TAG, false);
            this.displayRevision++;
        } finally {
            this.loadingData = false;
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider provider) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        this.writeConfiguration(output);
        final CompoundTag tag = output.buildResult();
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
        return this.overrideName(createFlowAnalyticsTranslation("block", "flow_monitor"));
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new FlowMonitorContainerMenu(id, player, this, this.filter.getFilterContainer());
    }

    @Override
    protected boolean doesBlockStateChangeWarrantNetworkNodeUpdate(final BlockState oldState, final BlockState newState) {
        return AbstractDirectionalBlock.didDirectionChange(oldState, newState);
    }
}
