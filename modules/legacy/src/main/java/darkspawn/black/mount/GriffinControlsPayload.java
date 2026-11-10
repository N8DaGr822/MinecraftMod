package darkspawn.black.mount;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Intent only: the server chooses velocity and validates the current rider. */
public record GriffinControlsPayload(int entityId, int buttons) implements CustomPacketPayload {
	public static final Type<GriffinControlsPayload> TYPE = new Type<>(Darkspawn.id("griffin_controls"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GriffinControlsPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, GriffinControlsPayload::entityId,
		ByteBufCodecs.VAR_INT, GriffinControlsPayload::buttons, GriffinControlsPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
