package com.ultramega.refinedflowanalytics.neoforge.datagen.tag;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

import static com.ultramega.refinedflowanalytics.registry.ModTags.FLOW_DETECTORS;
import static com.ultramega.refinedflowanalytics.registry.ModTags.FLOW_GRIDS;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class ItemTagsProvider extends BlockTagCopyingItemTagProvider {
    public ItemTagsProvider(final PackOutput packOutput,
                            final CompletableFuture<Provider> registries,
                            final CompletableFuture<TagLookup<Block>> blockTagsProvider) {
        super(packOutput, registries, blockTagsProvider, MOD_ID);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        this.addAllToTag(FLOW_GRIDS,
            ModBlocks.INSTANCE.getFlowGrid().values().stream()
                .map(block -> (Supplier<Item>) block::asItem)
                .toList());
        this.addAllToTag(FLOW_DETECTORS,
            ModBlocks.INSTANCE.getFlowDetector().values().stream()
                .map(block -> (Supplier<Item>) block::asItem)
                .toList());
    }

    private <T extends Item> void addAllToTag(final TagKey<Item> t, final Collection<Supplier<T>> items) {
        this.tag(t).add(items.stream().map(Supplier::get).toArray(Item[]::new));
    }
}
