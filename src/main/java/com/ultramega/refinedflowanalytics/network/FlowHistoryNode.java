package com.ultramega.refinedflowanalytics.network;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class FlowHistoryNode extends SimpleNetworkNode {
    private static final String HISTORY_ID = "FlowHistoryId";

    private Supplier<Level> levelSupplier = () -> null;
    private Runnable markDirty = () -> { };
    @Nullable
    private UUID historyId;

    public FlowHistoryNode(final long energyUsage) {
        super(energyUsage);
    }

    public void setOwner(final Supplier<Level> levelSupplier, final Runnable markDirty) {
        this.levelSupplier = levelSupplier;
        this.markDirty = markDirty;
    }

    @Nullable
    public ServerLevel getServerLevel() {
        return this.levelSupplier.get() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    @Nullable
    public UUID getHistoryId() {
        return this.historyId;
    }

    public void setHistoryId(final UUID id) {
        if (!id.equals(this.historyId)) {
            this.historyId = id;
            this.markDirty.run();
        }
    }

    public Optional<FlowHistoryNetworkComponent> getHistoryComponent() {
        return this.getNetwork() == null ? Optional.empty() : Optional.of(this.getNetwork().getComponent(FlowHistoryNetworkComponent.class));
    }

    public void saveHistoryId(final CompoundTag tag) {
        if (this.historyId != null) {
            tag.putUUID(HISTORY_ID, this.historyId);
        }
    }

    public void loadHistoryId(final CompoundTag tag) {
        this.historyId = tag.hasUUID(HISTORY_ID) ? tag.getUUID(HISTORY_ID) : null;
    }
}
