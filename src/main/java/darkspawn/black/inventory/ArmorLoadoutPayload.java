package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ArmorLoadoutPayload(int target, int revision, int stateId) implements CustomPacketPayload {
	public static final Type<ArmorLoadoutPayload> TYPE = new Type<>(Darkspawn.id("armor_loadout"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArmorLoadoutPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ArmorLoadoutPayload::target, ByteBufCodecs.VAR_INT, ArmorLoadoutPayload::revision,
		ByteBufCodecs.VAR_INT, ArmorLoadoutPayload::stateId, ArmorLoadoutPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
