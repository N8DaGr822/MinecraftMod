package darkspawn.black.cooking;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

public final class HusbandryFishingGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "husbandry-test"), false);
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
		player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
		player.setOnGround(true);
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }

    @GameTest public void animalHarvestCostsCooldownsAndNextEggPersist(GameTestHelper h){
        var p=player(h);try{
            var goat=EntityTypes.GOAT.create(h.getLevel(),EntitySpawnReason.COMMAND);goat.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            h.assertTrue(AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND),"Adult goat supplies specialty milk");h.assertTrue(!AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND)&&p.getMainHandItem().getCount()==1&&p.getInventory().countItem(AnimalUtility.GOAT_MILK)==1,"Cooldown prevents repeat milk and charges nothing");
            var chicken=EntityTypes.CHICKEN.create(h.getLevel(),EntitySpawnReason.COMMAND);chicken.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PUMPKIN_SEEDS,2));h.assertTrue(AnimalUtility.interact(p,chicken,InteractionHand.MAIN_HAND),"Feed queues the next egg");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());chicken.saveWithoutId(out);var restored=EntityTypes.CHICKEN.create(h.getLevel(),EntitySpawnReason.LOAD);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));h.assertTrue(AnimalUtility.state(restored).egg==1,"Feed choice survives native entity reload");
            restored.eggTime=1;restored.aiStep();h.assertTrue(AnimalUtility.state(restored).egg==0&&restored.eggTime>=6000,"One specialty egg consumes the pending choice and restarts the natural laying timer");
            goat.setBaby(true);AnimalUtility.state(goat).readyAt=0;h.assertTrue(!AnimalUtility.interact(p,goat,InteractionHand.MAIN_HAND),"Baby animals do not supply specialty harvests");
        }finally{remove(p);}h.succeed();
    }
    @GameTest public void pigHuntAndMooshroomHarvestAreFinite(GameTestHelper h){
        var p=player(h);var level=h.getLevel();var patch=h.absolutePos(new BlockPos(2,1,2));
        try{
            level.setBlockAndUpdate(patch.below(),Blocks.PODZOL.defaultBlockState());var pig=EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND);pig.setPos(Vec3.atCenterOf(patch));var state=AnimalUtility.state(pig);state.forage=patch;state.forageUntil=level.getGameTime()+400;pig.tickCount=20;
            var before=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,pig.getBoundingBox().inflate(3),e->e.getItem().is(AnimalUtility.TRUFFLE)).size();AnimalUtility.tick(pig);AnimalUtility.tick(pig);
            h.assertTrue(state.forage==null&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,pig.getBoundingBox().inflate(3),e->e.getItem().is(AnimalUtility.TRUFFLE)).size()==before+1,"Reachable patch awards exactly one truffle");h.assertTrue(level.getBlockState(patch.below()).is(Blocks.PODZOL),"Foraging preserves terrain");
            var cow=EntityTypes.MOOSHROOM.create(level,EntitySpawnReason.COMMAND);cow.setPos(p.position());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL,2));h.assertTrue(AnimalUtility.interact(p,cow,InteractionHand.MAIN_HAND)&&!AnimalUtility.interact(p,cow,InteractionHand.MAIN_HAND),"Mooshroom specialty harvest is cooldown limited");h.assertTrue(p.getInventory().countItem(AnimalUtility.FUNGAL_CREAM)==1&&p.getMainHandItem().getCount()==1,"Cream costs one bowl once");
        }finally{level.setBlockAndUpdate(patch.below(),Blocks.AIR.defaultBlockState());remove(p);}h.succeed();
    }
}
