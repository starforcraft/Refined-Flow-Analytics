package com.ultramega.refinedflowanalytics.config;

import com.ultramega.refinedflowanalytics.registry.DefaultEnergyUsage;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig { //TODO: change to interface configs
    public static final ServerConfig INSTANCE = new ServerConfig();

    private final ModConfigSpec spec;
    private final ModConfigSpec.LongValue flowScopeEnergyUsage;
    private final ModConfigSpec.LongValue flowScopeMonitorEnergyUsage;

    private ServerConfig() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("flowScope");
        this.flowScopeEnergyUsage = builder.comment("Energy consumed per tick by each Flow Scope. Set to 0 to disable energy consumption.")
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_SCOPE, 0L, Long.MAX_VALUE);
        builder.pop();

        builder.push("flowScopeMonitor");
        this.flowScopeMonitorEnergyUsage = builder.comment("Energy consumed per tick by each Flow Scope Monitor. Set to 0 to disable energy consumption.")
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_SCOPE_MONITOR, 0L, Long.MAX_VALUE);
        builder.pop();

        this.spec = builder.build();
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    public long getFlowScopeEnergyUsage() {
        return this.flowScopeEnergyUsage.get();
    }

    public long getFlowScopeMonitorEnergyUsage() {
        return this.flowScopeMonitorEnergyUsage.get();
    }
}
