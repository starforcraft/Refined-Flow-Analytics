package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import java.util.Locale;

import net.minecraft.resources.Identifier;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum ResourceView {
    CHANGED,
    ALL_STORED;

    public static ResourceView next(final ResourceView current) {
        return current == CHANGED ? ALL_STORED : CHANGED;
    }

    public Identifier getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/resource_view/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
