package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

public final class LineStyleSideButtonWidget extends FlowScopeSideButtonWidget<LineStyle> {
    public LineStyleSideButtonWidget(final FlowScopeContainerMenu menu) {
        super("line_style", menu::getLineStyle, menu::setLineStyle, LineStyle::next, null, LineStyle::getSprite);
    }
}
