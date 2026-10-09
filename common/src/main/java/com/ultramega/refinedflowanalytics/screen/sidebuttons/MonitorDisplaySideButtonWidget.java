package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public final class MonitorDisplaySideButtonWidget<T extends Enum<T>> extends FlowSideButtonWidget<T> {
    public MonitorDisplaySideButtonWidget(final String translation,
                                          final ClientProperty<T> property,
                                          final T[] values,
                                          final Function<T, ResourceLocation> sprite) {
        super(translation, property::getValue, property::setValue,
            current -> values[(current.ordinal() + 1) % values.length],
            current -> values[(current.ordinal() + values.length - 1) % values.length], sprite);
    }
}
