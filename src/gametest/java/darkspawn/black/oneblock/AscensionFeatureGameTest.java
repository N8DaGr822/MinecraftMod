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

public final class AscensionFeatureGameTest {
    private static final java.util.Map<ServerPlayer,String> messages=new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerPlayer,java.util.List<net.minecraft.world.level.ChunkPos>> forcedChunks=new java.util.HashMap<>();
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ascension-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component message){messages.put(this,message.getString());super.sendSystemMessage(message);}
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
	private static void remove(ServerPlayer player) {
        var server=player.level().getServer();server.getPlayerList().remove(player);player.discard();messages.remove(player);
        var forced=forcedChunks.remove(player);
        if(forced!=null)for(var chunk:forced)server.getLevel(Ascension.LEVEL).setChunkForced(chunk.x(),chunk.z(),false);
    }

    private static AscensionLedger.Team team(ServerPlayer p) {
        Ascension.create(p,"Test team");return AscensionLedger.get(p.level()).member(p.getUUID());
    }
    private static void travel(GameTestHelper h,ServerPlayer p,UUID id) {
        ready(p);h.assertTrue(Ascension.travel(p,id),"Safe Ascension travel succeeds");ready(p);
    }
    private static AscensionLedger.Team setup(GameTestHelper h,ServerPlayer p){
        var t=team(p);travel(h,p,t.id());
        // Fixture Chunks: Guardian and collector queries require tracked entities, not just loaded block data.
        var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();forcedChunks.put(p,forced);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var chunk=new net.minecraft.world.level.ChunkPos((t.center().getX()>>4)+x,(t.center().getZ()>>4)+z);
            if(p.level().setChunkForced(chunk.x(),chunk.z(),true))forced.add(chunk);
        }
        p.level().getChunkSource().tick(() -> true,false);
        for(int tasks=0;tasks<10000&&p.level().getChunkSource().pollTask();tasks++){ }
        for(int x:new int[]{-6,6})for(int z:new int[]{-6,6})p.level().getChunkAt(t.center().offset(x,0,z));
        var genesis=AscensionPhases.get(p.level()).phase("genesis");t.progress().breaks=genesis.breaks();
        for(var o:genesis.objectives())t.phases().submit(genesis,o.id(),o.item(),o.count());
        h.assertTrue(t.phases().advance(genesis,t.progress()),"Fixture completes Genesis");
        var caverns=AscensionPhases.get(p.level()).phase("caverns");t.progress().breaks=caverns.breaks();
        for(var o:caverns.objectives())t.phases().submit(caverns,o.id(),o.item(),o.count());
        p.level().setBlockAndUpdate(AscensionAutomation.output(t),Blocks.CHEST.defaultBlockState());
        return t;
    }
    private static void equip(ServerPlayer p,net.minecraft.world.item.Item item,int count){p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item,count));}
    private static void machine(AscensionLedger.Team t){t.phases().recordEncounter("cave_guardian");var f=t.phases().features();f.collector=true;f.miner=true;f.enabled=true;f.mineAfter=0;}
    @GameTest public void guardianRequiresPreparedTeamAndCreditsOnlyEligiblePlayerDeath(GameTestHelper h){
        var p=player(h);var visitor=player(h);
        try{
            var t=setup(h,p);team(visitor);travel(h,visitor,t.id());t.progress().breaks=499;AscensionGuardian.start(p);
            h.assertTrue(t.phases().features().guardian==null,"Unfinished threshold cannot summon");t.progress().breaks=500;
            AscensionGuardian.start(p);var id=t.phases().features().guardian;
            h.assertTrue(id!=null,"Prepared team summons Guardian");var mob=(net.minecraft.world.entity.Mob)p.level().getEntity(id);
            AscensionGuardian.start(p);h.assertTrue(id.equals(t.phases().features().guardian),"Duplicate start preserves actor");
            h.assertTrue(!mob.hurtServer(p.level(),visitor.damageSources().playerAttack(visitor),1000),"Visitor cannot damage Guardian");
            h.assertTrue(!mob.hurtServer(p.level(),mob.damageSources().fall(),1000),"Environmental damage cannot bypass encounter");
            mob.setTarget(visitor);h.assertTrue(mob.getTarget()==null&&!mob.canAttack(visitor)&&!AscensionGuardian.disallows(mob,p),"Native targeting rejects visitors");
            h.assertTrue(mob.hurtServer(p.level(),p.damageSources().playerAttack(p),1000),"Eligible player can defeat Guardian");
            h.assertTrue(t.phases().encountered("cave_guardian")&&t.phases().features().guardian==null,"Death clears actor and records victory");
            AscensionGuardian.start(p);h.assertTrue(t.phases().features().guardian==null,"Completed encounter cannot replay");
            ready(p);Ascension.advance(p);h.assertTrue(t.phases().phase().equals("verdant_wilds"),"Real victory unlocks advancement");
        }finally{remove(p);remove(visitor);AscensionGuardian.tick();}h.succeed();
    }
    @GameTest public void guardianExpiryAndAbsentTeamCancelWithoutVictory(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);AscensionGuardian.start(p);var f=t.phases().features();var id=f.guardian;
            h.assertTrue(id!=null,"Guardian spawned");f.expires=p.level().getGameTime();AscensionGuardian.tick();
            h.assertTrue(f.guardian==null&&!t.phases().encountered("cave_guardian"),"Expiry grants nothing");
            AscensionGuardian.start(p);h.assertTrue(f.guardian!=null,"Canceled encounter can retry");
            p.teleportTo(t.center().getX()+40,81,t.center().getZ());AscensionGuardian.tick();
            h.assertTrue(f.guardian==null&&!t.phases().encountered("cave_guardian"),"Absent teammates cancel without victory");
        }finally{remove(p);AscensionGuardian.tick();}h.succeed();
    }
    @GameTest public void upgradesConsumeExactOwnerCostAndSurviveSaveReload(GameTestHelper h){
        var p=player(h);var visitor=player(h);
        try{
            var t=setup(h,p);team(visitor);travel(h,visitor,t.id());equip(visitor,Items.IRON_INGOT,64);AscensionAutomation.buy(visitor,false);
            h.assertTrue(!t.phases().features().collector&&visitor.getMainHandItem().getCount()==64,"Visitor cannot purchase");
            equip(p,Items.IRON_INGOT,64);AscensionAutomation.buy(p,false);AscensionAutomation.buy(p,false);
            h.assertTrue(t.phases().features().collector&&p.getMainHandItem().getCount()==48,"Collector costs exactly 16 once");
            AscensionAutomation.buy(p,true);h.assertTrue(!t.phases().features().miner&&p.getMainHandItem().getCount()==48,"Guardian gates miner");
            t.phases().recordEncounter("cave_guardian");AscensionAutomation.buy(p,true);
            h.assertTrue(t.phases().features().miner&&p.getMainHandItem().getCount()==16,"Miner costs 32");
            AscensionAutomation.toggle(visitor,false);h.assertTrue(t.phases().features().enabled,"Visitor cannot disable machine");
            AscensionAutomation.toggle(p,false);var saved=AscensionPhaseProgress.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,t.phases()).getOrThrow();
            var restored=AscensionPhaseProgress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
            h.assertTrue(restored.features().collector&&restored.features().miner&&!restored.features().enabled&&restored.features().mineAfter==t.phases().features().mineAfter,"Upgrade state and timer persist");
        }finally{remove(p);remove(visitor);}h.succeed();
    }
    @GameTest public void collectorRespectsPickupDelayOutputCapacityAndReservedToolSlot(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);var f=t.phases().features();f.collector=true;f.enabled=true;var chest=AscensionAutomation.chest(p.level(),t);
            var item=new ItemEntity(p.level(),t.center().getX()+.5,82,t.center().getZ()+.5,new ItemStack(Items.DIRT,4));item.setNoPickUpDelay();p.level().addFreshEntity(item);
            AscensionAutomation.tick(p.level(),t);h.assertTrue(item.isRemoved()&&chest.getItem(0).isEmpty()&&chest.getItem(1).getCount()==4,"Collector fills output, never tool slot");
            var delayed=new ItemEntity(p.level(),t.center().getX()+.5,82,t.center().getZ()+.5,new ItemStack(Items.DIAMOND));delayed.setDefaultPickUpDelay();p.level().addFreshEntity(delayed);
            AscensionAutomation.tick(p.level(),t);h.assertTrue(!delayed.isRemoved(),"Pickup delay respected");delayed.setNoPickUpDelay();
            for(int i=1;i<27;i++)chest.setItem(i,new ItemStack(Items.DIRT,64));AscensionAutomation.tick(p.level(),t);
            h.assertTrue(!delayed.isRemoved()&&chest.getItem(0).isEmpty(),"Full output preserves entity");delayed.discard();
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void minerUsesNativeLootDurabilityAndSharedExactlyOnceGenerator(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);machine(t);var chest=AscensionAutomation.chest(p.level(),t);chest.setItem(0,new ItemStack(Items.IRON_PICKAXE));long total=t.progress().total;
            AscensionAutomation.tick(p.level(),t);
            h.assertTrue(t.progress().total==total+1&&t.generator().generation()==1,"Auto-miner commits one generation");
            h.assertTrue(chest.getItem(1).is(Items.DIRT)&&chest.getItem(1).getCount()==1&&chest.getItem(0).getDamageValue()==1,"Native dirt loot and tool durability");
            h.assertTrue(p.level().getBlockState(t.center()).is(OneBlock.CORE)&&t.progress().resource==1,"Core regenerated with next resource");
            h.assertTrue(!p.gameMode.destroyBlock(t.center())&&t.progress().total==total+1,"Manual mining cannot duplicate same-tick commit");
            AscensionAutomation.tick(p.level(),t);h.assertTrue(t.progress().total==total+1,"Saved cooldown prevents repeat operation");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void minerPausesForFullChestMissingToolAbsentMemberAndDisabledState(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);machine(t);var chest=AscensionAutomation.chest(p.level(),t);long total=t.progress().total;
            h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest),"Missing tool pauses");chest.setItem(0,new ItemStack(Items.STONE_PICKAXE));
            h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest),"Wrong tool pauses");chest.setItem(0,new ItemStack(Items.IRON_PICKAXE));
            for(int i=1;i<27;i++)chest.setItem(i,new ItemStack(Items.DIRT,64));h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest),"Full output pauses before generation");chest.setItem(1,ItemStack.EMPTY);
            t.phases().features().enabled=false;h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest),"Disabled machine pauses");t.phases().features().enabled=true;
            p.teleportTo(t.center().getX()+20,81,t.center().getZ());h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest),"No nearby teammate pauses");
            h.assertTrue(t.progress().total==total&&t.generator().generation()==0&&chest.getItem(0).getDamageValue()==0,"Paused attempts mutate neither progress nor tool");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void interruptedGuardiansAndOrphanActorsNeverGrantVictory(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);AscensionGuardian.start(p);
            var f=t.phases().features();var mob=(net.minecraft.world.entity.Mob)p.level().getEntity(f.guardian);
            h.assertTrue(mob!=null,"Live Guardian exists before recovery: "+messages.get(p));AscensionGuardian.recover(p.level().getServer());
            h.assertTrue(mob.isRemoved()&&f.guardian==null&&!t.phases().encountered("cave_guardian"),"Restart reconciliation cancels actor without credit");
            AscensionGuardian.start(p);mob=(net.minecraft.world.entity.Mob)p.level().getEntity(f.guardian);h.assertTrue(mob!=null,"Retry succeeds");
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.invoker().onUnload(mob,p.level());
            h.assertTrue(f.guardian==null&&!t.phases().encountered("cave_guardian"),"Chunk unload cancels binding without credit");
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(mob,p.level());h.assertTrue(mob.isRemoved(),"Unbound actor cannot resume or award loot");
            var ordinary=net.minecraft.world.entity.EntityTypes.IRON_GOLEM.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.EVENT);
            ordinary.setTarget(p);h.assertTrue(ordinary.getTarget()==p&&!AscensionGuardian.disallows(ordinary,p),"Ordinary mobs retain native targeting");ordinary.discard();
        }finally{remove(p);AscensionGuardian.tick();}h.succeed();
    }
    @GameTest public void collectorWorkIsBoundedAndReplacementChestMustRemainSingle(GameTestHelper h){
        var p=player(h);
        var created=setup(h,p);
        // Entity Visibility: Query limits require a tracked destination, not merely loaded block data.
        h.runAfterDelay(20,()->{try{
            var t=AscensionLedger.get(p.level()).team(created.id());var f=t.phases().features();f.collector=true;f.enabled=true;var items=new java.util.ArrayList<ItemEntity>();
            for(int i=0;i<12;i++){var item=new ItemEntity(p.level(),t.center().getX()+.5,82,t.center().getZ()+.5,new ItemStack(Items.DIRT));item.setNoPickUpDelay();items.add(item);p.level().addFreshEntity(item);}
            AscensionAutomation.tick(p.level(),t);h.assertTrue(items.stream().filter(ItemEntity::isRemoved).count()==8,"One pass removes eight of twelve visible items; removed="+items.stream().filter(ItemEntity::isRemoved).count()+", visible="+p.level().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(t.center()).inflate(3)).size());
            f.enabled=false;AscensionAutomation.tick(p.level(),t);h.assertTrue(items.stream().filter(ItemEntity::isRemoved).count()==8,"Disabled collector leaves remaining items");f.enabled=true;
            var pos=AscensionAutomation.output(t);p.level().setBlock(pos,Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT),18);
            AscensionAutomation.tick(p.level(),t);h.assertTrue(items.stream().filter(ItemEntity::isRemoved).count()==8,"Double chest cannot become automation output");items.forEach(ItemEntity::discard);
        }finally{remove(p);}h.succeed();});
    }

    @GameTest public void finalToolDurabilityStillDeliversNativeStoneLoot(GameTestHelper h){
        var p=player(h);
        try{
            var t=setup(h,p);machine(t);var chest=AscensionAutomation.chest(p.level(),t);var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(tool.getMaxDamage()-1);chest.setItem(0,tool);
            t.progress().resource=3;AscensionGenerator.reconcile(p.level(),t);
            h.assertTrue(AscensionGenerator.automine(p.level(),t,chest),"Last durability permits one harvest");
            h.assertTrue(chest.getItem(0).isEmpty()&&chest.getItem(1).is(Items.COBBLESTONE)&&chest.getItem(1).getCount()==1,"Native stone loot delivered and broken tool consumed");
            h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest)&&t.generator().generation()==1,"Broken tool cannot perform another harvest");
        }finally{remove(p);}h.succeed();
    }

}
