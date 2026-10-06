package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

public final class SortingDirectionSideButtonWidget extends FlowScopeSideButtonWidget<SortingDirection> {
    public SortingDirectionSideButtonWidget(final FlowScopeContainerMenu menu) {
        super("sorting_direction", menu::getSortingDirection, menu::setSortingDirection, SortingDirection::next, null, SortingDirection::getSprite);
    }
}
