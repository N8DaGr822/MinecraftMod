package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TrashInventoryPayload(int stateId) implements CustomPacketPayload {
	public static final Type<TrashInventoryPayload> TYPE = new Type<>(Darkspawn.id("empty_inventory_trash"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrashInventoryPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, TrashInventoryPayload::stateId, TrashInventoryPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
