package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ArmorLoadoutSyncPayload(int unlocked, int active, int revision, int pending) implements CustomPacketPayload {
	public static final Type<ArmorLoadoutSyncPayload> TYPE = new Type<>(Darkspawn.id("armor_loadout_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArmorLoadoutSyncPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ArmorLoadoutSyncPayload::unlocked, ByteBufCodecs.VAR_INT, ArmorLoadoutSyncPayload::active,
		ByteBufCodecs.VAR_INT, ArmorLoadoutSyncPayload::revision, ByteBufCodecs.VAR_INT, ArmorLoadoutSyncPayload::pending,
		ArmorLoadoutSyncPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
