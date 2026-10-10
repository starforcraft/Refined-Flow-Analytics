package com.ultramega.refinedflowanalytics.fabric;

import com.ultramega.refinedflowanalytics.AbstractModInitializer;
import com.ultramega.refinedflowanalytics.Platform;
import com.ultramega.refinedflowanalytics.RefinedFlowAnalyticsMod;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.CreativeModeTabItems;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.content.BlockEntityProvider;
import com.refinedmods.refinedstorage.common.content.BlockEntityTypeFactory;
import com.refinedmods.refinedstorage.common.content.DirectRegistryCallback;
import com.refinedmods.refinedstorage.common.content.ExtendedMenuTypeFactory;
import com.refinedmods.refinedstorage.fabric.api.RefinedStorageFabricApi;
import com.refinedmods.refinedstorage.fabric.api.RefinedStoragePlugin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModInitializerImpl extends AbstractModInitializer implements RefinedStoragePlugin {
    @Override
    public void onApiAvailable(final RefinedStorageApi api) {
        AutoConfig.register(ClientConfigImpl.class, Toml4jConfigSerializer::new);
        AutoConfig.register(ServerConfigImpl.class, Toml4jConfigSerializer::new);

        Platform.setClientConfigProvider(ClientConfigImpl::get);
        Platform.setServerConfigProvider(ServerConfigImpl::get);

        this.registerBlocks(new DirectRegistryCallback<>(BuiltInRegistries.BLOCK));
        this.registerItems(new DirectRegistryCallback<>(BuiltInRegistries.ITEM));
        this.registerBlockEntities(
            new DirectRegistryCallback<>(BuiltInRegistries.BLOCK_ENTITY_TYPE),
            new BlockEntityTypeFactory() {
                @Override
                public <T extends BlockEntity> BlockEntityType<T> create(final BlockEntityProvider<T> factory,
                                                                         final Block... allowedBlocks) {
                    return FabricBlockEntityTypeBuilder.create(factory::create, allowedBlocks).build();
                }
            }
        );
        this.registerMenus(new DirectRegistryCallback<>(BuiltInRegistries.MENU), new ExtendedMenuTypeFactory() {
            @Override
            public <T extends AbstractContainerMenu, D> MenuType<T> create(final MenuSupplier<T, D> supplier,
                                                                          final StreamCodec<RegistryFriendlyByteBuf, D> codec) {
                return new ExtendedMenuType<>(supplier::create, codec);
            }
        });

        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, api.getCreativeModeTabId()))
            .register(entries -> CreativeModeTabItems.appendNormal(entries::accept));
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, api.getColoredCreativeModeTabId()))
            .register(entries -> CreativeModeTabItems.appendColored(entries::accept));

        this.registerNetworkComponents();
        this.registerLookups();
        this.registerNetworking();

        ServerTickEvents.END_SERVER_TICK.register(server -> RefinedFlowAnalyticsMod.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RefinedFlowAnalyticsMod.serverStopped());
    }

    private void registerLookups() {
        RefinedStorageFabricApi.INSTANCE.getNetworkNodeContainerProviderLookup().registerForBlockEntity(
            (be, side) -> be.getContainerProvider(), ModBlockEntities.getFlowGrid());
        RefinedStorageFabricApi.INSTANCE.getNetworkNodeContainerProviderLookup().registerForBlockEntity(
            (be, side) -> be.getContainerProvider(), ModBlockEntities.getFlowMonitor());
        RefinedStorageFabricApi.INSTANCE.getNetworkNodeContainerProviderLookup().registerForBlockEntity(
            (be, side) -> be.getContainerProvider(), ModBlockEntities.getFlowDetector());
    }

    private void registerNetworking() {
        PayloadTypeRegistry.serverboundPlay().register(
            MenuStateUpdateMessage.TYPE,
            MenuStateUpdateMessage.STREAM_CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
            MenuStateUpdateMessage.TYPE,
            MenuStateUpdateMessage.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
            MenuStateUpdateMessage.TYPE,
            (message, context) -> message.handleServer(context.player())
        );
    }
}
