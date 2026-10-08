package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InventoryTierPayload(int tier) implements CustomPacketPayload {
	public static final Type<InventoryTierPayload> TYPE = new Type<>(Darkspawn.id("inventory_tier"));
	public static final StreamCodec<RegistryFriendlyByteBuf, InventoryTierPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, InventoryTierPayload::tier, InventoryTierPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
