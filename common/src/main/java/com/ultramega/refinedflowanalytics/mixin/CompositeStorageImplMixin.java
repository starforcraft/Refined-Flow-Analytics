package com.ultramega.refinedflowanalytics.mixin;

import com.ultramega.refinedflowanalytics.api.StorageSourceChangeContext;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;

import java.util.Collection;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CompositeStorageImpl.class, remap = false)
public abstract class CompositeStorageImplMixin {
    // These paths also handle drives added to/removed from nested composites
    @Redirect(
        method = {"addContentOfSourceToList", "removeContentOfSourceFromList"},
        at = @At(value = "INVOKE", target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"),
        require = 2
    )
    private void flowanalytics$updateSourceContents(final Collection<ResourceAmount> resources,
                                                    final Consumer<ResourceAmount> update) {
        StorageSourceChangeContext.run(() -> resources.forEach(update));
    }
}
