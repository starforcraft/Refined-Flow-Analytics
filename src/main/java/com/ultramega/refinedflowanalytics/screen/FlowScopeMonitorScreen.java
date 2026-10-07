package com.ultramega.refinedflowanalytics.screen;

import com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity;
import com.ultramega.refinedflowanalytics.container.FlowScopeMonitorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowScopeMonitorProperties;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.GranularitySideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyleSideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorDisplaySideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorFlowText;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import com.refinedmods.refinedstorage.common.support.widget.FuzzyModeSideButtonWidget;
import com.refinedmods.refinedstorage.common.support.widget.RedstoneModeSideButtonWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;

public class FlowScopeMonitorScreen extends AbstractBaseScreen<FlowScopeMonitorContainerMenu> {
    private static final ResourceLocation TEXTURE = createIdentifier("textures/gui/storage_monitor.png");

    public FlowScopeMonitorScreen(final FlowScopeMonitorContainerMenu menu,
                                final Inventory playerInventory,
                                final Component title) {
        super(menu, playerInventory, title);
        this.inventoryLabelY = 43;
        this.imageWidth = 211;
        this.imageHeight = 137;
    }

    @Override
    protected void init() {
        super.init();
        this.addSideButton(new RedstoneModeSideButtonWidget(
            this.getMenu().getProperty(PropertyTypes.REDSTONE_MODE)
        ));
        this.addSideButton(new FuzzyModeSideButtonWidget(
            this.getMenu().getProperty(PropertyTypes.FUZZY_MODE),
            () -> FuzzyModeSideButtonWidget.Type.GENERIC
        ));
        this.addSideButton(new MonitorDisplaySideButtonWidget<>(FlowScopeMonitorBlockEntity.ITEM_VISIBILITY_TAG,
            this.getMenu().getProperty(FlowScopeMonitorProperties.ITEM_VISIBILITY),
            MonitorItemVisibility.values(),
            MonitorItemVisibility::getSprite
        ));
        this.addSideButton(new MonitorDisplaySideButtonWidget<>(FlowScopeMonitorBlockEntity.FLOW_TEXT_TAG,
            this.getMenu().getProperty(FlowScopeMonitorProperties.FLOW_TEXT),
            MonitorFlowText.values(),
            MonitorFlowText::getSprite
        ));
        this.addSideButton(new GranularitySideButtonWidget(
            this.getMenu().getProperty(FlowScopeMonitorProperties.GRANULARITY)
        ));
        this.addSideButton(new LineStyleSideButtonWidget(
            this.getMenu().getProperty(FlowScopeMonitorProperties.LINE_STYLE)
        ));
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
