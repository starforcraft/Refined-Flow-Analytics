package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.screen.components.FlowGraph;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum FlowDirection {
    NET(FlowGraph.NET_GRAPH_COLOR),
    INFLOW(FlowGraph.PRODUCTION_GRAPH_COLOR),
    OUTFLOW(FlowGraph.CONSUMPTION_GRAPH_COLOR);

    private final int color;

    FlowDirection(final int color) {
        this.color = color;
    }

    public ResourceLocation getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/flow_text/" + this.toString().toLowerCase(Locale.ROOT));
    }

    public int getColor() {
        return this.color;
    }

    public long getValue(final long inflow, final long outflow) {
        return switch (this) {
            case NET -> inflow - outflow;
            case INFLOW -> inflow;
            case OUTFLOW -> -outflow;
        };
    }

    public long getComparisonValue(final long inflow, final long outflow) {
        return switch (this) {
            case NET -> inflow - outflow;
            case INFLOW -> inflow;
            case OUTFLOW -> outflow;
        };
    }

    public boolean matches(final long inflow, final long outflow, final long threshold, final DetectorMode mode) {
        final long amount = this.getComparisonValue(inflow, outflow);
        return switch (mode) {
            case UNDER -> amount < threshold;
            case EQUAL -> amount == threshold;
            case ABOVE -> amount > threshold;
        };
    }
}
