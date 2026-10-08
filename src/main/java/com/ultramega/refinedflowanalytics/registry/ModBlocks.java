package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.FlowDetectorBlock;
import com.ultramega.refinedflowanalytics.block.FlowGridBlock;
import com.ultramega.refinedflowanalytics.block.FlowMonitorBlock;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public final class ModBlocks {
    public static final DyeColor COLOR = DyeColor.LIGHT_BLUE;
    public static final ModBlocks INSTANCE = new ModBlocks();

    public static final DeferredHolder<Block, FlowMonitorBlock> FLOW_MONITOR = DeferredHolder.create(
        Registries.BLOCK, createFlowAnalyticsIdentifier("flow_monitor"));

    private final BlockColorMap<FlowGridBlock, BaseBlockItem> flowGrid = new BlockColorMap<>(
        FlowGridBlock::new,
        createFlowAnalyticsIdentifier("flow_grid"),
        createFlowAnalyticsTranslation("block", "flow_grid"),
        COLOR
    );

    private final BlockColorMap<FlowDetectorBlock, BaseBlockItem> flowDetector = new BlockColorMap<>(
        FlowDetectorBlock::new,
        createFlowAnalyticsIdentifier("flow_detector"),
        createFlowAnalyticsTranslation("block", "flow_detector"),
        DyeColor.LIGHT_BLUE
    );

    private ModBlocks() {
    }

    public BlockColorMap<FlowGridBlock, BaseBlockItem> getFlowGrid() {
        return this.flowGrid;
    }

    public BlockColorMap<FlowDetectorBlock, BaseBlockItem> getFlowDetector() {
        return this.flowDetector;
    }
}
