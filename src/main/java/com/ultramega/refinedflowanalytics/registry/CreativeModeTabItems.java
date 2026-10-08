package com.ultramega.refinedflowanalytics.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public final class CreativeModeTabItems {
    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REFINED_FLOW_ANALYTICS = REGISTRY.register(
        "refined_flow_analytics",
        () -> CreativeModeTab.builder()
            .title(createFlowAnalyticsTranslation("item_group", "refined_flow_analytics"))
            .icon(() -> new ItemStack(ModBlocks.INSTANCE.getFlowGrid().getDefault()))
            .displayItems((parameters, output) -> {
                ModItems.INSTANCE.getFlowGrids().forEach(item -> output.accept(item.get()));
                output.accept(ModItems.FLOW_MONITOR.get());
                ModItems.INSTANCE.getFlowDetectors().forEach(item -> output.accept(item.get()));
            })
            .build()
    );

    private CreativeModeTabItems() {
    }
}
