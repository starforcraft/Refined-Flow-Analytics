package com.ultramega.refinedflowanalytics.config;

import com.ultramega.refinedflowanalytics.registry.DefaultEnergyUsage;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig { //TODO: change to interface configs
    public static final ServerConfig INSTANCE = new ServerConfig();

    private final ModConfigSpec spec;
    private final ModConfigSpec.LongValue flowGridEnergyUsage;
    private final ModConfigSpec.LongValue flowMonitorEnergyUsage;

    private ServerConfig() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("flowGrid");
        this.flowGridEnergyUsage = builder.comment("Energy consumed per tick by each Flow Grid. Set to 0 to disable energy consumption.")
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_GRID, 0L, Long.MAX_VALUE);
        builder.pop();

        builder.push("flowMonitor");
        this.flowMonitorEnergyUsage = builder.comment("Energy consumed per tick by each Flow Monitor. Set to 0 to disable energy consumption.")
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_MONITOR, 0L, Long.MAX_VALUE);
        builder.pop();

        this.spec = builder.build();
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    public long getFlowGridEnergyUsage() {
        return this.flowGridEnergyUsage.get();
    }

    public long getFlowMonitorEnergyUsage() {
        return this.flowMonitorEnergyUsage.get();
    }
}
