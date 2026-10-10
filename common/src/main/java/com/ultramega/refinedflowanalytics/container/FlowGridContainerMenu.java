package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.Platform;
import com.ultramega.refinedflowanalytics.block.entity.FlowGridBlockEntity;
import com.ultramega.refinedflowanalytics.network.MenuState;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModMenus;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class FlowGridContainerMenu extends AbstractContainerMenu {
    public final Level world;
    public final Player entity;
    private final ContainerLevelAccess access;

    private final int x;
    private final int y;
    private final int z;

    public FlowGridContainerMenu(final int id, final Inventory inv, final BlockPos pos) {
        super(ModMenus.getFlowGridMenu(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.access = ContainerLevelAccess.create(this.world, pos);
    }

    @Override
    public boolean stillValid(final Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockEntity(pos) instanceof FlowGridBlockEntity blockEntity && Container.stillValidBlockEntity(blockEntity, player), true);
    }

    @Override
    public ItemStack quickMoveStack(final Player playerIn, final int index) {
        return ItemStack.EMPTY;
    }

    public void sendMenuStateUpdate(final Player player, final long requestId, final MenuState state) {
        final MenuStateUpdateMessage message = new MenuStateUpdateMessage(this.containerId, requestId, state);
        if (player instanceof ServerPlayer serverPlayer) {
            com.refinedmods.refinedstorage.common.Platform.INSTANCE.sendPacketToClient(serverPlayer, message);
        } else if (player.level().isClientSide()) {
            com.refinedmods.refinedstorage.common.Platform.INSTANCE.sendPacketToServer(message);
        }
    }

    public SortingDirection getSortingDirection() {
        return this.world.isClientSide() ? Platform.getClientConfig().getFlowGrid().getSortingDirection() : SortingDirection.DESCENDING;
    }

    public void setSortingDirection(final SortingDirection value) {
        if (this.world.isClientSide()) {
            Platform.getClientConfig().getFlowGrid().setSortingDirection(value);
        }
    }

    public SortingType getSortingType() {
        return this.world.isClientSide() ? Platform.getClientConfig().getFlowGrid().getSortingType() : SortingType.QUANTITY;
    }

    public void setSortingType(final SortingType value) {
        if (this.world.isClientSide()) {
            Platform.getClientConfig().getFlowGrid().setSortingType(value);
        }
    }

    public Granularity getGranularity() {
        return this.world.isClientSide() ? Platform.getClientConfig().getFlowGrid().getGranularity() : Granularity.MINUTE;
    }

    public void setGranularity(final Granularity value) {
        if (this.world.isClientSide()) {
            Platform.getClientConfig().getFlowGrid().setGranularity(value);
        }
    }

    public LineStyle getLineStyle() {
        return this.world.isClientSide() ? Platform.getClientConfig().getFlowGrid().getLineStyle() : LineStyle.BLOCKY;
    }

    public void setLineStyle(final LineStyle value) {
        if (this.world.isClientSide()) {
            Platform.getClientConfig().getFlowGrid().setLineStyle(value);
        }
    }

    public ResourceView getResourceView() {
        return this.world.isClientSide() ? Platform.getClientConfig().getFlowGrid().getResourceView() : ResourceView.CHANGED;
    }

    public void setResourceView(final ResourceView value) {
        if (this.world.isClientSide()) {
            Platform.getClientConfig().getFlowGrid().setResourceView(value);
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }
}
