package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import net.minecraft.resources.Identifier;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public enum Granularity {
    TICK(ChronoUnit.MILLIS, 1, "t"),
    SECOND(ChronoUnit.SECONDS, "s"),
    MINUTE(ChronoUnit.MINUTES, "m"),
    HOUR(ChronoUnit.HOURS, "h"),
    DAY(ChronoUnit.DAYS, "d");

    private static final int TICKS_PER_SECOND = 20;
    private static final Granularity[] VALUES = values();

    private final ChronoUnit chronoUnit;
    private final int tickAmount;
    private final String symbol;
    private final String spritePath;

    Granularity(final ChronoUnit chronoUnit, final String symbol) {
        this(chronoUnit, Math.toIntExact(chronoUnit.getDuration().getSeconds() * TICKS_PER_SECOND), symbol);
    }

    Granularity(final ChronoUnit chronoUnit, final int tickAmount, final String symbol) {
        this.chronoUnit = chronoUnit;
        this.tickAmount = tickAmount;
        this.symbol = symbol;
        this.spritePath = "widget/side_button/granularity/" + this.toString().toLowerCase(Locale.ROOT);
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

    public LocalDateTime subtractSamples(final LocalDateTime time, final int samples) {
        return time.minusNanos((long) samples * this.tickAmount * 50_000_000L);
    }

    public String perStr() {
        return "/" + this.symbol;
    }

    public Identifier getSprite() {
        return createFlowAnalyticsIdentifier(this.spritePath);
    }

    public double convertFrom(final Granularity from, final double amount) {
        return getPerTick(from, amount) * this.tickAmount;
    }
}
