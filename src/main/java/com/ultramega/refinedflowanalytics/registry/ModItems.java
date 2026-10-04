package com.ultramega.refinedflowanalytics.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class ModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<BlockItem> FLOW_SCOPE = REGISTRY.registerSimpleBlockItem(ModBlocks.FLOW_SCOPE, new Item.Properties());

    private ModItems() {
    }
}
