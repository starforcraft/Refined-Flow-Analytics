package com.ultramega.refinedflowanalytics.registry;

import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.world.item.BlockItem;

import static java.util.Objects.requireNonNull;

public final class ModItems {
    public static final ModItems INSTANCE = new ModItems();

    @Nullable
    private static Supplier<BlockItem> flowMonitor;

    private final List<Supplier<BaseBlockItem>> allFlowGrids = new ArrayList<>();
    private final List<Supplier<BaseBlockItem>> allFlowDetectors = new ArrayList<>();

    private ModItems() {
    }

    public static BlockItem getFlowMonitor() {
        return requireNonNull(flowMonitor, "Flow Monitor item is not registered").get();
    }

    public static void setFlowMonitor(final Supplier<BlockItem> supplier) {
        flowMonitor = supplier;
    }

    public void addFlowGrid(final Supplier<BaseBlockItem> supplier) {
        this.allFlowGrids.add(supplier);
    }

    public List<Supplier<BaseBlockItem>> getFlowGrids() {
        return Collections.unmodifiableList(this.allFlowGrids);
    }

    public void addFlowDetector(final Supplier<BaseBlockItem> supplier) {
        this.allFlowDetectors.add(supplier);
    }

    public List<Supplier<BaseBlockItem>> getFlowDetectors() {
        return Collections.unmodifiableList(this.allFlowDetectors);
    }
}
