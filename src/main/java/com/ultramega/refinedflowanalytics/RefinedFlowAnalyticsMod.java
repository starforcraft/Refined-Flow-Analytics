package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.CreativeModeTabItems;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;
import com.ultramega.refinedflowanalytics.registry.ModItems;
import com.ultramega.refinedflowanalytics.registry.ModMenus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.util.Tuple;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@Mod(MOD_ID)
public class RefinedFlowAnalyticsMod {
    private static final Collection<Tuple<Runnable, Integer>> WORK_QUEUE = new ConcurrentLinkedQueue<>();

    public RefinedFlowAnalyticsMod(final IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerNetworking);

        ModBlocks.REGISTRY.register(modEventBus);
        ModBlockEntities.REGISTRY.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);
        CreativeModeTabItems.REGISTRY.register(modEventBus);
        ModMenus.REGISTRY.register(modEventBus);
    }

    public static void queueServerWork(final int tick, final Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            WORK_QUEUE.add(new Tuple<>(action, tick));
        }
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playBidirectional(MenuStateUpdateMessage.TYPE, MenuStateUpdateMessage.STREAM_CODEC, MenuStateUpdateMessage::handleMenuState);
    }

    @SubscribeEvent
    public void tick(final ServerTickEvent.Post event) {
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
}
