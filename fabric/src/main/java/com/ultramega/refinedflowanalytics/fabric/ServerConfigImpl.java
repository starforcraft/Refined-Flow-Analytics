package com.ultramega.refinedflowanalytics.fabric;

import com.ultramega.refinedflowanalytics.config.ServerConfig;
import com.ultramega.refinedflowanalytics.registry.DefaultEnergyUsage;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;

@Config(name = MOD_ID + "-server")
public final class ServerConfigImpl implements ConfigData, ServerConfig {
    @ConfigEntry.Gui.CollapsibleObject
    private EnergyEntry flowGrid = new EnergyEntry(DefaultEnergyUsage.FLOW_GRID);
    @ConfigEntry.Gui.CollapsibleObject
    private EnergyEntry flowMonitor = new EnergyEntry(DefaultEnergyUsage.FLOW_MONITOR);
    @ConfigEntry.Gui.CollapsibleObject
    private EnergyEntry flowDetector = new EnergyEntry(DefaultEnergyUsage.FLOW_DETECTOR);

    public static ServerConfigImpl get() {
        return AutoConfig.getConfigHolder(ServerConfigImpl.class).getConfig();
    }

    @Override
    public long getFlowGridEnergyUsage() {
        return this.flowGrid.energyUsage;
    }

    @Override
    public long getFlowMonitorEnergyUsage() {
        return this.flowMonitor.energyUsage;
    }

    @Override
    public long getFlowDetectorEnergyUsage() {
        return this.flowDetector.energyUsage;
    }

    private static final class EnergyEntry {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = Long.MAX_VALUE)
        private long energyUsage;

        private EnergyEntry(final long energyUsage) {
            this.energyUsage = energyUsage;
        }
    }
}
