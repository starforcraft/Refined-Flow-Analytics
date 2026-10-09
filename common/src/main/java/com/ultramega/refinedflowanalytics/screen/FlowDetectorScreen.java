package com.ultramega.refinedflowanalytics.screen;

import com.ultramega.refinedflowanalytics.container.FlowDetectorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowDetectorProperties;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorDisplaySideButtonWidget;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;
import com.refinedmods.refinedstorage.common.support.amount.AbstractSingleAmountScreen;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes;
import com.refinedmods.refinedstorage.common.support.widget.FuzzyModeSideButtonWidget;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowDetectorScreen extends AbstractSingleAmountScreen<FlowDetectorContainerMenu> {
    public FlowDetectorScreen(final FlowDetectorContainerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title, menu.getAmount(), () -> -Double.MAX_VALUE);
    }

    @Override
    protected void init() {
        super.init();
        this.addSideButton(new FuzzyModeSideButtonWidget(
            this.getMenu().getProperty(PropertyTypes.FUZZY_MODE),
            () -> FuzzyModeSideButtonWidget.Type.GENERIC
        ));
        this.addSideButton(new MonitorDisplaySideButtonWidget<>("detector_mode",
            this.getMenu().getProperty(FlowDetectorProperties.MODE), DetectorMode.values(),
            mode -> createIdentifier("widget/side_button/detector_mode/" + mode.name().toLowerCase(Locale.ROOT))
        ));
        this.addSideButton(new MonitorDisplaySideButtonWidget<>("detector_direction",
            this.getMenu().getProperty(FlowDetectorProperties.DIRECTION), FlowDirection.values(),
            direction -> createFlowAnalyticsIdentifier("widget/side_button/flow_text/" + direction.name().toLowerCase(Locale.ROOT))
        ));
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, createFlowAnalyticsTranslation("gui", "flow_detector.per_second"), 140, 51, 4210752, false);
    }
}
