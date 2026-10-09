package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;

import static com.refinedmods.refinedstorage.common.util.IdentifierUtil.createIdentifier;

public enum SortingType {
    NAME,
    QUANTITY;

    private static final SortingType[] VALUES = values();

    public static SortingType next(final SortingType current) {
        return VALUES[(current.ordinal() + 1) % VALUES.length];
    }

    public SequencedMap<PlatformResourceKey, Map<Short, Long>> sort(final Map<PlatformResourceKey, Map<Short, Long>> map) {
        final Comparator<Map.Entry<PlatformResourceKey, Map<Short, Long>>> comparator = this == NAME
            ? Comparator.comparing(entry -> entry.getKey().getResourceType().getTitle().getString())
            : Comparator.comparingLong(entry -> {
                final Map<Short, Long> values = entry.getValue();
                final long generation = values.getOrDefault((short) 1, 0L);
                final long consumption = values.getOrDefault((short) -1, 0L);
                return generation - consumption;
            });
        return map.entrySet().stream()
            .sorted(comparator)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }

    public ResourceLocation getResourceLocation() {
        return createIdentifier("widget/side_button/grid/sorting_type/" + this.toString().toLowerCase(Locale.ROOT));
    }
}
