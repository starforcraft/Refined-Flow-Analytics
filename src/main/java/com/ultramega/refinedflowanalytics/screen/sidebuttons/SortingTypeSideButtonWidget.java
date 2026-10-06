package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;

public final class SortingTypeSideButtonWidget extends FlowScopeSideButtonWidget<SortingType> {
    public SortingTypeSideButtonWidget(final FlowScopeContainerMenu menu) {
        super("sorting_type", menu::getSortingType, menu::setSortingType, SortingType::next, null, SortingType::getResourceLocation);
    }
}
