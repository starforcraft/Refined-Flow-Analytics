package com.ultramega.refinedflowanalytics.neoforge.datagen.recipe;

import com.ultramega.refinedflowanalytics.registry.ContentIds;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.content.Items;
import com.refinedmods.refinedstorage.common.misc.ProcessorItem;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;

public final class MainRecipeProvider extends RecipeProvider {
    public MainRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        final var items = this.registries.lookupOrThrow(Registries.ITEM);
        final var advancedProcessor = Items.INSTANCE.getProcessor(ProcessorItem.Type.ADVANCED);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModBlocks.INSTANCE.getFlowGrid().getDefault())
            .pattern("PAG")
            .pattern("EMG")
            .pattern("PAG")
            .define('P', advancedProcessor)
            .define('A', Blocks.INSTANCE.getDetector().getDefault())
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('E', Items.INSTANCE.getQuartzEnrichedIron())
            .define('M', Blocks.INSTANCE.getStorageMonitor())
            .unlockedBy("has_advanced_processor", has(advancedProcessor))
            .save(output, recipeKey(ContentIds.FLOW_GRID));

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ModBlocks.getFlowMonitor())
            .requires(Blocks.INSTANCE.getStorageMonitor())
            .requires(advancedProcessor)
            .unlockedBy("has_storage_monitor", has(Blocks.INSTANCE.getStorageMonitor()))
            .save(output, recipeKey(ContentIds.FLOW_MONITOR));

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ModBlocks.INSTANCE.getFlowDetector().getDefault())
            .requires(Blocks.INSTANCE.getDetector().getDefault())
            .requires(advancedProcessor)
            .unlockedBy("has_detector", has(Blocks.INSTANCE.getDetector().getDefault()))
            .save(output, recipeKey(ContentIds.FLOW_DETECTOR));
    }

    private static ResourceKey<Recipe<?>> recipeKey(final Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries,
                                                      final RecipeOutput output) {
            return new MainRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Refined Flow Analytics recipes";
        }
    }
}
