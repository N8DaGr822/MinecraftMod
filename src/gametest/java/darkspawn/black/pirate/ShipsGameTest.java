package darkspawn.black.pirate;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class ShipsGameTest {
	private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
		var level = helper.getLevel().getServer().getLevel(Pirate.LEVEL);
		for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z);
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pirate-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5), Vec3.ZERO, 0,0,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING)); ready(player); return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
		Pirate.state(player).lastTravel = Long.MIN_VALUE;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static BlockPos location(GameTestHelper helper){return new BlockPos(2000+Math.floorMod(helper.absolutePos(BlockPos.ZERO).getX(),10000)*128,64,2000+Math.floorMod(helper.absolutePos(BlockPos.ZERO).getZ(),10000)*128);}
	private static void supplies(ServerPlayer player){player.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,64));player.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,64));player.getInventory().setItem(2,new ItemStack(Ships.HELM_ITEM));}
	private static PirateShip build(GameTestHelper helper,ServerPlayer player,BlockPos at){supplies(player);var ship=PirateShip.build(player,at);helper.assertTrue(ship!=null,"Paid vessel builds in clear water");return ship;}
	private static void clear(PirateShip ship){
		if(ship==null)return;
		var level=ship.level();var origin=ship.blockPosition();
		if(ship.docked())for(int x=-ship.width()/2-1;x<=ship.width()/2+1;x++)for(int z=-ship.length()/2-1;z<=ship.length()/2+1;z++)for(int y=0;y<=ship.height();y++){
			var pos=origin.offset(x,y,z);if(PirateShip.permitted(level.getBlockState(pos)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
		}
		for(var passenger:java.util.List.copyOf(ship.getPassengers())){passenger.stopRiding();if(!(passenger instanceof net.minecraft.world.entity.player.Player))passenger.discard();}ship.discard();
	}
    @GameTest public void cargoPredictionUsesSyncedCapacityWithoutShipReference(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());
        captain.level().setBlockAndUpdate(pos.above(),Ships.SHIPYARD.defaultBlockState());
        try {
            // Mirror the client menu's null ship, while a valid station permits exercising transfer logic here.
            var menu=new ShipMenu(1,captain.getInventory(),pos.above(),null);
            menu.setData(0,1);menu.setData(15,36);menu.setData(16,1);
            var cargo=new ItemStack(Items.GOLD_INGOT,7);cargo.set(DataComponents.CUSTOM_NAME,Component.literal("Voyage cargo"));
            captain.getInventory().setItem(0,cargo);
            int source=-1;for(int i=54;i<menu.slots.size();i++)if(menu.slots.get(i).getContainerSlot()==0)source=i;
            helper.assertTrue(source>=0&&!menu.quickMoveStack(captain,source).isEmpty(),"Shift-click works with synced capacity and no ship reference");
            helper.assertTrue(menu.slots.get(27).getItem().getCount()==7&&menu.slots.get(27).getItem().get(DataComponents.CUSTOM_NAME).getString().equals("Voyage cargo")&&captain.getInventory().getItem(0).isEmpty(),"Predicted transfer preserves count and components");
            helper.assertTrue(!menu.slots.get(0).isActive()&&!menu.slots.get(36).isActive(),"Hidden and locked slots remain inaccessible");
        } finally {captain.level().setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());remove(captain);}helper.succeed();
    }
    @GameTest public void dismountImmediatelyClearsQueuedCannonInput(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            captain.level().setBlockAndUpdate(pos.offset(-3,1,-3),Ships.CANNON.defaultBlockState());
            ship.cargo.setItem(0,new ItemStack(Ships.SHOT,3));
            helper.assertTrue(ship.launch(captain)==null&&ship.controls(captain,PirateShip.FORWARD|PirateShip.FIRE_LEFT),"Captain queues movement and cannon input");
            captain.stopRiding();ship.commonTick();ship.tick();
            helper.assertTrue(ship.cargo.getItem(0).getCount()==3&&ship.getDeltaMovement().horizontalDistanceSqr()==0,"Dismount cancels queued firing and thrust immediately");
        } finally {clear(ship);remove(captain);}helper.succeed();
    }
	@GameTest public void constructionAndRejectedLaunchAreAtomic(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());PirateShip ship=null;
		try{
			helper.assertTrue(PirateShip.build(captain,pos)==null&&captain.level().isEmptyBlock(pos),"Missing payment does not place a platform");
			ship=build(helper,captain,pos);helper.assertTrue(captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).getCount()==23&&captain.getInventory().getItem(2).isEmpty(),"Exact plank and helm cost consumed once");
			captain.level().setBlockAndUpdate(pos.offset(1,1,3),Blocks.CHEST.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,3));
			helper.assertTrue(ship.launch(captain)!=null&&ship.docked()&&captain.level().getBlockState(pos).is(Blocks.OAK_PLANKS)&&ship.cargo.getItem(0).getCount()==3,"Unsupported container rejects whole launch without losing blocks or cargo");
			captain.level().setBlockAndUpdate(pos.offset(1,1,3),Blocks.AIR.defaultBlockState());
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void launchDockAndBlockedDockPreserveBuildAndCargo(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			captain.level().setBlockAndUpdate(pos.offset(-3,1,-3),Ships.CANNON.defaultBlockState());
			ship.cargo.setItem(0,new ItemStack(Items.EMERALD,11));
			String error=ship.launch(captain);helper.assertTrue(error==null,"Launch succeeds: "+error);
			helper.assertTrue(!ship.docked()&&captain.getVehicle()==ship&&captain.level().isEmptyBlock(pos)&&ship.guns()==1,"Blocks become one owned moving ship with a seated captain");
			var obstruction=pos.offset(3,2,3);captain.level().setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());
			helper.assertTrue(ship.dock(captain)!=null&&!ship.docked()&&captain.level().isEmptyBlock(pos),"Blocked docking restores nothing");captain.level().setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
			helper.assertTrue(ship.dock(captain)==null&&ship.docked()&&captain.getVehicle()==null,"Docking restores editable world blocks and dismounts captain");
			helper.assertTrue(captain.level().getBlockState(pos.offset(-3,1,-3)).is(Ships.CANNON)&&ship.cargo.getItem(0).getCount()==11,"Custom gun and cargo survive conversion");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void largerPlatformRequiresProgressionAndExactExpansionCost(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			for(int i=0;i<3;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
			helper.assertTrue(ship.upgrade(captain)!=null&&ship.tier()==0&&captain.getInventory().getItem(0).getCount()==64,"Rank gate does not consume resources");
			Pirate.state(captain).navalVictories=5;helper.assertTrue(ship.upgrade(captain)==null&&ship.width()==11&&ship.length()==23&&ship.height()==8&&ship.gunLimit()==8,"Five victories unlock larger bounds");
			helper.assertTrue(captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).isEmpty()&&captain.getInventory().getItem(2).getCount()==44,"Expansion consumes exactly 148 extra platform blocks");
			helper.assertTrue(captain.level().getBlockState(pos.offset(5,0,11)).is(Blocks.OAK_PLANKS),"Expanded corners actually exist");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());Pirate.state(captain).write(output);
			var restored=new PiratePlayerState();restored.read(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));helper.assertTrue(restored.navalVictories==5,"Rank persists in player save");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
    @GameTest public void thirdShipTierPreservesCostBoundsCaptureAndSave(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var restored=new PirateShip(Ships.SHIP,captain.level());
        try {
            var state=Pirate.state(captain);state.harborContract=2;state.islandContracts=new int[]{2,2,2};
            for(int i=0;i<8;i++)state.logboundVisited.add(new BlockPos(i*256,65,0));
            captain.getInventory().clearContent();for(int i=0;i<3;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            helper.assertTrue(ship.upgrade(captain)==null&&ship.tier()==1,"Independent milestones unlock first expansion");
            helper.assertTrue(ship.upgrade(captain)!=null&&Ships.pay(captain,Items.OAK_PLANKS,44,false),"Second expansion rejects insufficient funds without consuming remainder");
            captain.getInventory().clearContent();for(int i=0;i<4;i++)captain.getInventory().setItem(i,new ItemStack(Items.OAK_PLANKS,64));
            captain.level().setBlockAndUpdate(pos.offset(7,0,15),Blocks.STONE.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)!=null&&Ships.pay(captain,Items.OAK_PLANKS,256,false),"Blocked outer corner rejects atomically");captain.level().setBlockAndUpdate(pos.offset(7,0,15),Blocks.AIR.defaultBlockState());
            captain.level().setBlockAndUpdate(pos.above(9),Blocks.STONE.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)!=null,"New height above existing deck must also be clear");captain.level().setBlockAndUpdate(pos.above(9),Blocks.AIR.defaultBlockState());
            helper.assertTrue(ship.upgrade(captain)==null&&ship.tier()==2&&ship.width()==15&&ship.length()==31&&ship.height()==10&&ship.blockLimit()==1536&&ship.gunLimit()==12,"Third tier has real expanded bounds");
            helper.assertTrue(Ships.pay(captain,Items.OAK_PLANKS,44,false)&&!Ships.pay(captain,Items.OAK_PLANKS,45,false)&&ship.upgrade(captain)!=null,"Third tier costs exactly 212 planks and cannot expand again");
            int added=0;
            outer:for(int y=3;y<=10;y++)for(int x=-7;x<=7;x++)for(int z=-15;z<=15;z++){
                captain.level().setBlockAndUpdate(pos.offset(x,y,z),Blocks.OAK_PLANKS.defaultBlockState());if(++added==1070)break outer;
            }
            helper.assertTrue(ship.launch(captain)==null&&ship.blocks().size()==1536,"Maximum permitted build captures without losing blocks");
            var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(ship.getId(),ship.getEntityData().packDirty());
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),captain.registryAccess());
            try {
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);
                var decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.packedItems().equals(packet.packedItems()),"Maximum build fits and round-trips the actual native entity sync packet");
            } finally {buffer.release();}
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.tier()==2&&restored.blocks().equals(ship.blocks())&&restored.ownedBy(captain),"Native save retains full third-tier layout and ownership");
            helper.assertTrue(ship.dock(captain)==null&&captain.level().getBlockState(pos.offset(7,0,15)).is(Blocks.OAK_PLANKS),"Largest captured ship restores its outer deck");
        } finally {restored.discard();clear(ship);remove(captain);}helper.succeed();
    }
    @GameTest public void factionCrewOffersChargeActualPricesAndRespectRoleLimits(GameTestHelper helper) {
        var pos=new BlockPos(-48,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        var originalDifficulty=captain.level().getDifficulty();
        try {
            captain.level().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
            var state=Pirate.state(captain);state.factionContracts=new int[]{2,2,2};state.factionStanding=new int[]{25,25,25};
            captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,32));
            for(int role=0;role<4;role++)helper.assertTrue(ship.recruit(captain,role),"All roles retain recruitment access");
            helper.assertTrue(captain.getInventory().getItem(0).isEmpty(),"Offers charge 8 + 8 + 4 + normal 12 for lookout");
            helper.assertTrue(ship.getPassengers().stream().anyMatch(e->e.getName().getString().equals("Fleet Cannoneer")),"Earned crew has its faction title");
            helper.assertTrue(ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.PIGLIN)&&ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.PILLAGER)&&ship.getPassengers().stream().anyMatch(e->e.getType()==net.minecraft.world.entity.EntityTypes.HUSK),"Faction offers recruit actual Piglin, Illager and undead species");
            helper.assertTrue(PirateDiplomacy.membership(captain,PirateProgression.Faction.FLEET,9)&&PirateDiplomacy.membership(captain,PirateProgression.Faction.FLEET,10)&&ship.crew(PirateShip.GUNNER)==1,"Resigning keeps hired crew despite lost standing");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(saved);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,saved.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.MATE)==1&&restored.crew(PirateShip.WORKER)==1&&restored.crew(PirateShip.GUNNER)==1,"Native recursive loading restores distinct-species role passengers");
            var oldDifficulty=captain.level().getDifficulty();
            try {
                captain.level().getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
                for(var entity:restored.getPassengers()) {
                    var mob=(net.minecraft.world.entity.Mob)entity;
                    helper.assertTrue(mob.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())&&!mob.canAttack(captain),"Saved crew retain captain loyalty and cannot attack their owner");
                    mob.checkDespawn();helper.assertTrue(!mob.isRemoved(),"Recruited hostile species survive Peaceful difficulty");
                }
            } finally {captain.level().getServer().setDifficulty(oldDifficulty,true);for(var entity:java.util.List.copyOf(restored.getPassengers()))entity.discard();restored.discard();}
            captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,64));helper.assertTrue(!ship.recruit(captain,PirateShip.MATE)&&captain.getInventory().getItem(0).getCount()==64,"Unique role cap still rejects without charging");
        } finally {captain.level().getServer().setDifficulty(originalDifficulty,true);clear(ship);remove(captain);}helper.succeed();
    }
	@GameTest public void ownershipControlsAndCannonAmmunitionAreAuthoritative(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var stranger=player(helper,pos.offset(12,1,0));var ship=build(helper,captain,pos);
		try{
			helper.assertTrue(ship.launch(stranger)!=null&&ship.docked(),"Non-owner cannot launch");captain.level().setBlockAndUpdate(pos.offset(-3,1,3),Ships.CANNON.defaultBlockState());
			helper.assertTrue(ship.launch(captain)==null,"Owner launches");
			helper.assertTrue(!ship.controls(stranger,PirateShip.FORWARD)&&!ship.controls(captain,1024)&&ship.controls(captain,PirateShip.FORWARD),"Only captain may submit valid button bits");
			helper.assertTrue(ship.fire(true)==0,"No free cannon ammunition");ship.cargo.setItem(0,new ItemStack(Ships.SHOT,3));
			helper.assertTrue(ship.fire(true)==1&&ship.cargo.getItem(0).getCount()==2,"One manual gun consumes one round");
			helper.assertTrue(ship.fire(true)==0&&ship.cargo.getItem(0).getCount()==2,"Reload prevents repeated packet firing");
			var before=ship.position();ship.tick();helper.assertTrue(ship.position().distanceToSqr(before)>0,"Server control moves ship across water");
		}finally{clear(ship);remove(captain);remove(stranger);}helper.succeed();
	}
	@GameTest public void paidCrewPerformRepairsAndPersistAsPassengers(GameTestHelper helper){
		var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
		try{
			captain.getInventory().setItem(3,new ItemStack(Items.EMERALD,64));
			for(int role=0;role<4;role++)helper.assertTrue(ship.recruit(captain,role),"All four crew roles can be recruited at harbor");
			helper.assertTrue(captain.getInventory().getItem(3).getCount()==20&&!ship.recruit(captain,PirateShip.MATE),"Crew costs exactly 44 emeralds and unique roles cannot duplicate");
			var legacy=ship.getPassengers().getFirst();legacy.removeTag(PirateShip.CREW_TAG);legacy.removeTag(PirateShip.CREW_OWNER+captain.getUUID());ship.tick();
            helper.assertTrue(legacy.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())&&ship.crew(PirateShip.MATE)==1,"Legacy villagers migrate to saved captain loyalty without losing their role");
            helper.assertTrue(ship.launch(captain)==null,"Crew launch seated with captain");ship.setPos(4000.5,64,4000.5);
			var enemy=PirateShip.spawnEnemy(captain.level(),new BlockPos(4040,64,4000));ship.cannonDamage(enemy,true);enemy.discard();
			ship.cargo.setItem(0,new ItemStack(Items.OAK_PLANKS,2));float hurt=ship.hull();ship.tickCount=79;ship.commonTick();ship.tick();
			helper.assertTrue(ship.hull()>hurt&&ship.cargo.getItem(0).getCount()==1,"Shipworker consumes stocked plank and repairs net hull damage");
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);
			helper.assertTrue(output.buildResult().toString().contains("naval_role_3")&&output.buildResult().toString().contains("naval_role_0"),"Native passenger save contains named role crew");
		}finally{clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void shipSaveRestoresBuildCargoOwnershipAndCooldown(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var restored=new PirateShip(Ships.SHIP,captain.level());
		try{
			captain.level().setBlockAndUpdate(pos.offset(3,1,3),Ships.CANNON.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Ships.SHOT,8));helper.assertTrue(ship.launch(captain)==null,"Launch fixture");ship.fire(false);
			var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
			helper.assertTrue(restored.ownedBy(captain)&&!restored.docked()&&restored.blocks().equals(ship.blocks())&&restored.cargo.getItem(0).getCount()==7,"Native save restores exact build, cargo and owner");
			helper.assertTrue(restored.fire(false)==0,"Reload survives save/load");
		}finally{restored.discard();clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void navalCombatProtectsHarborAndAwardsNpcVictoriesOnce(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var enemy=PirateShip.spawnEnemy(captain.level(),pos.east(40));
		try{
			float hull=ship.hull();ship.setPos(.5,64,60.5);helper.assertTrue(!ship.cannonDamage(enemy,false)&&ship.hull()==hull,"Harbor vessels reject cannon damage");ship.setPos(pos.getX()+.5,64,pos.getZ()+.5);
			for(int i=0;i<10;i++)enemy.cannonDamage(ship,false);
			helper.assertTrue(enemy.isRemoved()&&Pirate.state(captain).navalVictories==1,"NPC sinking grants one victory");enemy.cannonDamage(ship,false);
			helper.assertTrue(Pirate.state(captain).navalVictories==1,"Repeated hits cannot duplicate rank rewards");
		}finally{enemy.discard();clear(ship);remove(captain);}helper.succeed();
	}
	@GameTest public void playerShipDamageRespectsPvpAndDoesNotAwardRank(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var other=player(helper,pos.east(40).above());var ship=build(helper,captain,pos);var victim=build(helper,other,pos.east(40));
		var rules=captain.level().getGameRules();boolean original=rules.get(net.minecraft.world.level.gamerules.GameRules.PVP);
		try{
			rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,false,captain.level().getServer());helper.assertTrue(!victim.cannonDamage(ship,false)&&victim.hull()==200,"PVP-disabled worlds reject player-ship damage");
			rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,true,captain.level().getServer());helper.assertTrue(victim.cannonDamage(ship,false)&&victim.hull()==180,"PVP-enabled worlds permit enemy player vessels to take cannon damage");
			for(int i=0;i<9;i++)victim.cannonDamage(ship,false);
			helper.assertTrue(victim.isRemoved()&&Pirate.state(captain).navalVictories==0,"Player ship kills cannot farm rank");
		}finally{rules.set(net.minecraft.world.level.gamerules.GameRules.PVP,original,captain.level().getServer());clear(ship);clear(victim);remove(captain);remove(other);}helper.succeed();
	}
	@GameTest(maxTicks=60) public void actualCannonballHitsShipWithoutBreakingTerrain(GameTestHelper helper){
		var pos=location(helper);var captain=player(helper,pos.above());var other=player(helper,pos.west(40).above());var ship=build(helper,captain,pos);var victim=build(helper,other,pos.west(40));
		captain.level().setBlockAndUpdate(pos.offset(-3,1,0),Ships.CANNON.defaultBlockState());ship.cargo.setItem(0,new ItemStack(Ships.SHOT,2));
		String error=ship.launch(captain);helper.assertTrue(error==null,"Combat launch: "+error);for(int x=-4;x<=2;x++)for(int z=-2;z<=2;z++)captain.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,true);
		helper.runAfterDelay(5,()->helper.assertTrue(ship.fire(true)==1,"Cannonball leaves barrel"));
		helper.runAfterDelay(27,()->{
			try{
				helper.assertTrue(victim.hull()<200,"Real projectile collision damages target hull; PvP="+captain.level().isPvpAllowed()+" shipTicks="+ship.tickCount+" balls="+captain.level().getEntitiesOfClass(Cannonball.class,ship.getBoundingBox().inflate(100)).stream().map(ball->ball.tickCount+":"+ball.position()).toList());
				helper.assertTrue(captain.level().getBlockState(pos.west(40)).is(Blocks.OAK_PLANKS),"Impact does not explode terrain or ship blocks");
				helper.succeed();
			}finally{for(int x=-4;x<=2;x++)for(int z=-2;z<=2;z++)captain.level().setChunkForced((pos.getX()>>4)+x,(pos.getZ()>>4)+z,false);clear(ship);clear(victim);remove(captain);remove(other);}
		});
	}
    @GameTest public void patrolsRespectStandingRetaliateAndPersist(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var hostile=player(helper,pos.offset(0,1,32));var ship=build(helper,captain,pos);var other=build(helper,hostile,pos.offset(0,0,32));PirateShip patrol=null;
        try {
            helper.assertTrue(ship.launch(captain)==null&&other.launch(hostile)==null,"Both captains sail independently");
            Pirate.state(hostile).factionStanding[1]=-25;
            patrol=PirateShip.spawnPatrol(captain.level(),pos.offset(24,0,16),PirateProgression.Faction.FLEET);
            helper.assertTrue(patrol!=null&&patrol.getName().getString().contains("Iron Fleet")&&patrol.blocks().stream().anyMatch(b->net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.state().getBlock()).getPath().equals("blue_wool")),"Fleet patrol has recognizable name and blue sail");
            helper.assertTrue(!patrol.hostileTo(ship)&&patrol.hostileTo(other),"Patrol distinguishes neutral and hostile captains");float hull=ship.hull();
            helper.assertTrue(!ship.cannonDamage(patrol,false)&&ship.hull()==hull&&other.cannonDamage(patrol,false),"Projectile damage also respects personal standing");
            Pirate.state(captain).factionStanding[1]=50;
            helper.assertTrue(patrol.cannonDamage(ship,false)&&Pirate.state(captain).factionStanding[1]==40&&patrol.hostileTo(ship),"Attacking a friendly patrol costs reputation and provokes retaliation despite positive standing");
            patrol.cannonDamage(ship,false);helper.assertTrue(Pirate.state(captain).factionStanding[1]==40,"One broadside cannot apply a reputation penalty for each cannon");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());patrol.saveWithoutId(out);var restored=new PirateShip(Ships.SHIP,captain.level());
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),out.buildResult()));helper.assertTrue(restored.patrolFaction()==PirateProgression.Faction.FLEET&&restored.hostileTo(ship),"Faction and retaliation survive reload");restored.discard();
            var tag=out.buildResult();tag.putLong("ProvokedUntil",0);restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),tag));helper.assertTrue(!restored.hostileTo(ship),"Expired provocation returns to standing rules");restored.discard();
            other.setPos(0,64,70);helper.assertTrue(!patrol.hostileTo(other)&&!other.cannonDamage(patrol,false),"Harbor protects even hostile captains");
            tag.putLong("PatrolExpiresAt",1);restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),tag));restored.tick();helper.assertTrue(restored.isRemoved(),"Expired patrol is cleaned up after reload without reward");
            helper.assertTrue(PirateShip.spawnPatrol(captain.level(),new BlockPos(0,64,70),PirateProgression.Faction.CORSAIRS)==null,"Patrols cannot spawn in harbor");
        } finally {clear(patrol);clear(ship);clear(other);remove(captain);remove(hostile);}helper.succeed();
    }
    @GameTest public void outlawHuntOnlyCountsEligibleSinksAndPaysOnce(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);PirateShip outlaw=null,patrol=null;
        try {
            helper.assertTrue(ship.launch(captain)==null,"Captain sails for hunt");Pirate.state(captain).repeatContracts[1]=1;
            patrol=PirateShip.spawnPatrol(captain.level(),pos.offset(24,0,16),PirateProgression.Faction.CORSAIRS);helper.assertTrue(patrol!=null,"Faction patrol fixture spawns");
            for(int i=0;i<10;i++)patrol.cannonDamage(ship,false);
            helper.assertTrue(patrol.isRemoved()&&Pirate.state(captain).repeatContracts[1]==1,"Faction patrol sink does not fulfill outlaw hunt");
            outlaw=PirateShip.spawnEnemy(captain.level(),pos.offset(24,0,-16));for(int i=0;i<10;i++)outlaw.cannonDamage(ship,false);
            helper.assertTrue(outlaw.isRemoved()&&Pirate.state(captain).repeatContracts[1]==2,"Confirmed outlaw sink fulfills active hunt");int wins=Pirate.state(captain).navalVictories;
            helper.assertTrue(!outlaw.cannonDamage(ship,false)&&Pirate.state(captain).navalVictories==wins,"Duplicate sink cannot award victory twice");
            captain.stopRiding();captain.teleportTo(2,65,4);captain.getInventory().clearContent();helper.assertTrue(PirateActivities.contract(captain,1,5)&&!PirateActivities.contract(captain,1,5)&&Pirate.state(captain).factionStanding[1]==10,"Hunt pays matching standing once at harbor");
        } finally {clear(patrol);clear(outlaw);clear(ship);remove(captain);}helper.succeed();
    }
    @GameTest public void garrickRecruitmentRespectsBerthsAndNativePassengerPersistence(GameTestHelper helper) {
        var pos=new BlockPos(48,64,-40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            var state=Pirate.state(captain);state.garrickRescue=3;captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,64));
            for(int i=0;i<4;i++)helper.assertTrue(ship.recruit(captain,PirateShip.GUNNER),"Ordinary gunners fill available berths");
            helper.assertTrue(!ship.recruitGarrick(captain)&&state.garrickRescue==3,"Full crew preserves officer reward");ship.getPassengers().getFirst().discard();
            int emeralds=captain.getInventory().getItem(0).getCount();
            helper.assertTrue(ship.recruitGarrick(captain)&&state.garrickRescue==5&&ship.crew(PirateShip.GUNNER)==4&&!ship.recruitGarrick(captain)&&captain.getInventory().getItem(0).getCount()==emeralds,"Garrick boards once without an emerald charge");
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(saved);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,saved.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.GUNNER)==4&&restored.getPassengers().stream().anyMatch(e->e.entityTags().contains(PirateActivities.GARRICK)&&e.getName().getString().equals("Garrick the Deserter")&&e.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID())),"Named officer retains role and captain identity through native recursive load");
            for(var sailor:java.util.List.copyOf(restored.getPassengers()))sailor.discard();restored.discard();
        } finally {clear(ship);remove(captain);}helper.succeed();
    }

    @GameTest public void patrolSpawningRespectsLoadedBudgetAndRules(GameTestHelper helper) {
        var pos=location(helper);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);var level=captain.level();
        var watcher=player(helper,pos.offset(320,1,0));
        var spawned=new java.util.ArrayList<PirateShip>();var rule=net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS;boolean oldRule=level.getGameRules().get(rule);
        try {
            helper.assertTrue(ship.launch(captain)==null,"Captain sails before patrol spawning");var target=pos.offset(96,0,64);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk((target.getX()>>4)+x,(target.getZ()>>4)+z);
            Ships.spawnPatrols(level);spawned.addAll(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()));
            helper.assertTrue(spawned.size()==1,"Loaded clear ocean gets one bounded encounter");Ships.spawnPatrols(level);
            helper.assertTrue(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()).size()==1,"Existing nearby patrol prevents another spawn");spawned.getFirst().discard();
            var capPos=pos.offset(320,0,0);level.getChunk(capPos);
            for(int i=0;i<8;i++)spawned.add(PirateShip.spawnEnemy(level,capPos));
            int loaded=0;for(var entity:level.getAllEntities())if(entity instanceof PirateShip vessel&&vessel.enemy()&&vessel.isAlive())loaded++;helper.assertTrue(loaded>=8,"Cap fixture has eight registered loaded vessels, observed "+loaded);
            Ships.spawnPatrols(level);helper.assertTrue(level.getEntitiesOfClass(PirateShip.class,ship.getBoundingBox().inflate(192),e->e.enemy()&&e.isAlive()).isEmpty(),"Eight loaded NPC vessels prevent another encounter");
            for(var patrol:spawned)patrol.discard();level.getGameRules().set(rule,false,level.getServer());
            helper.assertTrue(PirateShip.spawnPatrol(level,target,PirateProgression.Faction.DROWNED)==null,"Mob spawn rule disables patrol creation");
            level.getGameRules().set(rule,true,level.getServer());var unloaded=new BlockPos(-1000000,64,-1000000);
            helper.assertTrue(level.getChunkSource().getChunkNow(unloaded.getX()>>4,unloaded.getZ()>>4)==null&&PirateShip.spawnPatrol(level,unloaded,PirateProgression.Faction.DROWNED)==null&&level.getChunkSource().getChunkNow(unloaded.getX()>>4,unloaded.getZ()>>4)==null,"Patrol attempts never load distant chunks");
        } finally {level.getGameRules().set(rule,oldRule,level.getServer());for(var patrol:spawned)clear(patrol);clear(ship);remove(captain);remove(watcher);}helper.succeed();
    }

    @GameTest public void shipIdentityCargoUpgradesAndPagesPreserveOwnership(GameTestHelper helper) {
        var pos=new BlockPos(-40,64,-40);var captain=player(helper,pos.above());var stranger=player(helper,pos.offset(12,1,0));var ship=build(helper,captain,pos);
        try {
            var menu=new ShipMenu(1,captain.getInventory(),null,ship);var foreign=new ShipMenu(2,stranger.getInventory(),null,ship);
            helper.assertTrue(menu.rename(captain,"  North Star  ")&&ship.getName().getString().equals("North Star")&&!foreign.rename(stranger,"Stolen"),"Only nearby owner may name a docked ship");
            helper.assertTrue(!menu.rename(captain,"x".repeat(33))&&!menu.rename(captain,"bad\nname")&&ship.getName().getString().equals("North Star"),"Overlong and control-character names are rejected");
            Pirate.state(captain).factionMembership="corsairs";helper.assertTrue(ship.cycleFlag(captain)&&ship.flag()==1&&!ship.cycleFlag(stranger),"Only owner selects membership flag");
            ship.cargo.setItem(0,new ItemStack(Items.DIAMOND,3));captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,48));captain.getInventory().setItem(1,new ItemStack(Items.EMERALD,12));
            helper.assertTrue(!ship.upgradeCargo(captain)&&ship.cargoSlots()==27&&captain.getInventory().getItem(0).getCount()==48,"Cargo milestone gate consumes nothing");
            Pirate.state(captain).navalVictories=5;helper.assertTrue(ship.upgradeCargo(captain)&&ship.cargoSlots()==36&&captain.getInventory().getItem(0).getCount()==32&&captain.getInventory().getItem(1).getCount()==8,"First cargo upgrade charges exactly sixteen iron and four emeralds");
            helper.assertTrue(!ship.upgradeCargo(captain),"Veteran required for final hold");menu.broadcastChanges();helper.assertTrue(!menu.slots.get(27).isActive()&&!menu.slots.get(27).mayPlace(new ItemStack(Items.DIAMOND)),"Second page is hidden and server-locked until selected");
            helper.assertTrue(menu.clickMenuButton(captain,ShipMenu.CARGO_PAGE)&&menu.slots.get(27).isActive()&&!menu.slots.get(36).isActive()&&!menu.slots.get(0).mayPickup(captain),"36-slot hold exposes exactly nine slots on page two");
            ship.cargo.setItem(1,new ItemStack(Items.GOLD_INGOT,5));captain.getInventory().setItem(2,new ItemStack(Items.GOLD_INGOT,7));int inventorySlot=-1;for(int i=54;i<menu.slots.size();i++)if(menu.slots.get(i).getContainerSlot()==2)inventorySlot=i;
            helper.assertTrue(inventorySlot>=0&&!menu.quickMoveStack(captain,inventorySlot).isEmpty()&&ship.cargo.getItem(27).is(Items.GOLD_INGOT)&&ship.cargo.getItem(27).getCount()==7&&ship.cargo.getItem(1).getCount()==5&&ship.cargo.getItem(0).getCount()==3,"Shift-click deposits into selected page without changing hidden cargo");
            helper.assertTrue(menu.quickMoveStack(captain,0).isEmpty(),"Stale hidden-slot extraction rejected");
            Pirate.state(captain).navalVictories=12;helper.assertTrue(ship.upgradeCargo(captain)&&ship.cargoSlots()==54&&captain.getInventory().getItem(0).isEmpty()&&captain.getInventory().getItem(1).isEmpty(),"Second cargo upgrade consumes thirty-two iron and eight emeralds");ship.cargo.setItem(53,new ItemStack(Items.PRISMARINE_SHARD,9));
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);var restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));
            helper.assertTrue(restored.cargoSlots()==54&&restored.cargo.getItem(0).getCount()==3&&restored.cargo.getItem(27).getCount()==7&&restored.cargo.getItem(53).getCount()==9&&restored.flag()==1&&restored.getName().getString().equals("North Star"),"Native save preserves all cargo pages, flag and name");restored.discard();
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),captain.registryAccess());try {var payload=new ShipIdentityPayload(1,7,"North Star");ShipIdentityPayload.CODEC.encode(buffer,payload);helper.assertTrue(ShipIdentityPayload.CODEC.decode(buffer).equals(payload),"Bounded identity payload round-trips");}finally{buffer.release();}
            captain.teleportTo(pos.getX()+60,65,pos.getZ());helper.assertTrue(!menu.rename(captain,"Remote")&&!menu.clickMenuButton(captain,ShipMenu.CARGO_PAGE),"Remote menu packets cannot rename or change pages");
        } finally {clear(ship);remove(captain);remove(stranger);}helper.succeed();
    }
    @GameTest public void officersResupplyAndShantyKeepSavedCrewAndItems(GameTestHelper helper) {
        var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);
        try {
            helper.assertTrue(!ship.recruitOfficer(captain,0)&&!ship.resupply(captain)&&!ship.toggleShanty(captain),"Uncompleted commissions cannot hire or grant officer abilities");var state=Pirate.state(captain);state.officerContracts=new int[]{3,3,3};
            for(int i=0;i<3;i++)helper.assertTrue(ship.recruitOfficer(captain,i)&&!ship.recruitOfficer(captain,i)&&ship.crew(PirateShip.NAVIGATOR+i)==1,"Each named officer boards once in a separate role");
            captain.getInventory().clearContent();var supply=new ItemStack(Ships.SHOT,20);supply.set(DataComponents.CUSTOM_NAME,Component.literal("Marked ammunition"));captain.getInventory().setItem(0,supply);captain.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,10));
            for(int i=0;i<ship.cargoSlots();i++)ship.cargo.setItem(i,new ItemStack(Items.STONE,64));
            helper.assertTrue(!ship.resupply(captain)&&supply.getCount()==20,"Full hold applies backpressure without item loss");ship.cargo.setItem(0,supply.copyWithCount(60));
            helper.assertTrue(ship.resupply(captain)&&ship.cargo.getItem(0).getCount()==64&&supply.getCount()==16&&captain.getInventory().getItem(1).getCount()==10&&ship.cargo.getItem(27).isEmpty(),"Quartermaster transfers only fitting matching-component supplies into unlocked slots");
            ship.cargo.setItem(1,ItemStack.EMPTY);helper.assertTrue(ship.resupply(captain)&&ship.cargo.getItem(1).getCount()==16&&ship.cargo.getItem(1).get(DataComponents.CUSTOM_NAME).getString().equals("Marked ammunition")&&captain.getInventory().getItem(0).isEmpty(),"Partial supply transfers conserve components and exact counts");
            helper.assertTrue(ship.toggleShanty(captain)&&ship.shantyPlaying()&&ship.shantyAudible(),"Captain starts synchronized shanty eligibility");ship.tick();helper.assertTrue(ship.toggleShanty(captain)&&!ship.shantyPlaying()&&!ship.shantyAudible(),"Captain immediately stops synchronized playback");ship.toggleShanty(captain);
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ship.saveWithoutId(output);
            var restored=(PirateShip)net.minecraft.world.entity.EntityType.loadEntityRecursive(Ships.SHIP,output.buildResult(),captain.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD,e->e);
            helper.assertTrue(restored!=null&&restored.crew(PirateShip.NAVIGATOR)==1&&restored.crew(PirateShip.QUARTERMASTER)==1&&restored.crew(PirateShip.SINGER)==1&&restored.shantyPlaying(),"All three roles and playback choice survive native recursive save/load");
            for(var officer:java.util.List.copyOf(restored.getPassengers())){helper.assertTrue(officer.entityTags().contains(PirateShip.CREW_OWNER+captain.getUUID()),"New officers retain captain loyalty");officer.discard();}restored.discard();
            helper.assertTrue(ship.launch(captain)==null,"Three new officer roles launch as assigned crew");helper.assertTrue(!ship.resupply(captain)&&!ship.rename(captain,"Sailing rename"),"Refit and supply transfers require docking");ship.tickCount=120;ship.tick();
        } finally {clear(ship);remove(captain);}helper.succeed();
    }


    @GameTest public void sinkingRecordsNamedRecoveryAndGhostVictory(GameTestHelper helper){
        var pos=new BlockPos(40,64,40);var captain=player(helper,pos.above());var ship=build(helper,captain,pos);PirateShip ghost=null;
        try {
            var state=Pirate.state(captain);state.officerContracts=new int[]{3,3,3};state.garrickRescue=3;state.sealContract=3;state.seals=1;state.repeatContracts[1]=1;state.ghostVoyage=true;
            for(int i=0;i<3;i++)helper.assertTrue(ship.recruitOfficer(captain,i),"Named crew boards before voyage");helper.assertTrue(ship.recruitGarrick(captain),"Garrick boards before voyage");
            var menu=new ShipMenu(1,captain.getInventory(),null,ship);for(var slot:menu.slots)helper.assertTrue(slot.x>=0&&slot.y>=0&&slot.x+16<=ShipMenu.WIDTH&&slot.y+16<=ShipMenu.HEIGHT,"Every inventory slot stays inside compact screen bounds");
            helper.assertTrue(ship.launch(captain)==null,"Vessel launches");var at=location(helper);for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)captain.level().getChunk((at.getX()>>4)+x,(at.getZ()>>4)+z);ship.setPos(at.getX(),64,at.getZ());captain.teleportTo(at.getX(),65,at.getZ());
            ghost=PirateShip.spawnSpecial(captain.level(),at.offset(30,0,0),1);helper.assertTrue(ghost!=null&&ghost.encounterKind()==1,"Ghost ship spawns using bounded native patrol placement");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,captain.registryAccess());ghost.saveWithoutId(output);var restored=new PirateShip(Ships.SHIP,captain.level());restored.load(TagValueInput.create(ProblemReporter.DISCARDING,captain.registryAccess(),output.buildResult()));helper.assertTrue(restored.encounterKind()==1,"Ghost type survives save");restored.discard();
            for(int n=0;n<20&&!ghost.isRemoved();n++)ghost.cannonDamage(ship,false);
            helper.assertTrue(ghost.isRemoved()&&(state.seals&32)!=0&&!state.ghostVoyage&&state.repeatContracts[1]==1,"Ghost win records Courage without satisfying outlaw hunt");
            var enemy=PirateShip.spawnSpecial(captain.level(),at.offset(-30,0,0),2);helper.assertTrue(enemy!=null&&!enemy.hostileTo(ship),"Hunters leave low-infamy captains alone");
            state.infamy=30;helper.assertTrue(enemy.hostileTo(ship),"Hunters pursue infamous captains");state.infamy=0;
            helper.assertTrue(enemy.cannonDamage(ship,false)&&enemy.hostileTo(ship),"Hunters retaliate when a low-infamy captain attacks them");
            for(int n=0;n<20&&!ship.isRemoved();n++)ship.cannonDamage(enemy,false);enemy.discard();
            helper.assertTrue(ship.isRemoved()&&SeaLedger.get(captain.level()).lost(captain.getUUID())==15,"Sinking creates rescue claims for all four named officers");
            captain.stopRiding();captain.teleportTo(2,65,4);captain.getInventory().clearContent();captain.getInventory().setItem(0,new ItemStack(Items.EMERALD,8));
            helper.assertTrue(SeaServices.execute(captain,7)==SeaServices.Reply.DONE&&state.officerContracts[0]==3,"Sunk officer can be paid for and rehired without repeating commission");
        }finally{if(ghost!=null)clear(ghost);clear(ship);remove(captain);}helper.succeed();
    }
}
