package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.entity.FlowDetectorBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class ModBlockEntities {
    @Nullable
    private static Supplier<BlockEntityType<FlowGridBlockEntity>> flowGrid;
    @Nullable
    private static Supplier<BlockEntityType<FlowMonitorBlockEntity>> flowMonitor;
    @Nullable
    private static Supplier<BlockEntityType<FlowDetectorBlockEntity>> flowDetector;

    private ModBlockEntities() {
    }

    public static void setFlowGrid(final Supplier<BlockEntityType<FlowGridBlockEntity>> supplier) {
        flowGrid = supplier;
    }

    public static void setFlowMonitor(final Supplier<BlockEntityType<FlowMonitorBlockEntity>> supplier) {
        flowMonitor = supplier;
    }

    public static void setFlowDetector(final Supplier<BlockEntityType<FlowDetectorBlockEntity>> supplier) {
        flowDetector = supplier;
    }

    public static BlockEntityType<FlowGridBlockEntity> getFlowGrid() {
        return requireNonNull(flowGrid, "FlowGrid is not registered").get();
    }

    public static BlockEntityType<FlowMonitorBlockEntity> getFlowMonitor() {
        return requireNonNull(flowMonitor, "FlowMonitor is not registered").get();
    }

    public static BlockEntityType<FlowDetectorBlockEntity> getFlowDetector() {
        return requireNonNull(flowDetector, "FlowDetector is not registered").get();
    }
}
