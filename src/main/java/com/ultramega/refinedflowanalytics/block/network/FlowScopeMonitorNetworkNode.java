package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowScopeMonitorNetworkNode extends FlowHistoryNode {
    public FlowScopeMonitorNetworkNode() {
        super(ServerConfig.INSTANCE.getFlowScopeMonitorEnergyUsage());
    }
}
