package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;

public final class SortingDirectionSideButtonWidget extends FlowSideButtonWidget<SortingDirection> {
    public SortingDirectionSideButtonWidget(final FlowGridContainerMenu menu) {
        super("sorting_direction", menu::getSortingDirection, menu::setSortingDirection, SortingDirection::next, null, SortingDirection::getSprite);
    }
}
