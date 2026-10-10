package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import java.util.Locale;

import net.minecraft.resources.Identifier;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum LineStyle {
    BLOCKY,
    EXACT;

    public static LineStyle next(final LineStyle current) {
        return LineStyle.values()[(current.ordinal() + 1) % LineStyle.values().length];
    }

    public Identifier getSprite() {
        return createFlowAnalyticsIdentifier("widget/side_button/line_style/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
