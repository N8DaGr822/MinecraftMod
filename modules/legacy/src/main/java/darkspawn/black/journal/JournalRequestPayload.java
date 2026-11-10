package darkspawn.black.journal;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JournalRequestPayload() implements CustomPacketPayload {
    public static final Type<JournalRequestPayload> TYPE = new Type<>(Darkspawn.id("journal_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalRequestPayload> CODEC = StreamCodec.unit(new JournalRequestPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
