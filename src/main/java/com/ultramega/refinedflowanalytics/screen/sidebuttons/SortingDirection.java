package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import java.util.Locale;
import java.util.SequencedMap;

import net.minecraft.resources.ResourceLocation;

public enum SortingDirection {
    ASCENDING,
    DESCENDING;

    private static final SortingDirection[] VALUES = values();

    public static SortingDirection next(final SortingDirection current) {
        return VALUES[(current.ordinal() + 1) % VALUES.length];
    }

    public <K, V> SequencedMap<K, V> sort(final SequencedMap<K, V> map) {
        return this == ASCENDING ? map.reversed() : map;
    }

    public ResourceLocation getSprite() {
        return ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/grid/sorting_direction/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
