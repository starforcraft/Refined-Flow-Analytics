package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;

public final class ResourceViewSideButtonWidget extends FlowSideButtonWidget<ResourceView> {
    public ResourceViewSideButtonWidget(final FlowGridContainerMenu menu, final Runnable onChange) {
        super("resource_view", menu::getResourceView, value -> {
            menu.setResourceView(value);
            onChange.run();
        }, ResourceView::next, ResourceView::next, ResourceView::getSprite);
    }
}
