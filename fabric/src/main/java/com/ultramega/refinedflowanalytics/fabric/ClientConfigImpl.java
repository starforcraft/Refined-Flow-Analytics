package com.ultramega.refinedflowanalytics.fabric;

import com.ultramega.refinedflowanalytics.config.ClientConfig;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;

import java.util.Objects;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@Config(name = MOD_ID + "-client")
public final class ClientConfigImpl implements ConfigData, ClientConfig {
    @ConfigEntry.Gui.CollapsibleObject
    private FlowGridEntryImpl flowGrid = new FlowGridEntryImpl();

    public static ClientConfigImpl get() {
        return AutoConfig.getConfigHolder(ClientConfigImpl.class).getConfig();
    }

    @Override
    public FlowGridEntry getFlowGrid() {
        return this.flowGrid;
    }

    private static final class FlowGridEntryImpl implements FlowGridEntry {
        @ConfigEntry.Gui.Tooltip
        private SortingDirection sortingDirection = SortingDirection.DESCENDING;
        @ConfigEntry.Gui.Tooltip
        private SortingType sortingType = SortingType.QUANTITY;
        @ConfigEntry.Gui.Tooltip
        private Granularity granularity = Granularity.SECOND;
        @ConfigEntry.Gui.Tooltip
        private LineStyle lineStyle = LineStyle.EXACT;
        @ConfigEntry.Gui.Tooltip
        private ResourceView resourceView = ResourceView.CHANGED;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 1200)
        private int minimumRefreshIntervalTicks = 5;

        @Override
        public SortingDirection getSortingDirection() {
            return this.sortingDirection;
        }

        @Override
        public void setSortingDirection(final SortingDirection value) {
            Objects.requireNonNull(value);
            if (this.sortingDirection != value) {
                this.sortingDirection = value;
                AutoConfig.getConfigHolder(ClientConfigImpl.class).save();
            }
        }

        @Override
        public SortingType getSortingType() {
            return this.sortingType;
        }

        @Override
        public void setSortingType(final SortingType value) {
            Objects.requireNonNull(value);
            if (this.sortingType != value) {
                this.sortingType = value;
                AutoConfig.getConfigHolder(ClientConfigImpl.class).save();
            }
        }

        @Override
        public Granularity getGranularity() {
            return this.granularity;
        }

        @Override
        public void setGranularity(final Granularity value) {
            Objects.requireNonNull(value);
            if (this.granularity != value) {
                this.granularity = value;
                AutoConfig.getConfigHolder(ClientConfigImpl.class).save();
            }
        }

        @Override
        public LineStyle getLineStyle() {
            return this.lineStyle;
        }

        @Override
        public void setLineStyle(final LineStyle value) {
            Objects.requireNonNull(value);
            if (this.lineStyle != value) {
                this.lineStyle = value;
                AutoConfig.getConfigHolder(ClientConfigImpl.class).save();
            }
        }

        @Override
        public ResourceView getResourceView() {
            return this.resourceView;
        }

        @Override
        public void setResourceView(final ResourceView value) {
            Objects.requireNonNull(value);
            if (this.resourceView != value) {
                this.resourceView = value;
                AutoConfig.getConfigHolder(ClientConfigImpl.class).save();
            }
        }

        @Override
        public int getMinimumRefreshIntervalTicks() {
            return Math.clamp(this.minimumRefreshIntervalTicks, 1, 1200);
        }
    }
}
