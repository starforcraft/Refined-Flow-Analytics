package com.ultramega.refinedflowanalytics.neoforge;

import com.ultramega.refinedflowanalytics.config.ClientConfig;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;

import java.util.Objects;

import net.neoforged.neoforge.common.ModConfigSpec;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ClientConfigImpl implements ClientConfig {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final ModConfigSpec spec;

    private final FlowGridEntry flowGrid;

    public ClientConfigImpl() {
        this.flowGrid = new FlowGridEntryImpl();
        this.spec = builder.build();
    }

    private static String createTranslationKey(final String path) {
        return "text.autoconfig." + MOD_ID + "-client.option." + path;
    }

    @Override
    public FlowGridEntry getFlowGrid() {
        return this.flowGrid;
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    private <T extends Enum<T>> void setValue(final ModConfigSpec.EnumValue<T> entry, final T value) {
        Objects.requireNonNull(value);
        if (entry.get() != value) {
            entry.set(value);
            this.spec.save();
        }
    }

    private class FlowGridEntryImpl implements FlowGridEntry {
        private final ModConfigSpec.EnumValue<SortingDirection> sortingDirection;
        private final ModConfigSpec.EnumValue<SortingType> sortingType;
        private final ModConfigSpec.EnumValue<Granularity> granularity;
        private final ModConfigSpec.EnumValue<LineStyle> lineStyle;
        private final ModConfigSpec.EnumValue<ResourceView> resourceView;
        private final ModConfigSpec.IntValue minimumRefreshIntervalTicks;

        private FlowGridEntryImpl() {
            builder.translation(createTranslationKey("flowGrid")).push("flowGrid");

            this.sortingDirection = builder.translation(createTranslationKey("flowGrid.sortingDirection"))
                .defineEnum("sortingDirection", SortingDirection.DESCENDING);
            this.sortingType = builder.translation(createTranslationKey("flowGrid.sortingType"))
                .defineEnum("sortingType", SortingType.QUANTITY);
            this.granularity = builder.translation(createTranslationKey("flowGrid.granularity"))
                .defineEnum("granularity", Granularity.SECOND);
            this.lineStyle = builder.translation(createTranslationKey("flowGrid.lineStyle"))
                .defineEnum("lineStyle", LineStyle.EXACT);
            this.resourceView = builder.translation(createTranslationKey("flowGrid.resourceView"))
                .defineEnum("resourceView", ResourceView.CHANGED);
            this.minimumRefreshIntervalTicks = builder.translation(createTranslationKey("flowGrid.minimumRefreshIntervalTicks"))
                .defineInRange("minimumRefreshIntervalTicks", 5, 1, 1200);

            builder.pop();
        }

        @Override
        public SortingDirection getSortingDirection() {
            return this.sortingDirection.get();
        }

        @Override
        public void setSortingDirection(final SortingDirection value) {
            ClientConfigImpl.this.setValue(this.sortingDirection, value);
        }

        @Override
        public SortingType getSortingType() {
            return this.sortingType.get();
        }

        @Override
        public void setSortingType(final SortingType value) {
            ClientConfigImpl.this.setValue(this.sortingType, value);
        }

        @Override
        public Granularity getGranularity() {
            return this.granularity.get();
        }

        @Override
        public void setGranularity(final Granularity value) {
            ClientConfigImpl.this.setValue(this.granularity, value);
        }

        @Override
        public LineStyle getLineStyle() {
            return this.lineStyle.get();
        }

        @Override
        public void setLineStyle(final LineStyle value) {
            ClientConfigImpl.this.setValue(this.lineStyle, value);
        }

        @Override
        public ResourceView getResourceView() {
            return this.resourceView.get();
        }

        @Override
        public void setResourceView(final ResourceView value) {
            ClientConfigImpl.this.setValue(this.resourceView, value);
        }

        @Override
        public int getMinimumRefreshIntervalTicks() {
            return this.minimumRefreshIntervalTicks.get();
        }
    }
}
