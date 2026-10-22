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

public final class AscensionHardeningGameTest {
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
    private static net.minecraft.world.level.storage.SavedDataStorage storage(java.nio.file.Path path,GameTestHelper h){
        return new net.minecraft.world.level.storage.SavedDataStorage(path,net.minecraft.util.datafix.DataFixers.getDataFixer(),h.getLevel().registryAccess());
    }
    private static java.nio.file.Path directory() throws java.io.IOException {
        var root=java.nio.file.Path.of("build","ascension-m6-fixtures");java.nio.file.Files.createDirectories(root);return java.nio.file.Files.createTempDirectory(root,"save-");
    }
    @GameTest public void diskBackupRestorePreservesTeamsUpgradesRewardsAndBridgeOwnership(GameTestHelper h) throws Exception {
        var root=directory();var owner=UUID.randomUUID();UUID id;java.util.Map<java.nio.file.Path,byte[]> backup=new java.util.HashMap<>();
        try(var data=storage(root,h)){
            var ledger=data.computeIfAbsent(AscensionLedger.TYPE);var t=ledger.create(owner,"Disk fixture");id=t.id();var phase=AscensionPhases.defaults().getFirst();
            t.progress().breaks=250;t.progress().total=250;for(var o:phase.objectives())t.phases().submit(phase,o.id(),o.item(),o.count());t.phases().advance(phase,t.progress());t.phases().claim("genesis_water");
            t.phases().features().collector=true;t.phases().features().enabled=true;t.phases().features().mineAfter=12345;
            data.computeIfAbsent(AscensionBridges.TYPE).record(new BlockPos(110,80,0),id,"0:1","minecraft:oak_planks");data.computeIfAbsent(AscensionPhases.TYPE);data.saveAndJoin();
        }
        for(var type:java.util.List.of(AscensionLedger.TYPE,AscensionBridges.TYPE,AscensionPhases.TYPE)){var file=type.id().withSuffix(".dat").resolveAgainst(root);backup.put(file,java.nio.file.Files.readAllBytes(file));}
        try(var data=storage(root,h)){var ledger=data.computeIfAbsent(AscensionLedger.TYPE);h.assertTrue(ledger.member(owner).id().equals(id),"Real native disk reload keeps team identity");ledger.archive(owner);}
        for(var entry:backup.entrySet())java.nio.file.Files.write(entry.getKey(),entry.getValue());
        try(var data=storage(root,h)){
            var t=data.computeIfAbsent(AscensionLedger.TYPE).member(owner);h.assertTrue(t!=null&&t.id().equals(id)&&t.progress().total==250,"Backup restores original allocation and progress");
            h.assertTrue(t.phases().features().collector&&t.phases().features().enabled&&t.phases().features().mineAfter==12345&&t.phases().unclaimed("genesis_water")==null,"Upgrade timer and claimed reward cannot reset on restore");
            h.assertTrue(data.computeIfAbsent(AscensionBridges.TYPE).at(new BlockPos(110,80,0)).owner().equals(id),"Bridge ownership restores with teams");
            h.assertTrue(data.computeIfAbsent(AscensionPhases.TYPE).phase("caverns").breaks()==500,"Catalog reloads from disk");
        }h.succeed();
    }
    @GameTest public void corruptAndPartiallyDecodableFilesNeverBecomeFreshLedgers(GameTestHelper h) throws Exception {
        var root=directory();var file=AscensionLedger.TYPE.id().withSuffix(".dat").resolveAgainst(root);
        try(var data=storage(root,h)){data.computeIfAbsent(AscensionLedger.TYPE).create(UUID.randomUUID(),"Original");}
        byte[] valid=java.nio.file.Files.readAllBytes(file);
        for(int kind=0;kind<2;kind++){
            if(kind==0)java.nio.file.Files.write(file,new byte[]{31,-117,0});
            if(kind==1){java.nio.file.Files.write(file,valid);var tag=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                var payload=tag.getCompoundOrEmpty("data");payload.getListOrEmpty("teams").add(new net.minecraft.nbt.CompoundTag());net.minecraft.nbt.NbtIo.writeCompressed(tag,file);}
            var damaged=java.nio.file.Files.readAllBytes(file);boolean rejected=false;
            try(var data=storage(root,h)){try{data.computeIfAbsent(AscensionLedger.TYPE);}catch(IllegalStateException expected){rejected=true;}}
            h.assertTrue(rejected&&java.util.Arrays.equals(damaged,java.nio.file.Files.readAllBytes(file)),"Corrupt/partial save is rejected and bytes remain unchanged");
        }h.succeed();
    }
    @GameTest public void everyAscensionStoreRejectsCorruptionAndUncompressedSavesStillLoad(GameTestHelper h) throws Exception {
        var root=directory();
        for(var type:java.util.List.of(AscensionLedger.TYPE,AscensionBridges.TYPE,AscensionPhases.TYPE)){
            try(var data=storage(root,h)){data.computeIfAbsent(type);}
            var file=type.id().withSuffix(".dat").resolveAgainst(root);var tag=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            try(var output=new java.io.DataOutputStream(java.nio.file.Files.newOutputStream(file))){net.minecraft.nbt.NbtIo.write(tag,output);}
            try(var data=storage(root,h)){h.assertTrue(data.get(type)!=null,"Native uncompressed NBT remains readable");}
            byte[] damaged={31,-117,0};java.nio.file.Files.write(file,damaged);boolean rejected=false;
            try(var data=storage(root,h)){try{data.computeIfAbsent(type);}catch(IllegalStateException expected){rejected=true;}}
            h.assertTrue(rejected&&java.util.Arrays.equals(damaged,java.nio.file.Files.readAllBytes(file)),"Every scoped store preserves unreadable bytes");
        }h.succeed();
    }
    @GameTest public void duplicateAndUnknownNetworkActionsCannotRestartOrRedirectTravel(GameTestHelper h){
        var p=player(h);
        try{
            var t=team(p);OneBlock.request(p,Integer.MIN_VALUE);OneBlock.request(p,Integer.MAX_VALUE);h.assertTrue(OneBlock.state(p).pendingTicks==0,"Unknown actions are ignored");
            Ascension.request(p,t.id());var state=OneBlock.state(p);state.pendingTicks=45;
            for(int i=0;i<100;i++){OneBlock.request(p,0);OneBlock.request(p,1);Ascension.request(p,null);}
            h.assertTrue(state.pendingTicks==45&&state.pendingAscension&&t.id().equals(state.pendingTeam),"Duplicate traffic neither resets countdown nor changes destination");
            state.pendingTicks=0;travel(h,p,t.id());int phase=t.progress().phase;OneBlock.request(p,1);
            h.assertTrue(t.progress().phase==phase&&state.pendingTicks==0,"Legacy advance payload cannot bypass MVP objectives");
        }finally{remove(p);}h.succeed();
    }
    @GameTest(maxTicks=300) public void twentyFiveActiveIslandsKeepMaintenanceBounded(GameTestHelper h){
        var players=new java.util.ArrayList<ServerPlayer>();var ids=new java.util.ArrayList<UUID>();
        try{
            for(int i=0;i<25;i++){
                var p=player(h);players.add(p);var t=team(p);ids.add(t.id());travel(h,p,t.id());
                for(int x:new int[]{-6,6})for(int z:new int[]{-6,6})p.level().getChunkAt(t.center().offset(x,0,z));
                var f=t.phases().features();f.collector=true;f.miner=true;f.enabled=true;f.mineAfter=Long.MAX_VALUE;
                p.level().setBlockAndUpdate(AscensionAutomation.output(t),Blocks.CHEST.defaultBlockState());AscensionAutomation.chest(p.level(),t).setItem(0,new ItemStack(Items.IRON_PICKAXE));
            }
        }catch(Throwable failure){players.forEach(AscensionHardeningGameTest::remove);throw failure;}
        h.runAfterDelay(20,()->{try{
            var level=players.getFirst().level();var data=AscensionLedger.get(level);int batches=(data.teams().size()+31)/32;long[] samples=new long[20];
            for(var id:ids){var t=data.team(id);t.phases().features().mineAfter=0;
                for(int i=0;i<8;i++){var item=new ItemEntity(level,t.center().getX()+.5,82,t.center().getZ()+.5,new ItemStack(Items.DIRT));item.setNoPickUpDelay();level.addFreshEntity(item);}
            }
            for(int sample=0;sample<samples.length;sample++){long start=System.nanoTime();for(int batch=0;batch<batches;batch++)Ascension.maintain(level);samples[sample]=System.nanoTime()-start;}
            for(var id:ids){var t=data.team(id);h.assertTrue(t.generator().generation()==1&&t.progress().total==1,"Each attended island mines once; repeated same-tick passes cannot catch up");h.assertTrue(AscensionAutomation.chest(level,t).getItem(1).is(Items.DIRT)&&AscensionAutomation.chest(level,t).getItem(1).getCount()==9,"Each island owns eight collected items and one mined output");}
            java.util.Arrays.sort(samples);darkspawn.black.Darkspawn.LOGGER.info("Ascension M6 profile: 25 active islands, {} total records, {} batches/rotation, 20 rotations: median={}ms p95={}ms max={}ms",data.teams().size(),batches,samples[10]/1e6,samples[18]/1e6,samples[19]/1e6);
        }finally{players.forEach(AscensionHardeningGameTest::remove);}h.succeed();});
    }
}
