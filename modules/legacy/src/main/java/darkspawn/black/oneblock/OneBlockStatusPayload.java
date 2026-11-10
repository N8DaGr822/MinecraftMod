package darkspawn.black.oneblock;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OneBlockStatusPayload(int phase, int breaks, int pending) implements CustomPacketPayload {
	public static final Type<OneBlockStatusPayload> TYPE = new Type<>(Darkspawn.id("oneblock_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OneBlockStatusPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OneBlockStatusPayload::phase, ByteBufCodecs.VAR_INT, OneBlockStatusPayload::breaks,
		ByteBufCodecs.VAR_INT, OneBlockStatusPayload::pending, OneBlockStatusPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
