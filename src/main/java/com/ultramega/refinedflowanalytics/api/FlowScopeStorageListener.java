package com.ultramega.refinedflowanalytics.api;

import com.ultramega.refinedflowanalytics.resource.ResourceChangeKey;

import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class FlowScopeStorageListener implements RootStorageListener {
    private final Map<ResourceChangeKey, Long> deltaChange = new HashMap<>();

    @Nullable
    private Map<ResourceChangeKey, Long> lastSnapshot;

    public FlowScopeStorageListener(@Nullable final Map<ResourceChangeKey, Long> savedLastSnapshot) {
        this.lastSnapshot = savedLastSnapshot;
    }

    public ResourceChangeKey getSnapshotKey(final PlatformResourceKey resourceKey, final short sign) {
        ResourceChangeKey snapshotKey = new ResourceChangeKey(resourceKey, sign);
        try {
            snapshotKey = new ResourceChangeKey(
                (PlatformResourceKey) ((ItemResource) snapshotKey.resourceKey()).normalize(), sign);
        } catch (final Exception ignored) {
        }
        return snapshotKey;
    }

    @Override
    public void changed(final MutableResourceList.OperationResult change) {
        if (change.change() == 0) {
            return;
        }
        // Abstract condition to filter out network change
        if ((Math.abs(change.change()) >= (change.amount() * .5)) && Math.abs(change.change()) > 30) {
            return;
        }
        final short sign = (short) (change.change() >= 0 ? +1 : -1);
        final ResourceChangeKey snapshotKey = this.getSnapshotKey((PlatformResourceKey) change.resource(), sign);
        final long newDeltaChange = this.deltaChange.getOrDefault(snapshotKey, 0L) + change.change();
        this.deltaChange.put(snapshotKey, newDeltaChange);
    }

    public Map<ResourceChangeKey, Long> flushDeltaChange() {
        this.lastSnapshot = this.deltaChange.isEmpty() ? Map.of() : new HashMap<>(this.deltaChange);
        this.deltaChange.clear();
        return this.lastSnapshot;
    }

    @Nullable
    public Map<ResourceChangeKey, Long> getLastSnapshot() {
        return this.lastSnapshot;
    }
}
