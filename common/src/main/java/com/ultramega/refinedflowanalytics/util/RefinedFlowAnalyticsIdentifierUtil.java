package com.ultramega.refinedflowanalytics.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class RefinedFlowAnalyticsIdentifierUtil {
    public static final String MOD_ID = "refinedflowanalytics";

    private RefinedFlowAnalyticsIdentifierUtil() {
    }

    public static ResourceLocation createFlowAnalyticsIdentifier(final String value) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, value);
    }

    public static MutableComponent createFlowAnalyticsTranslation(final String category, final String value) {
        return Component.translatable(createFlowAnalyticsTranslationKey(category, value));
    }

    public static MutableComponent createFlowAnalyticsTranslation(final String category,
                                                                  final String value,
                                                                  final Object... args) {
        return Component.translatable(createFlowAnalyticsTranslationKey(category, value), args);
    }

    public static String createFlowAnalyticsTranslationKey(final String category, final String value) {
        return String.format("%s.%s.%s", category, MOD_ID, value);
    }
}
