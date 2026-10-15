package darkspawn.black.inventory;
import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record HubRequestPayload(int action,long target,int stateId) implements CustomPacketPayload {
    public static final Type<HubRequestPayload> TYPE=new Type<>(Darkspawn.id("hub_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HubRequestPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,HubRequestPayload::action,ByteBufCodecs.LONG,HubRequestPayload::target,ByteBufCodecs.VAR_INT,HubRequestPayload::stateId,HubRequestPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
