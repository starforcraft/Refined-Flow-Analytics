package com.ultramega.refinedflowanalytics.datagen.recipe;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.registry.ModTags;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.content.Items;
import com.refinedmods.refinedstorage.common.misc.ProcessorItem;
import com.refinedmods.refinedstorage.common.support.RecoloringRecipe;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.Tags;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class MainRecipeProvider extends RecipeProvider {
    public MainRecipeProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(final RecipeOutput output) {
        final var advancedProcessor = Items.INSTANCE.getProcessor(ProcessorItem.Type.ADVANCED);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INSTANCE.getFlowScope().getDefault())
            .pattern("PAG")
            .pattern("EMG")
            .pattern("PAG")
            .define('P', advancedProcessor)
            .define('A', Blocks.INSTANCE.getDetector().getDefault())
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('E', Items.INSTANCE.getQuartzEnrichedIron())
            .define('M', Blocks.INSTANCE.getStorageMonitor())
            .unlockedBy("has_advanced_processor", has(advancedProcessor))
            .save(output, createFlowAnalyticsIdentifier("flow_scope"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.FLOW_SCOPE_MONITOR.get())
            .requires(Blocks.INSTANCE.getStorageMonitor())
            .requires(advancedProcessor)
            .unlockedBy("has_storage_monitor", has(Blocks.INSTANCE.getStorageMonitor()))
            .save(output, createFlowAnalyticsIdentifier("flow_scope_monitor"));

        ModBlocks.INSTANCE.getFlowScope().forEach((color, id, block) ->
            output.accept(this.recipeId(color, "flow_scopes"),
                RecoloringRecipe.create(ModTags.FLOW_SCOPES, color, block.get()), null));
    }

    private ResourceLocation recipeId(final DyeColor color, final String suffix) {
        return createFlowAnalyticsIdentifier("coloring/" + color.getName() + "_" + suffix);
    }
}
