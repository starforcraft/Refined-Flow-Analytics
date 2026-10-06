package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.api.FlowScopeStorageListener;
import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;
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
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import com.refinedmods.refinedstorage.common.util.PlatformUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowScopeBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowScopeBlockEntity.FlowScopeNetworkNode> implements ExtendedMenuProvider<BlockPos> {
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

    public Set<PlatformResourceKey> getStoredResourceKeys() {
        final Set<PlatformResourceKey> resources = new HashSet<>();
        this.getStorageNetworkComponent().ifPresent(storage -> storage.getAll().forEach(entry -> {
            if (entry.amount() > 0 && entry.resource() instanceof PlatformResourceKey key) {
                resources.add(key);
            }
        }));
        return resources;
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
        super.saveAdditional(tag, provider);
        tag.putInt(FACTORY_ID_TAG, this.tagFactoryId);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
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

    @Override
    public BlockPos getMenuData() {
        return this.worldPosition;
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, BlockPos> getMenuCodec() {
        return (buffer, pos) -> buffer.writeBlockPos(pos);
    }

    @Override
    public Component getName() {
        return createFlowAnalyticsTranslation("block", "flow_scope");
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new FlowScopeContainerMenu(id, inventory, this.worldPosition);
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
