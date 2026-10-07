package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowScopeNetworkNode extends FlowHistoryNode {
    public FlowScopeNetworkNode() {
        super(ServerConfig.INSTANCE.getFlowScopeEnergyUsage());
    }
}
