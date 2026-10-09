package com.ultramega.refinedflowanalytics.network;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.service.SnapshotService;

import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public record MenuStateUpdateMessage(int containerId, long requestId, MenuState state) implements CustomPacketPayload {
    public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(createFlowAnalyticsIdentifier("guistate_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MenuStateUpdateMessage::containerId,
        ByteBufCodecs.VAR_LONG, MenuStateUpdateMessage::requestId,
        MenuState.STREAM_CODEC, MenuStateUpdateMessage::state,
        MenuStateUpdateMessage::new
    );

    public MenuStateUpdateMessage {
        Objects.requireNonNull(state);
    }

    public void handleServer(final ServerPlayer player) {
        if (!(player.containerMenu instanceof FlowGridContainerMenu menu)
            || menu.containerId != this.containerId || !menu.stillValid(player)) {
            return;
        }
        switch (this.state) {
            case MenuState.SnapshotRequest request -> SnapshotService.execute(
                player.level(), menu.getX(), menu.getY(), menu.getZ(), player,
                this.requestId, request.granularity(), request.allStored());
            case MenuState.DetailedRequest request -> SnapshotService.executeDetailed(
                player.level(), menu.getX(), menu.getY(), menu.getZ(), player,
                this.requestId, request.resource(), request.granularity());
            default -> {
                // Responses are only accepted by the client
            }
        }
    }

    @Override
    public Type<MenuStateUpdateMessage> type() {
        return TYPE;
    }
}
