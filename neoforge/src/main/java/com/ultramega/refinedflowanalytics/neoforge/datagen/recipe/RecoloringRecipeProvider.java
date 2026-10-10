package com.ultramega.refinedflowanalytics.neoforge.datagen.recipe;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.registry.ModTags;

import com.refinedmods.refinedstorage.common.support.RecoloringRecipe;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.Recipe;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class RecoloringRecipeProvider extends RecipeProvider {
    public RecoloringRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        ModBlocks.INSTANCE.getFlowGrid().forEach((color, id, block) ->
            this.output.accept(this.recipeId(color, "grids"),
                RecoloringRecipe.create(ModTags.FLOW_GRIDS, color, block.get(), this.registries), null));

        ModBlocks.INSTANCE.getFlowDetector().forEach((color, id, block) ->
            this.output.accept(this.recipeId(color, "detectors"),
                RecoloringRecipe.create(ModTags.FLOW_DETECTORS, color, block.get(), this.registries), null));
    }

    private ResourceKey<Recipe<?>> recipeId(final DyeColor color, final String suffix) {
        return ResourceKey.create(Registries.RECIPE, createFlowAnalyticsIdentifier("coloring/" + color.getName() + "_" + suffix));
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries,
                                                      final RecipeOutput output) {
            return new RecoloringRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Refined Flow Analytics recoloring recipes";
        }
    }
}
