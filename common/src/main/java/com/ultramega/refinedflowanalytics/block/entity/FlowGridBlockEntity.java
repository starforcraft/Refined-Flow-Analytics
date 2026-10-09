package com.ultramega.refinedflowanalytics.block.entity;

import com.ultramega.refinedflowanalytics.block.network.FlowGridNetworkNode;
import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.SimpleConnectionStrategy;
import com.refinedmods.refinedstorage.common.util.PlatformUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

public class FlowGridBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<FlowGridNetworkNode> implements ExtendedMenuProvider<BlockPos> {
    public FlowGridBlockEntity(final BlockPos position, final BlockState state) {
        super(ModBlockEntities.getFlowGrid(), position, state, new FlowGridNetworkNode());
        this.mainNetworkNode.setOwner(this::getLevel, this::setChanged);
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
    protected InWorldNetworkNodeContainer createMainContainer(final FlowGridNetworkNode networkNode) {
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
        return this.getSnapshotData().map(data -> data.getLastSnapshotAggregated(granularity)).orElseGet(Map::of);
    }

    public Map<ResourceChangeGranularityKey, long[]> getDetailedSnapshot(final PlatformResourceKey itemKey, final int granularity) {
        final Map<ResourceChangeGranularityKey, long[]> ret = this.getSnapshotData()
            .map(data -> data.getGenerationDetails(itemKey, granularity)).orElseGet(HashMap::new);
        final long[] itemAmount = new long[1];
        itemAmount[0] = this.getStorageNetworkComponent().map(comp -> comp.get(itemKey)).orElse(0L);
        ret.put(new ResourceChangeGranularityKey(itemKey, (short) 0, granularity), itemAmount);
        return ret;
    }

    private Optional<FlowSnapshotData> getSnapshotData() {
        return this.mainNetworkNode.getHistoryComponent().flatMap(FlowHistoryNetworkComponent::getData);
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
        return this.overrideName(createFlowAnalyticsTranslation("block", "flow_grid"));
    }

    @Override
    public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
        return new FlowGridContainerMenu(id, inventory, this.worldPosition);
    }
}
