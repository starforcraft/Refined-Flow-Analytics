package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.block.FlowMonitorBlock;
import com.ultramega.refinedflowanalytics.block.entity.FlowDetectorBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity;
import com.ultramega.refinedflowanalytics.container.FlowDetectorContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.container.FlowMonitorContainerMenu;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.refinedflowanalytics.registry.ContentIds;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.registry.ModItems;
import com.ultramega.refinedflowanalytics.registry.ModMenus;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.content.BlockEntityTypeFactory;
import com.refinedmods.refinedstorage.common.content.ExtendedMenuTypeFactory;
import com.refinedmods.refinedstorage.common.content.RegistryCallback;
import com.refinedmods.refinedstorage.common.support.containermenu.SingleAmountData;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractModInitializer {
    protected final void registerBlocks(final RegistryCallback<Block> callback) {
        ModBlocks.INSTANCE.getFlowGrid().registerBlocks(callback);
        ModBlocks.setFlowMonitor(callback.register(ContentIds.FLOW_MONITOR,
            () -> new FlowMonitorBlock(ContentIds.FLOW_MONITOR)));
        ModBlocks.INSTANCE.getFlowDetector().registerBlocks(callback);
    }

    protected final void registerItems(final RegistryCallback<Item> callback) {
        ModBlocks.INSTANCE.getFlowGrid().registerItems(callback, ModItems.INSTANCE::addFlowGrid);
        ModItems.setFlowMonitor(callback.register(ContentIds.FLOW_MONITOR, () -> ModBlocks.getFlowMonitor().createBlockItem()));
        ModBlocks.INSTANCE.getFlowDetector().registerItems(callback, ModItems.INSTANCE::addFlowDetector);
    }

    protected final void registerBlockEntities(final RegistryCallback<BlockEntityType<?>> callback,
                                               final BlockEntityTypeFactory factory) {
        ModBlockEntities.setFlowGrid(callback.register(ContentIds.FLOW_GRID,
            () -> factory.create(FlowGridBlockEntity::new, ModBlocks.INSTANCE.getFlowGrid().toArray())));
        ModBlockEntities.setFlowMonitor(callback.register(ContentIds.FLOW_MONITOR,
            () -> factory.create(FlowMonitorBlockEntity::new, ModBlocks.getFlowMonitor())));
        ModBlockEntities.setFlowDetector(callback.register(ContentIds.FLOW_DETECTOR,
            () -> factory.create(FlowDetectorBlockEntity::new, ModBlocks.INSTANCE.getFlowDetector().toArray())));
    }

    protected final void registerMenus(final RegistryCallback<MenuType<?>> callback,
                                       final ExtendedMenuTypeFactory factory) {
        ModMenus.setFlowGridMenu(callback.register(ContentIds.FLOW_GRID,
            () -> factory.create(FlowGridContainerMenu::new, BlockPos.STREAM_CODEC.cast())));
        ModMenus.setFlowMonitorMenu(callback.register(ContentIds.FLOW_MONITOR,
            () -> factory.create(FlowMonitorContainerMenu::new, ResourceContainerData.STREAM_CODEC)));
        ModMenus.setFlowDetectorMenu(callback.register(ContentIds.FLOW_DETECTOR,
            () -> factory.create(FlowDetectorContainerMenu::new, SingleAmountData.STREAM_CODEC)));
    }

    protected final void registerNetworkComponents() {
        RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory()
            .addFactory(FlowHistoryNetworkComponent.class, FlowHistoryNetworkComponent::new);
    }
}
