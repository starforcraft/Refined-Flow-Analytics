package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;

import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;

import static com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity.GRANULARITY_TAG;

public final class GranularitySideButtonWidget extends FlowSideButtonWidget<Granularity> {
    public GranularitySideButtonWidget(final ClientProperty<Granularity> property) {
        super(GRANULARITY_TAG, property::getValue, property::setValue, Granularity::next, Granularity::prev, Granularity::getSprite);
    }

    public GranularitySideButtonWidget(final FlowGridContainerMenu menu, final Runnable onChange) {
        super(GRANULARITY_TAG, menu::getGranularity, value -> {
            menu.setGranularity(value);
            onChange.run();
        }, Granularity::next, Granularity::prev, Granularity::getSprite);
    }
}
