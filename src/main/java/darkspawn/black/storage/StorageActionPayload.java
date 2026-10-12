package darkspawn.black.storage;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StorageActionPayload(int menuId, int stateId, int action, String query) implements CustomPacketPayload {
	public static final Type<StorageActionPayload> TYPE = new Type<>(Darkspawn.id("storage_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StorageActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, StorageActionPayload::menuId,
		ByteBufCodecs.VAR_INT, StorageActionPayload::stateId,
		ByteBufCodecs.VAR_INT, StorageActionPayload::action,
		ByteBufCodecs.stringUtf8(64), StorageActionPayload::query, StorageActionPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
