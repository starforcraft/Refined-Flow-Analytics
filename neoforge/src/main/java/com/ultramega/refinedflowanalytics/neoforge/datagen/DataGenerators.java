package com.ultramega.refinedflowanalytics.neoforge.datagen;

import com.ultramega.refinedflowanalytics.neoforge.datagen.loot.LootTableProviderImpl;
import com.ultramega.refinedflowanalytics.neoforge.datagen.recipe.MainRecipeProvider;
import com.ultramega.refinedflowanalytics.neoforge.datagen.tag.BlockTagsProvider;
import com.ultramega.refinedflowanalytics.neoforge.datagen.tag.ItemTagsProviderImpl;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator.PackGenerator;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void onGatherData(final GatherDataEvent e) {
        registerBlockModelProviders(e.getGenerator(), e.getExistingFileHelper());
        registerBlockStateProviders(e.getGenerator(), e.getExistingFileHelper());
        registerItemModelProviders(e.getGenerator(), e.getExistingFileHelper());
        registerRecipeProviders(e.getGenerator(), e.getLookupProvider());
        registerLootTableProviders(e.getGenerator(), e.getLookupProvider());
        registerTagProviders(e.getGenerator(), e.getLookupProvider(), e.getExistingFileHelper());
    }

    private static void registerBlockStateProviders(final DataGenerator generator,
                                                    final ExistingFileHelper existingFileHelper) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        mainPack.addProvider(output -> new BlockStateProviderImpl(output, existingFileHelper));
    }

    private static void registerBlockModelProviders(final DataGenerator generator,
                                                    final ExistingFileHelper existingFileHelper) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        mainPack.addProvider(output -> new BlockModelProviderImpl(output, existingFileHelper));
    }

    private static void registerItemModelProviders(final DataGenerator generator,
                                                   final ExistingFileHelper existingFileHelper) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        mainPack.addProvider(output -> new ItemModelProviderImpl(output, existingFileHelper));
    }

    private static void registerLootTableProviders(final DataGenerator generator,
                                                   final CompletableFuture<Provider> provider) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        mainPack.addProvider(output -> new LootTableProviderImpl(output, provider));
    }

    private static void registerRecipeProviders(final DataGenerator generator,
                                                final CompletableFuture<HolderLookup.Provider> provider) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        mainPack.addProvider(output -> new MainRecipeProvider(output, provider));
    }

    private static void registerTagProviders(final DataGenerator generator,
                                             final CompletableFuture<HolderLookup.Provider> lookupProvider,
                                             final ExistingFileHelper existingFileHelper) {
        final PackGenerator mainPack = generator.getVanillaPack(true);
        final BlockTagsProvider blockTagsProvider = mainPack.addProvider(
            output -> new BlockTagsProvider(output, lookupProvider, existingFileHelper)
        );
        mainPack.addProvider(output -> new ItemTagsProviderImpl(
            output,
            lookupProvider,
            blockTagsProvider,
            existingFileHelper
        ));
    }
}
