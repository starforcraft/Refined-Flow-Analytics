package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowScopeMonitorContainerMenu;

import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FlowScopeContainerMenu>> FLOW_SCOPE_MENU =
        REGISTRY.register("flow_scope_menu", () -> IMenuTypeExtension.create(FlowScopeContainerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FlowScopeMonitorContainerMenu>> FLOW_SCOPE_MONITOR_MENU =
        REGISTRY.register("flow_scope_monitor", () -> IMenuTypeExtension.create((id, inventory, buffer) ->
            new FlowScopeMonitorContainerMenu(id, inventory, ResourceContainerData.STREAM_CODEC.decode(buffer))));

    private ModMenus() {
    }
}
