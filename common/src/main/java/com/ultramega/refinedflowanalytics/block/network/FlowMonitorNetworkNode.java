package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.Platform;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowMonitorNetworkNode extends FlowHistoryNode {
    public FlowMonitorNetworkNode() {
        super(Platform.getServerConfig().getFlowMonitorEnergyUsage());
    }
}
