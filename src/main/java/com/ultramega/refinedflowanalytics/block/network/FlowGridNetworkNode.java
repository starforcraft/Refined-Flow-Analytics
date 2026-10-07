package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowGridNetworkNode extends FlowHistoryNode {
    public FlowGridNetworkNode() {
        super(ServerConfig.INSTANCE.getFlowGridEnergyUsage());
    }
}
