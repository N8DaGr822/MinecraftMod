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

public final class AscensionPhaseGameTest {
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
    private static void offer(ServerPlayer p,String objective,net.minecraft.world.item.Item item,int count){
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item,count));
        try{p.level().getServer().getCommands().getDispatcher().execute("ascension submit "+objective,p.createCommandSourceStack());}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new AssertionError("Submission command was not registered",failure);}
    }
    private static void genesis(ServerPlayer p,AscensionLedger.Team t){
        t.progress().breaks=250;offer(p,"timber",Items.OAK_LOG,16);offer(p,"stone",Items.COBBLESTONE,32);ready(p);Ascension.advance(p);
    }
    @GameTest public void partialSubmissionsConsumeOnlyNeededHeldItemsAndVisitorsCannotContribute(GameTestHelper h){
        var p=player(h);var friend=player(h);var visitor=player(h);
        try{
            var t=team(p);AscensionLedger.get(p.level()).join(t.id(),friend.getUUID());t=AscensionLedger.get(p.level()).team(t.id());
            var v=team(visitor);travel(h,p,t.id());travel(h,friend,t.id());travel(h,visitor,t.id());
            offer(p,"timber",Items.DIRT,5);h.assertTrue(p.getMainHandItem().getCount()==5&&t.phases().submitted("timber")==0,"Wrong held item is untouched");
            offer(p,"timber",Items.OAK_LOG,5);h.assertTrue(p.getMainHandItem().isEmpty()&&t.phases().submitted("timber")==5,"Partial submission credits exactly five");
            offer(friend,"timber",Items.OAK_LOG,20);h.assertTrue(friend.getMainHandItem().getCount()==9&&t.phases().submitted("timber")==16,"Teammate contributes only remaining amount");
            AscensionObjectives.submit(friend,"timber");h.assertTrue(friend.getMainHandItem().getCount()==9,"Completed objective consumes nothing");
            offer(visitor,"timber",Items.OAK_LOG,16);h.assertTrue(visitor.getMainHandItem().getCount()==16&&v.phases().submitted("timber")==0,"Visiting does not allow submission to either island");
            p.getAbilities().instabuild=true;offer(p,"stone",Items.COBBLESTONE,32);h.assertTrue(p.getMainHandItem().getCount()==32&&t.phases().submitted("stone")==0,"Creative cannot submit resources");
            p.getAbilities().instabuild=false;OneBlock.state(p).pendingTicks=10;AscensionObjectives.submit(p,"stone");h.assertTrue(t.phases().submitted("stone")==0,"Travel countdown blocks inventory transactions");OneBlock.state(p).pendingTicks=0;
            AscensionObjectives.submit(p,"stone");h.assertTrue(t.phases().submitted("stone")==32,"Normal submission resumes");
        }finally{remove(p);remove(friend);remove(visitor);}h.succeed();
    }
    @GameTest public void nativeMiningContinuesPastThresholdAndEncounterGateCannotBeSkipped(GameTestHelper h){
        var p=player(h);
        try{
            var t=team(p);travel(h,p,t.id());t.progress().breaks=250;ready(p);Ascension.advance(p);
            h.assertTrue(t.phases().phase().equals("genesis"),"Threshold alone cannot advance");
            h.assertTrue(p.gameMode.destroyBlock(t.center())&&t.progress().breaks==251&&t.progress().resource==1,"Past-threshold mining regenerates with guaranteed wood");
            genesis(p,t);h.assertTrue(t.phases().phase().equals("caverns")&&t.progress().breaks==0,"Submission completion advances and resets only phase counter");
            t.progress().breaks=700;offer(p,"stone",Items.COBBLESTONE,64);offer(p,"iron",Items.IRON_INGOT,16);ready(p);Ascension.advance(p);
            h.assertTrue(t.phases().phase().equals("caverns")&&t.progress().breaks==700,"Cave Guardian gate remains mandatory beyond threshold");
            // Encounter Fixture: Isolate phase progression here; AscensionFeatureGameTest covers authoritative Guardian death.
            t.phases().recordEncounter("cave_guardian");Ascension.advance(p);h.assertTrue(t.phases().phase().equals("verdant_wilds"),"Encounter plus objectives plus threshold opens Verdant Wilds");
            t.progress().breaks=600;offer(p,"wheat",Items.WHEAT,32);offer(p,"carrots",Items.CARROT,16);Ascension.advance(p);
            h.assertTrue(t.phases().finished("verdant_wilds")&&t.phases().phase().equals("forgeworks")&&t.phases().rewards().size()==11,"First chapter completes once and opens Forgeworks");
            Ascension.advance(p);h.assertTrue(t.phases().rewards().size()==11,"Repeated advancement cannot issue earlier rewards again");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void fullInventoryPreservesRewardAndTeammatesCannotClaimItTwiceAfterReload(GameTestHelper h){
        var p=player(h);var friend=player(h);
        try{
            var t=team(p);AscensionLedger.get(p.level()).join(t.id(),friend.getUUID());t=AscensionLedger.get(p.level()).team(t.id());travel(h,p,t.id());travel(h,friend,t.id());genesis(p,t);
            int slot;while((slot=p.getInventory().getFreeSlot())>=0)p.getInventory().setItem(slot,new ItemStack(Items.DIRT,64));
            AscensionObjectives.claim(p,"genesis_water");h.assertTrue(t.phases().unclaimed("genesis_water")!=null&&p.getInventory().countItem(Items.WATER_BUCKET)==0,"Full inventory preserves entitlement");
            p.getInventory().setItem(0,ItemStack.EMPTY);AscensionObjectives.claim(p,"genesis_water");
            h.assertTrue(t.phases().unclaimed("genesis_water")==null&&p.getInventory().countItem(Items.WATER_BUCKET)==1,"Claim delivers one bucket and consumes entitlement");
            var saved=AscensionLedger.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,AscensionLedger.get(p.level())).getOrThrow();
            var restored=AscensionLedger.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();p.level().getDataStorage().set(AscensionLedger.TYPE,restored);
            AscensionObjectives.claim(friend,"genesis_water");h.assertTrue(friend.getInventory().countItem(Items.WATER_BUCKET)==0&&restored.team(t.id()).phases().unclaimed("genesis_water")==null,"Saved reward ID denies teammate replay");
            h.assertTrue(restored.team(t.id()).phases().submitted("stone")==0&&restored.team(t.id()).phases().phase().equals("caverns"),"Completed Genesis submissions cannot satisfy Caverns objectives");
        }finally{remove(p);remove(friend);}h.succeed();
    }
    @GameTest public void mvpMiningDoesNotIssueLegacySupplyChests(GameTestHelper h){
        var p=player(h);
        var created=team(p);travel(h,p,created.id());
        // Entity Visibility: Wait for destination tracking before asserting the native animal milestone.
        h.runAfterDelay(20,()->{try{
            var t=AscensionLedger.get(p.level()).team(created.id());t.progress().total=99;t.progress().breaks=99;
            h.assertTrue(p.gameMode.destroyBlock(t.center())&&t.progress().total==100,"Confirmed MVP mining keeps counters");
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)h.assertTrue(!p.level().getBlockState(t.center().offset(x,1,z)).is(Blocks.CHEST),"Legacy supplies cannot bypass objective rewards");
            h.assertTrue(t.phases().rewards().isEmpty(),"Threshold-independent mining does not unlock completion rewards");
            h.assertTrue(p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(t.center()).inflate(8)).size()==1,"Existing bounded animal milestone remains available");
        }finally{remove(p);}h.succeed();});
    }
    @GameTest public void actualResourceReloadRetainsLastGoodCatalogAcrossMalformedOrChangedIdentities(GameTestHelper h){
        var p=player(h);team(p);var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);var catalog=AscensionPhases.get(level);
        var original=AscensionPhases.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,catalog).getOrThrow();
        try{
            var definition=AscensionPhases.Phase.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,catalog.phase("genesis")).getOrThrow().getAsJsonObject();definition.addProperty("breaks",275);
            AscensionPhases.reload(level.getServer(),resources(level.getServer().getResourceManager(),definition.toString()));
            h.assertTrue(catalog.phase("genesis").breaks()==275,"Resource manager override is applied as a complete catalog");
            AscensionPhases.reload(level.getServer(),resources(level.getServer().getResourceManager(),"{broken json"));
            h.assertTrue(catalog.phase("genesis").breaks()==275&&!catalog.lastError().isEmpty(),"Malformed JSON preserves accepted catalog");
            definition.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("item","minecraft:birch_log");
            AscensionPhases.reload(level.getServer(),resources(level.getServer().getResourceManager(),definition.toString()));
            h.assertTrue(catalog.phase("genesis").objectives().getFirst().item().equals("minecraft:oak_log"),"Reload cannot reinterpret already paid objective items");
            var saved=AscensionPhases.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,catalog).getOrThrow();var restored=AscensionPhases.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
            h.assertTrue(restored.phase("genesis").breaks()==275,"Last accepted balance survives storage reconstruction");
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(level.getServer(),(net.minecraft.server.packs.resources.CloseableResourceManager)level.getServer().getResourceManager(),true);
            h.assertTrue(catalog.phase("genesis").breaks()==250&&catalog.lastError().isEmpty(),"Registered lifecycle hook reloads bundled defaults");
        }finally{level.getDataStorage().set(AscensionPhases.TYPE,AscensionPhases.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,original).getOrThrow());remove(p);}h.succeed();
    }
    @GameTest public void freshWorldAcceptsCustomObjectiveIdentityBeforeItsFirstMvpTeam(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(Ascension.LEVEL);var previous=AscensionLedger.get(level);var original=AscensionPhases.get(level);
        var copy=AscensionPhases.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,AscensionPhases.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,original).getOrThrow()).getOrThrow();
        try{
            level.getDataStorage().set(AscensionLedger.TYPE,new AscensionLedger());level.getDataStorage().set(AscensionPhases.TYPE,copy);
            var definition=AscensionPhases.Phase.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,copy.phase("genesis")).getOrThrow().getAsJsonObject();
            definition.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("id","planks");definition.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("item","minecraft:oak_planks");
            AscensionPhases.reload(level.getServer(),resources(level.getServer().getResourceManager(),definition.toString()));
            h.assertTrue(copy.lastError().isEmpty()&&copy.phase("genesis").objectives().getFirst().id().equals("planks"),"Fresh world accepts valid custom objectives");
            AscensionLedger.get(level).create(UUID.randomUUID(),"First MVP");definition.getAsJsonArray("objectives").get(0).getAsJsonObject().addProperty("item","minecraft:birch_planks");
            AscensionPhases.reload(level.getServer(),resources(level.getServer().getResourceManager(),definition.toString()));
            h.assertTrue(!copy.lastError().isEmpty()&&copy.phase("genesis").objectives().getFirst().item().equals("minecraft:oak_planks"),"First MVP allocation fixes objective identity for later reloads");
        }finally{level.getDataStorage().set(AscensionLedger.TYPE,previous);level.getDataStorage().set(AscensionPhases.TYPE,original);}h.succeed();
    }
    private static net.minecraft.server.packs.resources.ResourceManager resources(net.minecraft.server.packs.resources.ResourceManager delegate,String json){
        return new net.minecraft.server.packs.resources.ResourceManager(){
            public java.util.Set<String> getNamespaces(){return delegate.getNamespaces();}
            public java.util.Optional<net.minecraft.server.packs.resources.Resource> getResource(net.minecraft.resources.Identifier id){
                var original=delegate.getResource(id);
                if(!id.equals(darkspawn.black.Darkspawn.id("ascension/phases/genesis.json")))return original;
                return original.map(r->new net.minecraft.server.packs.resources.Resource(r.source(),()->new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            }
            public java.util.List<net.minecraft.server.packs.resources.Resource> getResourceStack(net.minecraft.resources.Identifier id){return delegate.getResourceStack(id);}
            public java.util.Map<net.minecraft.resources.Identifier,net.minecraft.server.packs.resources.Resource> listResources(String directory,Selector selector){return delegate.listResources(directory,selector);}
            public java.util.Map<net.minecraft.resources.Identifier,java.util.List<net.minecraft.server.packs.resources.Resource>> listResourceStacks(String directory,Selector selector){return delegate.listResourceStacks(directory,selector);}
            public java.util.stream.Stream<net.minecraft.server.packs.PackResources> listPacks(){return delegate.listPacks();}
        };
    }
}
