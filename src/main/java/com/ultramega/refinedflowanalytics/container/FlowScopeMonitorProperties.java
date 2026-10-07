package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorFlowText;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

import com.refinedmods.refinedstorage.common.support.containermenu.PropertyType;

import static com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity.FLOW_TEXT_TAG;
import static com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity.GRANULARITY_TAG;
import static com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity.ITEM_VISIBILITY_TAG;
import static com.ultramega.refinedflowanalytics.block.entity.FlowScopeMonitorBlockEntity.LINE_STYLE_TAG;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowScopeMonitorProperties {
    public static final PropertyType<MonitorItemVisibility> ITEM_VISIBILITY = create(ITEM_VISIBILITY_TAG, MonitorItemVisibility.values());
    public static final PropertyType<MonitorFlowText> FLOW_TEXT = create(FLOW_TEXT_TAG, MonitorFlowText.values());
    public static final PropertyType<Granularity> GRANULARITY = create(GRANULARITY_TAG, Granularity.values());
    public static final PropertyType<LineStyle> LINE_STYLE = create(LINE_STYLE_TAG, LineStyle.values());

    private FlowScopeMonitorProperties() {
    }

    private static <T extends Enum<T>> PropertyType<T> create(final String name, final T[] values) {
        return new PropertyType<>(createFlowAnalyticsIdentifier(name), Enum::ordinal,
            index -> values[index >= 0 && index < values.length ? index : 0]);
    }

    public static <T extends Enum<T>> T read(final String name, final T[] values, final T fallback) {
        for (final T value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return fallback;
    }
}
