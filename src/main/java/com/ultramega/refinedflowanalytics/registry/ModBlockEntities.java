package com.ultramega.refinedflowanalytics.registry;

import com.ultramega.refinedflowanalytics.block.entity.FlowDetectorBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity;

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

    public static final Supplier<BlockEntityType<FlowGridBlockEntity>> FLOW_GRID = REGISTRY.register("flow_grid", () -> BlockEntityType.Builder.of(
        FlowGridBlockEntity::new,
        ModBlocks.INSTANCE.getFlowGrid().toArray()
    ).build(null));

    public static final Supplier<BlockEntityType<FlowMonitorBlockEntity>> FLOW_MONITOR = REGISTRY.register("flow_monitor", () -> BlockEntityType.Builder.of(
        FlowMonitorBlockEntity::new,
        ModBlocks.FLOW_MONITOR.get()
    ).build(null));

    public static final Supplier<BlockEntityType<FlowDetectorBlockEntity>> FLOW_DETECTOR = REGISTRY.register("flow_detector", () -> BlockEntityType.Builder.of(
        FlowDetectorBlockEntity::new,
        ModBlocks.INSTANCE.getFlowDetector().toArray()
    ).build(null));

    private ModBlockEntities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            FLOW_GRID.get(),
            (be, side) -> be.getContainerProvider()
        );
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            FLOW_MONITOR.get(),
            (be, side) -> be.getContainerProvider()
        );
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            FLOW_DETECTOR.get(),
            (be, side) -> be.getContainerProvider()
        );
    }
}
