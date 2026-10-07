package com.ultramega.refinedflowanalytics.registry;

import com.refinedmods.refinedstorage.common.support.BaseBlockItem;
import com.refinedmods.refinedstorage.common.support.NetworkNodeBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ModItems {
    public static final ModItems INSTANCE = new ModItems();

    public static final DeferredHolder<Item, NetworkNodeBlockItem> FLOW_MONITOR = DeferredHolder.create(
        Registries.ITEM, createFlowAnalyticsIdentifier("flow_monitor"));

    private final List<Supplier<BaseBlockItem>> allFlowGrids = new ArrayList<>();

    private ModItems() {
    }

    public void addFlowGrid(final Supplier<BaseBlockItem> supplier) {
        this.allFlowGrids.add(supplier);
    }

    public List<Supplier<BaseBlockItem>> getFlowGrids() {
        return Collections.unmodifiableList(this.allFlowGrids);
    }
}
