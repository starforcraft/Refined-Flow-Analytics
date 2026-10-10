package com.ultramega.refinedflowanalytics.service;

import com.ultramega.refinedflowanalytics.RefinedFlowAnalyticsMod;
import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.network.MenuState;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SnapshotService {
    private SnapshotService() {
    }

    public static void execute(final LevelAccessor world,
                               final double x,
                               final double y,
                               final double z,
                               final Entity entity,
                               final long requestId,
                               final int granularity,
                               final boolean allStored) {
        if (!(entity instanceof ServerPlayer player) || !(player.containerMenu instanceof FlowGridContainerMenu menu)) {
            return;
        }
        RefinedFlowAnalyticsMod.queueServerWork(player.level().getServer(), () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            final Map<PlatformResourceKey, Map<Short, Long>> data = new HashMap<>();
            if (blockEntity instanceof FlowGridBlockEntity flowGrid && flowGrid.isActive()) {
                final Map<ResourceChangeKey, Long> snapshotMap = flowGrid.getLastSnapshotAggregated(granularity);

                for (final ResourceChangeKey resourceChangeKey : snapshotMap.keySet()) {
                    final PlatformResourceKey resourceKey = resourceChangeKey.resourceKey();
                    data.put(resourceKey, new HashMap<>());
                    final Map<Short, Long> itemChange = data.get(resourceKey);
                    itemChange.put((short) +1, snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) +1), 0L));
                    itemChange.put((short) -1, Math.abs(snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) -1), 0L)));
                }
                if (allStored) {
                    final var storedResources = flowGrid.getStoredResourceKeys();
                    data.keySet().retainAll(storedResources);
                    for (final PlatformResourceKey resourceKey : storedResources) {
                        data.putIfAbsent(resourceKey, Map.of((short) +1, 0L, (short) -1, 0L));
                    }
                }
            }
            menu.sendMenuStateUpdate(player, requestId, new MenuState.Snapshot(granularity, allStored, data));
        });
    }

    public static void executeDetailed(final LevelAccessor world,
                                       final double x,
                                       final double y,
                                       final double z,
                                       final Entity entity,
                                       final long requestId,
                                       final PlatformResourceKey itemKey,
                                       final int granularity) {
        if (!(entity instanceof ServerPlayer player) || !(player.containerMenu instanceof FlowGridContainerMenu menu)) {
            return;
        }
        RefinedFlowAnalyticsMod.queueServerWork(player.level().getServer(), () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            final Map<ResourceChangeGranularityKey, long[]> data = blockEntity instanceof FlowGridBlockEntity flowGrid && flowGrid.isActive()
                ? flowGrid.getDetailedSnapshot(itemKey, granularity) : Map.of();
            menu.sendMenuStateUpdate(player, requestId, new MenuState.DetailedSnapshot(data));
        });
    }
}
