package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SortInventoryPayload(int stateId) implements CustomPacketPayload {
	public static final Type<SortInventoryPayload> TYPE = new Type<>(Darkspawn.id("sort_inventory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SortInventoryPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SortInventoryPayload::stateId, SortInventoryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
