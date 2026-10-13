package darkspawn.black.journal;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossKind;
import darkspawn.black.cooking.Culinary;
import darkspawn.black.cooking.Cuisine;
import darkspawn.black.forsaken.Forsaken;
import darkspawn.black.health.BossHearts;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class JournalGameTest {
    private static ServerPlayer player(GameTestHelper helper) {
        var level=helper.getLevel();
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"journal-test"),false);
        var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        var pos=helper.absolutePos(new BlockPos(2,2,2)).atY(240);player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        return player;
    }
    private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player);player.discard(); }
    private static JournalSnapshotPayload.Entry entry(ServerPlayer player,String id) {
        return Journal.snapshot(player).entries().stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
    }
    @GameTest public void oldProgressAppearsWithoutGrantingKillsOrChangingRecipes(GameTestHelper helper) {
        var player=player(helper);
        try {
            helper.assertTrue(!entry(player,"entity/ancient_tree_spirit").discovered(),"New player starts with boss hint only");
            BossHearts.progress(player).consume(BossKind.ANCIENT_TREE_SPIRIT);Forsaken.state(player).researched=true;
            var name=Cuisine.SCROLLS.keySet().stream().filter(n->!Cuisine.canCook(player,n)).findFirst().orElseThrow();
            var locked=entry(player,"recipe/darkspawn:"+name);
            helper.assertTrue(!locked.discovered()&&!locked.body().contains("Cooking Station ingredients"),"Unknown recipes do not leak ingredients");
            Culinary.progress(player).unlock(name);
            helper.assertTrue(entry(player,"recipe/darkspawn:"+name).discovered(),"Existing recipe knowledge carries over");
            var boss=entry(player,"entity/ancient_tree_spirit");
            helper.assertTrue(boss.discovered()&&boss.body().contains("Credited kills: 0 | Heart consumed: yes"),"Consumed hearts reveal boss but do not invent a defeat");
            helper.assertTrue(entry(player,"forsaken/equipment").discovered(),"Existing research unlocks expedition guidance");
            helper.assertTrue(Journal.progress(player).saved().isEmpty(),"Read-only snapshot never duplicates existing progress");
            helper.assertTrue(Journal.snapshot(player).entries().stream().filter(e->e.section().equals("cooking")).noneMatch(e->e.title().startsWith("item.darkspawn.")),"Dedicated server resolves readable mod recipe titles");
        } finally { remove(player); } helper.succeed();
    }
    @GameTest public void saveAndDeathCopyKeepPersonalDiscoveries(GameTestHelper helper) {
        var player=player(helper);var other=player(helper);
        try {
            Journal.progress(player).discover("entity/leopard");Journal.progress(player).discover("landmark/druid_shrine");
            helper.assertTrue(!Journal.progress(other).knows("entity/leopard"),"Discoveries are personal");
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());player.saveWithoutId(out);
            var loaded=new ServerPlayer(player.level().getServer(),player.level(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"journal-load"),net.minecraft.server.level.ClientInformation.createDefault());
            loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),out.buildResult()));
            helper.assertTrue(Journal.progress(loaded).saved().equals(Journal.progress(player).saved()),"Player save/load restores discoveries");
            net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(player,other,false);
            helper.assertTrue(Journal.progress(other).saved().equals(Journal.progress(player).saved()),"Death copy retains discoveries");
        } finally { remove(player);remove(other); }helper.succeed();
    }
    @GameTest public void observationRequiresSightAndRangeWithoutLoadingChunks(GameTestHelper helper) {
        var player=player(helper);var level=player.level();var pos=player.blockPosition();
        for(var at:BlockPos.betweenClosed(pos.offset(-5,-1,-5),pos.offset(5,4,5)))level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        var seen=darkspawn.black.mount.Mounts.LEOPARD.create(level,EntitySpawnReason.COMMAND);
        var hidden=darkspawn.black.mount.Mounts.GIANT_SCORPION.create(level,EntitySpawnReason.COMMAND);
        seen.setPos(Vec3.atBottomCenterOf(pos.east(3)));hidden.setPos(Vec3.atBottomCenterOf(pos.west(3)));
        seen.setNoAi(true);hidden.setNoAi(true);seen.setNoGravity(true);hidden.setNoGravity(true);level.addFreshEntity(seen);level.addFreshEntity(hidden);
        for(var at:BlockPos.betweenClosed(pos.offset(-1,0,-2),pos.offset(-1,4,2)))level.setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(5,()->{
            try {
                int chunks=level.getChunkSource().getLoadedChunksCount();Journal.observe(player);
                helper.assertTrue(Journal.progress(player).knows("entity/leopard")&&!Journal.progress(player).knows("entity/giant_scorpion"),"Visible neighbor unlocks; wall-hidden creature does not");
                helper.assertTrue(chunks==level.getChunkSource().getLoadedChunksCount(),"Observation does not load terrain");
                Journal.progress(player).restore(java.util.List.of());player.teleportTo(pos.getX()+.5,pos.getY()+30,pos.getZ()+.5);Journal.observe(player);
                helper.assertTrue(!Journal.progress(player).knows("entity/leopard"),"Discovery range is three-dimensional");
            } finally {seen.discard();hidden.discard();remove(player);}helper.succeed();
        });
    }
    @GameTest public void nativeLandmarkVisitUnlocksAssociatedBossWithoutSearchingWorld(GameTestHelper helper) {
        var player=player(helper);var level=player.level();var origin=player.blockPosition().offset(-2,-2,-2);
        var chunk=level.getChunk(origin);var structure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getValue(Darkspawn.id("druid_shrine"));
        var piece=new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(),Darkspawn.id("landmarks/druid_shrine_0"),origin,net.minecraft.world.level.block.Rotation.NONE);
        var start=new net.minecraft.world.level.levelgen.structure.StructureStart(structure,chunk.getPos(),0,new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        var prior=chunk.getAllStarts().get(structure);var visitedChunk=level.getChunk(player.blockPosition());var refs=new java.util.HashMap<>(visitedChunk.getAllReferences());
        chunk.setStartForStructure(structure,start);visitedChunk.addReferenceForStructure(structure,chunk.getPos().pack());
        try {
            Journal.observe(player);
            helper.assertTrue(Journal.progress(player).knows("landmark/druid_shrine"),"Entering native bounds records landmark family");
            helper.assertTrue(entry(player,"entity/ancient_tree_spirit").discovered(),"Visited ritual site reveals its boss clues");
            helper.assertTrue(BossHearts.progress(player).savedBosses().isEmpty(),"Discovery grants no boss reward");
        } finally {chunk.setStartForStructure(structure,prior==null?net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START:prior);visitedChunk.setAllReferences(refs);remove(player);}helper.succeed();
    }
}
