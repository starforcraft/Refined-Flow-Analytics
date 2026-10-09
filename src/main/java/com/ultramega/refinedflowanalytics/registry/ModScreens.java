package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.screen.FlowDetectorScreen;
import com.ultramega.refinedflowanalytics.screen.FlowGridScreen;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorRenderer;
import com.ultramega.refinedflowanalytics.screen.FlowMonitorScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ModScreens {
    private ModScreens() {
    }

    @SubscribeEvent
    public static void clientLoad(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.FLOW_GRID_MENU.get(), FlowGridScreen::new);
        event.register(ModMenus.FLOW_MONITOR_MENU.get(), FlowMonitorScreen::new);
        event.register(ModMenus.FLOW_DETECTOR_MENU.get(), FlowDetectorScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLOW_MONITOR.get(), context -> new FlowMonitorRenderer());
    }

    public static void updateMenuState(final MenuStateUpdateMessage message) {
        if (Minecraft.getInstance().screen instanceof FlowGridScreen screen
            && screen.getMenu().containerId == message.containerId()) {
            screen.updateMenuState(message.requestId(), message.state());
        }
    }
}
