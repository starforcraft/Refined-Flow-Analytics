package com.ultramega.refinedflowanalytics.neoforge;

import com.ultramega.refinedflowanalytics.AbstractModInitializer;
import com.ultramega.refinedflowanalytics.Platform;
import com.ultramega.refinedflowanalytics.RefinedFlowAnalyticsMod;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.CreativeModeTabItems;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.content.BlockEntityProvider;
import com.refinedmods.refinedstorage.common.content.BlockEntityTypeFactory;
import com.refinedmods.refinedstorage.common.content.ExtendedMenuTypeFactory;
import com.refinedmods.refinedstorage.common.content.RegistryCallback;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@Mod(MOD_ID)
public final class ModInitializer extends AbstractModInitializer {
    public ModInitializer(final IEventBus eventBus, final ModContainer modContainer) {
        final ClientConfigImpl clientConfig = new ClientConfigImpl();
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientConfig.getSpec());
        Platform.setClientConfigProvider(() -> clientConfig);
        final ServerConfigImpl serverConfig = new ServerConfigImpl();
        modContainer.registerConfig(ModConfig.Type.SERVER, serverConfig.getSpec());
        Platform.setServerConfigProvider(() -> serverConfig);

        this.registerContent(eventBus);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::registerPayloads);
        eventBus.addListener(this::registerCapabilities);
        eventBus.addListener(this::buildCreativeTabs);
        NeoForge.EVENT_BUS.addListener((final ServerTickEvent.Post event) -> RefinedFlowAnalyticsMod.tick());
        NeoForge.EVENT_BUS.addListener((final ServerStoppedEvent event) -> RefinedFlowAnalyticsMod.serverStopped());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModInitializer.register(eventBus);
        }
    }

    private void registerContent(final IEventBus eventBus) {
        final DeferredRegister<Block> blocks = DeferredRegister.create(BuiltInRegistries.BLOCK, MOD_ID);
        final DeferredRegister<Item> items = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);
        final DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);
        final DeferredRegister<MenuType<?>> menus = DeferredRegister.create(BuiltInRegistries.MENU, MOD_ID);

        this.registerBlocks(new ForgeRegistryCallback<>(blocks));
        this.registerItems(new ForgeRegistryCallback<>(items));
        this.registerBlockEntities(new ForgeRegistryCallback<>(entities), new BlockEntityTypeFactory() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> create(final BlockEntityProvider<T> factory,
                                                                    final Block... allowedBlocks) {
                return BlockEntityType.Builder.of(factory::create, allowedBlocks).build(null);
            }
        });
        this.registerMenus(new ForgeRegistryCallback<>(menus), new ExtendedMenuTypeFactory() {
            @Override
            public <T extends AbstractContainerMenu, D> MenuType<T> create(final MenuSupplier<T, D> supplier,
                                                                          final StreamCodec<RegistryFriendlyByteBuf, D> codec) {
                return IMenuTypeExtension.create((id, inventory, buffer) -> supplier.create(id, inventory, codec.decode(buffer)));
            }
        });
        blocks.register(eventBus);
        items.register(eventBus);
        entities.register(eventBus);
        menus.register(eventBus);
    }

    private void buildCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        final ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(RefinedStorageApi.INSTANCE.getCreativeModeTabId())) {
            CreativeModeTabItems.appendNormal(event::accept);
        } else if (tab.equals(RefinedStorageApi.INSTANCE.getColoredCreativeModeTabId())) {
            CreativeModeTabItems.appendColored(event::accept);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.MAIN);

        registrar.playBidirectional(
            MenuStateUpdateMessage.TYPE,
            MenuStateUpdateMessage.STREAM_CODEC,
            (message, context) -> {
                if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player) {
                    message.handleServer(player);
                } else if (context.flow() == PacketFlow.CLIENTBOUND) {
                    ClientModInitializer.handleMenuState(message);
                }
            }
        );
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(this::registerNetworkComponents);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            ModBlockEntities.getFlowGrid(), (be, side) -> be.getContainerProvider());
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            ModBlockEntities.getFlowMonitor(), (be, side) -> be.getContainerProvider());
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            ModBlockEntities.getFlowDetector(), (be, side) -> be.getContainerProvider());
    }

    private record ForgeRegistryCallback<T>(DeferredRegister<T> registry) implements RegistryCallback<T> {
        @Override
        public <R extends T> Supplier<R> register(final ResourceLocation id, final Supplier<R> factory) {
            return this.registry.register(id.getPath(), factory);
        }
    }
}
