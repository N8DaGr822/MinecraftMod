package darkspawn.black.pirate;

import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** One captain-selected dock job; cargo stays in the hold until an atomic delivery. */
public final class DockedUnloading {
    public static final String[] STATUS={"Choose a chest", "Paused", "Fetching cargo", "Walking to chest", "Delivered cargo", "Chest full", "Route blocked", "Captain away", "Worker missing", "Chest unavailable", "Hold empty", "Recall shore crew"};
    private record Selection(PirateShip ship,long expires) { }
    private static final WeakHashMap<ServerPlayer,Selection> SELECTING=new WeakHashMap<>();
    private final PirateShip ship;
    private BlockPos target,deck;
    private ChestBlockEntity boundChest;
    private boolean active,fetching=true;
    private int status;
    DockedUnloading(PirateShip ship){this.ship=ship;}
    public boolean active(){return active;}
    public int status(){return status;}
    public BlockPos target(){return target;}
    public static void initialize(){
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!(player instanceof ServerPlayer captain)||hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
            var selection=SELECTING.remove(captain);
            if(selection==null)return InteractionResult.PASS;
            if(level.getGameTime()>selection.expires()){Ships.message(captain,"Chest selection expired. Choose Mark chest again.");return InteractionResult.PASS;}
            boolean selected=selection.ship().unloading.select(captain,hit.getBlockPos());
            Ships.message(captain,selected?"Unloading chest marked at "+hit.getBlockPos().toShortString()+". Return to Jobs and start unloading.":"Choose an unlocked, accessible single chest within 16 blocks of your docked ship; stand within 6 blocks of it.");
            return InteractionResult.SUCCESS;
        });
    }
    public boolean mark(ServerPlayer captain){
        if(!ship.isAlive()||!ship.refitAllowed(captain))return false;
        pause();SELECTING.put(captain,new Selection(ship,ship.level().getGameTime()+600));captain.closeContainer();
        Ships.message(captain,"Within 30 seconds, use the single chest to receive cargo (within 16 blocks of this ship).");return true;
    }
    private ChestBlockEntity chest(ServerPlayer captain){
        if(target==null||!ship.level().hasChunkAt(target)||ship.distanceToSqr(Vec3.atCenterOf(target))>16*16)return null;
        var state=ship.level().getBlockState(target);
        if(!state.is(Blocks.CHEST)||state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE||ChestBlock.isChestBlockedAt(ship.level(),target))return null;
        if(!(ship.level().getBlockEntity(target) instanceof ChestBlockEntity chest)||chest.getLootTable()!=null||!chest.canOpen(captain))return null;
        return chest;
    }
    public boolean select(ServerPlayer captain,BlockPos pos){
        if(!ship.isAlive()||!ship.refitAllowed(captain)||captain.distanceToSqr(Vec3.atCenterOf(pos))>36)return false;
        var previous=target;target=pos.immutable();var selected=chest(captain);
        if(selected==null){target=previous;return false;}
        pause();boundChest=selected;status=1;return true;
    }
    public boolean toggle(ServerPlayer captain){
        if(!ship.isAlive()||!ship.refitAllowed(captain))return false;
        if(active){pause();return true;}
        if(ship.crewRoaming()||ship.crewExpedition()){status=11;return false;}
        boundChest=chest(captain);if(boundChest==null){status=9;return false;}
        if(worker()==null){status=8;return false;}
        active=true;fetching=true;deck=null;status=2;return true;
    }
    public void pause(){active=false;fetching=true;deck=null;var worker=worker();if(worker!=null){worker.getNavigation().stop();worker.setNoAi(true);}status=target==null?0:1;}
    private Mob worker(){return ship.crewMembers().stream().filter(e->e instanceof Mob&&e.isAlive()&&e.entityTags().contains("naval_role_"+PirateShip.WORKER)).map(e->(Mob)e).findFirst().orElse(null);}
    public void tick(){
        if(ship.tickCount%20!=0)return;
        if(!active){
            // Reload Recovery: A saved off-ship worker stays put until resume, recall, or explicit shore orders.
            if(target!=null&&!ship.crewRoaming()&&!ship.crewExpedition()){var idle=worker();if(idle!=null&&!idle.isPassenger()){idle.getNavigation().stop();idle.setNoAi(true);}}
            return;
        }
        var captain=ship.owner()==null?null:ship.level().getServer().getPlayerList().getPlayer(ship.owner());
        var worker=worker();
        if(!ship.docked()||ship.enemy()){pause();return;}
        if(worker==null){status=8;return;}
        worker.getNavigation().stop();worker.setNoAi(true);
        if(captain==null||!ship.refitAllowed(captain)){status=7;return;}
        var chest=chest(captain);
        if(chest==null||chest!=boundChest){status=9;return;}
        if(worker.distanceToSqr(ship)>32*32){status=6;return;}
        if(!hasCargo()){status=10;return;}
        if(!ship.deployUnloadingWorker(worker)){status=6;return;}
        if(deck==null)deck=ship.unloadingDeck(worker);
        if(deck==null){status=6;return;}
        worker.setNoAi(false);worker.setTarget(null);
        // Route Retry: Native navigation cannot start mid-jump; keep physics active until the worker lands.
        if(!worker.onGround()){status=6;return;}
        if(fetching){
            status=2;
            if(worker.distanceToSqr(Vec3.atBottomCenterOf(deck))<1){worker.setNoAi(true);fetching=false;return;}
            walk(worker,deck);return;
        }
        status=3;
        for(var direction:Direction.Plane.HORIZONTAL){
            var stand=target.relative(direction);
            if(!ship.level().getBlockState(stand.below()).isSolidRender()||!ship.level().isEmptyBlock(stand)||!ship.level().isEmptyBlock(stand.above()))continue;
            if(worker.distanceToSqr(Vec3.atBottomCenterOf(stand))<1){
                // Delivery Transaction: No carried-item escrow; debit only what the destination accepted.
                worker.setNoAi(true);int moved=deliver(chest);status=moved==0?5:4;if(moved>0)fetching=true;return;
            }
            if(walk(worker,stand))return;
        }
        worker.setNoAi(true);status=6;
    }
    private boolean walk(Mob worker,BlockPos destination){
        // Loaded Routes: Native navigation may inspect a region around the worker; never generate it for a job.
        worker.setNoAi(true);var at=worker.blockPosition();
        if(worker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)>48){status=6;return false;}
        for(int x=at.getX()-64;x<=at.getX()+64;x+=16)for(int z=at.getZ()-64;z<=at.getZ()+64;z+=16)
            if(!ship.level().hasChunkAt(new BlockPos(x,at.getY(),z))){status=6;return false;}
        var path=worker.getNavigation().createPath(destination,0,24);
        if(path==null||!path.canReach()){status=6;return false;}
        worker.setNoAi(false);worker.getNavigation().moveTo(path,.65);return true;
    }
    private boolean hasCargo(){for(int i=0;i<ship.cargoSlots();i++)if(!ship.cargo.getItem(i).isEmpty())return true;return false;}
    private int deliver(ChestBlockEntity chest){
        for(int i=0;i<ship.cargoSlots();i++){
            var source=ship.cargo.getItem(i);if(source.isEmpty())continue;
            for(int j=0;j<chest.getContainerSize();j++){
                var existing=chest.getItem(j);if(!chest.canPlaceItem(j,source)||!existing.isEmpty()&&!ItemStack.isSameItemSameComponents(existing,source))continue;
                int take=Math.min(8,Math.min(source.getCount(),Math.min(chest.getMaxStackSize(),source.getMaxStackSize())-existing.getCount()));
                if(take<=0)continue;
                if(existing.isEmpty())chest.setItem(j,source.copyWithCount(take));else existing.grow(take);
                source.shrink(take);chest.setChanged();ship.cargo.setChanged();return take;
            }
        }
        return 0;
    }
    public void save(ValueOutput out){if(target!=null)out.store("UnloadChest",BlockPos.CODEC,target);}
    public void load(ValueInput in){target=in.read("UnloadChest",BlockPos.CODEC).orElse(null);active=false;fetching=true;deck=null;boundChest=null;status=target==null?0:1;}
}
