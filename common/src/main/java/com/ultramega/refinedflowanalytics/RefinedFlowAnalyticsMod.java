package com.ultramega.refinedflowanalytics;

import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.server.MinecraftServer;

public final class RefinedFlowAnalyticsMod {
    private static final Queue<Runnable> WORK_QUEUE = new ArrayDeque<>();

    private RefinedFlowAnalyticsMod() {
    }

    public static void queueServerWork(final MinecraftServer server, final Runnable action) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Flow snapshots must be requested on the server thread");
        }
        WORK_QUEUE.add(action);
    }

    public static void tick() {
        FlowHistoryNetworkComponent.tickAll();
        final int pending = WORK_QUEUE.size();
        for (int i = 0; i < pending; i++) {
            WORK_QUEUE.remove().run();
        }
    }

    public static void serverStopped() {
        WORK_QUEUE.clear();
        FlowHistoryNetworkComponent.clear();
    }
}
