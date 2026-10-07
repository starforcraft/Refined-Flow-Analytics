package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.ultramega.refinedflowanalytics.screen.components.FlowScopeGraph;

import java.util.Locale;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum MonitorFlowText {
    NET(FlowScopeGraph.NET_GRAPH_COLOR),
    INFLOW(FlowScopeGraph.PRODUCTION_GRAPH_COLOR),
    OUTFLOW(FlowScopeGraph.CONSUMPTION_GRAPH_COLOR);

    private final int color;

    MonitorFlowText(final int color) {
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
}
