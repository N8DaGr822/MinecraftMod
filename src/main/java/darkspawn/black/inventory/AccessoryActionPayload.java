package darkspawn.black.inventory;
import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record AccessoryActionPayload(int action,int stateId) implements CustomPacketPayload {
    public static final Type<AccessoryActionPayload> TYPE=new Type<>(Darkspawn.id("accessory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AccessoryActionPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AccessoryActionPayload::action,ByteBufCodecs.VAR_INT,AccessoryActionPayload::stateId,AccessoryActionPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
