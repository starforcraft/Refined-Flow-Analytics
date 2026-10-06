package com.ultramega.refinedflowanalytics.network;

import com.ultramega.refinedflowanalytics.container.FlowScopeContainerMenu;
import com.ultramega.refinedflowanalytics.registry.ModScreens;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.service.SnapshotService;

import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public record MenuStateUpdateMessage(int elementType, String name, Object elementState) implements CustomPacketPayload {
    public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(createFlowAnalyticsIdentifier("guistate_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC =
        StreamCodec.of(MenuStateUpdateMessage::write, MenuStateUpdateMessage::read);

    // TODO: switch to proper codecs
    @SuppressWarnings("unchecked")
    public static void write(final FriendlyByteBuf buffer, final MenuStateUpdateMessage message) {
        buffer.writeInt(message.elementType);
        buffer.writeUtf(message.name);
        if (message.elementType == 0) {
            buffer.writeUtf((String) message.elementState);
        } else if (message.elementType == 1) {
            buffer.writeBoolean((boolean) message.elementState);
        } else if (message.elementType == 2) {
            buffer.writeMap(
                (Map<PlatformResourceKey, Map<Short, Long>>) message.elementState,
                (buf, key) -> ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, key),
                (buf, value) -> buf.writeMap(
                    value,
                    (b, s) -> b.writeShort(s),
                    FriendlyByteBuf::writeLong
                )
            );
        } else if (message.elementType == 3) {
            final Map<ResourceChangeGranularityKey, long[]> state = (Map<ResourceChangeGranularityKey, long[]>) message.elementState;
            final ResourceChangeGranularityKey firstKey = state.keySet().iterator().next();
            ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, firstKey.resourceKey());
            buffer.writeMap(
                state,
                (buf, key) -> buf.writeShort(key.sign()),
                FriendlyByteBuf::writeLongArray
            );
            buffer.writeInt(firstKey.granularity());
        } else if (message.elementType == 4) {
            buffer.writeInt((int) message.elementState);
        } else if (message.elementType == 5) {
            final ResourceChangeGranularityKey itemKey = (ResourceChangeGranularityKey) message.elementState;
            ResourceCodecs.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, itemKey.resourceKey());
            buffer.writeInt(itemKey.granularity());
        }
    }

    public static MenuStateUpdateMessage read(final FriendlyByteBuf buffer) {
        final int elementType = buffer.readInt();
        final String name = buffer.readUtf();
        Object elementState = null;
        if (elementType == 0) {
            elementState = buffer.readUtf();
        } else if (elementType == 1) {
            elementState = buffer.readBoolean();
        } else if (elementType == 2) {
            elementState = buffer.readMap(
                (buf) -> ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf),
                (buf, map) -> buf.readMap(FriendlyByteBuf::readShort, FriendlyByteBuf::readLong)
            );
        } else if (elementType == 3) {
            final PlatformResourceKey itemKey = ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
            final Map<Short, long[]> map = buffer.readMap(
                FriendlyByteBuf::readShort,
                (buf, arr) -> buf.readLongArray()
            );
            final Integer granularity = buffer.readInt();
            elementState = map.entrySet().stream()
                .map(e -> Map.entry(new ResourceChangeGranularityKey(itemKey, e.getKey(), granularity), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        } else if (elementType == 4) {
            elementState = buffer.readInt();
        } else if (elementType == 5) {
            final PlatformResourceKey itemKey = ResourceCodecs.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
            final Integer granularity = buffer.readInt();
            elementState = new ResourceChangeGranularityKey(itemKey, (short) 0, granularity);
        }
        return new MenuStateUpdateMessage(elementType, name, elementState);
    }

    public static void handleMenuState(final MenuStateUpdateMessage message, final IPayloadContext context) {
        if (message.name.length() > 256 || message.elementState instanceof String string && string.length() > 8192) { //TODO: this should be handled in the stream codec instead
            return;
        }
        context.enqueueWork(() -> { //TODO: this is shit
            if (context.player().containerMenu instanceof FlowScopeContainerMenu menu) {
                if ("detailedFactoryGenerationRequest".equals(message.name)) {
                    final ResourceChangeGranularityKey itemKey = ((ResourceChangeGranularityKey) message.elementState);
                    SnapshotService.executeDetailed(context.player().level(), menu.getX(), menu.getY(), menu.getZ(), context.player(), itemKey.resourceKey(), itemKey.granularity());
                } else if (context.flow() == PacketFlow.SERVERBOUND && message.elementType == 4
                    && ("simpleFactoryGenerationRequest".equals(message.name) || "storedFactoryGenerationRequest".equals(message.name))
                    && message.elementState instanceof Integer granularity && granularity > 0) {
                    SnapshotService.execute(context.player().level(), menu.getX(), menu.getY(), menu.getZ(), context.player(),
                        granularity, "storedFactoryGenerationRequest".equals(message.name));
                }
                if (context.flow() == PacketFlow.CLIENTBOUND) {
                    ModScreens.updateMenuState(message);
                }
            }
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
        });
    }

    @Override
    public Type<MenuStateUpdateMessage> type() {
        return TYPE;
    }
}
