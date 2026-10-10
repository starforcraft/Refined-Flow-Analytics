package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.FlowDetectorBlock;
import com.ultramega.refinedflowanalytics.block.FlowGridBlock;
import com.ultramega.refinedflowanalytics.block.FlowMonitorBlock;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

import java.util.function.Supplier;

import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;
import static java.util.Objects.requireNonNull;

public final class ModBlocks {
    public static final DyeColor COLOR = DyeColor.LIGHT_BLUE;
    public static final ModBlocks INSTANCE = new ModBlocks();

    @Nullable
    private static Supplier<FlowMonitorBlock> flowMonitor;

    private final BlockColorMap<FlowGridBlock, BaseBlockItem> flowGrid = new BlockColorMap<>(
        FlowGridBlock::new,
        ContentIds.FLOW_GRID,
        createFlowAnalyticsTranslation("block", "flow_grid"),
        COLOR
    );

    private final BlockColorMap<FlowDetectorBlock, BaseBlockItem> flowDetector = new BlockColorMap<>(
        FlowDetectorBlock::new,
        ContentIds.FLOW_DETECTOR,
        createFlowAnalyticsTranslation("block", "flow_detector"),
        DyeColor.LIGHT_BLUE
    );

    private ModBlocks() {
    }

    public static FlowMonitorBlock getFlowMonitor() {
        return requireNonNull(flowMonitor, "Flow Monitor is not registered").get();
    }

    public static void setFlowMonitor(final Supplier<FlowMonitorBlock> supplier) {
        flowMonitor = supplier;
    }

    public BlockColorMap<FlowGridBlock, BaseBlockItem> getFlowGrid() {
        return this.flowGrid;
    }

    public BlockColorMap<FlowDetectorBlock, BaseBlockItem> getFlowDetector() {
        return this.flowDetector;
    }
}
