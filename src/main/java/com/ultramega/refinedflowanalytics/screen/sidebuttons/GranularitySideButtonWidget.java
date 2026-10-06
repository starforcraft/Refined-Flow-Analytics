package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

public final class GranularitySideButtonWidget extends FlowScopeSideButtonWidget<Granularity> {
    public GranularitySideButtonWidget(final FlowScopeContainerMenu menu, final Runnable onChange) {
        super("granularity", menu::getGranularity, value -> {
            menu.setGranularity(value);
            onChange.run();
        }, Granularity::next, Granularity::prev, Granularity::getSprite);
    }
}
