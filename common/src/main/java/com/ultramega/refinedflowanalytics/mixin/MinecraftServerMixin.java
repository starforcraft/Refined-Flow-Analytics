package com.ultramega.refinedflowanalytics.mixin;

import com.ultramega.refinedflowanalytics.data.FlowSnapshotData;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void flowanalytics$flushHistory(final boolean suppressLog,
                                            final boolean flush,
                                            final boolean forced,
                                            final CallbackInfoReturnable<Boolean> callback) {
        if (flush) {
            FlowSnapshotData.flushPendingSaves();
        }
    }
}
