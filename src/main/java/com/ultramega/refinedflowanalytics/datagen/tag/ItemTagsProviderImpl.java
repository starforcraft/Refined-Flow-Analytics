package com.ultramega.refinedflowanalytics.datagen.tag;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import static com.ultramega.refinedflowanalytics.registry.ModTags.FLOW_SCOPES;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class ItemTagsProviderImpl extends ItemTagsProvider {
    public ItemTagsProviderImpl(final PackOutput packOutput,
                                final CompletableFuture<Provider> registries,
                                final TagsProvider<Block> blockTagsProvider,
                                final ExistingFileHelper existingFileHelper) {
        super(packOutput, registries, blockTagsProvider.contentsGetter(), MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        this.addAllToTag(FLOW_SCOPES,
            ModBlocks.INSTANCE.getFlowScope().values().stream()
                .map(block -> (Supplier<Item>) block::asItem)
                .toList());
    }

    private <T extends Item> void addAllToTag(final TagKey<Item> t, final Collection<Supplier<T>> items) {
        this.tag(t).add(items.stream().map(Supplier::get).toArray(Item[]::new)).replace(false);
    }
}
