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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
                               final int granularity,
                               final boolean allStored) {
        if (!(entity instanceof Player player) || !(player.containerMenu instanceof FlowGridContainerMenu menu)) {
            return;
        }
        RefinedFlowAnalyticsMod.queueServerWork(1, () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowGridBlockEntity flowGrid && flowGrid.isActive()) {
                final Map<ResourceChangeKey, Long> snapshotMap = flowGrid.getLastSnapshotAggregated(granularity);
                final Map<PlatformResourceKey, Map<Short, Long>> data = new HashMap<>();

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
                menu.sendMenuStateUpdate(player, new MenuState.Snapshot(granularity, allStored, data));
            }
        });
    }

    public static void executeDetailed(final LevelAccessor world,
                                       final double x,
                                       final double y,
                                       final double z,
                                       final Entity entity,
                                       final PlatformResourceKey itemKey,
                                       final int granularity) {
        if (!(entity instanceof Player player) || !(player.containerMenu instanceof FlowGridContainerMenu menu)) {
            return;
        }
        RefinedFlowAnalyticsMod.queueServerWork(1, () -> {
            if (player.containerMenu != menu || !menu.stillValid(player)) {
                return;
            }
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowGridBlockEntity flowGrid && flowGrid.isActive()) {
                final Map<ResourceChangeGranularityKey, long[]> data = flowGrid.getDetailedSnapshot(itemKey, granularity);
                menu.sendMenuStateUpdate(player, new MenuState.DetailedSnapshot(data));
            }
        });
    }
}
