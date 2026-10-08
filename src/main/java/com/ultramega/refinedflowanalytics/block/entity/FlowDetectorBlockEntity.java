package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.block.FlowDetectorBlock;
import com.ultramega.refinedflowanalytics.block.network.FlowDetectorConnectionStrategy;
import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.container.FlowDetectorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowMonitorProperties;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.support.AbstractDirectionalBlock;
import com.refinedmods.refinedstorage.common.support.FilterWithFuzzyMode;
import com.refinedmods.refinedstorage.common.support.containermenu.NetworkNodeExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.containermenu.SingleAmountData;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowDetectorBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowHistoryNode>
    implements NetworkNodeExtendedMenuProvider<SingleAmountData> {
    private static final String AMOUNT_TAG = "amount";
    private static final String MODE_TAG = "mode";
    private static final String DIRECTION_TAG = "flow_direction";

    private final FilterWithFuzzyMode filter;
    private double amount;
    private DetectorMode mode = DetectorMode.EQUAL;
    private FlowDirection flowDirection = FlowDirection.INFLOW;

    public FlowDetectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ModBlockEntities.FLOW_DETECTOR.get(), pos, state, new FlowHistoryNode(ServerConfig.INSTANCE.getFlowDetectorEnergyUsage()));
        this.filter = FilterWithFuzzyMode.create(ResourceContainerImpl.createForFilter(1), this::setChanged);
        this.mainNetworkNode.setOwner(this::getLevel, this::setChanged);
    }

    @Override
    protected InWorldNetworkNodeContainer createMainContainer(final FlowHistoryNode networkNode) {
        return RefinedStorageApi.INSTANCE.createNetworkNodeContainer(this, networkNode)
            .connectionStrategy(new FlowDetectorConnectionStrategy(this::getBlockState, this.getBlockPos()))
            .build();
    }

    @Override
    public void doWork() {
        super.doWork();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        final boolean powered = this.shouldEmitSignal();
        final BlockState state = this.getBlockState();
        if (state.getValue(FlowDetectorBlock.POWERED) != powered) {
            this.level.setBlockAndUpdate(this.worldPosition, state.setValue(FlowDetectorBlock.POWERED, powered));
        }
    }

    private boolean shouldEmitSignal() {
        // Check current power as well as node activeness so losing energy always clears the output.
        if (!this.mainNetworkNode.isActive() || !this.calculateActive()) {
            return false;
        }
        final var resource = this.filter.getFilterContainer().getResource(0);
        final var history = this.mainNetworkNode.getHistoryComponent().orElse(null);
        if (resource == null || history == null) {
            return false;
        }
        final var samples = history.getSamples(resource, this.isFuzzyMode(), this.filter.createNormalizer(), Granularity.SECOND.getTickAmount());
        // The history is oldest-first. Never compare an unfinished second or the historical average.
        final int last = samples.inflow().length - 1;
        if (last < 0) {
            return false;
        }
        final long threshold = resource.getResourceType().normalizeAmount(this.amount);
        return this.flowDirection.matches(samples.inflow()[last], samples.outflow()[last], threshold, this.mode);
    }

    public void setAmount(final double amount) {
        if (Double.isFinite(amount)) {
            this.amount = amount;
            this.setChanged();
        }
    }

    public DetectorMode getMode() {
        return this.mode;
    }

    public void setMode(final DetectorMode mode) {
        this.mode = mode;
        this.setChanged();
    }

    public FlowDirection getFlowDirection() {
        return this.flowDirection;
    }

    public void setFlowDirection(final FlowDirection direction) {
        this.flowDirection = direction;
        this.setChanged();
    }

    public boolean isFuzzyMode() {
        return this.filter.isFuzzyMode();
    }

    public void setFuzzyMode(final boolean fuzzy) {
        this.filter.setFuzzyMode(fuzzy);
    }

    @Override
    public void writeConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.writeConfiguration(tag, provider);
        this.filter.save(tag, provider);
        tag.putDouble(AMOUNT_TAG, this.amount);
        tag.putString(MODE_TAG, this.mode.name());
        tag.putString(DIRECTION_TAG, this.flowDirection.name());
    }

    @Override
    public void readConfiguration(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.readConfiguration(tag, provider);
        this.filter.load(tag, provider);
        this.amount = Double.isFinite(tag.getDouble(AMOUNT_TAG)) ? tag.getDouble(AMOUNT_TAG) : 0;
        this.mode = FlowMonitorProperties.read(tag.getString(MODE_TAG), DetectorMode.values(), DetectorMode.EQUAL);
        this.flowDirection = FlowMonitorProperties.read(tag.getString(DIRECTION_TAG), FlowDirection.values(), FlowDirection.INFLOW);
    }

    @Override
    public void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        this.mainNetworkNode.saveHistoryId(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.mainNetworkNode.loadHistoryId(tag);
    }

    @Override
    public SingleAmountData getMenuData() {
        return new SingleAmountData(Optional.empty(), this.amount, ResourceContainerData.of(this.filter.getFilterContainer()));
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, SingleAmountData> getMenuCodec() {
        return SingleAmountData.STREAM_CODEC;
    }

    @Override
    public Component getName() {
        return this.overrideName(createFlowAnalyticsTranslation("block", "flow_detector"));
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new FlowDetectorContainerMenu(id, player, this, this.filter.getFilterContainer());
    }

    @Override
    protected boolean doesBlockStateChangeWarrantNetworkNodeUpdate(final BlockState oldState, final BlockState newState) {
        return AbstractDirectionalBlock.didDirectionChange(oldState, newState);
    }

    @Override
    protected boolean hasRedstoneMode() {
        return false;
    }
}
