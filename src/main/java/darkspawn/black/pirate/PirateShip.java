package darkspawn.black.pirate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Seated sailing, ordinary editable blocks when docked, and server-owned naval simulation. */
public final class PirateShip extends Entity {
	private static final EntityDataAccessor<String> BUILD = SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DOCKED = SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ENEMY = SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> FLAG=SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HULL = SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.FLOAT);
	public static final int FORWARD=1, BACK=2, LEFT=4, RIGHT=8, FIRE_LEFT=16, FIRE_RIGHT=32, DOCK=64, TARGET=128, MENU=256;
	public static final int MATE=0, WORKER=1, GUNNER=2, LOOKOUT=3;
	public static final int NAVIGATOR=4, QUARTERMASTER=5, SINGER=6;
    public final SimpleContainer cargo = new SimpleContainer(54);
    private int cargoTier;
    private boolean shantyPlaying;
    private static final EntityDataAccessor<Boolean> SHANTY=SynchedEntityData.defineId(PirateShip.class,EntityDataSerializers.BOOLEAN);
    public boolean shantyAudible(){return entityData.get(SHANTY);}
    public int cargoSlots(){return cargoTier==0?27:cargoTier==1?36:54;}
    public int flag(){return entityData.get(FLAG);}
    public boolean shantyPlaying(){return shantyPlaying;}
    public boolean refitAllowed(ServerPlayer captain){return captain.isAlive()&&!captain.isSpectator()&&captain.level()==level()&&ownedBy(captain)&&docked()&&captain.distanceToSqr(this)<=48*48;}
    public boolean rename(ServerPlayer captain,String name) {
        if(!refitAllowed(captain)||name.length()>32||name.codePoints().anyMatch(c->Character.isISOControl(c)||c==167))return false;
        name=name.strip();setCustomName(name.isEmpty()?null:Component.literal(name));setCustomNameVisible(!name.isEmpty());return true;
    }
    public boolean cycleFlag(ServerPlayer captain) {
        if(!refitAllowed(captain))return false;
        var faction=PirateDiplomacy.faction(Pirate.state(captain).factionMembership);
        entityData.set(FLAG,flag()!=0||faction==null?0:faction.ordinal()+1);return true;
    }
    public boolean upgradeCargo(ServerPlayer captain) {
        if(!refitAllowed(captain)||!protectedHarbor()||cargoTier>=2||PirateProgression.captainTier(Pirate.state(captain))<=cargoTier)return false;
        int iron=cargoTier==0?16:32,emeralds=cargoTier==0?4:8;
        if(!Ships.pay(captain,Items.IRON_INGOT,iron,false)||!Ships.pay(captain,Items.EMERALD,emeralds,false))return false;
        Ships.pay(captain,Items.IRON_INGOT,iron,true);Ships.pay(captain,Items.EMERALD,emeralds,true);cargoTier++;return true;
    }
    public boolean recruitOfficer(ServerPlayer captain,int index) {
        if(index<0||index>=3||!refitAllowed(captain)||!protectedHarbor()||Pirate.state(captain).officerContracts[index]!=3||crew(NAVIGATOR+index)>0)return false;
        var sailor=net.minecraft.world.entity.EntityTypes.VILLAGER.create(captain.level(),EntitySpawnReason.MOB_SUMMONED);if(sailor==null)return false;
        sailor.addTag(CREW_TAG);sailor.addTag(CREW_OWNER+owner);sailor.addTag("naval_role_"+(NAVIGATOR+index));
        sailor.setNoAi(true);sailor.setPersistenceRequired();sailor.setPermanentlyInvulnerable(true);sailor.setCustomName(Component.literal(PirateOfficers.NAMES[index]));sailor.setCustomNameVisible(true);sailor.setPos(getX(),getY()+1,getZ());
        if(!captain.level().addFreshEntity(sailor)||!sailor.startRiding(this)){sailor.discard();return false;}
        Pirate.state(captain).officerContracts[index]=4;return true;
    }
    public boolean resupply(ServerPlayer captain) {
        if(!refitAllowed(captain)||crew(QUARTERMASTER)==0)return false;
        boolean moved=false;
        // Quartermaster Transfer: Only explicit owner orders move supplies, preserving item components.
        for(int i=0;i<captain.getInventory().getContainerSize();i++) {
            var source=captain.getInventory().getItem(i);
            if(!source.is(Items.OAK_PLANKS)&&!source.is(Ships.SHOT)&&!source.is(Ships.FIRE_SHOT))continue;
            for(int slot=0;slot<cargoSlots()&&!source.isEmpty();slot++) {
                var target=cargo.getItem(slot);
                if(!target.isEmpty()&&!ItemStack.isSameItemSameComponents(source,target))continue;
                int count=Math.min(source.getCount(),Math.min(source.getMaxStackSize(),cargo.getMaxStackSize())-target.getCount());
                if(count<=0)continue;
                if(target.isEmpty())cargo.setItem(slot,source.copyWithCount(count));else target.grow(count);
                source.shrink(count);moved=true;
            }
        }
        if(moved){cargo.setChanged();captain.getInventory().setChanged();}return moved;
    }
    public boolean toggleShanty(ServerPlayer captain) {
        if(!ownedBy(captain)||captain.level()!=level()||!captain.isAlive()||captain.isSpectator()||captain.distanceToSqr(this)>48*48||crew(SINGER)==0)return false;
        shantyPlaying=!shantyPlaying;entityData.set(SHANTY,shantyPlaying);return true;
    }
    private void shantyTick(ServerLevel server) {
        // Shanty Eligibility: Clients attach streamed recordings to this ship; the server owns permission.
        entityData.set(SHANTY,shantyPlaying&&crew(SINGER)>0&&server.dimension().equals(Pirate.LEVEL));
    }

	private UUID owner;
    private int encounterKind; public int encounterKind(){return encounterKind;} public boolean burning(){return burning>0;}
    private PirateProgression.Faction patrolFaction;
    private UUID provokedBy;
    private long provokedUntil, patrolExpiresAt;
    public PirateProgression.Faction patrolFaction(){return patrolFaction;}
    public boolean hostileTo(PirateShip vessel) {
        if(!enemy()||vessel.enemy()||protectedHarbor()||vessel.protectedHarbor())return false;
        if(encounterKind==2){if(!(level() instanceof ServerLevel server)||vessel.owner==null)return false;var captain=server.getServer().getPlayerList().getPlayer(vessel.owner);return captain!=null&&captain.level()==level()&&(Pirate.state(captain).infamy>=30||(vessel.owner.equals(provokedBy)&&level().getGameTime()<provokedUntil));}
        if(patrolFaction==null)return true;
        if(!(level() instanceof ServerLevel server)||vessel.owner==null)return false;
        var captain=server.getServer().getPlayerList().getPlayer(vessel.owner);
        return captain!=null&&captain.level()==level()&&(PirateProgression.standing(Pirate.state(captain),patrolFaction)<0||(vessel.owner.equals(provokedBy)&&level().getGameTime()<provokedUntil));
    }
	private List<ShipBlock> blocks = List.of();
	private String renderedBuild = "";
	private int controls, previousControls, reload, burning, idle;
	private long lastInput=Long.MIN_VALUE;
	private UUID target, lastAttacker;
	private boolean holdFire=true, sinking;
	public PirateShip(EntityType<? extends PirateShip> type, Level level) { super(type,level); setNoGravity(true); }
	@Override protected void defineSynchedData(SynchedEntityData.Builder data) {
		data.define(SHANTY,false);data.define(FLAG,0);data.define(BUILD,""); data.define(TIER,0); data.define(DOCKED,true); data.define(ENEMY,false); data.define(HULL,200F);
	}
	public int tier() { return entityData.get(TIER); }
	public int width() { return tier()==0?7:tier()==1?11:15; }
	public int length() { return tier()==0?15:tier()==1?23:31; }
	public int height() { return tier()==0?6:tier()==1?8:10; }
	// Snapshot Budget: Keep the largest ASCII build below the native synced-string character limit.
	public int blockLimit() { return tier()==0?512:tier()==1?1024:1536; }
	public int gunLimit() { return tier()==0?4:tier()==1?8:12; }
	public float hull() { return entityData.get(HULL); }
	public float maxHull() { return tier()==0?200:tier()==1?400:600; }
	public boolean docked() { return entityData.get(DOCKED); }
	public boolean enemy() { return entityData.get(ENEMY); }
	public boolean ownedBy(Player player) { return player.getUUID().equals(owner) && !enemy(); }
	public boolean protectedHarbor() { return level().dimension().equals(Pirate.LEVEL) && getX()*getX()+getZ()*getZ()<96*96; }
	public boolean holdFire() { return holdFire; }
	public void toggleFire() { holdFire=!holdFire; }
	public static final String CREW_TAG="darkspawn_naval_crew", CREW_OWNER="darkspawn_crew_owner_";
    public static boolean loyalCrew(Entity entity) {
        return entity instanceof Mob&&entity.entityTags().contains(CREW_TAG)&&entity.entityTags().stream().anyMatch(tag->tag.startsWith(CREW_OWNER));
    }
    private boolean assignedCrew(Entity entity) {
        if(!(entity instanceof Mob)||owner==null)return false;
        boolean role=false;for(int i=0;i<7;i++)role|=entity.entityTags().contains("naval_role_"+i);
        // Crew Migration: Legacy stationed villagers belong to their saved parent vessel.
        return role&&(entity.entityTags().contains(CREW_OWNER+owner)||(entity instanceof Villager&&!entity.entityTags().stream().anyMatch(tag->tag.startsWith(CREW_OWNER))));
    }
    public int crew(int role) { return (int)getPassengers().stream().filter(e -> assignedCrew(e) && e.entityTags().contains("naval_role_"+role)).count(); }
	public int guns() { return (int)blocks.stream().filter(b -> b.state().is(Ships.CANNON)).count(); }
	public List<ShipBlock> blocks() {
		if (level().isClientSide() && !renderedBuild.equals(entityData.get(BUILD))) {
			renderedBuild=entityData.get(BUILD); var decoded=new ArrayList<ShipBlock>();
			for (String entry : renderedBuild.split(";")) if (!entry.isEmpty()) {
				String[] p=entry.split(","); if(p.length==4) decoded.add(new ShipBlock(new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])),Block.stateById(Integer.parseInt(p[3]))));
			}
			blocks=List.copyOf(decoded);
		}
		return blocks;
	}
	private void syncBuild(List<ShipBlock> build) {
		blocks=List.copyOf(build); var encoded=new StringBuilder();
		for(var b:blocks) encoded.append(b.pos().getX()).append(',').append(b.pos().getY()).append(',').append(b.pos().getZ()).append(',').append(Block.getId(b.state())).append(';');
		entityData.set(BUILD,encoded.toString());
	}
	public static boolean permitted(BlockState state) {
		if(state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
		if(state.is(Ships.HELM)||state.is(Ships.CANNON)) return true;
		String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		return id.endsWith("_planks")||id.endsWith("_log")||id.endsWith("_wood")||id.endsWith("_wool")||id.endsWith("_stairs")||id.endsWith("_slab")||id.endsWith("_fence")||id.endsWith("_glass")||id.equals("glass")||id.equals("ladder")||id.equals("lantern");
	}
	private Vec3 rotate(double x,double y,double z,float yaw) { double angle=Math.toRadians(yaw);return new Vec3(x*Math.cos(angle)-z*Math.sin(angle),y,x*Math.sin(angle)+z*Math.cos(angle)); }
	public Vec3 local(double x,double y,double z) { return position().add(rotate(x,y,z,getYRot())); }
	private BlockPos world(BlockPos local, float yaw, BlockPos anchor) {
		var rotated=rotate(local.getX(),local.getY(),local.getZ(),yaw);
		return anchor.offset((int)Math.round(rotated.x),local.getY(),(int)Math.round(rotated.z));
	}
	private AABB footprint(Vec3 center,float yaw) {
		double a=Math.toRadians(yaw), x=(Math.abs(Math.cos(a))*width()+Math.abs(Math.sin(a))*length())/2,
			z=(Math.abs(Math.sin(a))*width()+Math.abs(Math.cos(a))*length())/2;
		return new AABB(center.x-x,center.y,center.z-z,center.x+x,center.y+height()+1,center.z+z);
	}
	public static boolean clearWater(ServerLevel level,BlockPos center,int tier,float yaw,PirateShip ignore) {
		int width=tier==0?7:tier==1?11:15,length=tier==0?15:tier==1?23:31,height=tier==0?6:tier==1?8:10;
		double a=Math.toRadians(yaw); int rx=(int)Math.ceil((Math.abs(Math.cos(a))*width+Math.abs(Math.sin(a))*length)/2),rz=(int)Math.ceil((Math.abs(Math.sin(a))*width+Math.abs(Math.cos(a))*length)/2);
		for(int x=-rx;x<=rx;x++)for(int z=-rz;z<=rz;z++) {
			var at=center.offset(x,0,z);
			if(!level.getWorldBorder().isWithinBounds(at)||!level.hasChunkAt(at)||!level.getFluidState(at.below()).is(FluidTags.WATER)) return false;
			for(int y=0;y<=height;y++) if(!level.getBlockState(at.above(y)).isAir()) return false;
		}
		return level.getEntitiesOfClass(PirateShip.class,new AABB(center).inflate(rx,height,rz),ship -> ship!=ignore && !ship.isRemoved()).isEmpty();
	}
	public static PirateShip build(ServerPlayer player,BlockPos center) {
		if(!player.level().dimension().equals(Pirate.LEVEL)||!clearWater(player.level(),center,0,0,null)||!Ships.pay(player,Items.OAK_PLANKS,105,false)||!Ships.pay(player,Ships.HELM_ITEM,1,false)) return null;
		var ship=new PirateShip(Ships.SHIP,player.level());ship.owner=player.getUUID();ship.setPos(center.getX()+.5,center.getY(),center.getZ()+.5);
		if(!player.level().addFreshEntity(ship)) return null;
		Ships.pay(player,Items.OAK_PLANKS,105,true);Ships.pay(player,Ships.HELM_ITEM,1,true);
		for(int x=-3;x<=3;x++)for(int z=-7;z<=7;z++)player.level().setBlock(center.offset(x,0,z),Blocks.OAK_PLANKS.defaultBlockState(),3);
		player.level().setBlock(center.above(),Ships.HELM.defaultBlockState(),3);
		return ship;
	}
	public String launch(ServerPlayer captain) {
		if(!ownedBy(captain)||!docked()||captain.isPassenger()||!captain.inventoryMenu.getCarried().isEmpty()) return "Cannot launch now.";
		var captured=new ArrayList<ShipBlock>();int guns=0;
		var origin=blockPosition();
		for(int x=-width()/2;x<=width()/2;x++)for(int z=-length()/2;z<=length()/2;z++)for(int y=0;y<=height();y++) {
			var pos=world(new BlockPos(x,y,z),getYRot(),origin); if(!level().hasChunkAt(pos)) return "Load the whole ship before launching.";
			var state=level().getBlockState(pos);
			if(y==0 && !BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("_planks"))return "Keep a complete plank platform beneath the ship.";
			if(state.isAir())continue;
			if(!permitted(state))return "Unsupported block: "+state.getBlock().getName().getString()+". Use the ship hold for cargo.";
			if(state.is(Ships.CANNON)){guns++;if(x==0)return "Mount cannons on the port or starboard side.";}
			var inverse=Rotation.values()[Math.floorMod(-(int)Math.round(getYRot()/90),4)];
			captured.add(new ShipBlock(new BlockPos(x,y,z),state.is(Ships.CANNON)?state.setValue(ShipStationBlock.FACING,x<0?net.minecraft.core.Direction.WEST:net.minecraft.core.Direction.EAST):state.rotate(inverse)));
		}
		if(captured.size()>blockLimit()||guns>gunLimit())return "Ship exceeds its block or cannon limit.";
		if(!level().getBlockState(origin.above()).is(Ships.HELM))return "Restore the helm at the center of the platform.";
		var aboard=level().getEntities(this,footprint(position(),getYRot()),e -> !(assignedCrew(e) && e.getVehicle()==this));
		if(aboard.stream().anyMatch(e -> !(e instanceof Player))||aboard.stream().filter(e -> e instanceof Player).count()>4)return "Only four players and assigned crew may launch aboard.";
		// Seat Clearance: Sailing uses fixed seats; reject builds enclosing any occupied seat.
		int seatCount=getPassengers().size()+Math.max(1,aboard.size());
		for(int seat=-1;seat<seatCount;seat++){int sx=seat<0?0:(seat%2==0?-2:2),sz=seat<0?2:-length()/2+2+(seat/2)*2;for(int y=1;y<=2;y++)if(!level().isEmptyBlock(world(new BlockPos(sx,y,sz),getYRot(),origin)))return "Clear two blocks of headroom above the crew and passenger seats.";}
		// Conversion: Validate the entire footprint before removing any blocks; cargo never enters this snapshot.
		entityData.set(DOCKED,false);
		if(!captain.startRiding(this)){entityData.set(DOCKED,true);return "Cannot take the helm yet.";}
		syncBuild(captured);
		for(var b:captured)level().setBlock(world(b.pos(),getYRot(),origin),Blocks.AIR.defaultBlockState(),2);
		captain.closeContainer();
		for(var passenger:aboard)if(passenger instanceof Player p && p!=captain)p.startRiding(this);
		return null;
	}
	public String dock(ServerPlayer captain) {
		if(!ownedBy(captain)||docked()||getDeltaMovement().horizontalDistanceSqr()>.0025||burning>0)return "Stop the ship and extinguish fires before docking.";
		float yaw=Math.round(getYRot()/90)*90F;var anchor=blockPosition();
		if(blocks.isEmpty()||blocks.size()>blockLimit()||blocks.stream().anyMatch(b->Math.abs(b.pos().getX())>width()/2||Math.abs(b.pos().getZ())>length()/2||b.pos().getY()<0||b.pos().getY()>height()||!permitted(b.state())))return "Saved ship layout is invalid; docking is disabled to protect the world.";
		if(!level().getEntities(this,footprint(Vec3.atBottomCenterOf(anchor),yaw),e->e instanceof LivingEntity&&e.getVehicle()!=this).isEmpty())return "Move swimmers and creatures out of the docking area.";
		if(!clearWater(captain.level(),anchor,tier(),yaw,this))return "Docking needs clear open water and room for the whole ship.";
		var rotation=Rotation.values()[Math.floorMod(Math.round(yaw/90),4)];
		for(var b:blocks)level().setBlock(world(b.pos(),yaw,anchor),b.state().rotate(rotation),2);
		setYRot(yaw);setPos(anchor.getX()+.5,anchor.getY(),anchor.getZ()+.5);setDeltaMovement(Vec3.ZERO);entityData.set(DOCKED,true);
		var riders=List.copyOf(getPassengers());
		for(int i=0;i<riders.size();i++)if(riders.get(i) instanceof Player p){var at=seatPosition(p,i);p.stopRiding();p.teleportTo(at.x,at.y,at.z);}
		controls=0;return null;
	}
	public String upgrade(ServerPlayer captain) {
		if(!ownedBy(captain)||!docked()||tier()>=2)return "Dock a ship below the maximum tier to upgrade.";
		if(PirateProgression.captainTier(Pirate.state(captain))<=tier() && !captain.getAbilities().instabuild)return tier()==0?"Need 5 NPC wins OR 4 discoveries and 2 claimed contracts. See Journal.":"Need 12 NPC wins OR 8 discoveries and 4 claimed contracts. See Journal.";
        int nextTier=tier()+1,nextWidth=nextTier==1?11:15,nextLength=nextTier==1?23:31,nextHeight=nextTier==1?8:10;
        int planks=nextWidth*nextLength-width()*length();
		if(!Ships.pay(captain,Items.OAK_PLANKS,planks,false))return "Expansion costs "+planks+" oak planks.";
		var anchor=blockPosition();
		for(int x=-nextWidth/2;x<=nextWidth/2;x++)for(int z=-nextLength/2;z<=nextLength/2;z++) {
            boolean outside=Math.abs(x)>width()/2||Math.abs(z)>length()/2;
			var at=world(new BlockPos(x,0,z),getYRot(),anchor);
			if(!level().getWorldBorder().isWithinBounds(at)||!level().hasChunkAt(at)||!level().getFluidState(at.below()).is(FluidTags.WATER))return "The expanded platform needs clear water.";
			for(int y=outside?0:height()+1;y<=nextHeight;y++)if(!level().getBlockState(at.above(y)).isAir())return "The expanded build volume is obstructed.";
		}
		var expanded=new AABB(anchor).inflate(Math.max(nextWidth,nextLength),nextHeight,Math.max(nextWidth,nextLength));
        double radians=Math.toRadians(getYRot()),rx=(Math.abs(Math.cos(radians))*nextWidth+Math.abs(Math.sin(radians))*nextLength)/2,rz=(Math.abs(Math.sin(radians))*nextWidth+Math.abs(Math.cos(radians))*nextLength)/2;
        var bounds=new AABB(getX()-rx,getY(),getZ()-rz,getX()+rx,getY()+nextHeight+1,getZ()+rz);
        if(level().getEntitiesOfClass(PirateShip.class,expanded,other->other!=this&&other.footprint(other.position(),other.getYRot()).intersects(bounds)).size()>0)return "Move other vessels out of the expanded footprint.";
        Ships.pay(captain,Items.OAK_PLANKS,planks,true);
		for(int x=-nextWidth/2;x<=nextWidth/2;x++)for(int z=-nextLength/2;z<=nextLength/2;z++)if(Math.abs(x)>width()/2||Math.abs(z)>length()/2)level().setBlock(world(new BlockPos(x,0,z),getYRot(),anchor),Blocks.OAK_PLANKS.defaultBlockState(),3);
		entityData.set(TIER,nextTier);entityData.set(HULL,Math.min(maxHull(),hull()+200));return null;
	}
	public boolean recruit(ServerPlayer captain,int role) {
		if(!ownedBy(captain)||!docked()||!protectedHarbor()||role<0||role>3||crew(role)>=(role==GUNNER?gunLimit():1)||!Ships.pay(captain,Items.EMERALD,PirateProgression.crewCost(Pirate.state(captain),role),false))return false;
		Mob sailor;
        if(PirateProgression.crewOffer(Pirate.state(captain),role))sailor=switch(role) {
            case MATE -> net.minecraft.world.entity.EntityTypes.PIGLIN.create(captain.level(),new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED,true));
            case GUNNER -> net.minecraft.world.entity.EntityTypes.PILLAGER.create(captain.level(),new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED,true));
            default -> net.minecraft.world.entity.EntityTypes.HUSK.create(captain.level(),new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED,true));
        };else sailor=net.minecraft.world.entity.EntityTypes.VILLAGER.create(captain.level(),new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED,true));
        if(sailor==null)return false;
        if(sailor instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin piglin)piglin.setImmuneToZombification(true);
        if(sailor instanceof net.minecraft.world.entity.raid.Raider raider)raider.setCanJoinRaid(false);
        sailor.addTag(CREW_TAG);sailor.addTag(CREW_OWNER+owner);
		sailor.addTag("naval_role_"+role);sailor.setNoAi(true);sailor.setPermanentlyInvulnerable(true);sailor.setPersistenceRequired();
		sailor.setCustomName(Component.literal(PirateProgression.crewName(Pirate.state(captain),role)));sailor.setCustomNameVisible(true);sailor.setPos(getX(),getY()+1,getZ());
		if(!captain.level().addFreshEntity(sailor)){sailor.discard();return false;}
		if(!sailor.startRiding(this)){sailor.discard();return false;}
		Ships.pay(captain,Items.EMERALD,PirateProgression.crewCost(Pirate.state(captain),role),true);return true;
	}
    public boolean recruitGarrick(ServerPlayer captain) {
        if(!captain.isAlive()||captain.isSpectator()||!ownedBy(captain)||!docked()||!protectedHarbor()||captain.level()!=level()||captain.distanceToSqr(this)>48*48||Pirate.state(captain).garrickRescue!=3||crew(GUNNER)>=gunLimit())return false;
        var sailor=net.minecraft.world.entity.EntityTypes.PILLAGER.create(captain.level(),new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED,true));if(sailor==null)return false;
        sailor.addTag(PirateActivities.GARRICK);sailor.addTag(CREW_TAG);sailor.addTag(CREW_OWNER+owner);sailor.addTag("naval_role_"+GUNNER);
        sailor.setNoAi(true);sailor.setCanJoinRaid(false);sailor.setPersistenceRequired();sailor.setPermanentlyInvulnerable(true);
        sailor.setCustomName(Component.literal("Garrick the Deserter"));sailor.setCustomNameVisible(true);sailor.setPos(getX(),getY()+1,getZ());
        if(!captain.level().addFreshEntity(sailor)||!sailor.startRiding(this)){sailor.discard();return false;}
        // Officer Reward: Commit on successful boarding, including calls outside the harbor menu.
        Pirate.state(captain).garrickRescue=5;return true;
    }

	@Override protected boolean canAddPassenger(Entity passenger) {
		if(assignedCrew(passenger))return getPassengers().stream().filter(this::assignedCrew).count()<18;
		return !docked() && passenger instanceof Player && getPassengers().stream().filter(e->e instanceof Player).count()<4;
	}
	@Override public LivingEntity getControllingPassenger() { return getPassengers().stream().filter(e->e instanceof Player p && ownedBy(p)).map(e->(LivingEntity)e).findFirst().orElse(null); }
	@Override public boolean isClientAuthoritative() { return false; }
	@Override protected boolean isLocalClientAuthoritative() { return false; }
	@Override public boolean isPickable() { return !isRemoved(); }
	@Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) { return false; }
	@Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 location) {
		if(player instanceof ServerPlayer server && !player.isSpectator() && player.getItemInHand(hand).isEmpty()) {
			if(ownedBy(player) && player.isShiftKeyDown())server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new ShipMenu(id,inv,null,this),Component.literal("Ship & Crew")));
			else if(!enemy()&&!docked()&&getDeltaMovement().horizontalDistanceSqr()<.01)player.startRiding(this);
		}
		return InteractionResult.SUCCESS;
	}
	private Vec3 seatPosition(Entity passenger,int seat) {
		boolean captain=passenger instanceof Player p && ownedBy(p);
		return local(captain?0:((seat%2==0)?-2:2),1.1,captain?2:-length()/2+2+(seat/2)*2);
	}
	@Override protected void positionRider(Entity passenger,MoveFunction move) {
		var at=seatPosition(passenger,getPassengers().indexOf(passenger));move.accept(passenger,at.x,at.y,at.z);
	}
	public boolean controls(ServerPlayer sender,int buttons) {
		if(docked()||getControllingPassenger()!=sender||buttons<0||(buttons&~511)!=0)return false;
		controls=buttons;lastInput=level().getGameTime();return true;
	}
	private boolean consume(Item item) { for(int i=0;i<cargoSlots();i++)if(cargo.getItem(i).is(item)){cargo.getItem(i).shrink(1);cargo.setChanged();return true;}return false; }
	private PirateShip selected() { return target==null||!(level() instanceof ServerLevel server)?null:server.getEntity(target) instanceof PirateShip ship?ship:null; }
	public void selectTarget(ServerPlayer captain) {
		int range=crew(LOOKOUT)>0?192:80;
		var candidates=level().getEntitiesOfClass(PirateShip.class,getBoundingBox().inflate(range),s->s!=this && !s.protectedHarbor() && (s.enemy()||!java.util.Objects.equals(owner,s.owner)))
			.stream().sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
		int index=-1;for(int i=0;i<candidates.size();i++)if(candidates.get(i).getUUID().equals(target))index=i;
		target=candidates.isEmpty()?null:candidates.get((index+1)%candidates.size()).getUUID();
		Ships.message(captain,target==null?"No ships in contact range.":"Target: "+(selected().enemy()?selected().getName().getString():"Player vessel")+" — "+Math.round(distanceTo(selected()))+" blocks");
	}
	public int fire(boolean port) {
		if(docked()||reload>0||protectedHarbor()||!(level() instanceof ServerLevel server))return 0;
		var guns=blocks.stream().filter(b->b.state().is(Ships.CANNON) && (b.pos().getX()<0)==port).toList();
		int available=enemy()?gunLimit():Math.max(1,crew(GUNNER));int fired=0;
		for(var gun:guns) {
			if(fired>=available)break;
			boolean fire=consume(Ships.FIRE_SHOT);if(!fire&&!consume(Ships.SHOT))break;
			var muzzle=local(gun.pos().getX()+(port?-1.2:1.2),gun.pos().getY()+.7,gun.pos().getZ());
			var aim=rotate(port?-1:1,.03,0,getYRot());var marked=selected();
			if(crew(MATE)>0 && marked!=null && distanceToSqr(marked)<160*160) {
				var towards=marked.position().add(0,1,0).subtract(muzzle).normalize();if(towards.dot(aim)>.75)aim=towards;
			}
			var ball=new Cannonball(Ships.BALL,server);ball.setOwner(this);ball.setItem(new ItemStack(fire?Ships.FIRE_SHOT:Ships.SHOT));ball.setPos(muzzle);ball.shoot(aim.x,aim.y,aim.z,2.5F,0);server.addFreshEntity(ball);fired++;
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,muzzle.x,muzzle.y,muzzle.z,8,.15,.15,.15,.02);
		}
		if(fired>0){reload=60;server.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.HOSTILE,2F,.7F);}return fired;
	}
	public boolean cannonDamage(PirateShip attacker,boolean fire) {
		if(!(level() instanceof ServerLevel server)||sinking||attacker==this||protectedHarbor()||attacker.protectedHarbor()||attacker.level()!=level())return false;
		if(enemy()&&patrolExpiresAt>0&&server.getGameTime()>=patrolExpiresAt)return false;
		if(!enemy()&&!attacker.enemy()&&(!server.isPvpAllowed()||java.util.Objects.equals(owner,attacker.owner)))return false;
		if(attacker.enemy()&&(enemy()||!attacker.hostileTo(this)))return false;
        if(enemy()&&(patrolFaction!=null||encounterKind==2)&&attacker.owner!=null) {
            var captain=server.getServer().getPlayerList().getPlayer(attacker.owner);
            if(captain!=null&&captain.level()==level()) {
                provokedBy=attacker.owner;provokedUntil=level().getGameTime()+1200;
                if(patrolFaction!=null)PirateDiplomacy.penalizeAttack(captain,patrolFaction,"naval patrol");
            }
        }
        lastAttacker=attacker.getUUID();
		entityData.set(HULL,hull()-(fire?8:20));if(fire)burning=Math.max(burning,120);
		if(hull()<=0)sink(attacker);return true;
	}
	public boolean harborRepair(ServerPlayer captain) {
        if(!refitAllowed(captain)||hull()>=maxHull()&&burning==0||!Ships.pay(captain,Items.OAK_PLANKS,10,false)||!Ships.pay(captain,Items.EMERALD,2,false))return false;
        Ships.pay(captain,Items.OAK_PLANKS,10,true);Ships.pay(captain,Items.EMERALD,2,true);entityData.set(HULL,Math.min(maxHull(),hull()+50));burning=0;return true;
    }
    private void sink(PirateShip attacker) {
		if(!(level() instanceof ServerLevel server)||sinking)return;sinking=true;
        if(owner!=null){int lost=0;for(int i=0;i<3;i++)if(crew(NAVIGATOR+i)>0)lost|=1<<i;if(getPassengers().stream().anyMatch(e->assignedCrew(e)&&e.entityTags().contains(PirateActivities.GARRICK)))lost|=8;SeaLedger.get(server).lose(owner,lost);}
		// One-Way Wreck: Remove the vessel once, drop cargo once; player-ship kills never award progression.
		if(enemy()&&attacker.owner!=null && server.getServer().getPlayerList().getPlayer(attacker.owner) instanceof ServerPlayer captain) {
			SeaLegends.victory(captain,this);PirateActivities.navalVictory(captain,this);Pirate.state(captain).navalVictories++;Ships.message(captain,"Naval victories: "+Pirate.state(captain).navalVictories+" | Captain tier: "+PirateProgression.captainTier(Pirate.state(captain)));
		}
		if(!enemy()&&attacker!=this&&attacker.owner!=null&&!attacker.owner.equals(owner)&&server.getServer().getPlayerList().getPlayer(attacker.owner) instanceof ServerPlayer pirate)SeaLegends.infamy(pirate,15);
        for(int i=0;i<cargo.getContainerSize();i++){if(!cargo.getItem(i).isEmpty())spawnAtLocation(server,cargo.getItem(i).copy());cargo.setItem(i,ItemStack.EMPTY);}
		if(docked())for(int x=-width()/2;x<=width()/2;x++)for(int z=-length()/2;z<=length()/2;z++)for(int y=0;y<=height();y++){
			var pos=world(new BlockPos(x,y,z),getYRot(),blockPosition());if(permitted(level().getBlockState(pos)))level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
		}
		spawnAtLocation(server,new ItemStack(Items.OAK_PLANKS,enemy()?16:32));
		for(var passenger:List.copyOf(getPassengers())){passenger.stopRiding();if(assignedCrew(passenger))passenger.discard();}
		discard();
	}
	@Override public void tick() {
		super.tick();setBoundingBox(footprint(position(),getYRot()));if(!(level() instanceof ServerLevel server))return;
        // Patrol Expiry: Reconcile before fire damage can turn cleanup into loot or quest credit.
        if(enemy()) {
            if(patrolExpiresAt==0)patrolExpiresAt=server.getGameTime()+12000;
            if(server.getGameTime()>=patrolExpiresAt||server.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}
        }
		for(var sailor:getPassengers())if(assignedCrew(sailor)&&!sailor.entityTags().contains(CREW_TAG)){sailor.addTag(CREW_TAG);sailor.addTag(CREW_OWNER+owner);}
        shantyTick(server);
        if(encounterKind==1&&tickCount%10==0)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+2,getZ(),4,2,1,4,.01);
        if(reload>0)reload--;
		if(burning>0){burning--;if(tickCount%20==0){entityData.set(HULL,hull()-2);server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY()+1,getZ(),8,2,.5,3,.01);if(hull()<=0){sink(lastAttacker!=null && server.getEntity(lastAttacker) instanceof PirateShip attacker?attacker:this);return;}}}
		if(crew(WORKER)>0&&hull()<maxHull()&&tickCount%80==0&&consume(Items.OAK_PLANKS)){entityData.set(HULL,Math.min(maxHull(),hull()+5));burning=Math.max(0,burning-40);}
		if(docked())return;
		if(enemy()){enemyTick(server);if(isRemoved())return;}
		else if(getControllingPassenger()==null||lastInput==Long.MIN_VALUE||server.getGameTime()-lastInput>10)controls=0;
		float yaw=getYRot()+(((controls&RIGHT)!=0?1:0)-((controls&LEFT)!=0?1:0))*(tier()==0?1.4F:tier()==1?.9F:.65F);
		double throttle=((controls&FORWARD)!=0?1:0)-((controls&BACK)!=0?.45:0);
		var desired=rotate(0,0,throttle*(tier()==0?.27:tier()==1?.2:.16),yaw);var motion=getDeltaMovement().scale(.86).add(desired.scale(.14));
		var next=position().add(motion);if((!enemy()||next.x*next.x+next.z*next.z>=112*112)&&clearWater(server,BlockPos.containing(next),tier(),yaw,this)){setYRot(yaw);setPos(next);setDeltaMovement(motion);}else setDeltaMovement(Vec3.ZERO);
		if((controls&FIRE_LEFT)!=0)fire(true);if((controls&FIRE_RIGHT)!=0)fire(false);
		if(getControllingPassenger() instanceof ServerPlayer captain) {
			if((controls&MENU)!=0&&(previousControls&MENU)==0)captain.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new ShipMenu(id,inv,null,this),Component.literal("Ship & Crew")));
			if((controls&TARGET)!=0&&(previousControls&TARGET)==0)selectTarget(captain);
			if((controls&DOCK)!=0&&(previousControls&DOCK)==0){var error=dock(captain);if(error!=null)Ships.message(captain,error);}
			if(crew(MATE)>0&&!holdFire&&selected()!=null&&distanceToSqr(selected())<160*160){double side=rotate(1,0,0,getYRot()).dot(selected().position().subtract(position()).normalize());if(Math.abs(side)>.75)fire(side<0);}
			if(tickCount%40==0)report(captain);
		}
		previousControls=controls;
	}
	private void report(ServerPlayer captain) {
        if(crew(NAVIGATOR)>0&&tickCount%120==0){Ships.message(captain,"Mara: "+LogboundCompass.status(captain));return;}
		String report="Hull "+Math.round(hull())+"/"+Math.round(maxHull())+" | "+(holdFire?"Hold fire":"Fire at target");
		if(crew(LOOKOUT)>0){
			var contact=level().getEntitiesOfClass(PirateShip.class,getBoundingBox().inflate(192),s->s!=this).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			if(contact!=null)report+=" | "+(contact.enemy()?contact.getName().getString():"PLAYER SHIP")+" "+Math.round(distanceTo(contact))+"m";
			// Lookout samples loaded shoreline only; never increases client render distance or loads chunks.
			outer:for(int r=32;r<=192;r+=16)for(int angle=0;angle<360;angle+=30){var delta=rotate(0,0,r,angle);var pos=BlockPos.containing(getX()+delta.x,64,getZ()+delta.z);if(level().hasChunkAt(pos)&&(level().getBlockState(pos).is(Blocks.SAND)||level().getBlockState(pos).is(Blocks.GRASS_BLOCK)||level().getBlockState(pos).is(Blocks.STONE)||level().getBlockState(pos).is(Blocks.SANDSTONE))){report+=" | ISLAND ~"+r+"m";break outer;}}
		}
		Ships.message(captain,report);
	}
	private void enemyTick(ServerLevel level) {
		var victim=level.getEntitiesOfClass(PirateShip.class,getBoundingBox().inflate(144),s->hostileTo(s)&&s.getControllingPassenger() instanceof ServerPlayer captain&&captain.isAlive()&&!captain.isCreative()&&!captain.isSpectator()).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		controls=0;target=null;if(victim==null){if(++idle>2400){for(var p:List.copyOf(getPassengers())){p.stopRiding();if(!(p instanceof Player))p.discard();}discard();}else controls=FORWARD|(idle%400<80?RIGHT:0);return;}idle=0;target=victim.getUUID();
		var delta=victim.position().subtract(position());float goal=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z))+(delta.length()<55?90:0);
		float turn=net.minecraft.util.Mth.wrapDegrees(goal-getYRot());if(Math.abs(turn)>3)controls|=turn>0?RIGHT:LEFT;
		if(delta.length()>35)controls|=FORWARD;
		var side=rotate(1,0,0,getYRot()).dot(delta.normalize());if(Math.abs(side)>.75&&delta.length()<110)fire(side<0);
	}
	public static PirateShip spawnEnemy(ServerLevel level,BlockPos pos) {return createEnemy(level,pos,null);}
    public static PirateShip spawnPatrol(ServerLevel level,BlockPos pos,PirateProgression.Faction faction) {
        if(!level.dimension().equals(Pirate.LEVEL)||level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)||(long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()<128*128||!clearWater(level,pos,0,0,null))return null;
        return createEnemy(level,pos,faction);
    }
    public static PirateShip spawnSpecial(ServerLevel level,BlockPos pos,int kind) {
        if(kind<1||kind>2)return null;var ship=spawnPatrol(level,pos,null);if(ship==null)return null;ship.encounterKind=kind;
        ship.setCustomName(Component.literal(kind==1?"The Pale Reckoning":"Iron Writ Bounty Hunters"));
        ship.syncBuild(ship.blocks.stream().map(b->b.state().is(BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_wool")))?new ShipBlock(b.pos(),BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(kind==1?"light_blue_wool":"black_wool")).defaultBlockState()):b).toList());return ship;
    }
    private static PirateShip createEnemy(ServerLevel level,BlockPos pos,PirateProgression.Faction faction) {
		var ship=new PirateShip(Ships.SHIP,level);ship.entityData.set(ENEMY,true);ship.entityData.set(DOCKED,false);ship.holdFire=false;ship.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
		ship.patrolFaction=faction;ship.entityData.set(FLAG,faction==null?0:faction.ordinal()+1);ship.patrolExpiresAt=level.getGameTime()+12000;
        ship.setCustomName(Component.literal(faction==null?"Outlaw Raider":faction.title+" Patrol"));ship.setCustomNameVisible(true);
        var sail=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(faction==null?"white_wool":faction==PirateProgression.Faction.CORSAIRS?"red_wool":faction==PirateProgression.Faction.FLEET?"blue_wool":"cyan_wool"));
        var build=new ArrayList<ShipBlock>();for(int x=-3;x<=3;x++)for(int z=-7;z<=7;z++){build.add(new ShipBlock(new BlockPos(x,0,z),Blocks.SPRUCE_PLANKS.defaultBlockState()));if(Math.abs(x)==3)build.add(new ShipBlock(new BlockPos(x,1,z),Blocks.SPRUCE_FENCE.defaultBlockState()));}
		for(int y=1;y<=5;y++)build.add(new ShipBlock(new BlockPos(0,y,0),Blocks.SPRUCE_LOG.defaultBlockState()));
		for(int x=-2;x<=2;x++)for(int y=3;y<=5;y++)if(x!=0)build.add(new ShipBlock(new BlockPos(x,y,0),sail.defaultBlockState()));
		for(int x:new int[]{-3,3})for(int z:new int[]{-3,3}){build.removeIf(b->b.pos().equals(new BlockPos(x,1,z)));build.add(new ShipBlock(new BlockPos(x,1,z),Ships.CANNON.defaultBlockState().setValue(ShipStationBlock.FACING,x<0?net.minecraft.core.Direction.WEST:net.minecraft.core.Direction.EAST)));}
		ship.syncBuild(build);ship.cargo.setItem(0,new ItemStack(Ships.SHOT,64));ship.cargo.setItem(1,new ItemStack(Items.EMERALD,4));ship.cargo.setItem(2,new ItemStack(Items.IRON_INGOT,6));if(!level.addFreshEntity(ship)){ship.discard();return null;}return ship;
	}
	@Override protected void addAdditionalSaveData(ValueOutput out) {
		out.putInt("EncounterKind",encounterKind);out.putInt("CargoTier",cargoTier);out.putInt("Flag",flag());out.putBoolean("ShantyPlaying",shantyPlaying);
        out.putString("PatrolFaction",patrolFaction==null?"":patrolFaction.id);out.putLong("PatrolExpiresAt",patrolExpiresAt);out.putInt("PatrolIdle",idle);
        if(provokedBy!=null)out.store("ProvokedBy",UUIDUtil.CODEC,provokedBy);out.putLong("ProvokedUntil",provokedUntil);
        if(lastAttacker!=null)out.store("LastAttacker",UUIDUtil.CODEC,lastAttacker);
		if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putInt("Tier",tier());out.putBoolean("Docked",docked());out.putBoolean("Enemy",enemy());out.putFloat("Hull",hull());out.putInt("Burning",burning);out.putInt("Reload",reload);out.putBoolean("HoldFire",holdFire);
		out.store("Build",ShipBlock.CODEC.listOf(),blocks);out.store("Cargo",ItemContainerContents.CODEC,ItemContainerContents.fromItems(cargo.getItems()));
	}
	@Override protected void readAdditionalSaveData(ValueInput in) {
		encounterKind=Math.clamp(in.getIntOr("EncounterKind",0),0,2);cargoTier=Math.clamp(in.getIntOr("CargoTier",0),0,2);entityData.set(FLAG,Math.clamp(in.getIntOr("Flag",0),0,3));shantyPlaying=in.getBooleanOr("ShantyPlaying",false);
        patrolFaction=PirateDiplomacy.faction(in.getStringOr("PatrolFaction",""));patrolExpiresAt=Math.max(0,in.getLongOr("PatrolExpiresAt",0));idle=Math.clamp(in.getIntOr("PatrolIdle",0),0,2401);
        provokedBy=in.read("ProvokedBy",UUIDUtil.CODEC).orElse(null);provokedUntil=Math.max(0,in.getLongOr("ProvokedUntil",0));
        lastAttacker=in.read("LastAttacker",UUIDUtil.CODEC).orElse(null);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);entityData.set(TIER,Math.clamp(in.getIntOr("Tier",0),0,2));entityData.set(DOCKED,in.getBooleanOr("Docked",true));entityData.set(ENEMY,in.getBooleanOr("Enemy",false));entityData.set(HULL,Math.clamp(in.getFloatOr("Hull",200),0,maxHull()));
		burning=Math.clamp(in.getIntOr("Burning",0),0,120);reload=Math.clamp(in.getIntOr("Reload",0),0,60);holdFire=in.getBooleanOr("HoldFire",true);
		syncBuild(in.read("Build",ShipBlock.CODEC.listOf()).orElse(List.of()).stream().limit(blockLimit()).toList());cargo.clearContent();in.read("Cargo",ItemContainerContents.CODEC).ifPresent(c->c.copyInto(cargo.getItems()));
	}
}
