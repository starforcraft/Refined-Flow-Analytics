package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.screen.sidebuttons.FlowDirection;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyType;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class FlowDetectorProperties {
    public static final PropertyType<DetectorMode> MODE = new PropertyType<>(
        createFlowAnalyticsIdentifier("flow_mode"), Enum::ordinal,
        index -> index >= 0 && index < DetectorMode.values().length ? DetectorMode.values()[index] : DetectorMode.EQUAL
    );
    public static final PropertyType<FlowDirection> DIRECTION = new PropertyType<>(
        createFlowAnalyticsIdentifier("flow_direction"), Enum::ordinal,
        index -> index >= 0 && index < FlowDirection.values().length ? FlowDirection.values()[index] : FlowDirection.INFLOW
    );

    private FlowDetectorProperties() {
    }
}
