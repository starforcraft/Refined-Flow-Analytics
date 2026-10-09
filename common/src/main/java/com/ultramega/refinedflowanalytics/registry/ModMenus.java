package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.container.FlowDetectorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowMonitorContainerMenu;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.world.inventory.MenuType;

import static java.util.Objects.requireNonNull;

public final class ModMenus {
    @Nullable
    private static Supplier<MenuType<FlowGridContainerMenu>> flowGridMenu;
    @Nullable
    private static Supplier<MenuType<FlowMonitorContainerMenu>> flowMonitorMenu;
    @Nullable
    private static Supplier<MenuType<FlowDetectorContainerMenu>> flowDetectorMenu;

    private ModMenus() {
    }

    public static void setFlowGridMenu(final Supplier<MenuType<FlowGridContainerMenu>> supplier) {
        flowGridMenu = supplier;
    }

    public static void setFlowMonitorMenu(final Supplier<MenuType<FlowMonitorContainerMenu>> supplier) {
        flowMonitorMenu = supplier;
    }

    public static void setFlowDetectorMenu(final Supplier<MenuType<FlowDetectorContainerMenu>> supplier) {
        flowDetectorMenu = supplier;
    }

    public static MenuType<FlowGridContainerMenu> getFlowGridMenu() {
        return requireNonNull(flowGridMenu, "FlowGridMenu is not registered").get();
    }

    public static MenuType<FlowMonitorContainerMenu> getFlowMonitorMenu() {
        return requireNonNull(flowMonitorMenu, "FlowMonitorMenu is not registered").get();
    }

    public static MenuType<FlowDetectorContainerMenu> getFlowDetectorMenu() {
        return requireNonNull(flowDetectorMenu, "FlowDetectorMenu is not registered").get();
    }
}
