package com.ultramega.refinedflowanalytics.neoforge;

import com.ultramega.refinedflowanalytics.AbstractClientModInitializer;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorRenderer;

import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenConstructor;
import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenRegistration;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class ClientModInitializer extends AbstractClientModInitializer {
    private ClientModInitializer() {
    }

    public static void register(final IEventBus eventBus) {
        eventBus.addListener(ClientModInitializer::onRegisterMenuScreens);
        eventBus.addListener(ClientModInitializer::registerClientPayloads);
        eventBus.addListener(ClientModInitializer::registerRenderers);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent e) {
        registerScreens(new ScreenRegistration() {
            @Override
            public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
                final MenuType<? extends M> type,
                final ScreenConstructor<M, U> factory
            ) {
                e.register(type, factory::create);
            }
        });
    }

    private static void registerClientPayloads(final RegisterClientPayloadHandlersEvent event) {
        event.register(
            MenuStateUpdateMessage.TYPE,
            (message, context) -> handleMenuState(message)
        );
    }

    private static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.getFlowMonitor(), context -> new FlowMonitorRenderer());
    }

    public static void handleMenuState(final MenuStateUpdateMessage message) {
        updateMenuState(message);
    }
}
