package com.ultramega.refinedflowanalytics.neoforge.datagen.tag;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class BlockTagsProvider extends IntrinsicHolderTagsProvider<Block> {
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("mineable/pickaxe"));

    @SuppressWarnings("deprecation")
    public BlockTagsProvider(final PackOutput packOutput,
                             final CompletableFuture<Provider> providerCompletableFuture) {
        super(packOutput, Registries.BLOCK, providerCompletableFuture, block -> block.builtInRegistryHolder().key(), MOD_ID);
    }

    @Override
    protected void addTags(final Provider provider) {
        this.markAsMineable(ModBlocks.INSTANCE.getFlowGrid());
        this.markAsMineable(ModBlocks.getFlowMonitor());
        this.markAsMineable(ModBlocks.INSTANCE.getFlowDetector());
    }

    private void markAsMineable(final BlockColorMap<?, ?> map) {
        this.tag(MINEABLE).add(map.values().toArray(Block[]::new));
    }

    private void markAsMineable(final Block block) {
        this.tag(MINEABLE).add(block);
    }
}
