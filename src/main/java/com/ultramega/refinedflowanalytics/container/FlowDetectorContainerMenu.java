package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.block.entity.FlowDetectorBlockEntity;
import com.ultramega.refinedflowanalytics.registry.ModMenus;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer;
import com.refinedmods.refinedstorage.common.support.containermenu.AbstractSingleAmountContainerMenu;
import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import com.refinedmods.refinedstorage.common.support.containermenu.ServerProperty;
import com.refinedmods.refinedstorage.common.support.containermenu.SingleAmountData;

import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowDetectorContainerMenu extends AbstractSingleAmountContainerMenu {
    private static final Component FILTER_HELP = createFlowAnalyticsTranslation("gui", "flow_detector.filter_help");

    private final Predicate<Player> stillValid;

    @Nullable
    private FlowDetectorBlockEntity detector;

    public FlowDetectorContainerMenu(final int syncId,
                                     final Inventory playerInventory,
                                     final SingleAmountData singleAmountData) {
        super(ModMenus.FLOW_DETECTOR_MENU.get(), syncId, playerInventory, singleAmountData, FILTER_HELP);
        this.registerProperty(new ClientProperty<>(PropertyTypes.FUZZY_MODE, false));
        this.registerProperty(new ClientProperty<>(FlowDetectorProperties.MODE, DetectorMode.EQUAL));
        this.registerProperty(new ClientProperty<>(FlowDetectorProperties.DIRECTION, FlowDirection.INFLOW));
        this.stillValid = p -> true;
    }

    public FlowDetectorContainerMenu(final int syncId,
                                     final Player player,
                                     final FlowDetectorBlockEntity detector,
                                     final ResourceContainer resourceContainer) {
        super(ModMenus.FLOW_DETECTOR_MENU.get(), syncId, player, resourceContainer, FILTER_HELP, null);
        this.detector = detector;
        this.registerProperty(new ServerProperty<>(
            PropertyTypes.FUZZY_MODE,
            detector::isFuzzyMode,
            detector::setFuzzyMode
        ));
        this.registerProperty(new ServerProperty<>(
            FlowDetectorProperties.MODE,
            detector::getMode,
            detector::setMode
        ));
        this.registerProperty(new ServerProperty<>(
            FlowDetectorProperties.DIRECTION,
            detector::getFlowDirection,
            detector::setFlowDirection
        ));
        this.stillValid = p -> Container.stillValidBlockEntity(detector, p);
    }

    @Override
    public void changeAmountOnServer(final double newAmount) {
        if (this.detector == null) {
            return;
        }
        this.detector.setAmount(newAmount);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.stillValid.test(player);
    }
}
