package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShipStatusPayload(int entityId, UUID shipId, String navigation, String combat, String crew) implements CustomPacketPayload {
    public static final Type<ShipStatusPayload> TYPE = new Type<>(Darkspawn.id("ship_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipStatusPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ShipStatusPayload::entityId, UUIDUtil.STREAM_CODEC, ShipStatusPayload::shipId,
        ByteBufCodecs.stringUtf8(160), ShipStatusPayload::navigation, ByteBufCodecs.stringUtf8(160), ShipStatusPayload::combat,
        ByteBufCodecs.stringUtf8(160), ShipStatusPayload::crew, ShipStatusPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
