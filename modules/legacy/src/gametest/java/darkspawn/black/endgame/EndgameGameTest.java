package darkspawn.black.endgame;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.boss.*;
import darkspawn.black.ecosystem.*;
import darkspawn.black.health.BossHearts;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class EndgameGameTest {
    @GameTest public void nexusRoomUpgradePreservesObstructionsBeforePayment(GameTestHelper h){
        var p=player(h);var server=p.level().getServer();var storage=server.overworld().getDataStorage();
        var previous=WorldAwakening.get(p.level());var oldRun=Nexus.run(p.level());var level=server.getLevel(Nexus.LEVEL);
        var obstacle=new BlockPos(-16,65,20);var probe=new BlockPos(-16,64,16);
        var oldObstacle=level.getBlockState(obstacle);var oldProbe=level.getBlockState(probe);
        try{
            var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
            for(var kind:BossKind.values())data.unlock(kind,1);BossHearts.progress(p).restore(Arrays.stream(BossKind.values()).map(BossKind::id).toList());
            var saved=new com.google.gson.JsonObject();saved.addProperty("built",true);
            storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow());
            p.getInventory().setItem(1,new ItemStack(Items.NETHER_STAR,2));
            level.setBlock(obstacle,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);level.setBlock(probe,Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(!Nexus.start(p)&&p.getInventory().countItem(Items.NETHER_STAR)==2,"Legacy room obstruction rejects entry before payment");
            h.assertTrue(level.getBlockState(obstacle).is(Blocks.DIAMOND_BLOCK)&&level.getBlockState(probe).isAir(),"Upgrade preserves obstruction and performs no partial room writes");
            h.assertTrue(Nexus.run(level).stage()==0&&!Nexus.run(level).awaiting(),"Legacy save defaults do not fabricate a pending run");
        }finally{
            level.setBlock(obstacle,oldObstacle,2);level.setBlock(probe,oldProbe,2);
            remove(p);storage.set(WorldAwakening.TYPE,previous);storage.set(Nexus.Run.TYPE,oldRun);
        }h.succeed();
    }
    @GameTest public void nexusPreparationTimeoutCleansEnemiesButPreservesEquipment(GameTestHelper h){
        var p=player(h);var server=p.level().getServer();var storage=server.overworld().getDataStorage();var oldRun=Nexus.run(p.level());var level=server.getLevel(Nexus.LEVEL);
        var saved=new com.google.gson.JsonObject();saved.addProperty("stage",1);saved.addProperty("deadline",0);saved.addProperty("built",true);saved.addProperty("awaiting",true);
        storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow());
        // Fixture Chunks: Immediate cleanup only visits tracked entities; unloaded enemies use the load hook.
        boolean mobForced=level.setChunkForced(0,-1,true),equipmentForced=level.setChunkForced(0,1,true);
        level.getChunkSource().tick(() -> true,false);
        for(int tasks=0;tasks<10000&&level.getChunkSource().pollTask();tasks++){ }
        var equipment=new net.minecraft.world.entity.item.ItemEntity(level,0,64,24,new ItemStack(Items.DIAMOND));level.addFreshEntity(equipment);
        var mob=RegionalEcosystems.MOBS.get(RegionalKind.VOIDLING).create(level,EntitySpawnReason.EVENT);mob.setPos(0,64,-12);mob.addTag(Nexus.TAG);level.addFreshEntity(mob);
        try{
            h.assertTrue(level.getEntity(mob.getUUID())==mob,"Fixture enemy is tracked before timeout");
            p.teleport(new net.minecraft.world.level.portal.TeleportTransition(level,new Vec3(0,64,24),Vec3.ZERO,0,0,net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET));
            Nexus.advance(server);
            h.assertTrue(Nexus.run(level).stage()==7&&!Nexus.run(level).awaiting(),"Expired preparation ends");
            h.assertTrue(mob.isRemoved(),"Expired preparation clears tracked enemies");
            h.assertTrue(!equipment.isRemoved(),"Failed run preserves dropped equipment");
            var encoded=Nexus.Run.CODEC.encodeStart(JsonOps.INSTANCE,Nexus.run(level)).getOrThrow();storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
            Nexus.advance(server);
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-36,60,-36,36,100,36),e->e.getItem().is(LegendaryRelics.CORE)).isEmpty(),"Reloaded failed run awards no victory core");
        }finally{
            equipment.discard();mob.discard();remove(p);storage.set(Nexus.Run.TYPE,oldRun);
            if(mobForced)level.setChunkForced(0,-1,false);if(equipmentForced)level.setChunkForced(0,1,false);
        }h.succeed();
    }
    @GameTest public void polishNexusRejectsHazardousArrivalWithoutPayment(GameTestHelper h){
        var p=player(h);var server=p.level().getServer();var storage=server.overworld().getDataStorage();var previous=WorldAwakening.get(p.level());var oldRun=Nexus.run(p.level());var level=server.getLevel(Nexus.LEVEL);
        var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
        var saved=new com.google.gson.JsonObject();saved.addProperty("built",true);storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow());
        try{
            for(var kind:BossKind.values())data.unlock(kind,1);BossHearts.progress(p).restore(Arrays.stream(BossKind.values()).map(BossKind::id).toList());
            p.getInventory().setItem(0,new ItemStack(Nexus.KEY));p.getInventory().setItem(1,new ItemStack(Items.NETHER_STAR,2));
            for(int x=-12;x<=12;x++)for(int z=22;z<=26;z++)for(int y=63;y<=66;y++)level.setBlock(new BlockPos(x,y,z),(y==63?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            var pad=new BlockPos(-10,64,24);level.setBlock(pad,Blocks.LAVA.defaultBlockState(),2);
            h.assertTrue(!Nexus.start(p)&&p.level()!=level&&p.getInventory().countItem(Items.NETHER_STAR)==2&&Nexus.run(level).stage()==0,"Lava arrival must reject entry without payment or starting a run");
            level.setBlock(pad,Blocks.AIR.defaultBlockState(),2);level.setBlock(pad.below(),Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(!Nexus.start(p)&&p.getInventory().countItem(Items.NETHER_STAR)==2,"Missing arrival floor also rejects entry without payment");
        }finally{
            var cleanup=new ArrayList<Entity>();level.getAllEntities().forEach(cleanup::add);for(var entity:cleanup)if(entity.entityTags().contains(Nexus.TAG))entity.discard();
            remove(p);storage.set(WorldAwakening.TYPE,previous);storage.set(Nexus.Run.TYPE,oldRun);
            for(int x=-12;x<=12;x++)for(int z=22;z<=26;z++)for(int y=63;y<=66;y++)level.setBlock(new BlockPos(x,y,z),(y==63?Blocks.DEEPSLATE_BRICKS:Blocks.AIR).defaultBlockState(),2);
        }h.succeed();
    }
	private static ServerPlayer player(GameTestHelper helper) {
		return player(helper,UUID.randomUUID());
	}
	private static ServerPlayer player(GameTestHelper helper,UUID id) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "nexus-"+id.toString().substring(0,6)), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 2)));
		player.teleportTo(pos.x, pos.y, pos.z);
		// Mock Connection: Acknowledge loading so vanilla permits actual damage in interruption tests.
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.setOnGround(true);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }


    @GameTest public void sharedUnlocksGateTreeGrowthAndPersist(GameTestHelper h){
        var level=h.getLevel();var storage=level.getServer().overworld().getDataStorage();var previous=WorldAwakening.get(level);
        var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
        var pos=h.absolutePos(new BlockPos(0,10,0));
        try{
            for(var p:BlockPos.betweenClosed(pos.offset(-2,0,-2),pos.offset(2,6,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,HeartwoodGrove.SEEDLING.defaultBlockState(),3);
            ((HeartwoodGrove)HeartwoodGrove.SEEDLING).advanceTree(level,pos,level.getBlockState(pos),level.getRandom());h.assertTrue(level.getBlockState(pos).is(HeartwoodGrove.SEEDLING),"No pre-victory growth");
            data.unlock(BossKind.ANCIENT_TREE_SPIRIT,1);level.setBlock(pos.above(3),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
            ((HeartwoodGrove)HeartwoodGrove.SEEDLING).advanceTree(level,pos,level.getBlockState(pos),level.getRandom());h.assertTrue(level.getBlockState(pos).is(HeartwoodGrove.SEEDLING)&&level.getBlockState(pos.above(3)).is(Blocks.DIAMOND_BLOCK),"Growth preserves all obstructions atomically");
            level.setBlock(pos.above(3),Blocks.AIR.defaultBlockState(),3);((HeartwoodGrove)HeartwoodGrove.SEEDLING).advanceTree(level,pos,level.getBlockState(pos),level.getRandom());h.assertTrue(level.getBlockState(pos).is(HeartwoodGrove.LOG),"Unlocked seedling grows in clear space");
            var encoded=WorldAwakening.TYPE.codec().encodeStart(JsonOps.INSTANCE,data).getOrThrow();h.assertTrue(WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,encoded).getOrThrow().defeated(BossKind.ANCIENT_TREE_SPIRIT),"Shared victory persists");
        }finally{storage.set(WorldAwakening.TYPE,previous);}h.succeed();
    }
    @GameTest public void awakenedHarvestPaysContainerOnceAndPersistsCooldown(GameTestHelper h){
        var p=player(h);var storage=p.level().getServer().overworld().getDataStorage();var previous=WorldAwakening.get(p.level());
        var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
        try{
            p.setShiftKeyDown(true);p.getInventory().setItem(0,new ItemStack(Items.BRUSH));var pos=p.blockPosition().east();p.level().setBlock(pos.below(),Blocks.MYCELIUM.defaultBlockState(),3);p.level().setBlock(pos,Blocks.RED_MUSHROOM.defaultBlockState(),3);
            data.unlock(BossKind.MYCELIAL_SOVEREIGN,1);h.assertTrue(!WorldAwakening.harvest(p,pos),"Cream requires an empty bowl");
            p.getInventory().setItem(1,new ItemStack(Items.BOWL,2));h.assertTrue(WorldAwakening.harvest(p,pos)&&p.getInventory().countItem(Items.BOWL)==1&&p.getInventory().countItem(darkspawn.black.cooking.AnimalUtility.FUNGAL_CREAM)==1,"Exactly one bowl becomes cream");
            var saved=WorldAwakening.TYPE.codec().encodeStart(JsonOps.INSTANCE,data).getOrThrow();storage.set(WorldAwakening.TYPE,WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,saved).getOrThrow());
            h.assertTrue(!WorldAwakening.harvest(p,pos)&&p.getInventory().countItem(Items.BOWL)==1&&p.level().getBlockState(pos).is(Blocks.RED_MUSHROOM),"Saved cooldown blocks repeat payment and preserves the mushroom");
        }finally{remove(p);storage.set(WorldAwakening.TYPE,previous);}h.succeed();
    }
    @GameTest public void direWolfTamingRequiresVictoryAndKeepsOwner(GameTestHelper h){
        var p=player(h);var storage=p.level().getServer().overworld().getDataStorage();var previous=WorldAwakening.get(p.level());
        var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
        var wolf=TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE).create(p.level(),EntitySpawnReason.COMMAND);wolf.setPos(p.position());p.level().addFreshEntity(wolf);
        try{
            p.getInventory().setItem(0,new ItemStack(Items.COOKED_BEEF,8));h.assertTrue(!wolf.tame(p)&&p.getMainHandItem().getCount()==8,"Pre-victory taming does not consume food");
            data.unlock(BossKind.MUTANT_WOLF,1);h.assertTrue(wolf.tame(p)&&p.getMainHandItem().getCount()==4,"Shared victory unlocks exact four-beef payment");
            h.assertTrue(!darkspawn.black.bonfire.BonfireWards.ordinaryHostile(wolf),"Tamed dire wolf is welcome inside bonfire wards");
            wolf.setTarget(p);h.assertTrue(wolf.getTarget()==null,"Companion cannot target its owner");
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess());wolf.saveWithoutId(out);
            var copy=TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE).create(p.level(),EntitySpawnReason.COMMAND);copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess(),out.buildResult()));
            h.assertTrue(p.getUUID().equals(copy.companionOwner()),"Owner survives entity save");copy.discard();
        }finally{wolf.discard();remove(p);storage.set(WorldAwakening.TYPE,previous);}h.succeed();
    }
    @GameTest public void legendaryBowAndLanternUseNativeCosts(GameTestHelper h){
        var p=player(h);try{
            p.getInventory().setItem(0,new ItemStack(LegendaryRelics.VOIDBOW));p.getInventory().setItem(1,new ItemStack(Items.ARROW,3));
            var bow=p.getMainHandItem();bow.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);bow.getItem().releaseUsing(bow,p.level(),p,71980);
            var arrows=p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,p.getBoundingBox().inflate(8),a->a.getOwner()==p);
            h.assertTrue(arrows.size()==1&&arrows.getFirst().isNoGravity()&&p.getInventory().countItem(Items.ARROW)==2&&bow.getDamageValue()>0,"Flat-flight bow retains native ammunition and durability costs");
            var arrow=arrows.getFirst();for(var tag:List.copyOf(arrow.entityTags()))if(tag.startsWith("darkspawn_void_until_"))arrow.removeTag(tag);arrow.addTag("darkspawn_void_until_0");arrow.tick();h.assertTrue(!arrow.isNoGravity(),"Expired void flight returns to normal gravity instead of hovering forever");arrows.forEach(Entity::discard);
            p.stopUsingItem();p.getInventory().setItem(0,new ItemStack(LegendaryRelics.LANTERN));
            LegendaryRelics.LANTERN.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(p.getCooldowns().isOnCooldown(p.getMainHandItem())&&p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"Lantern grants temporary sight with a cooldown");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void nexusPartyGateRewardsAndRecoveryAreBounded(GameTestHelper h){
        var p=player(h);var friend=player(h);var server=p.level().getServer();var storage=server.overworld().getDataStorage();var previous=WorldAwakening.get(p.level());var oldRun=Nexus.run(p.level());
        var data=WorldAwakening.TYPE.codec().parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow();storage.set(WorldAwakening.TYPE,data);
        storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow());
        var level=server.getLevel(Nexus.LEVEL);
        try{
            h.assertTrue(!Nexus.start(p),"Unqualified survival entry refused");
            for(var kind:BossKind.values())data.unlock(kind,1);BossHearts.progress(p).restore(Arrays.stream(BossKind.values()).map(BossKind::id).toList());
            p.getInventory().setItem(0,new ItemStack(Nexus.KEY));p.getInventory().setItem(1,new ItemStack(Items.NETHER_STAR,2));friend.getInventory().setItem(0,new ItemStack(Nexus.KEY));
            var origin=p.blockPosition();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)p.level().setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
            h.assertTrue(Nexus.start(p)&&p.level()==level&&friend.level()==level,"Qualified captain brings an opted-in unqualified friend");h.assertTrue(p.getInventory().countItem(Items.NETHER_STAR)==1,"Exactly one star charged for group");
            h.assertTrue(Nexus.run(level).awaiting()&&!Nexus.begin(p),"Entry hall does not begin combat before the party reaches the chamber");
            h.assertTrue(level.getBlockState(new BlockPos(0,72,24)).is(Blocks.DEEPSLATE_TILES)&&level.getBlockState(new BlockPos(0,64,16)).isAir(),"Sheltered approach has a clear doorway");
            Nexus.advance(server);h.assertTrue(Nexus.run(level).stage()==1&&Nexus.run(level).awaiting(),"Empty preparation stage cannot award or skip waves");
            var save=Nexus.Run.CODEC.encodeStart(JsonOps.INSTANCE,Nexus.run(level)).getOrThrow();storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,save).getOrThrow());
            h.assertTrue(Nexus.run(level).stage()==1&&Nexus.run(level).awaiting(),"Preparation survives codec reload");
            p.teleportTo(-2,64,4);
            var friendId=friend.getUUID();remove(friend);
            h.assertTrue(!Nexus.begin(p),"Disconnected party member cannot be silently omitted from preparation");
            friend=player(h,friendId);
            h.assertTrue(Nexus.recover(friend),"Reconnected entrant can recover into the saved preparation stage");
            friend.teleportTo(2,64,4);
            h.assertTrue(Nexus.begin(p)&&!Nexus.begin(friend),"Gathered party starts exactly one first wave");
            save=Nexus.Run.CODEC.encodeStart(JsonOps.INSTANCE,Nexus.run(level)).getOrThrow();storage.set(Nexus.Run.TYPE,Nexus.Run.CODEC.parse(JsonOps.INSTANCE,save).getOrThrow());
            h.assertTrue(!Nexus.run(level).awaiting(),"Active combat survives codec reload without rearming preparation");
            for(int wave=1;wave<=6;wave++){
                var enemies=new ArrayList<LivingEntity>();for(var entity:level.getAllEntities())if(entity instanceof LivingEntity mob&&mob.isAlive()&&mob.entityTags().contains(Nexus.TAG))enemies.add(mob);
                h.assertTrue(!enemies.isEmpty(),"Wave contains actual entities");
                for(var mob:enemies)mob.die(level.damageSources().playerAttack(p));Nexus.advance(server);
            }
            h.assertTrue(Nexus.run(level).stage()==7,"Five waves and Warden complete");
            long rewards=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-36,60,-36,36,100,36),e->e.getItem().is(LegendaryRelics.CORE)).stream().mapToInt(e->e.getItem().getCount()).sum();Nexus.advance(server);
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-36,60,-36,36,100,36),e->e.getItem().is(LegendaryRelics.CORE)).stream().mapToInt(e->e.getItem().getCount()).sum()==rewards,"Repeated completion tick cannot duplicate cores");
            h.assertTrue(rewards==2,"One core for each survivor");h.assertTrue(Nexus.leave(p)&&Nexus.leave(friend),"Both return safely");Nexus.advance(server);
            h.assertTrue(Nexus.run(server.overworld()).stage()==0&&Nexus.recover(friend),"Unqualified friend can retrieve equipment after the run");h.assertTrue(Nexus.leave(friend),"Recovery has a safe exit");
        }finally{
            if(level!=null){var cleanup=new ArrayList<Entity>();level.getAllEntities().forEach(cleanup::add);for(var entity:cleanup)if(entity.entityTags().contains(Nexus.TAG)||entity instanceof net.minecraft.world.entity.item.ItemEntity)entity.discard();}
            remove(friend);remove(p);storage.set(WorldAwakening.TYPE,previous);storage.set(Nexus.Run.TYPE,oldRun);
        }h.succeed();
    }
}
