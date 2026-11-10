package darkspawn.black.oneblock;

import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Phase Guardians: Bounded team encounters; missing actors, restarts and environmental damage never grant victory. */
public final class AscensionGuardian {
    public static final EntityType<AscensionGuardianEntity> TYPE=net.minecraft.core.Registry.register(
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE,darkspawn.black.Darkspawn.id("ascension_guardian"),
        EntityType.Builder.of(AscensionGuardianEntity::new,MobCategory.MISC).sized(1.4F,2.7F).eyeHeight(2.3F).clientTrackingRange(10)
            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,darkspawn.black.Darkspawn.id("ascension_guardian"))));
    public static final String TAG="darkspawn_cave_guardian",TEAM="darkspawn_guardian_team_";
    public static final int LIMIT=8,DURATION=6000;
    private record Warning(Vec3 center,long strike) { }
    private static final Map<UUID,Mob> loaded=new HashMap<>();
    private static final Map<UUID,ServerBossEvent> bars=new HashMap<>();
    private static final Map<UUID,Warning> warnings=new HashMap<>();
    private AscensionGuardian(){ }
    private static String name(AscensionLedger.Team team){return switch(team.phases().phase()){
        case "stormbound"->"Storm Guardian";case "emberlands"->"Ember Guardian";case "frostbound"->"Frost Guardian";
        case "tidal_reach"->"Tidal Guardian";case "crystal_depths"->"Crystal Guardian";case "endreach"->"End Guardian";
        case "astral_forge"->"Astral Guardian";case "ascension_summit"->"Summit Guardian";default->"Cave Guardian";};}
    static String hint(AscensionLedger.Team team){return switch(team.phases().phase()){
        case "stormbound"->"Leave the marked cross before it strikes.";case "emberlands"->"Leave the ember ring or step into its center.";
        case "frostbound"->"Frost lanes: step between the three marked north-south lanes.";
        case "tidal_reach"->"Tidal fronts: step between the three marked east-west bands.";
        case "crystal_depths"->"Crystal fracture: leave the marked diagonal X.";
        case "endreach"->"End pulse: move outside the center or into a gap between the two rings.";
        case "astral_forge"->"Astral hammer: leave the four marked impact circles.";
        case "ascension_summit"->"Summit seal: find a diagonal gap between the cross and outer ring.";
        default->"Move off the marked ground before the slam.";};}
    static boolean inStrike(String phase,Vec3 center,Vec3 position){
        var delta=position.subtract(center);if(Math.abs(delta.y)>3)return false;
        double square=delta.x*delta.x+delta.z*delta.z;
        return switch(phase){case "stormbound"->Math.abs(delta.x)<=6&&Math.abs(delta.z)<=6&&(Math.abs(delta.x)<=.8||Math.abs(delta.z)<=.8);
            case "emberlands"->square>=4&&square<=25;
            case "frostbound"->Math.abs(delta.z)<=6&&(Math.abs(delta.x)<=.65||Math.abs(Math.abs(delta.x)-4)<=.65);
            case "tidal_reach"->Math.abs(delta.x)<=6&&(Math.abs(delta.z)<=.65||Math.abs(Math.abs(delta.z)-4)<=.65);
            case "crystal_depths"->square<=36&&Math.abs(Math.abs(delta.x)-Math.abs(delta.z))<=.7;
            case "endreach"->square<=1||square>=6.25&&square<=12.25||square>=25&&square<=36;
            case "astral_forge"->Math.pow(Math.abs(delta.x)-3,2)+Math.pow(Math.abs(delta.z)-3,2)<=2.25;
            case "ascension_summit"->square<=36&&(Math.abs(delta.x)<=.65||Math.abs(delta.z)<=.65||square>=25);
            default->position.distanceToSqr(center)<=2.5*2.5;};
    }
    private static void mark(ServerLevel level,AscensionLedger.Team team,Vec3 at){
        String phase=team.phases().phase();
        // Encounter Telegraphs: Local particles never change weather, ignite blocks or damage the player's island.
        for(double x=-6;x<=6;x+=.5)for(double z=-6;z<=6;z+=.5)if(inStrike(phase,Vec3.ZERO,new Vec3(x,0,z)))
            level.sendParticles(switch(phase){case "stormbound","astral_forge"->ParticleTypes.ELECTRIC_SPARK;
                case "frostbound"->ParticleTypes.SNOWFLAKE;case "tidal_reach"->ParticleTypes.END_ROD;
                case "crystal_depths","endreach","ascension_summit"->ParticleTypes.WITCH;default->ParticleTypes.FLAME;},at.x+x,at.y+.1,at.z+z,1,0,0,0,0);
    }
    public static boolean guardian(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    private static AscensionLedger.Team team(Entity entity){
        if(!(entity.level() instanceof ServerLevel level)||!Ascension.island(level))return null;
        try{return entity.entityTags().stream().filter(t->t.startsWith(TEAM)).findFirst().map(t->AscensionLedger.get(level).team(UUID.fromString(t.substring(TEAM.length())))).orElse(null);}
        catch(IllegalArgumentException ignored){return null;}
    }
    private static boolean active(Entity entity,AscensionLedger.Team team){
        if(team==null||team.phases()==null||AscensionPhases.encounter(team.phases().phase()).isEmpty()||team.phases().encountered(AscensionPhases.encounter(team.phases().phase())))return false;
        var f=team.phases().features();return entity.getUUID().equals(f.guardian)&&entity.level().getGameTime()<f.expires&&entity.distanceToSqr(Vec3.atCenterOf(team.center()))<=32*32;
    }
    static boolean participant(ServerPlayer p,AscensionLedger.Team team){
        return team!=null&&p.isAlive()&&!p.isSpectator()&&!p.preventsBlockDrops()&&Ascension.island(p.level())&&team.members().contains(p.getUUID())
            &&p.distanceToSqr(Vec3.atCenterOf(team.center()))<=32*32;
    }
    public static boolean disallows(Mob mob,LivingEntity target){return guardian(mob)&&(!(target instanceof ServerPlayer p)||!active(mob,team(mob))||!participant(p,team(mob)));}
    private static int message(ServerPlayer p,String text){AscensionGuide.feedback(p,text);p.sendSystemMessage(Component.literal(text));return 1;}
    public static int start(ServerPlayer p){
        if(!Ascension.island(p.level())||!OneBlock.safe(p)||OneBlock.state(p).pendingTicks>0)return message(p,"Stand safely near your own core before starting the Guardian.");
        var level=p.level();var data=AscensionLedger.get(level);var team=data.member(p.getUUID());
        if(team==null||team.phases()==null||!participant(p,team)||p.distanceToSqr(Vec3.atCenterOf(team.center()))>16*16||AscensionPhases.encounter(team.phases().phase()).isEmpty()||team.phases().encountered(AscensionPhases.encounter(team.phases().phase())))return message(p,"No uncompleted Guardian is available in this phase.");
        if(!team.phases().prepared(AscensionPhases.get(level).phase(team.phases().phase()),team.progress()))return message(p,"Finish this phase's break threshold and submissions first.");
        var f=team.phases().features();if(f.guardian!=null&&f.expires>level.getGameTime()&&level.getEntity(f.guardian)!=null)return message(p,"Your team's Guardian encounter is already active.");
        if(level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().get(GameRules.SPAWN_MOBS))return message(p,"The Guardian requires mob spawning and a non-Peaceful difficulty.");
        if(loaded.size()>=LIMIT)return message(p,"Eight Guardians are already active; finish an encounter first.");
        if(!AscensionAutomation.loadedArea(level,team))return message(p,"The area around the core must be loaded.");
        var pos=OneBlock.space(level,team.center());if(pos==null)return message(p,"Clear safe standing space around the core. No blocks will be removed.");
        var mob=TYPE.create(level,EntitySpawnReason.EVENT);if(mob==null)return 0;
        mob.chapter(AscensionPhases.IDS.indexOf(team.phases().phase()));
        mob.setPos(Vec3.atBottomCenterOf(pos));if(!level.noCollision(mob)){mob.discard();return message(p,"Clear enough space for the Guardian.");}
        mob.addTag(TAG);mob.addTag(TEAM+team.id());mob.setPersistenceRequired();mob.setCustomName(Component.literal(name(team)));mob.setCustomNameVisible(true);
        float health=switch(team.phases().phase()){case "caverns"->80;case "stormbound"->100;case "emberlands"->120;
            default->140+(AscensionPhases.IDS.indexOf(team.phases().phase())-6)*20;};
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth(health);mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);
        mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.5);
        f.guardian=mob.getUUID();f.expires=level.getGameTime()+DURATION;data.setDirty();
        if(!level.addFreshEntity(mob)){f.clearGuardian();data.setDirty();mob.discard();return message(p,"Guardian spawn failed; retry when the area is clear.");}
        mob.setTarget(p);return message(p,name(team)+" awakened! Fight within 32 blocks of the core. "+hint(team)+" You have five minutes.");
    }
    private static void forget(UUID id){loaded.remove(id);warnings.remove(id);var bar=bars.remove(id);if(bar!=null)bar.removeAllPlayers();}
    private static void cancel(Mob mob){
        var team=team(mob);if(team!=null&&team.phases()!=null&&mob.getUUID().equals(team.phases().features().guardian)){team.phases().features().clearGuardian();AscensionLedger.get((ServerLevel)mob.level()).setDirty();}
        forget(mob.getUUID());mob.discard();
    }
    static void tick(){
        for(var mob:List.copyOf(loaded.values())){
            var level=(ServerLevel)mob.level();var team=team(mob);
            if(mob.isRemoved()||!mob.isAlive()||!active(mob,team)||level.getDifficulty()==Difficulty.PEACEFUL||!level.getGameRules().get(GameRules.SPAWN_MOBS)){cancel(mob);continue;}
            var players=level.players().stream().filter(p->participant(p,team)).toList();if(players.isEmpty()){cancel(mob);continue;}
            mob.setTarget(players.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElseThrow());
            var bar=bars.get(mob.getUUID());bar.setProgress(mob.getHealth()/mob.getMaxHealth());
            for(var p:List.copyOf(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);players.forEach(bar::addPlayer);
            long now=level.getGameTime();var warning=warnings.get(mob.getUUID());
            if(warning!=null&&now>=warning.strike()){
                if(mob instanceof AscensionGuardianEntity guardian)guardian.warning(0);
                warnings.remove(mob.getUUID());for(var p:players)if(inStrike(team.phases().phase(),warning.center(),p.position()))p.hurtServer(level,mob.damageSources().mobAttack(mob),6);
                level.sendParticles(ParticleTypes.CRIT,warning.center().x,warning.center().y+.2,warning.center().z,24,2,.2,2,.05);
            }else if(warning==null&&now%100<10){
                var at=mob.getTarget().position();warnings.put(mob.getUUID(),new Warning(at,now+(team.phases().phase().equals("caverns")?20:40)));
                if(mob instanceof AscensionGuardianEntity guardian)guardian.warning(team.phases().phase().equals("caverns")?20:40);
                players.forEach(p->p.sendOverlayMessage(Component.literal(hint(team))));mark(level,team,at);
            }
        }
    }
    static void recover(net.minecraft.server.MinecraftServer server){
        var level=server.getLevel(Ascension.LEVEL);if(level==null)return;
        // Restart Policy: Interrupted encounters are free to retry; old actors are discarded when their chunks load.
        for(var team:AscensionLedger.get(level).teams())if(team.phases()!=null&&team.phases().features().guardian!=null){team.phases().features().clearGuardian();AscensionLedger.get(level).setDirty();}
        for(var mob:List.copyOf(loaded.values()))cancel(mob);
    }
    public static void initialize(){
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TYPE,net.minecraft.world.entity.animal.golem.IronGolem.createAttributes());
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(!(entity instanceof Mob mob)||!guardian(mob))return;
            if(!active(mob,team(mob))||loaded.size()>=LIMIT){mob.discard();return;}
            loaded.put(mob.getUUID(),mob);bars.put(mob.getUUID(),new ServerBossEvent(mob.getUUID(),Component.literal(name(team(mob))),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS));
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if(!guardian(entity))return;var team=team(entity);
            if(team!=null&&team.phases()!=null&&entity.getUUID().equals(team.phases().features().guardian)){team.phases().features().clearGuardian();AscensionLedger.get(level).setDirty();}
            forget(entity.getUUID());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,amount)->{
            if(guardian(victim))return source.getEntity() instanceof ServerPlayer p&&active(victim,team(victim))&&participant(p,team(victim));
            if(source.getEntity() instanceof Mob mob&&guardian(mob))return !disallows(mob,victim);
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!guardian(entity)||!(entity.level() instanceof ServerLevel level))return;var team=team(entity);
            if(active(entity,team)&&source.getEntity() instanceof ServerPlayer p&&participant(p,team)&&team.phases().recordEncounter(AscensionPhases.encounter(team.phases().phase()))){
                team.phases().features().clearGuardian();AscensionLedger.get(level).setDirty();
                level.players().stream().filter(member->team.members().contains(member.getUUID())).forEach(member->message(member,name(team)+" defeated! Complete this phase from Ascension or /ascension next."));
            }
            forget(entity.getUUID());
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10==0)tick();});
        ServerLifecycleEvents.SERVER_STARTED.register(AscensionGuardian::recover);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{loaded.clear();bars.values().forEach(ServerBossEvent::removeAllPlayers);bars.clear();warnings.clear();});
    }
}
