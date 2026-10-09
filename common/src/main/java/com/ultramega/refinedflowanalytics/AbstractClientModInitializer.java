package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.network.MenuState;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModMenus;
import com.ultramega.refinedflowanalytics.screen.FlowDetectorScreen;
import com.ultramega.refinedflowanalytics.screen.FlowGridScreen;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorScreen;

import com.refinedmods.refinedstorage.common.AbstractClientModInitializer.ScreenRegistration;

import net.minecraft.client.Minecraft;

public abstract class AbstractClientModInitializer {
    protected static void registerScreens(final ScreenRegistration registration) {
        registration.register(ModMenus.getFlowGridMenu(), FlowGridScreen::new);
        registration.register(ModMenus.getFlowMonitorMenu(), FlowMonitorScreen::new);
        registration.register(ModMenus.getFlowDetectorMenu(), FlowDetectorScreen::new);
    }

    protected static void updateMenuState(final MenuStateUpdateMessage message) {
        if ((message.state() instanceof MenuState.Snapshot
            || message.state() instanceof MenuState.DetailedSnapshot)
            && Minecraft.getInstance().screen instanceof FlowGridScreen screen
            && screen.getMenu().containerId == message.containerId()) {
            screen.updateMenuState(message.requestId(), message.state());
        }
    }
}
