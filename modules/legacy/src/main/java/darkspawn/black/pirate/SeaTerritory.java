package darkspawn.black.pirate;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Capture sessions are bounded and temporary; completed ownership is durable world state. */
public final class SeaTerritory {
    public static final String DEFENDER="darkspawn_fort_defender",TARGET="darkspawn_sea_target_";
    private static final Map<ServerLevel,Map<BlockPos,Siege>> SIEGES=new WeakHashMap<>();
    private static final class Siege {final UUID captain;final BlockPos center;final List<Mob> guards=new ArrayList<>();final long expires;int held;Siege(UUID captain,BlockPos center,long expires){this.captain=captain;this.center=center;this.expires=expires;}}
    private SeaTerritory() { }
    private static IslandDiscoveries.Island here(ServerPlayer p){var island=IslandDiscoveries.at(p.level(),p.blockPosition());return island!=null&&island.kind()==1?island:null;}
    public static boolean ownsHere(ServerPlayer p){var island=here(p);var fort=island==null?null:SeaLedger.get(p.level()).fort(island.center());return fort!=null&&fort.owner().equals(p.getUUID());}
    public static boolean contested(ServerPlayer player){var island=here(player);return island!=null&&SIEGES.getOrDefault(player.level(),Map.of()).containsKey(island.center());}
    public static int progress(ServerPlayer p){var island=here(p);var siege=island==null?null:SIEGES.getOrDefault(p.level(),Map.of()).get(island.center());return siege==null?0:siege.held;}
    public static SeaServices.Reply order(ServerPlayer p,int action){
        if(action<11||action>13||!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Pirate.LEVEL))return SeaServices.Reply.FORT;
        var island=here(p);if(island==null)return SeaServices.Reply.FORT;
        var level=p.level();var ledger=SeaLedger.get(level);var fort=ledger.fort(island.center());boolean owner=fort!=null&&fort.owner().equals(p.getUUID());
        if(action==11){
            if(owner)return SeaServices.Reply.ALREADY_OWNER;
            var active=SIEGES.computeIfAbsent(level,k->new HashMap<>());
            if(active.containsKey(island.center()))return SeaServices.Reply.CONTESTED;
            if(active.size()>=4)return SeaServices.Reply.BUSY;
            if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS))return SeaServices.Reply.PEACEFUL;
            int required=2+(fort==null?0:fort.defenses()*2);
            var siege=new Siege(p.getUUID(),island.center(),level.getGameTime()+12000);
            outer:for(int x:new int[]{-7,7,-5,5})for(int z:new int[]{10,8,-8,-10}){
                var at=island.center().offset(x,0,z);
                if(!level.hasChunkAt(at)||!level.isEmptyBlock(at)||!level.isEmptyBlock(at.above())||!level.getBlockState(at.below()).isSolidRender())continue;
                var mob=EntityTypes.PILLAGER.create(level,EntitySpawnReason.EVENT);if(mob==null)continue;mob.setPos(Vec3.atBottomCenterOf(at));
                if(!level.noCollision(mob)||!level.getEntities(mob,mob.getBoundingBox()).isEmpty()){mob.discard();continue;}
                mob.addTag(DEFENDER);mob.addTag(TARGET+p.getUUID());mob.setPersistenceRequired();mob.setCustomName(Component.literal("Saltwatch Garrison"));mob.setCustomNameVisible(true);
                mob.setCanJoinRaid(false);mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));mob.setDropChance(EquipmentSlot.MAINHAND,0);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);mob.setHealth(30);mob.setTarget(p);siege.guards.add(mob);
                if(siege.guards.size()==required)break outer;
            }
            if(siege.guards.size()!=required){siege.guards.forEach(Entity::discard);return SeaServices.Reply.BLOCKED;}
            active.put(island.center(),siege);
            for(var mob:siege.guards)if(!level.addFreshEntity(mob)){abort(level,siege);return SeaServices.Reply.BLOCKED;}
            if(fort!=null){var previous=level.getServer().getPlayerList().getPlayer(fort.owner());if(previous!=null)previous.sendSystemMessage(Component.literal("Your Saltwatch Fort at "+island.center().getX()+", "+island.center().getZ()+" is under attack by "+p.getName().getString()+"."));}
            return SeaServices.Reply.CAPTURE;
        }
        if(!owner)return SeaServices.Reply.OWNER;
        if(SIEGES.getOrDefault(level,Map.of()).containsKey(island.center()))return SeaServices.Reply.CONTESTED;
        if(action==12){
            if(fort.defenses()>=2)return SeaServices.Reply.MAX_DEFENSE;
            if(!Ships.pay(p,Items.IRON_INGOT,8,false)||!Ships.pay(p,Items.OAK_PLANKS,24,false))return SeaServices.Reply.COST;
            Ships.pay(p,Items.IRON_INGOT,8,true);Ships.pay(p,Items.OAK_PLANKS,24,true);ledger.fort(new SeaLedger.Fort(fort.center(),fort.owner(),fort.name(),fort.defenses()+1,fort.suppliesAfter()));return SeaServices.Reply.DONE;
        }
        if(level.getGameTime()<fort.suppliesAfter())return SeaServices.Reply.SUPPLY_WAIT;
        if(p.getInventory().getFreeSlot()<0)return SeaServices.Reply.SPACE;
        ledger.fort(new SeaLedger.Fort(fort.center(),fort.owner(),fort.name(),fort.defenses(),level.getGameTime()+24000));p.getInventory().add(new ItemStack(Items.EMERALD,4));return SeaServices.Reply.DONE;
    }
    private static void abort(ServerLevel level,Siege siege){siege.guards.forEach(Entity::discard);SIEGES.getOrDefault(level,new HashMap<>()).remove(siege.center);}
    public static void tick(ServerLevel level){
        var active=SIEGES.get(level);if(active==null)return;
        for(var siege:List.copyOf(active.values())){
            var p=level.getServer().getPlayerList().getPlayer(siege.captain);
            if(p==null||p.level()!=level||!p.isAlive()||p.isSpectator()||p.distanceToSqr(Vec3.atCenterOf(siege.center))>24*24||level.getGameTime()>=siege.expires||level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){abort(level,siege);continue;}
            for(var guard:siege.guards)if(guard.isAlive())guard.setTarget(p);
            boolean rivals=level.players().stream().anyMatch(other->other!=p&&other.isAlive()&&!other.isSpectator()&&!other.isAlliedTo(p)&&other.distanceToSqr(Vec3.atCenterOf(siege.center))<=24*24);
            boolean guards=siege.guards.stream().anyMatch(Entity::isAlive);
            if(guards||rivals){Ships.message(p,guards?"Saltwatch: defeat the garrison":"Saltwatch: rival captain contests your claim");continue;}
            siege.held++;Ships.message(p,"Saltwatch capture: "+siege.held+"/60 seconds");
            if(siege.held<60)continue;
            var ledger=SeaLedger.get(level);var old=ledger.fort(siege.center);
            ledger.fort(new SeaLedger.Fort(siege.center,p.getUUID(),p.getName().getString(),0,level.getGameTime()+24000));
            var state=Pirate.state(p);if(!state.conqueredForts.contains(siege.center)){state.conqueredForts.add(siege.center);SeaLegends.renown(p,10);}
            if(old!=null&&!old.owner().equals(p.getUUID())){SeaLegends.infamy(p,10);var owner=level.getServer().getPlayerList().getPlayer(old.owner());if(owner!=null)owner.sendSystemMessage(Component.literal("Saltwatch at "+siege.center.getX()+", "+siege.center.getZ()+" was captured by "+p.getName().getString()+". Builds and stored items remain in place."));}
            p.sendSystemMessage(Component.literal("Saltwatch is yours. Fortify, collect supplies, and repair docked ships through shore services. Other captains may take it even while you are offline."));abort(level,siege);
        }
    }
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0){var level=server.getLevel(Pirate.LEVEL);if(level!=null)tick(level);}});
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity.entityTags().contains(DEFENDER)&&SIEGES.getOrDefault(level,Map.of()).values().stream().noneMatch(s->s.guards.stream().anyMatch(g->g.getUUID().equals(entity.getUUID()))))entity.discard();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->SIEGES.clear());
    }
}
