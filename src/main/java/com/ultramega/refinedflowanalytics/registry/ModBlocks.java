package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.FlowScopeBlock;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

import net.minecraft.world.item.DyeColor;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public final class ModBlocks {
    public static final DyeColor COLOR = DyeColor.LIGHT_BLUE;
    public static final ModBlocks INSTANCE = new ModBlocks();

    private final BlockColorMap<FlowScopeBlock, BaseBlockItem> flowScope = new BlockColorMap<>(
        FlowScopeBlock::new,
        createFlowAnalyticsIdentifier("flow_scope"),
        createFlowAnalyticsTranslation("block", "flow_scope"),
        COLOR
    );

    private ModBlocks() {
    }

    public BlockColorMap<FlowScopeBlock, BaseBlockItem> getFlowScope() {
        return this.flowScope;
    }
}
