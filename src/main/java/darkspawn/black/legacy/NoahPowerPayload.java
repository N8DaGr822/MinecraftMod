package darkspawn.black.legacy;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Intent only; equipment, bond, target selection and cooldowns are checked on the server. */
public record NoahPowerPayload(boolean ultimate) implements CustomPacketPayload {
    public static final Type<NoahPowerPayload> TYPE = new Type<>(Darkspawn.id("noah_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NoahPowerPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, NoahPowerPayload::ultimate, NoahPowerPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
