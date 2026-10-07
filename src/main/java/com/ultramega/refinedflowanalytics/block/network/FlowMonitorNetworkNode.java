package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowMonitorNetworkNode extends FlowHistoryNode {
    public FlowMonitorNetworkNode() {
        super(ServerConfig.INSTANCE.getFlowMonitorEnergyUsage());
    }
}
