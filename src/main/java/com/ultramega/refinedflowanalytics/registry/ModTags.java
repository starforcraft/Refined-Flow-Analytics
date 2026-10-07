package com.ultramega.refinedflowanalytics.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public final class ModTags {
    public static final TagKey<Item> FLOW_GRIDS = createTag("flow_grids");

    private ModTags() {
    }

    private static TagKey<Item> createTag(final String id) {
        return TagKey.create(Registries.ITEM, createFlowAnalyticsIdentifier(id));
    }
}
