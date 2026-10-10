package com.ultramega.refinedflowanalytics.block;

import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlock;
import com.refinedmods.refinedstorage.common.support.AbstractBlockEntityTicker;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;
import com.refinedmods.refinedstorage.common.support.BlockItemProvider;
import com.refinedmods.refinedstorage.common.support.network.NetworkNodeBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowGridBlock extends AbstractGridBlock<FlowGridBlock, BaseBlockItem> implements BlockItemProvider<BaseBlockItem> {
    private static final Component HELP = createFlowAnalyticsTranslation("item", "flow_grid.help");
    private static final AbstractBlockEntityTicker<FlowGridBlockEntity> TICKER = new NetworkNodeBlockEntityTicker<>(ModBlockEntities::getFlowGrid, ACTIVE);

    private final Identifier id;

    public FlowGridBlock(final Identifier id, final DyeColor color, final MutableComponent name) {
        super(id, color, name);
        this.id = id;
    }

    @Override
    public BlockColorMap<FlowGridBlock, BaseBlockItem> getBlockColorMap() {
        return ModBlocks.INSTANCE.getFlowGrid();
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new FlowGridBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TICKER.get(level, type);
    }

    @Override
    public BaseBlockItem createBlockItem() {
        return new BaseBlockItem(id, this, HELP);
    }
}
