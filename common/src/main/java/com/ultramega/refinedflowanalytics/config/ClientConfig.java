package com.ultramega.refinedflowanalytics.config;

import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;

public interface ClientConfig {
    FlowGridEntry getFlowGrid();

    interface FlowGridEntry {
        SortingDirection getSortingDirection();

        void setSortingDirection(SortingDirection value);

        SortingType getSortingType();

        void setSortingType(SortingType value);

        Granularity getGranularity();

        void setGranularity(Granularity value);

        LineStyle getLineStyle();

        void setLineStyle(LineStyle value);

        ResourceView getResourceView();

        void setResourceView(ResourceView value);

        int getMinimumRefreshIntervalTicks();
    }
}
