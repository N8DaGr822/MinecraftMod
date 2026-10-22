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

public final class AscensionGameTest {
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
    @GameTest public void threeProfilesAndCompanionLedgersStaySeparate(GameTestHelper h) {
        var p=player(h);
        try {
            var outside=p.level();var team=team(p);
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));p.getEnderChestInventory().setItem(0,new ItemStack(Items.EMERALD,9));
            h.assertTrue(OneBlock.travel(p),"Legacy remains accessible");ready(p);p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,3));
            h.assertTrue(!Ascension.travel(p,team.id()),"No direct legacy-to-Ascension profile hop");
            h.assertTrue(OneBlock.travel(p),"Return from legacy");ready(p);
            travel(h,p,team.id());
            h.assertTrue(p.getInventory().isEmpty()&&p.getEnderChestInventory().isEmpty(),"Ascension starts without either profile's possessions");
            h.assertTrue(darkspawn.black.mount.MountRegistry.get(p.level())!=darkspawn.black.mount.MountRegistry.get(outside),"Recovery registry is isolated");
            h.assertTrue(darkspawn.black.legacy.NoahLedger.get(p.level())!=darkspawn.black.legacy.NoahLedger.get(outside),"Stored Thalassar cargo is isolated");
            p.getInventory().setItem(0,new ItemStack(Items.DIRT,5));
            h.assertTrue(!OneBlock.travel(p),"No direct Ascension-to-legacy hop");travel(h,p,null);
            h.assertTrue(p.getInventory().getItem(0).getCount()==7&&p.getInventory().getItem(0).is(Items.DIAMOND)&&p.getEnderChestInventory().getItem(0).getCount()==9,"Outside inventory returns exactly");
            h.assertTrue(OneBlock.travel(p),"Legacy return works");ready(p);
            h.assertTrue(p.getInventory().getItem(0).is(Items.IRON_INGOT)&&p.getInventory().getItem(0).getCount()==3,"Legacy profile unchanged");
            h.assertTrue(OneBlock.travel(p),"Leave legacy again");ready(p);travel(h,p,team.id());
            h.assertTrue(p.getInventory().getItem(0).is(Items.DIRT)&&p.getInventory().getItem(0).getCount()==5,"Ascension inventory persists across visits");
        } finally {remove(p);}h.succeed();
    }
    @GameTest public void teamCoresProgressIndependentlyAndVisitsCannotEdit(GameTestHelper h) {
        var a=player(h);var b=player(h);
        try {
            var ta=team(a);var tb=team(b);travel(h,a,ta.id());travel(h,b,tb.id());
            h.assertTrue(a.gameMode.destroyBlock(ta.center()),"Owner can mine own core");
            h.assertTrue(ta.progress().total==1&&tb.progress().total==0&&a.level().getBlockState(ta.center()).is(OneBlock.CORE),"Only own team's counter changes and core regenerates");
            h.assertTrue(!a.gameMode.destroyBlock(ta.center())&&ta.progress().total==1,"Duplicate same-tick core break denied");
            travel(h,b,ta.id());
            h.assertTrue(!b.gameMode.destroyBlock(ta.center())&&ta.progress().total==1,"Visitor cannot mine source");
            var chest=ta.center().offset(2,1,0);a.level().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
            var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(chest),net.minecraft.core.Direction.UP,chest,false);
            h.assertTrue(b.gameMode.useItemOn(b,b.level(),ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,hit)==net.minecraft.world.InteractionResult.FAIL&&b.containerMenu==b.inventoryMenu,"Visitor cannot open container");
            h.assertTrue(!b.level().mayInteract(b,chest),"Remote inventory access observes claim permissions");
            h.assertTrue(!AscensionProtection.canEdit(a,ta.center().offset(101,0,0)),"Neutral columns cannot be edited before bridges");
            h.assertTrue(AscensionProtection.canEdit(a,ta.center().offset(100,0,0)),"Inclusive owner edge remains editable");
            h.assertTrue(b.findRespawnPositionAndUseSpawnBlock(true,TeleportTransition.DO_NOTHING).position().distanceToSqr(Vec3.atCenterOf(tb.center()))<25,"Visitor death respawns at own island");
        }finally {remove(a);remove(b);}h.succeed();
    }
    @GameTest public void edgePlacementAndBucketsCannotWriteNeutralColumns(GameTestHelper h) {
        var p=player(h);
        try {
            var t=team(p);travel(h,p,t.id());var edge=t.center().offset(100,0,20);p.level().setBlockAndUpdate(edge,Blocks.STONE.defaultBlockState());
            var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(edge),net.minecraft.core.Direction.EAST,edge,false);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            var context=new net.minecraft.world.item.context.BlockPlaceContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,p.getMainHandItem(),hit);
            h.assertTrue(((net.minecraft.world.item.BlockItem)Items.DIRT).place(context)==net.minecraft.world.InteractionResult.FAIL&&p.level().isEmptyBlock(edge.east()),"Actual placement target outside claim is denied");
            h.assertTrue(!((net.minecraft.world.item.BucketItem)Items.WATER_BUCKET).emptyContents(p,p.level(),edge.east(),hit)&&p.level().getFluidState(edge.east()).isEmpty(),"Bucket cannot pour across claim edge");
        }finally {remove(p);}h.succeed();
    }
    @GameTest public void invitationsRequireOwnerAndAcceptanceAndMembershipPersists(GameTestHelper h) {
        var owner=player(h);var guest=player(h);var other=player(h);
        try {
            var t=team(owner);var data=AscensionLedger.get(owner.level());
            Ascension.invite(guest,other);Ascension.accept(other);h.assertTrue(data.member(other.getUUID())==null,"Nonowner cannot invite");
            Ascension.invite(owner,guest);h.assertTrue(data.member(guest.getUUID())==null,"Invitation is not membership");Ascension.accept(guest);
            h.assertTrue(data.member(guest.getUUID()).id().equals(t.id()),"Explicit acceptance joins owner team");
            travel(h,guest,t.id());Ascension.leave(guest);h.assertTrue(data.member(guest.getUUID())!=null,"Membership cannot change while on an island");
            var saved=AscensionLedger.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data).getOrThrow();
            var restored=AscensionLedger.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
            h.assertTrue(restored.member(guest.getUUID()).owner().equals(owner.getUUID())&&restored.member(guest.getUUID()).center().equals(t.center()),"Reload preserves role and cell");
            travel(h,guest,null);Ascension.leave(guest);h.assertTrue(data.member(guest.getUUID())==null&&data.member(owner.getUUID())!=null,"Leaving preserves owner's island");
        }finally {remove(owner);remove(guest);remove(other);}h.succeed();
    }
    @GameTest public void saveDeathAndCancelledTravelPreserveInactiveProfiles(GameTestHelper h) {
        var p=player(h);var copy=player(h);
        try {
            var t=team(p);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,11));
            Ascension.request(p,t.id());h.assertTrue(OneBlock.state(p).pendingTicks==60,"Ascension uses existing stationary countdown");
            p.teleportTo(p.getX()+1,p.getY(),p.getZ());OneBlock.tick(p);h.assertTrue(OneBlock.state(p).pendingTicks==0&&!Ascension.island(p.level()),"Movement cancels without swapping profiles");
            Ascension.request(p,t.id());for(int tick=0;tick<60;tick++)OneBlock.tick(p);ready(p);
            h.assertTrue(Ascension.island(p.level()),"Completed countdown dispatches to the Ascension destination");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(output);
            OneBlock.state(copy).read(TagValueInput.create(ProblemReporter.DISCARDING,copy.registryAccess(),output.buildResult()));
            h.assertTrue(OneBlock.state(copy).ascensionInside&&!OneBlock.state(copy).inside&&OneBlock.state(copy).ascensionOther.inventory().getFirst().getCount()==11,"Saved marker and inactive Ascension profile survive serialization");
            ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(p,copy,false);
            h.assertTrue(OneBlock.state(copy).ascensionInside&&OneBlock.state(copy).ascensionOther.inventory().getFirst().is(Items.DIAMOND),"Death copying retains inactive possessions");
            h.assertTrue(p.findRespawnPositionAndUseSpawnBlock(true,TeleportTransition.DO_NOTHING).newLevel()==p.level(),"Respawn stays inside Ascension");
        }finally {remove(p);remove(copy);}h.succeed();
    }
    @GameTest public void obstructionNeverOverwritesStarterAndVisitorArrivalNeverClearsBlocks(GameTestHelper h) {
        var a=player(h);var b=player(h);
        try {
            var ta=team(a);var tb=team(b);var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);
            level.setBlockAndUpdate(ta.center().offset(2,2,2),Blocks.DIAMOND_BLOCK.defaultBlockState());
            h.assertTrue(!Ascension.travel(a,ta.id())&&!ta.progress().initialized&&level.isEmptyBlock(ta.center()),"Obstructed new site produces no partial platform");
            level.removeBlock(ta.center().offset(2,2,2),false);travel(h,a,ta.id());travel(h,b,tb.id());
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(ta.center().offset(x,1,z),Blocks.STONE.defaultBlockState());
            h.assertTrue(!Ascension.travel(b,ta.id())&&level.getBlockState(ta.center().above()).is(Blocks.STONE),"Blocked visitor arrival cancels without destruction");
        }finally {remove(a);remove(b);}h.succeed();
    }
    @GameTest public void externalTeleportsAndLooseItemsCannotCrossAscensionBoundary(GameTestHelper h) {
        var p=player(h);
        try {
            var t=team(p);var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);
            var transition=new TeleportTransition(level,Vec3.atBottomCenterOf(t.center().above()),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING);
            h.assertTrue(p.teleport(transition)==null&&!Ascension.island(p.level()),"External teleport cannot bypass profile swap");
            var item=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),new ItemStack(Items.DIAMOND));p.level().addFreshEntity(item);
            h.assertTrue(item.teleport(transition)==null&&!item.isRemoved(),"Loose items cannot enter Ascension");item.discard();
        }finally {remove(p);}h.succeed();
    }
    @GameTest(maxTicks=120) public void nativeDeathDropsOnlyActiveItemsAndReturnsToTeamHome(GameTestHelper h) {
        var p=player(h);var t=team(p);p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,5));travel(h,p,t.id());
        h.runAfterDelay(20,()->{
            ServerPlayer respawned=null;var level=p.level();var rule=net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY;boolean keep=level.getGameRules().get(rule);
            try {
                level.getGameRules().set(rule,false,level.getServer());var area=p.getBoundingBox().inflate(5);
                p.getInventory().setItem(0,new ItemStack(Items.APPLE,7));p.hurtServer(level,p.damageSources().magic(),1000);
                h.assertTrue(!p.isAlive()&&p.getInventory().isEmpty(),"Death follows native active-inventory drop rules");
                var drops=level.getEntitiesOfClass(ItemEntity.class,area);
                h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(Items.APPLE))&&drops.stream().noneMatch(e->e.getItem().is(Items.DIAMOND)),"Inactive outside items never drop in Ascension");
                var connection=p.connection;connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                respawned=connection.player;ready(respawned);
                h.assertTrue(Ascension.island(respawned.level())&&t.contains(respawned.blockPosition())&&respawned.getInventory().isEmpty(),"Native respawn stays on team island without duplicating drops");
                travel(h,respawned,null);h.assertTrue(respawned.getInventory().getItem(0).is(Items.DIAMOND)&&respawned.getInventory().getItem(0).getCount()==5,"Return after death restores only outside items");drops.forEach(ItemEntity::discard);
            }finally {level.getGameRules().set(rule,keep,level.getServer());remove(respawned==null?p:respawned);}h.succeed();
        });
    }

}
