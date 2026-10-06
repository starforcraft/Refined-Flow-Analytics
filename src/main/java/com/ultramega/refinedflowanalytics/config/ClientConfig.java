package com.ultramega.refinedflowanalytics.config;

import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;

import java.util.Objects;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ClientConfig INSTANCE = new ClientConfig();

    private final ModConfigSpec spec;
    private final ModConfigSpec.EnumValue<SortingDirection> sortingDirection;
    private final ModConfigSpec.EnumValue<SortingType> sortingType;
    private final ModConfigSpec.EnumValue<Granularity> granularity;
    private final ModConfigSpec.EnumValue<LineStyle> lineStyle;
    private final ModConfigSpec.EnumValue<ResourceView> resourceView;

    private ClientConfig() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("flowScope");
        this.sortingDirection = builder.comment("Resource sorting direction.") //TODO: switch to .translation
            .defineEnum("sortingDirection", SortingDirection.DESCENDING);
        this.sortingType = builder.comment("Resource sorting criterion.")
            .defineEnum("sortingType", SortingType.QUANTITY);
        this.granularity = builder.comment("Time interval used for flow snapshots and graph points.")
            .defineEnum("granularity", Granularity.MINUTE);
        this.lineStyle = builder.comment("Rendering style of the flow graph.")
            .defineEnum("lineStyle", LineStyle.EXACT);
        this.resourceView = builder.comment("Show changed resources or all resources currently stored in the network.")
            .defineEnum("resourceView", ResourceView.CHANGED);
        builder.pop();

        this.spec = builder.build();
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    public SortingDirection getSortingDirection() {
        return this.sortingDirection.get();
    }

    public void setSortingDirection(final SortingDirection value) {
        this.setValue(this.sortingDirection, value);
    }

    public SortingType getSortingType() {
        return this.sortingType.get();
    }

    public void setSortingType(final SortingType value) {
        this.setValue(this.sortingType, value);
    }

    public Granularity getGranularity() {
        return this.granularity.get();
    }

    public void setGranularity(final Granularity value) {
        this.setValue(this.granularity, value);
    }

    public LineStyle getLineStyle() {
        return this.lineStyle.get();
    }

    public void setLineStyle(final LineStyle value) {
        this.setValue(this.lineStyle, value);
    }

    public ResourceView getResourceView() {
        return this.resourceView.get();
    }

    public void setResourceView(final ResourceView value) {
        this.setValue(this.resourceView, value);
    }

    private <T extends Enum<T>> void setValue(final ModConfigSpec.EnumValue<T> entry, final T value) {
        Objects.requireNonNull(value);
        if (entry.get() != value) {
            entry.set(value);
            this.spec.save();
        }
    }
}
