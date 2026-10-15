package darkspawn.black.ecosystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import darkspawn.black.ServerTuning;
import darkspawn.black.boss.BossKind;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.saveddata.*;

/** Personal, non-destructive habitat events: saved cooldowns prevent relog farming and offline stockpiles. */
public final class RegionalEvents {
    public record Visit(UUID player,long next) {
        static final Codec<Visit> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("player").forGetter(Visit::player),Codec.LONG.fieldOf("next").forGetter(Visit::next)).apply(i,Visit::new));
    }
    public static final class Timers extends SavedData {
        public static final Codec<Timers> CODEC=Visit.CODEC.listOf().xmap(Timers::new,s->s.next.entrySet().stream().map(e->new Visit(e.getKey(),e.getValue())).toList());
        public static final SavedDataType<Timers> TYPE=new SavedDataType<>(Darkspawn.id("regional_events"),()->new Timers(List.of()),CODEC,null);
        private final Map<UUID,Long> next=new HashMap<>();
        private Timers(List<Visit> values){values.forEach(v->next.put(v.player(),Math.max(0,v.next())));}
        public boolean ready(UUID player,long now) {
            Long due=next.get(player);if(due!=null&&now<due)return false;
            next.put(player,now+ServerTuning.eventTicks);setDirty();return due!=null;
        }
    }
    private RegionalEvents() { }
    public static boolean observe(ServerPlayer player,BossKind kind) {
        if(!ServerTuning.regionalEvents||kind==null||!player.isAlive()||player.isCreative()||player.isSpectator())return false;
        var world=player.level().getServer().overworld();if(!world.getDataStorage().computeIfAbsent(Timers.TYPE).ready(player.getUUID(),world.getGameTime()))return false;
        var effect=switch(kind) {
            case ANCIENT_TREE_SPIRIT,TITAN_BOA,MYCELIAL_SOVEREIGN -> MobEffects.REGENERATION;
            case MOUNTAIN_TITAN,CAVE_CRAWLER,SHADOW_CREEPER_QUEEN,FOSSIL_TYRANT -> MobEffects.HASTE;
            case KRAKEN -> MobEffects.DOLPHINS_GRACE;
            default -> MobEffects.SPEED;
        };
        String title=effect==MobEffects.REGENERATION?"A restorative spore bloom drifts through the habitat":effect==MobEffects.HASTE?"Mineral resonance quickens your hands":effect==MobEffects.DOLPHINS_GRACE?"A swift sea current gathers around you":"A wayfarer's breeze rises";
        player.addEffect(new MobEffectInstance(effect,600,0));player.sendSystemMessage(Component.literal(title+". Regional boon: 30 seconds."));
        player.level().sendParticles(player,RegionalAtmosphere.particle(kind),false,false,player.getX(),player.getY()+1.4,player.getZ(),24,3,1,3,.02);
        var sound=darkspawn.black.audio.DarkspawnSounds.get("ambient."+kind.id());
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),net.minecraft.sounds.SoundSource.AMBIENT,player.getX(),player.getY(),player.getZ(),.35F,.95F,player.getRandom().nextLong()));return true;
    }
}
