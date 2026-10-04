package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.screen.FlowScopeScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ModScreens {
    private ModScreens() {
    }

    @SubscribeEvent
    public static void clientLoad(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.FLOW_SCOPE_MENU.get(), FlowScopeScreen::new);
    }

    public static void updateMenuState(final MenuStateUpdateMessage message) {
        if (Minecraft.getInstance().screen instanceof FlowScopeScreen screen) {
            screen.updateMenuState(message.elementType(), message.name(), message.elementState());
        }
    }
}
