package com.ultramega.refinedflowanalytics.network;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

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

    public void saveHistoryId(final ValueOutput output) {
        if (this.historyId != null) {
            output.store(HISTORY_ID, UUIDUtil.CODEC, this.historyId);
        }
    }

    public void loadHistoryId(final ValueInput input) {
        this.historyId = input.read(HISTORY_ID, UUIDUtil.CODEC).orElse(null);
    }
}
