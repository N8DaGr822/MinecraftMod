package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShipIdentityPayload(int menuId,int revision,String name) implements CustomPacketPayload {
    public static final Type<ShipIdentityPayload> TYPE=new Type<>(Darkspawn.id("ship_identity"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ShipIdentityPayload> CODEC=new StreamCodec<>() {
        public ShipIdentityPayload decode(RegistryFriendlyByteBuf buf){return new ShipIdentityPayload(buf.readVarInt(),buf.readVarInt(),buf.readUtf(32));}
        public void encode(RegistryFriendlyByteBuf buf,ShipIdentityPayload value){buf.writeVarInt(value.menuId);buf.writeVarInt(value.revision);buf.writeUtf(value.name,32);}
    };
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
