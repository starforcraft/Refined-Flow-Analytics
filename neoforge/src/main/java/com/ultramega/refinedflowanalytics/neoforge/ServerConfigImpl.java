package com.ultramega.refinedflowanalytics.neoforge;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.registry.DefaultEnergyUsage;

import net.neoforged.neoforge.common.ModConfigSpec;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

public final class ServerConfigImpl implements ServerConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.LongValue flowGridEnergyUsage;
    private final ModConfigSpec.LongValue flowMonitorEnergyUsage;
    private final ModConfigSpec.LongValue flowDetectorEnergyUsage;

    public ServerConfigImpl() {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.translation(createTranslationKey("flowGrid")).push("flowGrid");
        this.flowGridEnergyUsage = builder.translation(createTranslationKey("flowGrid.energyUsage"))
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_GRID, 0L, Long.MAX_VALUE);
        builder.pop();

        builder.translation(createTranslationKey("flowMonitor")).push("flowMonitor");
        this.flowMonitorEnergyUsage = builder.translation(createTranslationKey("flowMonitor.energyUsage"))
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_MONITOR, 0L, Long.MAX_VALUE);
        builder.pop();

        builder.translation(createTranslationKey("flowDetector")).push("flowDetector");
        this.flowDetectorEnergyUsage = builder.translation(createTranslationKey("flowDetector.energyUsage"))
            .defineInRange("energyUsage", DefaultEnergyUsage.FLOW_DETECTOR, 0L, Long.MAX_VALUE);
        builder.pop();

        this.spec = builder.build();
    }

    private static String createTranslationKey(final String path) {
        return "text.autoconfig." + MOD_ID + "-server.option." + path;
    }

    public ModConfigSpec getSpec() {
        return this.spec;
    }

    @Override
    public long getFlowGridEnergyUsage() {
        return this.flowGridEnergyUsage.get();
    }

    @Override
    public long getFlowDetectorEnergyUsage() {
        return this.flowDetectorEnergyUsage.get();
    }

    @Override
    public long getFlowMonitorEnergyUsage() {
        return this.flowMonitorEnergyUsage.get();
    }
}
