package com.ultramega.refinedflowanalytics.container;

import com.ultramega.refinedflowanalytics.block.entity.FlowScopeBlockEntity;
import com.ultramega.refinedflowanalytics.network.MenuStateUpdateMessage;
import com.ultramega.refinedflowanalytics.registry.ModMenus;
import com.ultramega.refinedflowanalytics.registry.ModScreens;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class FlowScopeContainerMenu extends AbstractContainerMenu {
    public final Level world;
    public final Player entity;
    private final ContainerLevelAccess access;

    private final int x;
    private final int y;
    private final int z;

    public FlowScopeContainerMenu(final int id, final Inventory inv, final FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos());
    }

    public FlowScopeContainerMenu(final int id, final Inventory inv, final BlockPos pos) {
        super(ModMenus.FLOW_SCOPE_MENU.get(), id);
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
            level.getBlockEntity(pos) instanceof FlowScopeBlockEntity blockEntity && Container.stillValidBlockEntity(blockEntity, player), true);
    }

    @Override
    public ItemStack quickMoveStack(final Player playerIn, final int index) {
        return ItemStack.EMPTY;
    }

    public void sendMenuStateUpdate(final Player player, final int elementType, final String name, final Object elementState, final boolean needClientUpdate) {
        final MenuStateUpdateMessage message = new MenuStateUpdateMessage(elementType, name, elementState);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        } else if (player.level().isClientSide) {
            if (needClientUpdate) {
                ModScreens.updateMenuState(message);
            }
            PacketDistributor.sendToServer(message);
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
