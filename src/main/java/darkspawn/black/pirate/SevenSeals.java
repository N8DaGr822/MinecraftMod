package darkspawn.black.pirate;

import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Seven physical discoveries and personal endings; no shared-world reset or terrain destruction. */
public final class SevenSeals {
    public static final String ADMIRAL="darkspawn_bound_admiral",OWNER="darkspawn_admiral_owner_";
    public static final String[] NAMES={"Lantern","Oath","Memory","Hunger","Mercy","Courage","Name"};
    public static final String[] HINTS={"Inspect Tideglass Shrine, accept The First Binding, climb Lastlight Beacon and claim at harbor.","After the First Binding, inspect Saltwatch Fort's dispatch chest.","Inspect the Pale Gull cargo chest after the First Binding.","Inspect Rootbound Camp's manifest cache after the First Binding.","At Tideglass Shrine, sneak-use the Logbound Compass and offer eight prismarine shards through shore services.","Sink a spectral vessel with your own ship after the First Binding. Harbor services offer a ghost voyage.","With the first six seals recorded, climb Lastlight Beacon's lantern again."};
    private static final String[] LORE={"The lantern was a warning, not a treasure marker. Seven captains bound the Undertide beneath the sea.","The watch swore to guard the prison, then sold its oath for an empire. Saltwatch's dispatch names the first betrayal.","The Pale Gull carried the memories of drowned sailors. Its captain chose to remember those the fleet erased.","The Corsairs sought endless wealth. Their manifest records a price paid in years of other people's lives.","Your offering wakes no monster. It restores a line the captains scratched away: even the condemned deserve mercy.","The ghost captain yields a name only to someone who faces the dead. Courage is refusing the same bargain twice.","The final signal names the Bound Admiral, the Undertide's living vessel. The prison can be claimed, broken through battle, or renewed."};
    private static final Map<UUID,Mob> LOADED=new HashMap<>();
    private static final Map<UUID,Long> WARNINGS=new HashMap<>();
    private SevenSeals() { }
    public static boolean discover(ServerPlayer p,int index){
        var s=Pirate.state(p);if(index<0||index>6||!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Pirate.LEVEL)||s.sealContract!=3||(s.seals&(1<<index))!=0)return false;
        s.seals|=1<<index;SeaLegends.renown(p,5);p.sendSystemMessage(Component.literal("Seal "+(index+1)+" — "+NAMES[index]+": "+LORE[index]));return true;
    }
    public static void inspect(ServerPlayer p,IslandDiscoveries.Island island,BlockPos chest){
        if(island.kind()==1&&chest.equals(island.center().offset(-1,0,-5)))discover(p,1);
        if(island.kind()==2&&chest.equals(island.center().offset(3,0,-4)))discover(p,2);
        if(island.kind()==5&&chest.equals(island.center().offset(1,0,-4)))discover(p,3);
    }
    public static void observe(ServerPlayer p,IslandDiscoveries.Island island){
        if((Pirate.state(p).seals&63)==63&&island.kind()==3&&p.getY()>=76&&Math.abs(p.getX()-island.center().getX())<=5&&Math.abs(p.getZ()-island.center().getZ())<=6)discover(p,6);
    }
    public static String rumor(PiratePlayerState s){for(int i=0;i<7;i++)if((s.seals&(1<<i))==0)return "Tavern rumor — "+NAMES[i]+": "+HINTS[i];return s.ending==0?"Seven lights are known. Return to Tideglass Shrine and choose what kind of captain you will be.":"Your legend is written, but the sea remains open. Trade, capture forts, sail and discover new shores.";}
    public static String journal(PiratePlayerState s){
        var text=new StringBuilder("Seven bindings contain the Undertide. These are personal discoveries, not consumable keys; no faction oath is required.\n");
        for(int i=0;i<7;i++)text.append("\n").append(i+1).append(". ").append(NAMES[i]).append(": ").append((s.seals&(1<<i))!=0?LORE[i]:HINTS[i]).append("\n");
        text.append("\nAt Tideglass Shrine, sneak-use the Logbound Compass to choose an ending after all seven discoveries. Pirate King claims a crown and riches at the cost of infamy. Liberator defeats the Bound Admiral and frees the drowned souls. Eternal Guardian renews the prison and earns renown. Each choice is final for your character and never closes exploration or changes another player's ending.");
        text.append("\n\nYour ending: ").append(switch(s.ending){case 1->"Pirate King — the crown is yours, and hunters will remember its price.";case 2->"Liberator — the Admiral's vessel is shattered and the drowned souls are free.";case 3->"Eternal Guardian — the bindings hold; you carry the Warden's Vow.";default->"Undecided";});return text.toString();
    }
    public static SeaServices.Reply order(ServerPlayer p,int action){
        if(action<14||action>17||!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(Pirate.LEVEL))return SeaServices.Reply.BLOCKED;
        var island=IslandDiscoveries.at(p.level(),p.blockPosition());if(island==null||island.kind()!=4)return SeaServices.Reply.SHRINE;
        var state=Pirate.state(p);if(state.sealContract!=3)return SeaServices.Reply.FIRST;
        if(action==14){if((state.seals&16)!=0)return SeaServices.Reply.RECORDED;if(!Ships.pay(p,Items.PRISMARINE_SHARD,8,false))return SeaServices.Reply.COST;Ships.pay(p,Items.PRISMARINE_SHARD,8,true);discover(p,4);return SeaServices.Reply.DONE;}
        if(state.ending!=0)return SeaServices.Reply.ENDED;if(state.seals!=127)return SeaServices.Reply.SEALS;
        var ledger=SeaLedger.get(p.level());var battle=ledger.battle(p.getUUID());
        if(action==16&&(battle==null||!battle.defeated()))return summon(p,island.center());
        int empty=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty())empty++;
        if(empty<(action==16?1:2))return action==16?SeaServices.Reply.SPACE:SeaServices.Reply.ENDING_SPACE;
        state.ending=action-14;
        if(battle!=null)ledger.battle(new SeaLedger.Battle(p.getUUID(),battle.entity(),battle.center(),0,battle.defeated()));
        ItemStack trophy=new ItemStack(action==15?Items.GOLDEN_HELMET:action==16?Items.TRIDENT:Items.HEART_OF_THE_SEA);
        trophy.set(DataComponents.CUSTOM_NAME,Component.literal(action==15?"Crown of the Seven Tides":action==16?"Tidebreaker": "Warden's Vow"));
        p.getInventory().add(trophy);if(action!=16)p.getInventory().add(new ItemStack(Items.EMERALD,action==15?24:16));
        if(action==15)SeaLegends.infamy(p,40);else {SeaLegends.renown(p,30);SeaLegends.infamy(p,-20);}
        p.sendSystemMessage(Component.literal(switch(state.ending){case 1->"The seven lights bend toward your crown. You are Pirate King; the sea is still free to challenge you.";case 2->"The Bound Admiral falls silent. The lost crews sail into dawn. You are their Liberator.";default->"You return the seven names to the stone. The prison holds, and the Warden's Vow passes to you.";}));return SeaServices.Reply.DONE;
    }
    private static UUID owner(Entity mob){try{return mob.entityTags().stream().filter(t->t.startsWith(OWNER)).findFirst().map(t->UUID.fromString(t.substring(OWNER.length()))).orElse(null);}catch(IllegalArgumentException ignored){return null;}}
    private static SeaServices.Reply summon(ServerPlayer p,BlockPos center){
        var level=p.level();var ledger=SeaLedger.get(level);var old=ledger.battle(p.getUUID());long now=level.getGameTime();
        if(old!=null&&!old.defeated()&&old.expires()>now)return SeaServices.Reply.BATTLE_ACTIVE;
        if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS))return SeaServices.Reply.PEACEFUL;
        if(ledger.activeBattles(now)>=8)return SeaServices.Reply.BUSY;
        var mob=EntityTypes.DROWNED.create(level,EntitySpawnReason.EVENT);if(mob==null)return SeaServices.Reply.BLOCKED;
        boolean found=false;for(int x:new int[]{-6,6,-8,8}){var at=center.offset(x,0,8);if(!level.hasChunkAt(at)||!level.isEmptyBlock(at)||!level.isEmptyBlock(at.above())||!level.getBlockState(at.below()).isSolidRender())continue;mob.setPos(Vec3.atBottomCenterOf(at));if(level.noCollision(mob)&&level.getEntities(mob,mob.getBoundingBox()).isEmpty()){found=true;break;}}
        if(!found){mob.discard();return SeaServices.Reply.BLOCKED;}
        mob.addTag(ADMIRAL);mob.addTag(OWNER+p.getUUID());mob.addTag(SeaTerritory.TARGET+p.getUUID());mob.setPersistenceRequired();mob.setCustomName(Component.literal("The Bound Admiral"));mob.setCustomNameVisible(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(120);mob.setHealth(120);mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.NETHERITE_HELMET));mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));mob.setDropChance(EquipmentSlot.HEAD,0);mob.setDropChance(EquipmentSlot.MAINHAND,0);mob.setTarget(p);
        ledger.battle(new SeaLedger.Battle(p.getUUID(),mob.getUUID(),center,now+12000,false));
        if(!level.addFreshEntity(mob)){ledger.battle(new SeaLedger.Battle(p.getUUID(),mob.getUUID(),center,0,false));mob.discard();return SeaServices.Reply.BLOCKED;}
        return SeaServices.Reply.BATTLE;
    }
    private static void ledgerExpired(ServerLevel level,SeaLedger.Battle battle){SeaLedger.get(level).battle(new SeaLedger.Battle(battle.captain(),battle.entity(),battle.center(),0,false));}
    public static void tick(){
        for(var mob:List.copyOf(LOADED.values())){
            if(mob.isRemoved()||!(mob.level() instanceof ServerLevel level)){LOADED.remove(mob.getUUID());WARNINGS.remove(mob.getUUID());continue;}
            UUID id=owner(mob);var battle=id==null?null:SeaLedger.get(level).battle(id);long now=level.getGameTime();
            if(battle==null||!battle.entity().equals(mob.getUUID())||battle.defeated()||now>=battle.expires()||level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){mob.discard();continue;}
            var p=level.getServer().getPlayerList().getPlayer(id);
            if(p==null||p.level()!=level||!p.isAlive()||p.isSpectator()||p.distanceToSqr(Vec3.atCenterOf(battle.center()))>48*48){mob.setTarget(null);mob.setNoAi(true);WARNINGS.remove(mob.getUUID());continue;}
            mob.setNoAi(false);mob.setTarget(p);
            if(mob.distanceToSqr(Vec3.atCenterOf(battle.center()))>32*32){ledgerExpired(level,battle);mob.discard();continue;}
            int interval=mob.getHealth()>80?120:mob.getHealth()>40?100:60;
            Long strike=WARNINGS.get(mob.getUUID());
            if(strike!=null&&now>=strike){WARNINGS.remove(mob.getUUID());if(p.distanceToSqr(mob)<=6*6)p.hurtServer(level,mob.damageSources().mobAttack(mob),mob.getHealth()>40?5:7);level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,mob.getX(),mob.getY()+1,mob.getZ(),18,3,.3,3,.03);}
            else if(strike==null&&now%interval<20){WARNINGS.put(mob.getUUID(),now+20);Ships.message(p,"The Admiral gathers a tide pulse — move beyond six blocks!");level.sendParticles(ParticleTypes.SOUL,mob.getX(),mob.getY()+1,mob.getZ(),10,1,.5,1,.02);}
        }
    }
    public static void initialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity instanceof Mob mob&&entity.entityTags().contains(ADMIRAL)){var id=owner(mob);var battle=id==null?null:SeaLedger.get(level).battle(id);if(battle==null||!battle.entity().equals(mob.getUUID())||battle.defeated()||battle.expires()<=level.getGameTime())mob.discard();else LOADED.put(mob.getUUID(),mob);}});
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{LOADED.remove(entity.getUUID());WARNINGS.remove(entity.getUUID());});
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(!entity.entityTags().contains(ADMIRAL)||!(entity.level() instanceof ServerLevel level))return;var id=owner(entity);if(id==null)return;var ledger=SeaLedger.get(level);var battle=ledger.battle(id);if(battle!=null&&battle.entity().equals(entity.getUUID())&&battle.expires()>level.getGameTime()&&!battle.defeated()){ledger.battle(new SeaLedger.Battle(id,battle.entity(),battle.center(),battle.expires(),true));var p=level.getServer().getPlayerList().getPlayer(id);if(p!=null)p.sendSystemMessage(Component.literal("The Bound Admiral is defeated. Return to Tideglass Shrine to confirm liberation and claim Tidebreaker."));}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)tick();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{LOADED.clear();WARNINGS.clear();});
    }
}
