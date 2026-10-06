package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

public final class ResourceViewSideButtonWidget extends FlowScopeSideButtonWidget<ResourceView> {
    public ResourceViewSideButtonWidget(final FlowScopeContainerMenu menu, final Runnable onChange) {
        super("resource_view", menu::getResourceView, value -> {
            menu.setResourceView(value);
            onChange.run();
        }, ResourceView::next, ResourceView::next, ResourceView::getSprite);
    }
}
