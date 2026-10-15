package darkspawn.black.pirate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-owned receipts work even when a sunk ship's captain is offline. */
public final class SeaLedger extends SavedData {
    public record Rescue(UUID captain,int mask) {
        static final Codec<Rescue> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("captain").forGetter(Rescue::captain),Codec.INT.fieldOf("mask").forGetter(Rescue::mask)).apply(i,Rescue::new));
    }
    public record Fort(BlockPos center,UUID owner,String name,int defenses,long suppliesAfter) {
        static final Codec<Fort> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("center").forGetter(Fort::center),UUIDUtil.CODEC.fieldOf("owner").forGetter(Fort::owner),Codec.STRING.fieldOf("name").forGetter(Fort::name),Codec.INT.fieldOf("defenses").forGetter(Fort::defenses),Codec.LONG.fieldOf("suppliesAfter").forGetter(Fort::suppliesAfter)).apply(i,Fort::new));
    }
    public record Battle(UUID captain,UUID entity,BlockPos center,long expires,boolean defeated) {
        static final Codec<Battle> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("captain").forGetter(Battle::captain),UUIDUtil.CODEC.fieldOf("entity").forGetter(Battle::entity),BlockPos.CODEC.fieldOf("center").forGetter(Battle::center),Codec.LONG.fieldOf("expires").forGetter(Battle::expires),Codec.BOOL.fieldOf("defeated").forGetter(Battle::defeated)).apply(i,Battle::new));
    }
    public static final Codec<SeaLedger> CODEC=RecordCodecBuilder.create(i->i.group(Rescue.CODEC.listOf().optionalFieldOf("rescues",List.of()).forGetter(s->s.rescues.entrySet().stream().map(e->new Rescue(e.getKey(),e.getValue())).toList()),Fort.CODEC.listOf().optionalFieldOf("forts",List.of()).forGetter(s->List.copyOf(s.forts.values())),Battle.CODEC.listOf().optionalFieldOf("battles",List.of()).forGetter(s->List.copyOf(s.battles.values()))).apply(i,SeaLedger::new));
    public static final SavedDataType<SeaLedger> TYPE=new SavedDataType<>(Darkspawn.id("sea_ledger"),()->new SeaLedger(List.of(),List.of(),List.of()),CODEC,null);
    private final Map<UUID,Integer> rescues=new HashMap<>();
    private final Map<BlockPos,Fort> forts=new HashMap<>();
    private final Map<UUID,Battle> battles=new HashMap<>();
    private SeaLedger(List<Rescue> rescue,List<Fort> fort,List<Battle> battle){rescue.forEach(r->rescues.put(r.captain(),r.mask()&15));fort.forEach(f->forts.put(f.center(),new Fort(f.center(),f.owner(),f.name(),Math.clamp(f.defenses(),0,2),Math.max(0,f.suppliesAfter()))));battle.forEach(b->battles.put(b.captain(),b));}
    public static SeaLedger get(ServerLevel level){return level.getServer().getLevel(Pirate.LEVEL).getDataStorage().computeIfAbsent(TYPE);}
    public int lost(UUID captain){return rescues.getOrDefault(captain,0);}
    public void lose(UUID captain,int mask){if(mask!=0){rescues.merge(captain,mask,(a,b)->a|b);setDirty();}}
    public void recover(UUID captain,int bit){int remaining=lost(captain)&~bit;if(remaining==0)rescues.remove(captain);else rescues.put(captain,remaining);setDirty();}
    public Fort fort(BlockPos center){return forts.get(center);}
    public void fort(Fort fort){forts.put(fort.center(),fort);setDirty();}
    public Battle battle(UUID captain){return battles.get(captain);}
    public void battle(Battle battle){battles.put(battle.captain(),battle);setDirty();}
    public long activeBattles(long now){return battles.values().stream().filter(b->!b.defeated()&&b.expires()>now).count();}
}
