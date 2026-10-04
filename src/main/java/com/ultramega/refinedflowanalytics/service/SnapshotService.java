package com.ultramega.refinedflowanalytics.service;

import com.ultramega.refinedflowanalytics.RefinedFlowAnalyticsMod;
import com.ultramega.refinedflowanalytics.block.entity.FlowScopeBlockEntity;
import com.ultramega.refinedflowanalytics.container.FlowScopeMenu;
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

    public static void execute(final LevelAccessor world, final double x, final double y, final double z, final Entity entity, final int granularity) {
        RefinedFlowAnalyticsMod.queueServerWork(1, () -> {
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowScopeBlockEntity flowScope && flowScope.isActive()) {
                final Map<ResourceChangeKey, Long> snapshotMap = flowScope.getLastSnapshotAggregated(granularity);
                final Map<PlatformResourceKey, Map<Short, Long>> data = new HashMap<>();

                for (final ResourceChangeKey resourceChangeKey : snapshotMap.keySet()) {
                    final PlatformResourceKey resourceKey = resourceChangeKey.resourceKey();
                    data.put(resourceKey, new HashMap<>());
                    final Map<Short, Long> itemChange = data.get(resourceKey);
                    itemChange.put((short) +1, snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) +1), 0L));
                    itemChange.put((short) -1, Math.abs(snapshotMap.getOrDefault(new ResourceChangeKey(resourceKey, (short) -1), 0L)));
                }
                if (entity instanceof Player player && player.containerMenu instanceof FlowScopeMenu menu) {
                    menu.sendMenuStateUpdate(player, 2, "lastSnapshot", data, true);
                }
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
        RefinedFlowAnalyticsMod.queueServerWork(1, () -> {
            final BlockEntity blockEntity = (world.getBlockEntity(BlockPos.containing(x, y, z)));
            if (blockEntity instanceof FlowScopeBlockEntity flowScope && flowScope.isActive()) {
                final Map<ResourceChangeGranularityKey, long[]> data = flowScope.getDetailedSnapshot(itemKey, granularity);
                if (entity instanceof Player player && player.containerMenu instanceof FlowScopeMenu menu) {
                    menu.sendMenuStateUpdate(player, 3, "detailedFactoryGeneration", data, true);
                }
            }
        });
    }
}
