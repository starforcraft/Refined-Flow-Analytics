package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;

import static com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity.LINE_STYLE_TAG;

public final class LineStyleSideButtonWidget extends FlowScopeSideButtonWidget<LineStyle> {
    public LineStyleSideButtonWidget(final ClientProperty<LineStyle> property) {
        super(LINE_STYLE_TAG, property::getValue, property::setValue, LineStyle::next, null, LineStyle::getSprite);
    }

    public LineStyleSideButtonWidget(final FlowScopeContainerMenu menu) {
        super(LINE_STYLE_TAG, menu::getLineStyle, menu::setLineStyle, LineStyle::next, null, LineStyle::getSprite);
    }
}
