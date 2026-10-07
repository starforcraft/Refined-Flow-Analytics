package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.block.FlowMonitorBlock;
import com.ultramega.refinedflowanalytics.config.ClientConfig;
import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.CreativeModeTabItems;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.registry.ModItems;
import com.ultramega.refinedflowanalytics.registry.ModMenus;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.content.RegistryCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@Mod(MOD_ID)
public class RefinedFlowAnalyticsMod {
    private static final Collection<Tuple<Runnable, Integer>> WORK_QUEUE = new ConcurrentLinkedQueue<>();

    // TODO: make multiloader-project
    public RefinedFlowAnalyticsMod(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.INSTANCE.getSpec());
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.INSTANCE.getSpec());
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(this::registerNetworkComponents);

        this.registerBlocksAndItems(modEventBus);
        ModBlockEntities.REGISTRY.register(modEventBus);
        CreativeModeTabItems.REGISTRY.register(modEventBus);
        ModMenus.REGISTRY.register(modEventBus);
    }

    private void registerBlocksAndItems(final IEventBus modEventBus) {
        final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(MOD_ID);
        final DeferredRegister.Items items = DeferredRegister.createItems(MOD_ID);

        ModBlocks.INSTANCE.getFlowGrid().registerBlocks(new RegistryCallback<>() {
            @Override
            public <R extends Block> Supplier<R> register(final ResourceLocation id, final Supplier<R> factory) {
                return blocks.register(id.getPath(), factory);
            }
        });
        ModBlocks.INSTANCE.getFlowGrid().registerItems(new RegistryCallback<>() {
            @Override
            public <R extends Item> Supplier<R> register(final ResourceLocation id, final Supplier<R> factory) {
                return items.register(id.getPath(), factory);
            }
        }, ModItems.INSTANCE::addFlowGrid);

        blocks.register("flow_monitor", FlowMonitorBlock::new);
        items.register("flow_monitor", () -> ModBlocks.FLOW_MONITOR.get().createBlockItem());

        blocks.register(modEventBus);
        items.register(modEventBus);
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playBidirectional(MenuStateUpdateMessage.TYPE, MenuStateUpdateMessage.STREAM_CODEC, MenuStateUpdateMessage::handleMenuState);
    }

    private void registerNetworkComponents(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory()
            .addFactory(FlowHistoryNetworkComponent.class, FlowHistoryNetworkComponent::new));
    }

    public static void queueServerWork(final int tick, final Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            WORK_QUEUE.add(new Tuple<>(action, tick));
        }
    }

    @SubscribeEvent
    public void tick(final ServerTickEvent.Post event) {
        FlowHistoryNetworkComponent.tickAll();
        final List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
        WORK_QUEUE.forEach(work -> {
            work.setB(work.getB() - 1);
            if (work.getB() == 0) {
                actions.add(work);
            }
        });
        actions.forEach(e -> e.getA().run());
        WORK_QUEUE.removeAll(actions);
    }

    @SubscribeEvent
    public void serverStopped(final ServerStoppedEvent event) {
        FlowHistoryNetworkComponent.clear();
    }
}
