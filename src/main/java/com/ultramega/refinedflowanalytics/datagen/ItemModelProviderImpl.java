package com.ultramega.refinedflowanalytics.datagen;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.MOD_ID;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class ItemModelProviderImpl extends ItemModelProvider {
    public ItemModelProviderImpl(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.registerFlowGrid();
        this.withExistingParent("flow_monitor", createFlowAnalyticsIdentifier("block/flow_monitor"));
    }

    private void registerFlowGrid() {
        final var blocks = ModBlocks.INSTANCE.getFlowGrid();
        blocks.forEach((color, id, block) -> this.withExistingParent(
            id.getPath(),
            createFlowAnalyticsIdentifier("block/flow_grid/" + color.getName())
        ));
    }
}
