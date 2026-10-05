package com.ultramega.refinedflowanalytics.registry;

import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final ModItems INSTANCE = new ModItems();

    private final List<Supplier<BaseBlockItem>> allFlowScopes = new ArrayList<>();

    private ModItems() {
    }

    public void addFlowScope(final Supplier<BaseBlockItem> supplier) {
        this.allFlowScopes.add(supplier);
    }

    public List<Supplier<BaseBlockItem>> getFlowScopes() {
        return Collections.unmodifiableList(this.allFlowScopes);
    }
}
