package com.ultramega.refinedflowanalytics.fabric;

import com.ultramega.refinedflowanalytics.AbstractClientModInitializer;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorRenderer;

import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenConstructor;
import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenRegistration;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;


public final class ClientModInitializerImpl extends AbstractClientModInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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

    private void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
            MenuStateUpdateMessage.TYPE,
            (message, context) -> updateMenuState(message)
        );
    }

    private void registerBlockEntityRenderers() {
        BlockEntityRenderers.register(ModBlockEntities.getFlowMonitor(), context -> new FlowMonitorRenderer());
    }

}
