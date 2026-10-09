package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity;
import com.ultramega.refinedflowanalytics.registry.ModMenus;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

import com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer;
import com.refinedmods.refinedstorage.common.support.RedstoneMode;
import com.refinedmods.refinedstorage.common.support.containermenu.AbstractResourceContainerMenu;
import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import com.refinedmods.refinedstorage.common.support.containermenu.ResourceSlot;
import com.refinedmods.refinedstorage.common.support.containermenu.ResourceSlotType;
import com.refinedmods.refinedstorage.common.support.containermenu.ServerProperty;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl;

import java.util.function.Predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createTranslation;

public class FlowMonitorContainerMenu extends AbstractResourceContainerMenu {
    private static final Component FILTER_HELP = createTranslation("gui", "storage_monitor.filter_help");

    private final Predicate<Player> stillValid;

    public FlowMonitorContainerMenu(final int syncId,
                                    final Inventory playerInventory,
                                    final ResourceContainerData resourceContainerData) {
        super(ModMenus.FLOW_MONITOR_MENU.get(), syncId);
        this.registerProperty(new ClientProperty<>(PropertyTypes.FUZZY_MODE, false));
        this.registerProperty(new ClientProperty<>(PropertyTypes.REDSTONE_MODE, RedstoneMode.IGNORE));
        this.registerProperty(new ClientProperty<>(FlowMonitorProperties.ITEM_VISIBILITY, MonitorItemVisibility.SHOW));
        this.registerProperty(new ClientProperty<>(FlowMonitorProperties.FLOW_TEXT, FlowDirection.NET));
        this.registerProperty(new ClientProperty<>(FlowMonitorProperties.GRANULARITY, Granularity.SECOND));
        this.registerProperty(new ClientProperty<>(FlowMonitorProperties.LINE_STYLE, LineStyle.EXACT));
        this.addSlots(playerInventory, ResourceContainerImpl.createForFilter(resourceContainerData));
        this.stillValid = p -> true;
    }

    public FlowMonitorContainerMenu(final int syncId,
                                    final Player player,
                                    final FlowMonitorBlockEntity storageMonitor,
                                    final ResourceContainer resourceContainer) {
        super(ModMenus.FLOW_MONITOR_MENU.get(), syncId, player);
        this.registerProperty(new ServerProperty<>(
            PropertyTypes.FUZZY_MODE,
            storageMonitor::isFuzzyMode,
            storageMonitor::setFuzzyMode
        ));
        this.registerProperty(new ServerProperty<>(
            PropertyTypes.REDSTONE_MODE,
            storageMonitor::getRedstoneMode,
            storageMonitor::setRedstoneMode
        ));
        this.registerProperty(new ServerProperty<>(
            FlowMonitorProperties.ITEM_VISIBILITY,
            storageMonitor::getItemVisibility,
            storageMonitor::setItemVisibility
        ));
        this.registerProperty(new ServerProperty<>(
            FlowMonitorProperties.FLOW_TEXT,
            storageMonitor::getFlowText,
            storageMonitor::setFlowText
        ));
        this.registerProperty(new ServerProperty<>(
            FlowMonitorProperties.GRANULARITY,
            storageMonitor::getGranularity,
            storageMonitor::setGranularity
        ));
        this.registerProperty(new ServerProperty<>(
            FlowMonitorProperties.LINE_STYLE,
            storageMonitor::getLineStyle,
            storageMonitor::setLineStyle
        ));
        this.addSlots(player.getInventory(), resourceContainer);
        this.stillValid = p -> Container.stillValidBlockEntity(storageMonitor, p);
    }

    private void addSlots(final Inventory playerInventory, final ResourceContainer resourceContainer) {
        this.addSlot(new ResourceSlot(resourceContainer, 0, FILTER_HELP, 80, 20, ResourceSlotType.FILTER));
        this.addPlayerInventory(playerInventory, 8, 55);
        this.transferManager.addFilterTransfer(playerInventory);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.stillValid.test(player);
    }
}
