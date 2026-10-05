package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.entity.FlowScopeBlockEntity;

import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final Supplier<BlockEntityType<FlowScopeBlockEntity>> FLOW_SCOPE = REGISTRY.register("flow_scope", () -> BlockEntityType.Builder.of(
        FlowScopeBlockEntity::new,
        ModBlocks.INSTANCE.getFlowScope().toArray()
    ).build(null));

    private ModBlockEntities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(), FLOW_SCOPE.get(),
            (be, side) -> be.getContainerProvider());
    }
}
