package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;

public final class SortingTypeSideButtonWidget extends FlowSideButtonWidget<SortingType> {
    public SortingTypeSideButtonWidget(final FlowGridContainerMenu menu) {
        super("sorting_type", menu::getSortingType, menu::setSortingType, SortingType::next, null, SortingType::getIdentifier);
    }
}
