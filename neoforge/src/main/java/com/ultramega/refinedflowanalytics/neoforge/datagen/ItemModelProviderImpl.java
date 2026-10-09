package com.ultramega.refinedflowanalytics.neoforge.datagen;

import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.support.AbstractBaseBlock;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;

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
        this.registerFlowGrids();
        this.withExistingParent("flow_monitor", createFlowAnalyticsIdentifier("block/flow_monitor"));
        this.registerFlowDetectors();
    }

    private void registerFlowGrids() {
        ModBlocks.INSTANCE.getFlowGrid().forEach((color, id, block) -> this.withExistingParent(
            id.getPath(),
            createFlowAnalyticsIdentifier("block/flow_grid/" + color.getName())
        ));
    }

    private void registerFlowDetectors() {
        final BlockColorMap<? extends AbstractBaseBlock, BaseBlockItem> blocks = ModBlocks.INSTANCE.getFlowDetector();
        blocks.forEach((color, id, block) -> this.withExistingParent(
            id.getPath(),
            createFlowAnalyticsIdentifier("block/" + id.getPath())
        ));
    }
}
