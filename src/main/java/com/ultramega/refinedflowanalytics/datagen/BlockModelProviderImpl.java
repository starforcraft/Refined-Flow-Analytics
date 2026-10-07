package com.ultramega.refinedflowanalytics.datagen;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class BlockModelProviderImpl extends BlockModelProvider {
    private static final String PARTICLE_TEXTURE = "particle";
    private static final String CUTOUT_TEXTURE = "cutout";
    private static final String BLOCK_PREFIX = "block";

    private static final ResourceLocation EMISSIVE_NORTH_CUTOUT = createIdentifier("block/emissive_north_cutout");
    private static final ResourceLocation NORTH_CUTOUT = createIdentifier("block/north_cutout");
    private static final ResourceLocation BOTTOM_TEXTURE = createIdentifier("block/bottom");

    private static final String NORTH = "north";
    private static final String EAST = "east";
    private static final String SOUTH = "south";
    private static final String WEST = "west";
    private static final String UP = "up";
    private static final String DOWN = "down";

    public BlockModelProviderImpl(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.registerRightLeftBackFrontTopModel(ModBlocks.INSTANCE.getFlowGrid(), "flow_grid");
        this.withExistingParent("block/flow_monitor", createIdentifier("block/storage_monitor"));
    }

    private void registerRightLeftBackFrontTopModel(final BlockColorMap<?, ?> blockMap, final String name) {
        this.registerRightLeftBackFrontTopModel(blockMap, name, "");
    }

    private void registerRightLeftBackFrontTopModel(final BlockColorMap<?, ?> blockMap,
                                                    final String name,
                                                    final String modelPrefix) {
        blockMap.forEach((color, id, block) -> {
            final ResourceLocation cutout = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/cutouts/" + color.getName());
            this.registerRightLeftBackFrontTopModel(name, modelPrefix + color.getName(), cutout, EMISSIVE_NORTH_CUTOUT);
        });
        final ResourceLocation inactiveCutout = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/cutouts/inactive");
        this.registerRightLeftBackFrontTopModel(name, "inactive", inactiveCutout, NORTH_CUTOUT);
    }

    private void registerRightLeftBackFrontTopModel(final String name,
                                                    final String variantName,
                                                    final ResourceLocation cutout,
                                                    final ResourceLocation baseModel) {
        final ResourceLocation right = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/right");
        final ResourceLocation left = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/left");
        final ResourceLocation back = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/back");
        final ResourceLocation front = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/front");
        final ResourceLocation top = createFlowAnalyticsIdentifier(BLOCK_PREFIX + "/" + name + "/top");
        this.withExistingParent(BLOCK_PREFIX + "/" + name + "/" + variantName, baseModel)
            .texture(PARTICLE_TEXTURE, right)
            .texture(NORTH, front)
            .texture(EAST, right)
            .texture(SOUTH, back)
            .texture(WEST, left)
            .texture(UP, top)
            .texture(DOWN, BOTTOM_TEXTURE)
            .texture(CUTOUT_TEXTURE, cutout);
    }
}
