package com.ultramega.refinedflowanalytics.mixin;

import com.ultramega.refinedflowanalytics.data.FlowHistoryStorage;
import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

@Mixin(SavedDataStorage.class)
public abstract class SavedDataStorageMixin implements FlowHistoryStorage {
    // Keep histories out of the vanilla cache, which materializes a tag tree before scheduling IO
    @Unique
    private final Map<Path, FlowSnapshotData> refinedflowanalytics$histories = new HashMap<>();

    @Invoker("getDataFile")
    protected abstract Path refinedflowanalytics$getDataFile(Identifier id);

    @Override
    public FlowSnapshotData refinedflowanalytics$getHistory(final UUID networkId) {
        final Path path = this.refinedflowanalytics$getDataFile(createFlowAnalyticsIdentifier("network_history/" + networkId));
        return this.refinedflowanalytics$histories.computeIfAbsent(path, FlowSnapshotData::open);
    }

    @Inject(method = "collectDirtyTagsToSave", at = @At("HEAD"))
    private void refinedflowanalytics$scheduleHistorySaves(final CallbackInfoReturnable<?> callback) {
        this.refinedflowanalytics$histories.forEach((path, history) -> history.save(path));
    }

    // Both a flushed world save and SavedDataStorage.close() use this method.
    @Inject(method = "saveAndJoin", at = @At("RETURN"))
    private void refinedflowanalytics$awaitHistorySaves(final CallbackInfo callback) {
        if (!this.refinedflowanalytics$histories.isEmpty()) {
            FlowSnapshotData.flushPendingSaves();
        }
    }
}
