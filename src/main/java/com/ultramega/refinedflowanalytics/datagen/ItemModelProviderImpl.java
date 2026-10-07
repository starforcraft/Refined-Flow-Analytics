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
        this.registerFlowScope();
        this.withExistingParent("flow_scope_monitor", createFlowAnalyticsIdentifier("block/flow_scope_monitor"));
    }

    private void registerFlowScope() {
        final var blocks = ModBlocks.INSTANCE.getFlowScope();
        blocks.forEach((color, id, block) -> this.withExistingParent(
            id.getPath(),
            createFlowAnalyticsIdentifier("block/flow_scope/" + color.getName())
        ));
    }
}
