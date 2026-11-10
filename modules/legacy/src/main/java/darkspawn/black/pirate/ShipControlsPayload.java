package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShipControlsPayload(int entityId, int buttons) implements CustomPacketPayload {
	public static final Type<ShipControlsPayload> TYPE = new Type<>(Darkspawn.id("ship_controls"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShipControlsPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ShipControlsPayload::entityId, ByteBufCodecs.VAR_INT, ShipControlsPayload::buttons, ShipControlsPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
