package com.ultramega.refinedflowanalytics.fabric;

import com.ultramega.refinedflowanalytics.AbstractClientModInitializer;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorRenderer;

import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenConstructor;
import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenRegistration;
import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.fabric.support.render.EmissiveModelRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ClientModInitializerImpl extends AbstractClientModInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        this.setRenderLayers();
        this.registerEmissiveModels();
        this.registerPacketHandlers();
        this.registerBlockEntityRenderers();
        registerScreens(new ScreenRegistration() {
            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
                final MenuType<? extends M> type,
                final ScreenConstructor<M, U> factory
            ) {
                MenuScreens.register(type, factory::create);
            }
        });
    }

    private void setRenderLayers() {
        this.setCutout(ModBlocks.INSTANCE.getFlowGrid());
        this.setCutout(ModBlocks.INSTANCE.getFlowDetector());
    }

    private void registerEmissiveModels() {
        ModBlocks.INSTANCE.getFlowGrid().forEach((color, id, block) -> {
            final ResourceLocation sprite = createFlowAnalyticsIdentifier("block/flow_grid/cutouts/" + color.getName());
            EmissiveModelRegistry.INSTANCE.register(createFlowAnalyticsIdentifier("block/flow_grid/" + color.getName()), sprite);
            EmissiveModelRegistry.INSTANCE.register(id.withPath("item/" + id.getPath()), sprite);
        });
        ModBlocks.INSTANCE.getFlowDetector().forEach((color, id, block) -> {
            final ResourceLocation sprite = createIdentifier("block/detector/cutouts/" + color.getName());
            EmissiveModelRegistry.INSTANCE.register(id.withPath("block/" + id.getPath()), sprite);
            EmissiveModelRegistry.INSTANCE.register(id.withPath("item/" + id.getPath()), sprite);
        });
    }

    private void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
            MenuStateUpdateMessage.TYPE,
            (message, context) -> updateMenuState(message)
        );
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.getFlowMonitor(), context -> new FlowMonitorRenderer());
    }

    private void setCutout(final BlockColorMap<?, ?> blockMap) {
        blockMap.values().forEach(this::setCutout);
    }

    private void setCutout(final Block block) {
        BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout());
    }
}
