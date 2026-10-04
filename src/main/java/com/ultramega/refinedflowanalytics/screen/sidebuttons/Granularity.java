package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import java.time.temporal.ChronoUnit;

import net.minecraft.resources.ResourceLocation;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum Granularity { //TODO: add TICK?
    SECOND(ChronoUnit.SECONDS, "s", "second"),
    MINUTE(ChronoUnit.MINUTES, "m", "minute"),
    HOUR(ChronoUnit.HOURS, "h", "hour"),
    DAY(ChronoUnit.DAYS, "d", "day");

    private static final int TICKS_PER_SECOND = 20;
    private static final Granularity[] VALUES = values();

    private final ChronoUnit chronoUnit;
    private final int tickAmount;
    private final String symbol;
    private final String spritePath;

    Granularity(final ChronoUnit chronoUnit, final String symbol, final String name) {
        this.chronoUnit = chronoUnit;
        this.tickAmount = Math.toIntExact(chronoUnit.getDuration().getSeconds() * TICKS_PER_SECOND);
        this.symbol = symbol;
        this.spritePath = "widget/side_button/granularity/" + name;
    }

    public static Granularity next(final Granularity current) {
        return VALUES[(current.ordinal() + 1) % VALUES.length];
    }

    public static Granularity prev(final Granularity current) {
        return VALUES[(current.ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static double getPerTick(final Granularity from, final double amount) {
        return amount / from.tickAmount;
    }

    public ChronoUnit getChronoUnit() {
        return this.chronoUnit;
    }

    public int getTickAmount() {
        return this.tickAmount;
    }

    public String perStr() {
        return "/" + this.symbol;
    }

    public ResourceLocation getSprite() {
        return createFlowAnalyticsIdentifier(this.spritePath);
    }

    public double convertFrom(final Granularity from, final double amount) {
        return getPerTick(from, amount) * this.tickAmount;
    }
}
