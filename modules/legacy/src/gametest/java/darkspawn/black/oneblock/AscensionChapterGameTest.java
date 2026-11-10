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

public final class AscensionChapterGameTest {
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
        Ascension.create(p,"Test team");return AscensionLedger.get(p.level()).member(p.getUUID());
    }
    private static void travel(GameTestHelper h,ServerPlayer p,UUID id) {
        ready(p);h.assertTrue(Ascension.travel(p,id),"Safe Ascension travel succeeds");ready(p);
    }

    private static void finishTo(AscensionLedger.Team t,String phase){
        for(var definition:AscensionPhases.defaults()){
            if(t.phases().phase().equals(phase))return;
            if(!definition.id().equals(t.phases().phase()))continue;
            t.progress().breaks=definition.breaks();
            for(var goal:definition.objectives())t.phases().submit(definition,goal.id(),goal.item(),goal.count());
            for(var encounter:definition.encounters())t.phases().recordEncounter(encounter);
            if(!t.phases().advance(definition,t.progress()))throw new AssertionError("Phase fixture failed");
        }
    }
    private static void arena(ServerPlayer p,AscensionLedger.Team t){
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            p.level().getChunkAt(t.center().offset(x,0,z));
            if(x!=0||z!=0)p.level().setBlockAndUpdate(t.center().offset(x,0,z),Blocks.STONE.defaultBlockState());
        }
    }
    @GameTest public void chapterGuardiansCreditTheirOwnPhasesAndLeaveTerrainUntouched(GameTestHelper h){
        var p=player(h);
        try{
            var t=team(p);travel(h,p,t.id());arena(p,t);finishTo(t,"stormbound");
            for(String phase:AscensionPhases.IDS.subList(4,12)){
                var definition=AscensionPhases.get(p.level()).phase(phase);t.progress().breaks=definition.breaks();
                for(var goal:definition.objectives())t.phases().submit(definition,goal.id(),goal.item(),goal.count());
                ready(p);AscensionGuardian.start(p);var id=t.phases().features().guardian;
                h.assertTrue(id!=null,"New phase summons its encounter");var mob=(net.minecraft.world.entity.Mob)p.level().getEntity(id);
                h.assertTrue(mob.getName().getString().endsWith("Guardian"),"Encounter presentation identifies phase");
                h.assertTrue(mob instanceof AscensionGuardianEntity,"Dedicated type supplies synchronized presentation");
                var guardian=(AscensionGuardianEntity)mob;
                h.assertTrue(guardian.chapter()==AscensionPhases.IDS.indexOf(phase)&&guardian.intensity()==1,"Chapter and full-health music intensity match");
                guardian.warning(40);
                h.assertTrue(darkspawn.black.creaturekit.CreatureAudio.warning(guardian)==40&&darkspawn.black.creaturekit.CreatureAudio.boss(guardian),"Warning is available to encounter music");
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.level().registryAccess());
                guardian.saveWithoutId(output);
                var restored=AscensionGuardian.TYPE.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.LOAD);
                restored.load(TagValueInput.create(ProblemReporter.DISCARDING,p.level().registryAccess(),output.buildResult()));
                h.assertTrue(restored.chapter()==guardian.chapter()&&restored.warning()==0,"Saved identity survives; interrupted warning does not resume");
                restored.discard();
                guardian.setHealth(guardian.getMaxHealth()/2);
                h.assertTrue(guardian.intensity()==2,"Half-health score intensifies");
                guardian.setHealth(guardian.getMaxHealth()/4);
                h.assertTrue(guardian.intensity()==3,"Low-health score intensifies");
                h.assertTrue(mob.hurtServer(p.level(),p.damageSources().playerAttack(p),1000),"Team may defeat its Guardian");
                h.assertTrue(t.phases().encountered(AscensionPhases.encounter(phase)),"Correct encounter receives credit");
                h.assertTrue(p.level().getBlockState(t.center().offset(4,0,4)).is(Blocks.STONE),"Arena survives encounter");
                ready(p);Ascension.advance(p);
            }
            h.assertTrue(t.phases().completed()&&t.phases().phase().equals("ascension_summit"),"Expanded chapter completes");
        }finally{remove(p);AscensionGuardian.tick();}h.succeed();
    }
    @GameTest public void expandedResourcesYieldRequiredMaterials(GameTestHelper h){
        var expected=java.util.List.of(Items.SNOWBALL,Items.PRISMARINE,Items.AMETHYST_SHARD,Items.PURPUR_BLOCK,Items.OBSIDIAN,Items.ANCIENT_DEBRIS);
        for(int i=0;i<expected.size();i++){
            var state=OneBlock.CORE.defaultBlockState().setValue(OneBlockCore.RESOURCE,20+i);
            var tool=new ItemStack(i==0?Items.DIAMOND_SHOVEL:Items.DIAMOND_PICKAXE);
            var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),h.absolutePos(BlockPos.ZERO),null,null,tool);
            final var item=expected.get(i);
            h.assertTrue(drops.stream().anyMatch(stack->stack.is(item)&&stack.getCount()>0),"Expanded resource supplies its crafting material: "+item);
        }
        var p=player(h);
        try{
            var t=team(p);travel(h,p,t.id());arena(p,t);t.phases().features().collector=t.phases().features().miner=t.phases().features().enabled=true;
            p.level().setBlockAndUpdate(AscensionAutomation.output(t),Blocks.CHEST.defaultBlockState());
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(AscensionAutomation.output(t));
            chest.setItem(0,new ItemStack(Items.IRON_PICKAXE));t.progress().resource=24;AscensionGenerator.reconcile(p.level(),t);
            h.assertTrue(AscensionAutomation.status(p.level(),t).contains("manually"),"Iron miner explains diamond-tier resource blockage: "+AscensionAutomation.status(p.level(),t));
            int total=t.progress().total;
            h.assertTrue(!AscensionGenerator.automine(p.level(),t,chest)&&t.progress().total==total,"Blocked miner preserves resource and progress");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void relayAllowsDistantAttendedMiningAndDrivePurchasePersists(GameTestHelper h){
        var p=player(h);
        try{
            var t=team(p);travel(h,p,t.id());arena(p,t);finishTo(t,"stormbound");
            var f=t.phases().features();f.collector=f.miner=f.enabled=true;
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COPPER_INGOT,64));
            AscensionAutomation.buy(p,"relay");AscensionAutomation.buy(p,"relay");
            h.assertTrue(f.relay&&p.getMainHandItem().getCount()==32,"Relay paid exactly once");
            p.level().setBlockAndUpdate(AscensionAutomation.output(t),Blocks.CHEST.defaultBlockState());
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(AscensionAutomation.output(t));
            chest.setItem(0,new ItemStack(Items.IRON_PICKAXE));f.mineAfter=0;
            p.teleportTo(t.center().getX()+40.5,81,t.center().getZ()+.5);
            h.assertTrue(AscensionAutomation.present(p.level(),t),"Relay attends entire claim");
            int total=t.progress().total;AscensionAutomation.tick(p.level(),t);
            h.assertTrue(t.progress().total==total+1,"Loaded miner works while member builds farther away");
            finishTo(t,"emberlands");p.teleportTo(t.center().getX()+.5,81,t.center().getZ()+1.5);ready(p);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE,64));AscensionAutomation.buy(p,"drive");
            var restored=AscensionFeatures.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,AscensionFeatures.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,f).getOrThrow()).getOrThrow();
            h.assertTrue(restored.relay&&restored.drive&&p.getMainHandItem().getCount()==32,"Chapter upgrades survive saves with exact cost");
            chest.setItem(0,ItemStack.EMPTY);
            h.assertTrue(AscensionAutomation.status(p.level(),t).contains("pickaxe missing"),"Live status identifies missing tool");
            p.teleportTo(t.center().getX()+101.5,81,t.center().getZ()+.5);
            h.assertTrue(!AscensionAutomation.present(p.level(),t),"Leaving claim stops relay attendance");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void removedMemberRecoveryRestoresOutsidePossessionsEvenWhenAirborne(GameTestHelper h){
        var owner=player(h);var member=player(h);
        try{
            var t=team(owner);AscensionLedger.get(owner.level()).join(t.id(),member.getUUID());
            member.getInventory().setItem(9,new ItemStack(Items.EMERALD,7));travel(h,member,t.id());
            member.getInventory().setItem(9,new ItemStack(Items.DIAMOND,3));
            h.assertTrue(AscensionLedger.get(owner.level()).remove(owner.getUUID(),member.getUUID()),"Owner removes offline-equivalent membership");
            member.setOnGround(false);
            h.assertTrue(Ascension.recover(member),"Removed airborne member has a safe return");
            h.assertTrue(!Ascension.island(member.level())&&member.getInventory().getItem(9).is(Items.EMERALD)
                &&member.getInventory().getItem(9).getCount()==7,"Outside items restored");
            h.assertTrue(OneBlock.state(member).ascensionOther.inventory().get(9).is(Items.DIAMOND)
                &&OneBlock.state(member).ascensionOther.inventory().get(9).getCount()==3,"Island possessions remain saved");
            h.assertTrue(!Ascension.recover(member),"Recovery cannot swap inventories twice");
        }finally{remove(owner);remove(member);}h.succeed();
    }
    @GameTest public void recoveryRefusesCursorItemsAndManagementRefusesOnlineIslandMembers(GameTestHelper h){
        var owner=player(h);var member=player(h);
        try{
            var t=team(owner);AscensionLedger.get(owner.level()).join(t.id(),member.getUUID());travel(h,member,t.id());
            Ascension.manage(owner,member.getUUID(),false);
            h.assertTrue(AscensionLedger.get(owner.level()).member(member.getUUID())!=null,"Online member must return before removal");
            AscensionLedger.get(owner.level()).remove(owner.getUUID(),member.getUUID());
            member.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND,2));
            h.assertTrue(!Ascension.recover(member)&&member.inventoryMenu.getCarried().getCount()==2,"Cursor cannot cross inventory profiles");
            member.inventoryMenu.setCarried(ItemStack.EMPTY);ready(member);
            h.assertTrue(Ascension.recover(member),"Clearing cursor allows recovery");
        }finally{remove(owner);remove(member);}h.succeed();
    }
    @GameTest public void guideTokenRejectsRepeatedSubmissions(GameTestHelper h){
        var p=player(h);var t=team(p);travel(h,p,t.id());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_LOG,5));
        var snapshot=AscensionGuide.handle(p,new AscensionGuideRequest("","",""));
        var request=new AscensionGuideRequest(snapshot.token(),"submit","timber");
        h.runAfterDelay(6,()->{
            var reply=AscensionGuide.handle(p,request);
            h.assertTrue(reply.notice().contains("Submitted 5"),"The screen receives authoritative action feedback");
            h.assertTrue(t.phases().submitted("timber")==5,"First offered action submits held stack");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_LOG,5));
        });
        h.runAfterDelay(12,()->{
            try{
                AscensionGuide.handle(p,request);
                h.assertTrue(t.phases().submitted("timber")==5&&p.getMainHandItem().getCount()==5,"Replayed token cannot spend another stack");
            }finally{remove(p);}h.succeed();
        });
    }
    @GameTest public void guideTokenCannotCrossPhaseOrOfferUnknownActions(GameTestHelper h){
        var p=player(h);var t=team(p);travel(h,p,t.id());
        var snapshot=AscensionGuide.handle(p,new AscensionGuideRequest("","",""));
        finishTo(t,"caverns");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,32));
        h.runAfterDelay(6,()->{
            try{
                var fresh=AscensionGuide.handle(p,new AscensionGuideRequest(snapshot.token(),"submit","stone"));
                h.assertTrue(t.phases().submitted("stone")==0&&p.getMainHandItem().getCount()==32,"Stale Genesis click cannot submit Caverns items");
                h.assertTrue(fresh.entries().stream().anyMatch(e->e.section().equals("Overview")&&e.title().contains("Caverns")),"Fresh snapshot explains current phase");
            }finally{remove(p);}h.succeed();
        });
    }
    @GameTest public void offlineMembershipCanBeRemovedTransferredAndArchivedWithoutReallocating(GameTestHelper h){
        var owner=player(h);
        try{
            var t=team(owner);var data=AscensionLedger.get(owner.level());var offline=UUID.randomUUID();data.join(t.id(),offline);
            Ascension.manage(owner,offline,true);
            h.assertTrue(data.team(t.id()).owner().equals(offline)&&data.team(t.id()).center().equals(t.center()),"Offline successor receives the same island");
            Ascension.manage(owner,offline,false);
            h.assertTrue(data.member(offline)!=null,"Former owner cannot remove successor");
            h.assertTrue(data.transfer(offline,owner.getUUID()),"Fixture returns ownership");
            Ascension.manage(owner,offline,false);h.assertTrue(data.member(offline)==null,"Owner can remove offline member");
            data.join(t.id(),offline);Ascension.archive(owner);
            h.assertTrue(data.archived(t.id())&&data.member(offline)==null,"Offline teammate no longer blocks archive");
            h.assertTrue(data.teams().stream().anyMatch(saved->saved.id().equals(t.id())&&saved.center().equals(t.center())),"Archived island stays reserved");
        }finally{remove(owner);}h.succeed();
    }
    @GameTest public void guideSnapshotRoundTripsAndRejectsOversizedPayload(GameTestHelper h){
        var p=player(h);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{
            team(p);var snapshot=AscensionGuide.snapshot(p);
            AscensionGuideSnapshot.CODEC.encode(buffer,snapshot);
            h.assertTrue(snapshot.equals(AscensionGuideSnapshot.CODEC.decode(buffer)),"Titles, actions and confirmations survive transport");
            // Chapter Capacity: Maximum custom reward catalogs can exceed the former 128-entry limit.
            buffer.clear();var expanded=new AscensionGuideSnapshot("token","",java.util.Collections.nCopies(129,snapshot.entries().getFirst()));
            AscensionGuideSnapshot.CODEC.encode(buffer,expanded);
            h.assertTrue(expanded.equals(AscensionGuideSnapshot.CODEC.decode(buffer)),"Expanded guide survives transport");
            buffer.clear();buffer.writeUtf("token",40);buffer.writeUtf("",4096);buffer.writeVarInt(161);
            boolean rejected=false;try{AscensionGuideSnapshot.CODEC.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}
            h.assertTrue(rejected,"Network entry count is bounded before allocation");
        }finally{buffer.release();remove(p);}h.succeed();
    }
}
