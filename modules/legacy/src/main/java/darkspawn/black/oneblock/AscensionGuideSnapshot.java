package darkspawn.black.oneblock;

import darkspawn.black.Darkspawn;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AscensionGuideSnapshot(String token,String notice,List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String section,String title,String body,String action,String argument,boolean confirm) { }
    public static final Type<AscensionGuideSnapshot> TYPE=new Type<>(Darkspawn.id("ascension_guide_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AscensionGuideSnapshot> CODEC=new StreamCodec<>(){
        public AscensionGuideSnapshot decode(RegistryFriendlyByteBuf b){
            String token=b.readUtf(40),notice=b.readUtf(4096);int size=b.readVarInt();if(size<0||size>160)throw new IllegalArgumentException("Ascension guide limit");
            var entries=new ArrayList<Entry>();for(int i=0;i<size;i++)entries.add(new Entry(b.readUtf(32),b.readUtf(128),b.readUtf(4096),b.readUtf(64),b.readUtf(64),b.readBoolean()));
            return new AscensionGuideSnapshot(token,notice,List.copyOf(entries));
        }
        public void encode(RegistryFriendlyByteBuf b,AscensionGuideSnapshot p){
            if(p.entries.size()>160)throw new IllegalArgumentException("Ascension guide limit");b.writeUtf(p.token,40);b.writeUtf(p.notice,4096);b.writeVarInt(p.entries.size());
            for(var e:p.entries){b.writeUtf(e.section,32);b.writeUtf(e.title,128);b.writeUtf(e.body,4096);b.writeUtf(e.action,64);b.writeUtf(e.argument,64);b.writeBoolean(e.confirm);}
        }
    };
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
