package com.ultramega.refinedflowanalytics.network;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.registry.ModScreens;
import com.ultramega.refinedflowanalytics.service.SnapshotService;

import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public record MenuStateUpdateMessage(int containerId, MenuState state) implements CustomPacketPayload {
    public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(createFlowAnalyticsIdentifier("guistate_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MenuStateUpdateMessage::containerId,
        MenuState.STREAM_CODEC, MenuStateUpdateMessage::state,
        MenuStateUpdateMessage::new
    );

    public MenuStateUpdateMessage {
        Objects.requireNonNull(state);
    }

    public static void handleMenuState(final MenuStateUpdateMessage message, final IPayloadContext context) {
        if (!(context.player().containerMenu instanceof FlowGridContainerMenu menu)
            || menu.containerId != message.containerId()) {
            return;
        }
        if (context.flow() == PacketFlow.SERVERBOUND) {
            if (!menu.stillValid(context.player())) {
                return;
            }
            switch (message.state()) {
                case MenuState.SnapshotRequest request -> SnapshotService.execute(
                    context.player().level(), menu.getX(), menu.getY(), menu.getZ(), context.player(),
                    request.granularity(), request.allStored());
                case MenuState.DetailedRequest request -> SnapshotService.executeDetailed(
                    context.player().level(), menu.getX(), menu.getY(), menu.getZ(), context.player(),
                    request.resource(), request.granularity());
                default -> {
                }
            }
        } else if (message.state() instanceof MenuState.Snapshot || message.state() instanceof MenuState.DetailedSnapshot) {
            ModScreens.updateMenuState(message);
        }
    }

    @Override
    public Type<MenuStateUpdateMessage> type() {
        return TYPE;
    }
}
