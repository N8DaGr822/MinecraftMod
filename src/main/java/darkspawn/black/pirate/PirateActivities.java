package darkspawn.black.pirate;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Optional repeat voyages and one personal officer rescue, using existing menu and save conventions. */
public final class PirateActivities {
    public static final long REPEAT_COOLDOWN=20*60*10;
    public static final String GARRICK="darkspawn_garrick", CAPTIVE="darkspawn_garrick_captive";
    private PirateActivities() { }
    public static long cooldown(ServerPlayer player,int index) {
        return Math.max(0,Pirate.state(player).repeatAvailableAt[index]-player.level().getServer().overworld().getGameTime());
    }
    public static boolean contract(ServerPlayer player,int index,int action) {
        if(index<0||index>=3||!PirateDiplomacy.canParley(player,PirateProgression.Faction.values()[index]))return false;
        var state=Pirate.state(player);
        if(action==4&&state.repeatContracts[index]==0&&cooldown(player,index)==0){state.repeatContracts[index]=1;return true;}
        if(action==6&&state.repeatContracts[index]!=0){state.repeatContracts[index]=0;return true;}
        if(action!=5||state.repeatContracts[index]!=2||player.getInventory().getFreeSlot()<0)return false;
        // Repeat Reward: Consume completion before paying; cooldown survives restart and death.
        state.repeatContracts[index]=0;state.repeatAvailableAt[index]=player.level().getServer().overworld().getGameTime()+REPEAT_COOLDOWN;
        SeaLegends.renown(player,2);PirateProgression.addStanding(state,PirateProgression.Faction.values()[index],10);
        player.getInventory().add(new ItemStack(Items.EMERALD,6));
        Ships.message(player,"Voyage rewarded: +10 standing, 6 emeralds. Next commission in ten minutes.");return true;
    }
    private static boolean complete(ServerPlayer player,int index) {
        if(Pirate.state(player).repeatContracts[index]!=1)return false;
        Pirate.state(player).repeatContracts[index]=2;
        Ships.message(player,"Repeat voyage complete. Return to Briggs, the board or the matching faction contact.");return true;
    }
    static boolean inspect(ServerPlayer player,IslandDiscoveries.Island island,BlockPos chest) {
        var state=Pirate.state(player);
        if(island.kind()==2&&chest.equals(island.center().offset(3,0,-4)))return complete(player,0);
        if(island.kind()==4&&chest.equals(island.center().offset(0,0,-6))&&state.repeatContracts[2]==1) {
            if(!Ships.pay(player,Items.PRISMARINE_SHARD,4,false)){Ships.message(player,"Shrine expedition: bring four prismarine shards.");return false;}
            Ships.pay(player,Items.PRISMARINE_SHARD,4,true);return complete(player,2);
        }
        if(island.kind()==1&&chest.equals(island.center().offset(-1,0,-5))&&state.garrickRescue==1) {
            // Rescue Papers: Bind the personal objective to the same fort as this captain's captive.
            if(state.garrickPrison!=null&&!state.garrickPrison.equals(island.center()))return false;
            state.garrickPrison=island.center();state.garrickRescue=2;
            Ships.message(player,"Release papers recovered. Speak to Garrick inside this fort.");return true;
        }
        return false;
    }
    public static void navalVictory(ServerPlayer captain,PirateShip defeated) {
        if(captain.isAlive()&&!captain.isSpectator()&&captain.level()==defeated.level()&&defeated.enemy()&&defeated.patrolFaction()==null&&defeated.encounterKind()==0)complete(captain,1);
    }
    public static boolean rescueOrder(ServerPlayer player,int action) {
        if(!HarborGuide.inHarbor(player))return false;
        var state=Pirate.state(player);
        if(action==4&&state.garrickRescue==0){state.garrickRescue=1;return true;}
        if(action==6&&state.garrickRescue==3){state.garrickRescue=4;Ships.message(player,"Garrick is free to choose his own voyage. Rescue complete.");return true;}
        if(action==6&&(state.garrickRescue==1||state.garrickRescue==2)){state.garrickRescue=0;return true;}
        if(action!=5||state.garrickRescue!=3)return false;
        var ship=Ships.nearby(player,player.blockPosition());
        if(ship==null||!ship.recruitGarrick(player)){Ships.message(player,"Bring your docked ship within 48 blocks with a free cannoneer berth.");return false;}
        state.garrickRescue=5;Ships.message(player,"Garrick the Deserter joined your ship as a loyal cannoneer.");return true;
    }
    public static void observe(ServerPlayer player,IslandDiscoveries.Island island) {
        var state=Pirate.state(player);
        if(island.kind()!=1||state.garrickRescue<1||state.garrickRescue>2||!player.isAlive()||player.isSpectator())return;
        if(state.garrickPrison!=null&&!state.garrickPrison.equals(island.center()))return;
        // One Captive Per Captain: Keep the durable identity even while its chunk is unloaded.
        if(state.garrickCaptive!=null)return;
        var level=player.level();
        var captive=EntityTypes.PILLAGER.create(level,new EntitySpawnRequest(EntitySpawnReason.EVENT,true));if(captive==null)return;
        boolean placed=false;
        for(int x:new int[]{4,2})for(int z:new int[]{-4,-2,0,2}) {
            if(placed)break;
            var pos=island.center().offset(x,0,z);
            if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above())||!level.getBlockState(pos.below()).isSolidRender())continue;
            captive.setPos(Vec3.atBottomCenterOf(pos));
            if(level.noCollision(captive)&&level.getEntities(captive,captive.getBoundingBox()).isEmpty())placed=true;
        }
        if(!placed){captive.discard();return;}
        captive.addTag(CAPTIVE);captive.addTag(PirateShip.CREW_TAG);captive.addTag(PirateShip.CREW_OWNER+player.getUUID());
        captive.setNoAi(true);captive.setPersistenceRequired();captive.setPermanentlyInvulnerable(true);captive.setCanJoinRaid(false);
        captive.setCustomName(Component.literal("Garrick (for "+player.getName().getString()+")"));captive.setCustomNameVisible(true);
        if(level.addFreshEntity(captive)){state.garrickCaptive=captive.getUUID();state.garrickPrison=island.center();}else captive.discard();
    }
    public static boolean rescue(ServerPlayer player,Entity captive) {
        var state=Pirate.state(player);
        if(!player.isAlive()||player.isSpectator()||!player.level().dimension().equals(Pirate.LEVEL)||captive.level()!=player.level()||!captive.isAlive()||player.distanceToSqr(captive)>36||!captive.entityTags().contains(CAPTIVE)||!captive.getUUID().equals(state.garrickCaptive)||!captive.entityTags().contains(PirateShip.CREW_OWNER+player.getUUID()))return false;
        if(state.garrickRescue!=2){Ships.message(player,"Find my release papers in this fort's dispatch chest first.");return false;}
        state.garrickRescue=3;captive.discard();state.garrickCaptive=null;
        Ships.message(player,"Garrick escaped. Return to the harbor board: recruit him aboard your ship or set him free.");return true;
    }
    public static void initialize() {
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            if(!entity.entityTags().contains(CAPTIVE))return InteractionResult.PASS;
            if(player instanceof ServerPlayer server&&hand==InteractionHand.MAIN_HAND)rescue(server,entity);
            return InteractionResult.SUCCESS;
        });
    }
}
