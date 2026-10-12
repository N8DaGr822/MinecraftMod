package darkspawn.black.equipment;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TemperaturePayload(float exposure, float target, int wetSeconds) implements CustomPacketPayload {
	public static final Type<TemperaturePayload> TYPE = new Type<>(Darkspawn.id("temperature"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TemperaturePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, TemperaturePayload::exposure, ByteBufCodecs.FLOAT, TemperaturePayload::target,
		ByteBufCodecs.VAR_INT, TemperaturePayload::wetSeconds, TemperaturePayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
