package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.api.FlowScopeStorageListener;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;
import com.ultramega.refinedflowanalytics.util.TickScheduler;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import com.refinedmods.refinedstorage.common.util.PlatformUtil;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class FlowScopeBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowScopeBlockEntity.FlowScopeNetworkNode> {
    private static final String FACTORY_ID_TAG = "FactoryId";

    public int tagFactoryId;

    private TickScheduler saveScheduler = new TickScheduler(20);
    private FlowSnapshotData snapshotData;

    public FlowScopeBlockEntity(final BlockPos position, final BlockState state) {
        super(ModBlockEntities.FLOW_SCOPE.get(), position, state, new FlowScopeNetworkNode(100));
        this.setFactoryId();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            this.snapshotData = FlowSnapshotData.get((ServerLevel) this.getLevel(), this.tagFactoryId);
            this.saveScheduler = new TickScheduler(this.snapshotData.dataGranularity);
        }
    }

    @Override
    public Component getName() {
        return Component.literal("Flow Scope");
    }

    @Override
    public void activenessChanged(final boolean newActive) {
        super.activenessChanged(newActive);
        PlatformUtil.sendBlockUpdateToClient(this.level, this.worldPosition);
    }

    public boolean isActive() {
        return this.mainNetworkNode.isActive();
    }

    @Override
    protected InWorldNetworkNodeContainer createMainContainer(final FlowScopeNetworkNode networkNode) {
        return RefinedStorageApi.INSTANCE.createNetworkNodeContainer(this, networkNode)
            .priority(10)
            .connectionStrategy(new SimpleConnectionStrategy(this.getBlockPos()))
            .build();
    }

    private Optional<StorageNetworkComponent> getStorageNetworkComponent() {
        final Network network = this.mainNetworkNode.getNetwork();
        if (network == null) {
            return Optional.empty();
        }
        return Optional.of(network.getComponent(StorageNetworkComponent.class));
    }

    @Override
    public void doWork() {
        if (this.mainNetworkNode.isActive() && this.saveScheduler.shouldRun() && this.mainNetworkNode.networkChangeListener != null) {
            this.snapshotData.recordSnapshot(this.mainNetworkNode.networkChangeListener.flushDeltaChange());
        }
        this.ticker.tick(this.mainNetworkNode);
    }

    public Map<ResourceChangeKey, Long> getLastSnapshotAggregated(final int granularity) {
        return this.snapshotData.getLastSnapshotAggregated(granularity);
    }

    public Map<ResourceChangeGranularityKey, long[]> getDetailedSnapshot(final PlatformResourceKey itemKey, final int granularity) {
        final Map<ResourceChangeGranularityKey, long[]> ret = this.snapshotData.getGenerationDetails(itemKey, granularity);
        final long[] itemAmount = new long[1];
        itemAmount[0] = this.getStorageNetworkComponent().map(comp -> comp.get(itemKey)).orElse(0L);
        ret.put(new ResourceChangeGranularityKey(itemKey, (short) 0, granularity), itemAmount);
        return ret;
    }

    @Override
    public void saveAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        tag.putInt(FACTORY_ID_TAG, this.tagFactoryId);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        if (tag.contains(FACTORY_ID_TAG)) {
            this.tagFactoryId = tag.getInt(FACTORY_ID_TAG);
        }
    }

    public void setFactoryId() {
        this.tagFactoryId = this.hashCode();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider lookupProvider) {
        return this.saveWithFullMetadata(lookupProvider);
    }

    public static class FlowScopeNetworkNode extends SimpleNetworkNode {
        @Nullable
        private FlowScopeStorageListener networkChangeListener;

        public FlowScopeNetworkNode(final long energyUsage) {
            super(energyUsage);
        }

        @Override
        public void setNetwork(@Nullable final Network network) {
            if (this.getNetwork() != null && this.networkChangeListener != null) {
                this.getNetwork().getComponent(StorageNetworkComponent.class).removeListener(this.networkChangeListener);
            }

            super.setNetwork(network);
            if (network == null) {
                return;
            }

            this.networkChangeListener = new FlowScopeStorageListener(this.networkChangeListener != null ? this.networkChangeListener.getLastSnapshot() : null);
            network.getComponent(StorageNetworkComponent.class).addListener(this.networkChangeListener);
        }
    }
}
