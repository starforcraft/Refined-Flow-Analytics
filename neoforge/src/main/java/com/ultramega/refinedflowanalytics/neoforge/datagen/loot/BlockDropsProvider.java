package com.ultramega.refinedflowanalytics.neoforge.datagen.loot;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class BlockDropsProvider extends BlockLootSubProvider {
    public BlockDropsProvider(final HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override
    protected void generate() {
        ModBlocks.INSTANCE.getFlowGrid().forEach((color, id, block) -> this.drop(block.get()));
        this.drop(ModBlocks.getFlowMonitor());
        ModBlocks.INSTANCE.getFlowDetector().forEach((color, id, block) -> this.drop(block.get()));
    }

    private void drop(final Block block) {
        this.add(block, this.createSingleItemTable(block)
            .apply(copyName()));
    }

    private static CopyComponentsFunction.Builder copyName() {
        return CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
            .include(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        final List<Block> blocks = new ArrayList<>();
        blocks.addAll(ModBlocks.INSTANCE.getFlowGrid().values());
        blocks.add(ModBlocks.getFlowMonitor());
        blocks.addAll(ModBlocks.INSTANCE.getFlowDetector().values());
        return blocks;
    }
}
