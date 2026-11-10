package darkspawn.black.oneblock;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class AscensionGeneratorGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ascension-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL); GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		var pos = helper.absolutePos(new BlockPos(1, 2, 2)); level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); ready(player);
		return player;
	}
	private static void ready(ServerPlayer player) {
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); player.hasChangedDimension(); player.setOnGround(true);
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

    private static AscensionLedger.Team team(ServerPlayer p) {
        Ascension.create(p,"Test team");var data=AscensionLedger.get(p.level());
        // Legacy Fixture: M3 regression checks retain the original six-phase milestone behavior.
        var saved=AscensionLedger.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data).getOrThrow().getAsJsonObject();
        for(var entry:saved.getAsJsonArray("teams"))if(entry.getAsJsonObject().get("id").equals(net.minecraft.core.UUIDUtil.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data.member(p.getUUID()).id()).getOrThrow()))entry.getAsJsonObject().remove("phases");
        var restored=AscensionLedger.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
        p.level().getServer().getLevel(Ascension.LEVEL).getDataStorage().set(AscensionLedger.TYPE,restored);return restored.member(p.getUUID());
    }
    private static void travel(GameTestHelper h,ServerPlayer p,UUID id) {
        ready(p);h.assertTrue(Ascension.travel(p,id),"Safe Ascension travel succeeds");ready(p);
    }
    private static final java.util.Set<UUID> canceled=new java.util.HashSet<>(),nested=new java.util.HashSet<>();
    static {
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->{
            if(nested.remove(player.getUUID())&&player instanceof ServerPlayer p){
                if(p.gameMode.destroyBlock(pos))throw new AssertionError("Nested break entered a reserved generator");
            }
            return !canceled.contains(player.getUUID());
        });
    }
    @GameTest public void cancellationNestedAndDuplicateAttemptsProduceOnlyOneNativeDrop(GameTestHelper h){
        var p=player(h);var friend=player(h);
        var created=team(p);AscensionLedger.get(p.level()).join(created.id(),friend.getUUID());travel(h,p,created.id());travel(h,friend,created.id());
        // Entity Visibility: Let destination chunk tracking settle before counting native spawned entities.
        h.runAfterDelay(20,()->{try{
            var t=AscensionLedger.get(p.level()).team(created.id());var pos=t.center();canceled.add(p.getUUID());nested.add(p.getUUID());
            h.assertTrue(!p.gameMode.destroyBlock(pos),"Other Fabric listener can cancel native mining");
            h.assertTrue(t.progress().total==0&&t.generator().generation()==0&&t.generator().state()==AscensionGenerator.State.READY,"Cancellation releases reservation without credit");
            h.assertTrue(p.level().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).isEmpty(),"Canceled break emits no loot");
            canceled.remove(p.getUUID());nested.add(p.getUUID());h.assertTrue(p.gameMode.destroyBlock(pos),"Uncancelled break succeeds on same tick");
            h.assertTrue(!friend.gameMode.destroyBlock(pos),"Teammate cannot repeat the generation in the same tick");Ascension.mined(p,pos);
            h.assertTrue(t.progress().total==1&&t.generator().generation()==1&&t.generator().state()==AscensionGenerator.State.READY,"Exactly one credit and one regeneration");
            var drops=p.level().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3));
            h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(Items.DIRT)&&drops.getFirst().getItem().getCount()==1,"Native harvesting drops exactly one represented dirt");
            h.assertTrue(p.level().getBlockState(pos).getValue(OneBlockCore.RESOURCE)==1,"Renewable wood follows first confirmed break");
        }finally{canceled.remove(p.getUUID());nested.remove(p.getUUID());remove(p);remove(friend);}h.succeed();});
    }
    @GameTest public void toolRestrictionsAndCreativeBreaksPreserveNativeHarvestRules(GameTestHelper h){
        var p=player(h);var creative=player(h);
        try{
            var t=team(p);travel(h,p,t.id());t.progress().resource=14;AscensionGenerator.reconcile(p.level(),t);
            h.assertTrue(p.gameMode.destroyBlock(t.center())&&t.progress().total==1,"Confirmed wrong-tool mining counts but obeys native loot rules");
            h.assertTrue(p.level().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(t.center()).inflate(3)).isEmpty(),"Bare hands cannot harvest diamond ore loot");
            var c=team(creative);travel(h,creative,c.id());creative.getAbilities().instabuild=true;
            creative.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
            h.assertTrue(!creative.gameMode.destroyBlock(c.center())&&c.generator().state()==AscensionGenerator.State.READY&&c.generator().generation()==0,"Native tool rejection before Fabric callbacks releases the reservation");
            creative.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(creative.gameMode.destroyBlock(c.center()),"Creative removal restores the core");
            h.assertTrue(c.progress().total==0&&c.progress().resource==0&&c.generator().generation()==1&&c.generator().state()==AscensionGenerator.State.READY,"Creative never increments progression or rolls resources");
            h.assertTrue(creative.level().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(c.center()).inflate(3)).isEmpty(),"Creative emits no resource loot");
        }finally{remove(p);remove(creative);}h.succeed();
    }
    @GameTest public void pendingAndRegeneratingSaveRecoveryNeverReplaysMilestoneRewards(GameTestHelper h){
        var p=player(h);AscensionLedger original=null;
        try{
            var t=team(p);travel(h,p,t.id());var level=p.level();original=AscensionLedger.get(level);var id=t.id();
            t.progress().total=99;t.progress().breaks=99;var attempt=t.generator().begin(p.getUUID(),level.getGameTime(),true);
            var pending=reload(level);var unconfirmed=pending.team(id);level.setBlockAndUpdate(t.center(),Blocks.AIR.defaultBlockState());
            h.assertTrue(AscensionGenerator.reconcile(level,unconfirmed)&&unconfirmed.progress().total==99&&unconfirmed.generator().generation()==0,"Interrupted unconfirmed removal restores old resource with zero credit");
            level.getDataStorage().set(AscensionLedger.TYPE,original);t.generator().removed(p.getUUID());
            h.assertTrue(t.generator().finish(attempt,t.progress(),()->14),"Fixture commits generation 100 before its world write");
            var committed=reload(level).team(id);level.setBlockAndUpdate(t.center(),Blocks.AIR.defaultBlockState());
            for(int i=0;i<3;i++)h.assertTrue(AscensionGenerator.reconcile(level,committed),"Repeated recovery is idempotent");
            h.assertTrue(committed.progress().total==100&&committed.generator().generation()==1&&level.getBlockState(t.center()).getValue(OneBlockCore.RESOURCE)==14,"Reload repairs the saved resource without a new generation");
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)h.assertTrue(!level.getBlockState(t.center().offset(x,1,z)).is(Blocks.CHEST),"Recovery cannot replay milestone chest");
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(t.center()).inflate(8)).isEmpty(),"Recovery cannot replay milestone creature");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(t.center()).inflate(3)).isEmpty(),"Recovery emits no mining drops");
        }finally{if(original!=null)p.level().getDataStorage().set(AscensionLedger.TYPE,original);remove(p);}h.succeed();
    }
    @GameTest public void mismatchedResourcesAndErrorStatesRepairButLockedAndArchivedDoNot(GameTestHelper h){
        var p=player(h);
        try{
            var t=team(p);travel(h,p,t.id());var level=p.level();t.progress().resource=5;
            h.assertTrue(!p.gameMode.destroyBlock(t.center())&&t.progress().total==0,"Stale world resource cannot be harvested");
            h.assertTrue(AscensionGenerator.reconcile(level,t)&&level.getBlockState(t.center()).getValue(OneBlockCore.RESOURCE)==5,"READY mismatch repaired to saved resource");
            t.generator().repaired(false);h.assertTrue(!p.gameMode.destroyBlock(t.center()),"ERROR blocks harvesting until reconciliation");
            h.assertTrue(AscensionGenerator.reconcile(level,t)&&t.generator().state()==AscensionGenerator.State.READY,"Recoverable write error retries safely");
            t.generator().lock();level.setBlockAndUpdate(t.center(),Blocks.AIR.defaultBlockState());
            h.assertTrue(!AscensionGenerator.reconcile(level,t)&&level.isEmptyBlock(t.center())&&!p.gameMode.destroyBlock(t.center()),"LOCKED never regenerates or mines");
            AscensionLedger.get(level).archive(p.getUUID());h.assertTrue(!Ascension.ensureIsland(level,AscensionLedger.get(level).at(t.center())),"Archived island stays frozen");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void recoveryDoesNotLoadIdleOrAdjacentChunks(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);var owner=UUID.randomUUID();var progress=new OneBlockProgress();progress.initialized=true;
        // Distant cell lies on a chunk boundary; only its core chunk is explicitly loaded by the fixture.
        var distant=new AscensionLedger.Team(UUID.randomUUID(),owner,"Distant",103040,java.util.List.of(owner),progress,false,new AscensionGenerator(0),null);
        var pos=distant.center();h.assertTrue(!level.hasChunkAt(pos),"Fixture starts unloaded");
        h.assertTrue(!AscensionGenerator.reconcile(level,distant)&&!level.hasChunkAt(pos),"Repair never loads an idle core chunk");
        level.getChunkAt(pos);var neighbor=new BlockPos((pos.getX()>>4)*16-1,80,pos.getZ());
        h.assertTrue(!level.hasChunkAt(neighbor),"Adjacent chunk starts unloaded");
        h.assertTrue(AscensionGenerator.reconcile(level,distant)&&!level.hasChunkAt(neighbor),"Repair suppresses neighbor loads");h.succeed();
    }
    @GameTest public void liveMilestoneRewardsOccurOnceAndRecoveryDoesNotRepeatThem(GameTestHelper h){
        var p=player(h);
        var created=team(p);travel(h,p,created.id());
        // Entity Visibility: Native drops and animals are queryable after destination tracking settles.
        h.runAfterDelay(20,()->{try{
            var t=AscensionLedger.get(p.level()).team(created.id());var level=p.level();t.progress().total=99;t.progress().breaks=99;
            h.assertTrue(p.gameMode.destroyBlock(t.center())&&t.progress().total==100,"Live milestone commits normally");
            for(int i=0;i<3;i++){level.setBlockAndUpdate(t.center(),Blocks.AIR.defaultBlockState());AscensionGenerator.reconcile(level,t);}
            int chests=0;for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(level.getBlockState(t.center().offset(x,1,z)).is(Blocks.CHEST))chests++;
            h.assertTrue(chests==1&&level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(t.center()).inflate(8)).size()==1,"One supply chest and one creature, with no recovery replay");
            h.assertTrue(t.progress().total==100&&t.generator().generation()==1&&level.getBlockState(t.center()).getValue(OneBlockCore.RESOURCE)==0,"Recovery keeps committed essentials and counters");
        }finally{remove(p);}h.succeed();});
    }
    private static AscensionLedger reload(net.minecraft.server.level.ServerLevel level){
        var restored=AscensionLedger.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,AscensionLedger.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,AscensionLedger.get(level)).getOrThrow()).getOrThrow();
        level.getDataStorage().set(AscensionLedger.TYPE,restored);return restored;
    }
}
