package com.ultramega.refinedflowanalytics.neoforge.datagen.model;

import com.ultramega.refinedflowanalytics.block.FlowDetectorBlock;
import com.ultramega.refinedflowanalytics.registry.ContentIds;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.support.AbstractActiveColoredDirectionalBlock;
import com.refinedmods.refinedstorage.common.support.direction.DefaultDirectionType;
import com.refinedmods.refinedstorage.common.support.direction.OrientedDirection;
import com.refinedmods.refinedstorage.common.support.direction.OrientedDirectionType;

import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

public final class ModelProviders extends ModelProvider {
    private static final TextureSlot CUTOUT = TextureSlot.create("cutout");
    private static final ModelTemplate NORTH_CUTOUT_MODEL = ModelTemplates.create(
        "refinedstorage:north_cutout", TextureSlot.PARTICLE, TextureSlot.NORTH, TextureSlot.EAST,
        TextureSlot.SOUTH, TextureSlot.WEST, TextureSlot.UP, TextureSlot.DOWN, CUTOUT
    );
    private static final ModelTemplate EMISSIVE_NORTH_CUTOUT_MODEL = ModelTemplates.create(
        "refinedstorage:emissive_north_cutout", TextureSlot.PARTICLE, TextureSlot.NORTH, TextureSlot.EAST,
        TextureSlot.SOUTH, TextureSlot.WEST, TextureSlot.UP, TextureSlot.DOWN, CUTOUT
    );

    private static final ModelTemplate MONITOR_MODEL = ModelTemplates.create("refinedstorage:storage_monitor");
    private static final ModelTemplate POWERED_DETECTOR_MODEL = ModelTemplates.create(
        "refinedstorage:detector/powered", TextureSlot.TORCH
    );

    public ModelProviders(final PackOutput output) {
        super(output, MOD_ID);
    }

    @Override
    protected void registerModels(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        this.registerFlowGrids(blockModels, itemModels);
        this.registerFlowMonitor(blockModels, itemModels);
        this.registerFlowDetectors(blockModels, itemModels);
    }

    private void registerFlowGrids(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        final Identifier inactive = NORTH_CUTOUT_MODEL.create(
            createFlowAnalyticsIdentifier("block/flow_grid/inactive"), gridTextures("inactive"), blockModels.modelOutput
        );
        ModBlocks.INSTANCE.getFlowGrid().forEach((color, id, block) -> {
            final Identifier active = EMISSIVE_NORTH_CUTOUT_MODEL.create(
                createFlowAnalyticsIdentifier("block/flow_grid/" + color.getName()),
                gridTextures(color.getName()), blockModels.modelOutput
            );
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
                .with(PropertyDispatch.initial(AbstractActiveColoredDirectionalBlock.ACTIVE)
                    .select(false, plainVariant(inactive))
                    .select(true, plainVariant(active)))
                .with(PropertyDispatch.modify(OrientedDirectionType.INSTANCE.getProperty())
                    .generate(direction -> variant -> variant.withXRot(getXRot(direction)).withYRot(getYRot(direction)))));
            itemModels.itemModelOutput.accept(block.get().asItem(), ItemModelUtils.plainModel(active));
        });
    }

    private static TextureMapping gridTextures(final String color) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, texture(createFlowAnalyticsIdentifier("block/flow_grid/right")))
            .put(TextureSlot.NORTH, texture(createFlowAnalyticsIdentifier("block/flow_grid/front")))
            .put(TextureSlot.EAST, texture(createFlowAnalyticsIdentifier("block/flow_grid/right")))
            .put(TextureSlot.SOUTH, texture(createFlowAnalyticsIdentifier("block/flow_grid/back")))
            .put(TextureSlot.WEST, texture(createFlowAnalyticsIdentifier("block/flow_grid/left")))
            .put(TextureSlot.UP, texture(createFlowAnalyticsIdentifier("block/flow_grid/top")))
            .put(TextureSlot.DOWN, texture(createIdentifier("block/bottom")))
            .put(CUTOUT, texture(createFlowAnalyticsIdentifier("block/flow_grid/cutouts/" + color)));
    }

    private void registerFlowMonitor(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        final Identifier model = MONITOR_MODEL.create(
            ContentIds.FLOW_MONITOR.withPrefix("block/"), new TextureMapping(), blockModels.modelOutput
        );
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.getFlowMonitor(), plainVariant(model))
            .with(PropertyDispatch.modify(OrientedDirectionType.INSTANCE.getProperty())
                .generate(direction -> variant -> variant.withXRot(getXRot(direction)).withYRot(getYRot(direction)))));
        itemModels.itemModelOutput.accept(ModBlocks.getFlowMonitor().asItem(), ItemModelUtils.plainModel(model));
    }

    private void registerFlowDetectors(final BlockModelGenerators blockModels, final ItemModelGenerators itemModels) {
        final Identifier unpowered = createIdentifier("block/detector/unpowered");
        ModBlocks.INSTANCE.getFlowDetector().forEach((color, id, block) -> {
            final TextureMapping textures = new TextureMapping()
                .put(TextureSlot.TORCH, texture(createIdentifier("block/detector/cutouts/" + color.getName())));
            final Identifier powered = POWERED_DETECTOR_MODEL.create(id.withPrefix("block/"), textures, blockModels.modelOutput);
            final Identifier item = POWERED_DETECTOR_MODEL.create(id.withPrefix("item/"), textures, itemModels.modelOutput);
            blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
                .with(PropertyDispatch.initial(FlowDetectorBlock.POWERED)
                    .select(false, plainVariant(unpowered))
                    .select(true, plainVariant(powered)))
                .with(PropertyDispatch.modify(DefaultDirectionType.FACE_CLICKED.getProperty())
                    .generate(direction -> variant -> variant.withXRot(getXRot(direction)).withYRot(getYRot(direction)))));
            itemModels.itemModelOutput.accept(block.get().asItem(), ItemModelUtils.plainModel(item));
        });
    }

    private static Material texture(final Identifier id) {
        return new Material(id);
    }

    private static Quadrant getXRot(final OrientedDirection direction) {
        return switch (direction) {
            case NORTH, EAST, SOUTH, WEST -> Quadrant.R0;
            case DOWN_NORTH, DOWN_EAST, DOWN_SOUTH, DOWN_WEST -> Quadrant.R90;
            case UP_NORTH, UP_EAST, UP_SOUTH, UP_WEST -> Quadrant.R270;
        };
    }

    private static Quadrant getXRot(final Direction direction) {
        return switch (direction) {
            case DOWN -> Quadrant.R0;
            case UP -> Quadrant.R180;
            case NORTH, SOUTH, WEST, EAST -> Quadrant.R90;
        };
    }

    private static Quadrant getYRot(final OrientedDirection direction) {
        return switch (direction) {
            case NORTH, UP_SOUTH, DOWN_NORTH -> Quadrant.R0;
            case EAST, UP_WEST, DOWN_WEST -> Quadrant.R90;
            case SOUTH, UP_NORTH, DOWN_SOUTH -> Quadrant.R180;
            case WEST, UP_EAST, DOWN_EAST -> Quadrant.R270;
        };
    }

    private static Quadrant getYRot(final Direction direction) {
        return switch (direction) {
            case DOWN, UP, SOUTH -> Quadrant.R0;
            case NORTH -> Quadrant.R180;
            case EAST -> Quadrant.R270;
            case WEST -> Quadrant.R90;
        };
    }
}
