package com.ultramega.refinedflowanalytics.registry;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;

public final class CreativeModeTabItems {
    private CreativeModeTabItems() {
    }

    public static void appendNormal(final Consumer<ItemStack> consumer) {
        consumer.accept(new ItemStack(ModBlocks.INSTANCE.getFlowGrid().getDefault()));
        consumer.accept(new ItemStack(ModItems.getFlowMonitor()));
        consumer.accept(new ItemStack(ModBlocks.INSTANCE.getFlowDetector().getDefault()));
    }

    public static void appendColored(final Consumer<ItemStack> consumer) {
        appendColored(consumer, ModBlocks.INSTANCE.getFlowGrid());
        appendColored(consumer, ModBlocks.INSTANCE.getFlowDetector());
    }

    private static void appendColored(final Consumer<ItemStack> consumer, final BlockColorMap<?, ?> blocks) {
        blocks.forEach((color, id, block) -> {
            if (!blocks.isDefaultColor(color)) {
                consumer.accept(new ItemStack(block.get()));
            }
        });
    }
}
