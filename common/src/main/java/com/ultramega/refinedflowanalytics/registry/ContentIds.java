package com.ultramega.refinedflowanalytics.registry;

import net.minecraft.resources.Identifier;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ContentIds {
    public static final Identifier FLOW_GRID = createFlowAnalyticsIdentifier("flow_grid");
    public static final Identifier FLOW_MONITOR = createFlowAnalyticsIdentifier("flow_monitor");
    public static final Identifier FLOW_DETECTOR = createFlowAnalyticsIdentifier("flow_detector");

    private ContentIds() {
    }
}
