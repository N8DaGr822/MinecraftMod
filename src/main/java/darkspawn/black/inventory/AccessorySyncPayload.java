package darkspawn.black.inventory;
import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record AccessorySyncPayload(int mask,boolean glasses,long favorites) implements CustomPacketPayload {
    public static final Type<AccessorySyncPayload> TYPE=new Type<>(Darkspawn.id("accessory_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AccessorySyncPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AccessorySyncPayload::mask,ByteBufCodecs.BOOL,AccessorySyncPayload::glasses,ByteBufCodecs.LONG,AccessorySyncPayload::favorites,AccessorySyncPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
