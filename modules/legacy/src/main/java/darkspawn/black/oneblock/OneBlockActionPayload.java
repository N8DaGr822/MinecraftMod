package darkspawn.black.oneblock;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OneBlockActionPayload(int action) implements CustomPacketPayload {
	public static final Type<OneBlockActionPayload> TYPE = new Type<>(Darkspawn.id("oneblock_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OneBlockActionPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OneBlockActionPayload::action, OneBlockActionPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
