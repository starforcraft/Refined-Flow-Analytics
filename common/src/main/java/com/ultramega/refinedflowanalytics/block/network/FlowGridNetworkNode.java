package com.ultramega.refinedflowanalytics.block.network;

import com.ultramega.refinedflowanalytics.Platform;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNode;

public class FlowGridNetworkNode extends FlowHistoryNode {
    public FlowGridNetworkNode() {
        super(Platform.getServerConfig().getFlowGridEnergyUsage());
    }
}
