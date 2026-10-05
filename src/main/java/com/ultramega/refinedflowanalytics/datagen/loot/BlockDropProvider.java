package com.ultramega.refinedflowanalytics.datagen.loot;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;

public final class BlockDropProvider extends BlockLootSubProvider {
    public BlockDropProvider(final HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override
    protected void generate() {
        ModBlocks.INSTANCE.getFlowScope().forEach((color, id, block) -> this.drop(block.get()));
    }

    private void drop(final Block block) {
        this.add(block, this.createSingleItemTable(block)
            .apply(copyName()));
    }

    private static CopyComponentsFunction.Builder copyName() {
        return CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
            .include(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.INSTANCE.getFlowScope().values().stream()
            .map(block -> (Block) block).toList();
    }
}
