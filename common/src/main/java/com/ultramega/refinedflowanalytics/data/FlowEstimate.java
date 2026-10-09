package com.ultramega.refinedflowanalytics.data;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public record FlowEstimate(long recordedTicks, double netPerTick) {
    // Metadata entry in detailed snapshots; +1/-1 are flow and 0 is stored amount.
    public static final short DURATION_SIGN = 2;
    public static final FlowEstimate EMPTY = new FlowEstimate(0, 0);

    public static FlowEstimate from(final long[] inflow, final long[] outflow, final long ticks) {
        if (ticks <= 0) {
            return EMPTY;
        }
        double net = 0;
        for (int i = 0; i < Math.max(inflow.length, outflow.length); i++) {
            net += (i < inflow.length ? (double) inflow[i] : 0) - (i < outflow.length ? (double) outflow[i] : 0);
        }
        return new FlowEstimate(ticks, net / ticks);
    }

    public boolean available() {
        return this.recordedTicks > 0;
    }

    public double forTicks(final int ticks) {
        return this.netPerTick * ticks;
    }

    public String observationWindow() {
        final double seconds = this.recordedTicks / 20.0;
        if (seconds < 60) {
            return formatNumber(seconds) + " s";
        }
        if (seconds < 3600) {
            return formatNumber(seconds / 60) + " min";
        }
        if (seconds < 86400) {
            return formatNumber(seconds / 3600) + " h";
        }
        return formatNumber(seconds / 86400) + " d";
    }

    public static String formatNumber(final double value) {
        final double magnitude = Math.abs(value);
        final String pattern = magnitude > 0 && magnitude < 0.01 ? "0.##E0" : "0.##";
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }
}
