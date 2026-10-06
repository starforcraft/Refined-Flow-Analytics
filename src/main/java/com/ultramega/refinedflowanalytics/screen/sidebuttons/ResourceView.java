package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import net.minecraft.resources.ResourceLocation;

public enum ResourceView {
    CHANGED("non_autocraftable"),
    ALL_STORED("all");

    private final ResourceLocation sprite;

    ResourceView(final String spriteName) {
        this.sprite = ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/grid/view_type/" + spriteName);
    }

    public static ResourceView next(final ResourceView current) {
        return current == CHANGED ? ALL_STORED : CHANGED;
    }

    public ResourceLocation getSprite() {
        return this.sprite;
    }
}
