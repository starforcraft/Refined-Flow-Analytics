package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.refinedmods.refinedstorage.api.network.impl.node.detector.DetectorMode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: add tests for flow monitor and flow grid
class FlowDirectionTest {
    @Test
    void detectsBothDirectionsEvenWhenTheyCancelOut() {
        assertTrue(FlowDirection.INFLOW.matches(64, 64, 0, DetectorMode.ABOVE));
        assertTrue(FlowDirection.OUTFLOW.matches(64, 64, 0, DetectorMode.ABOVE));
        assertTrue(FlowDirection.NET.matches(64, 64, 0, DetectorMode.EQUAL));
        assertFalse(FlowDirection.NET.matches(64, 64, 0, DetectorMode.ABOVE));
    }

    @Test
    void outflowIsPositiveButNetCanBeNegative() {
        assertEquals(40, FlowDirection.OUTFLOW.getComparisonValue(10, 40));
        assertEquals(-30, FlowDirection.NET.getComparisonValue(10, 40));
        assertTrue(FlowDirection.NET.matches(10, 40, -20, DetectorMode.UNDER));
        assertTrue(FlowDirection.NET.matches(10, 40, -30, DetectorMode.EQUAL));
        assertTrue(FlowDirection.NET.matches(10, 40, -40, DetectorMode.ABOVE));
    }

    @Test
    void thresholdBoundariesAreStrictForEveryDirection() {
        for (final FlowDirection direction : FlowDirection.values()) {
            final long threshold = direction.getComparisonValue(64, 16);
            assertTrue(direction.matches(64, 16, threshold, DetectorMode.EQUAL));
            assertFalse(direction.matches(64, 16, threshold, DetectorMode.UNDER));
            assertFalse(direction.matches(64, 16, threshold, DetectorMode.ABOVE));
            assertTrue(direction.matches(64, 16, threshold + 1, DetectorMode.UNDER));
            assertTrue(direction.matches(64, 16, threshold - 1, DetectorMode.ABOVE));
        }
    }

    @Test
    void idleFlowAndLargeResourceAmountsRemainExact() {
        for (final FlowDirection direction : FlowDirection.values()) {
            assertTrue(direction.matches(0, 0, 0, DetectorMode.EQUAL));
            assertTrue(direction.matches(0, 0, 1, DetectorMode.UNDER));
            assertFalse(direction.matches(0, 0, 0, DetectorMode.ABOVE));
        }
        assertTrue(FlowDirection.NET.matches(Long.MAX_VALUE, Long.MAX_VALUE - 1, 1, DetectorMode.EQUAL));
        assertTrue(FlowDirection.NET.matches(0, Long.MAX_VALUE, -Long.MAX_VALUE, DetectorMode.EQUAL));
    }
}
