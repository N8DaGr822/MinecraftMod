package darkspawn.black.pirate;

import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Diplomacy applies only to explicitly tagged expedition NPCs, never every mob of a species. */
public final class PirateDiplomacy {
    public static final String FACTION_TAG="darkspawn_faction_", CONTACT_TAG="darkspawn_faction_contact";
    private static final WeakHashMap<ServerPlayer,long[]> INCIDENTS=new WeakHashMap<>();
    private PirateDiplomacy() { }
    public static PirateProgression.Faction faction(String id) {
        for(var faction:PirateProgression.Faction.values())if(faction.id.equals(id))return faction;
        return null;
    }
    public static PirateProgression.Faction faction(Entity entity) {
        for(var faction:PirateProgression.Faction.values())if(entity.entityTags().contains(FACTION_TAG+faction.id))return faction;
        return null;
    }
    public static PirateProgression.Faction islandFaction(int kind) {
        for(var faction:PirateProgression.Faction.values())if(faction.island==kind)return faction;
        return null;
    }
    public static boolean contact(Entity entity) {return entity instanceof Mob&&entity.entityTags().contains(CONTACT_TAG)&&faction(entity)!=null;}
    private static boolean harbor(Entity entity) {return entity.level().dimension().equals(Pirate.LEVEL)&&entity.getX()*entity.getX()+entity.getZ()*entity.getZ()<96*96;}
    public static boolean canParley(ServerPlayer player,PirateProgression.Faction faction) {
        if(!player.isAlive()||player.isSpectator()||!player.level().dimension().equals(Pirate.LEVEL))return false;
        if(HarborGuide.inHarbor(player))return true;
        return !player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(6),mob->mob.isAlive()&&contact(mob)&&faction(mob)==faction&&player.distanceToSqr(mob)<=36).isEmpty();
    }
    public static boolean membership(ServerPlayer player,PirateProgression.Faction selected,int action) {
        if(selected==null||!canParley(player,selected))return false;
        var state=Pirate.state(player);
        if(action==9) {
            if(!state.factionMembership.isEmpty()||PirateProgression.standing(state,selected)<25)return false;
            state.factionMembership=selected.id;return true;
        }
        if(action==10) {
            var old=faction(state.factionMembership);if(old==null)return false;
            state.factionMembership="";PirateProgression.addStanding(state,old,-25);return true;
        }
        if(action==11&&PirateProgression.standing(state,selected)<25&&Ships.pay(player,Items.GOLD_INGOT,8,false)) {
            Ships.pay(player,Items.GOLD_INGOT,8,true);
            state.factionStanding[selected.ordinal()]=Math.min(25,PirateProgression.standing(state,selected)+5);return true;
        }
        return false;
    }
    public static boolean disallows(Mob attacker,LivingEntity target) {
        if(contact(attacker)||PirateShip.loyalCrew(attacker))return true;
        var personal=attacker.entityTags().stream().filter(tag->tag.startsWith(SeaTerritory.TARGET)).findFirst();if(personal.isPresent())return !(target instanceof ServerPlayer)||!personal.get().equals(SeaTerritory.TARGET+target.getUUID());
        var faction=faction(attacker);if(faction==null||!attacker.level().dimension().equals(Pirate.LEVEL))return false;
        if(harbor(attacker)||harbor(target)||contact(target)||PirateShip.loyalCrew(target))return true;
        if(FactionSkirmishes.combatant(attacker))return !FactionSkirmishes.canAttack(attacker,target);
        return !(target instanceof ServerPlayer player)||PirateProgression.standing(Pirate.state(player),faction)>=0;
    }
    public static void initialize() {
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            if(PirateShip.loyalCrew(entity)) {
                if(player instanceof ServerPlayer server&&server.isAlive()&&!server.isSpectator()&&hand==InteractionHand.MAIN_HAND&&server.distanceToSqr(entity)<=36&&entity.getVehicle() instanceof PirateShip ship&&ship.ownedBy(server))
                    server.openMenu(new SimpleMenuProvider((id,inventory,viewer)->new ShipMenu(id,inventory,null,ship),Component.literal("Ship & Crew")));
                return InteractionResult.SUCCESS;
            }
            if(!contact(entity))return InteractionResult.PASS;
            if(player instanceof ServerPlayer server&&hand==InteractionHand.MAIN_HAND&&server.isAlive()&&!server.isSpectator()&&level.dimension().equals(Pirate.LEVEL)&&server.distanceToSqr(entity)<=36)
                server.openMenu(new SimpleMenuProvider((id,inventory,viewer)->new HarborMenu(id,inventory,entity,null),entity.getName()));
            return InteractionResult.SUCCESS;
        });
        // Damage Backstop: Also covers projectiles fired before a relationship changed.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
            if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return true;
            if(contact(target)||PirateShip.loyalCrew(target))return false;
            if(source.getEntity() instanceof Mob mob&&disallows(mob,target))return false;
            return faction(target)==null||!target.level().dimension().equals(Pirate.LEVEL)||(!harbor(target)&&(source.getEntity()==null||!harbor(source.getEntity())));
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked)->{if(taken>0&&!blocked&&source.getEntity() instanceof ServerPlayer player)aggression(player,target);});
        ServerLivingEntityEvents.AFTER_DEATH.register((target,source)->{if(source.getEntity() instanceof ServerPlayer player)aggression(player,target);});
    }
    private static void aggression(ServerPlayer player,LivingEntity target) {
        var faction=faction(target);
        if(faction==null||contact(target)||PirateShip.loyalCrew(target)||!target.level().dimension().equals(Pirate.LEVEL)||harbor(target))return;
        penalizeAttack(player,faction,"outpost guard");
    }
    public static void penalizeAttack(ServerPlayer player,PirateProgression.Faction faction,String target) {
        long now=player.level().getGameTime();var times=INCIDENTS.computeIfAbsent(player,key->new long[]{Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE});
        if(times[faction.ordinal()]!=Long.MIN_VALUE&&now-times[faction.ordinal()]<20)return;
        times[faction.ordinal()]=now;SeaLegends.infamy(player,2);PirateProgression.addStanding(Pirate.state(player),faction,-10);
        Ships.message(player,faction.title+": -10 standing for attacking its "+target+".");
    }
    public static void updateGuard(Mob mob) {
        if(contact(mob)||faction(mob)==null||!(mob.level() instanceof ServerLevel level))return;
        var target=level.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.distanceToSqr(mob)<=24*24&&!disallows(mob,p)&&mob.hasLineOfSight(p))
            .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        mob.setTarget(target);
        if(mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin) {
            mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            if(target==null)mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);else mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,target);
        }
    }
    public static void observe(ServerPlayer player,IslandDiscoveries.Island island) {
        var faction=islandFaction(island.kind());if(faction==null)return;
        var level=player.level();
        for(var mob:level.getEntitiesOfClass(Mob.class,new AABB(island.center()).inflate(28),m->m.entityTags().contains(IslandDiscoveries.ENCOUNTER_TAG))) {
            // Existing Outposts: Adopt only native guards of this outpost, preserving old camp spiders.
            if((faction==PirateProgression.Faction.CORSAIRS&&mob.getType()!=EntityTypes.PIGLIN)||(faction==PirateProgression.Faction.FLEET&&mob.getType()!=EntityTypes.PILLAGER)||(faction==PirateProgression.Faction.DROWNED&&mob.getType()!=EntityTypes.DROWNED))continue;
            mob.addTag(FACTION_TAG+faction.id);
            if(mob instanceof net.minecraft.world.entity.raid.Raider raider)raider.setCanJoinRaid(false);
            updateGuard(mob);
        }
        var saved=level.getDataStorage().computeIfAbsent(IslandDiscoveries.Encounters.TYPE);
        if((saved.mask(island.center())&4)!=0)return;
        var pos=island.center().offset(0,0,10);
        if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above())||!level.getBlockState(pos.below()).isSolidRender())return;
        Mob contact=switch(faction){case CORSAIRS->EntityTypes.PIGLIN.create(level,new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.EVENT,true));case FLEET->EntityTypes.PILLAGER.create(level,new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.EVENT,true));case DROWNED->EntityTypes.HUSK.create(level,new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.EVENT,true));};
        if(contact==null)return;
        contact.setPos(Vec3.atBottomCenterOf(pos));
        if(!level.noCollision(contact)||!level.getEntities(contact,contact.getBoundingBox()).isEmpty()){contact.discard();return;}
        contact.addTag(CONTACT_TAG);contact.addTag(FACTION_TAG+faction.id);contact.setNoAi(true);contact.setPersistenceRequired();contact.setPermanentlyInvulnerable(true);
        if(contact instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin)piglin.setImmuneToZombification(true);
        if(contact instanceof net.minecraft.world.entity.raid.Raider raider)raider.setCanJoinRaid(false);
        contact.setCustomName(Component.literal(switch(faction){case CORSAIRS->"Corsair Quartermaster";case FLEET->"Fleet Liaison";case DROWNED->"Tidebound Emissary";}));contact.setCustomNameVisible(true);
        if(level.addFreshEntity(contact))saved.record(island.center(),saved.mask(island.center())|4);else contact.discard();
    }
}
