package darkspawn.black.oneblock;

import darkspawn.black.Darkspawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AscensionGuideRequest(String token,String action,String argument) implements CustomPacketPayload {
    public static final Type<AscensionGuideRequest> TYPE=new Type<>(Darkspawn.id("ascension_guide_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AscensionGuideRequest> CODEC=new StreamCodec<>(){
        public AscensionGuideRequest decode(RegistryFriendlyByteBuf b){return new AscensionGuideRequest(b.readUtf(40),b.readUtf(64),b.readUtf(64));}
        public void encode(RegistryFriendlyByteBuf b,AscensionGuideRequest p){b.writeUtf(p.token,40);b.writeUtf(p.action,64);b.writeUtf(p.argument,64);}
    };
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
