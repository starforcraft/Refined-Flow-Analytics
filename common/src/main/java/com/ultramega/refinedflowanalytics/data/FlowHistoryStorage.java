package com.ultramega.refinedflowanalytics.data;

import java.util.UUID;

public interface FlowHistoryStorage {
    FlowSnapshotData refinedflowanalytics$getHistory(UUID networkId);
}
