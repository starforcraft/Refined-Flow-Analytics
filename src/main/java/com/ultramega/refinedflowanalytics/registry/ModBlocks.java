package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.FlowScopeBlock;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class ModBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredBlock<Block> FLOW_SCOPE = REGISTRY.register("flow_scope", FlowScopeBlock::new);

    private ModBlocks() {
    }
}
