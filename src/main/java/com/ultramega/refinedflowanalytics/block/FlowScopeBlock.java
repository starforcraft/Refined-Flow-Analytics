package com.ultramega.refinedflowanalytics.block;

import com.ultramega.refinedflowanalytics.block.entity.FlowScopeBlockEntity;
import com.ultramega.refinedflowanalytics.registry.ModBlockEntities;
import com.ultramega.refinedflowanalytics.registry.ModBlocks;

import com.refinedmods.refinedstorage.common.content.BlockColorMap;
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlock;
import com.refinedmods.refinedstorage.common.support.AbstractBlockEntityTicker;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;
import com.refinedmods.refinedstorage.common.support.BlockItemProvider;
import com.refinedmods.refinedstorage.common.support.network.NetworkNodeBlockEntityTicker;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FlowScopeBlock extends AbstractGridBlock<FlowScopeBlock, BaseBlockItem> implements BlockItemProvider<BaseBlockItem> {
//    public static final IntegerProperty SORT_TYPE = IntegerProperty.create("sort_type", 0, 1); //TODO: why is this an integerproperty?
//    public static final IntegerProperty SORT_DIRECTION = IntegerProperty.create("sort_direction", 0, 1);
//    public static final IntegerProperty GRANULARITY = IntegerProperty.create("granularity", 0, 3);

    private static final AbstractBlockEntityTicker<FlowScopeBlockEntity> TICKER = new NetworkNodeBlockEntityTicker<>(ModBlockEntities.FLOW_SCOPE, ACTIVE);

    public FlowScopeBlock(final DyeColor color, final MutableComponent name) {
        super(name, color);
    }

    @Override
    public BlockColorMap<FlowScopeBlock, BaseBlockItem> getBlockColorMap() {
        return ModBlocks.INSTANCE.getFlowScope();
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new FlowScopeBlockEntity(pos, state);
    }

    // TODO: what is this? Is this required?
    @Override
    public boolean triggerEvent(final BlockState state, final Level level, final BlockPos pos, final int eventId, final int eventParam) {
        final boolean handled = super.triggerEvent(state, level, pos, eventId, eventParam);
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        return (blockEntity != null && blockEntity.triggerEvent(eventId, eventParam)) || handled;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TICKER.get(level, type);
    }

    @Override
    public BaseBlockItem createBlockItem() {
        return new BaseBlockItem(this);
    }
}
