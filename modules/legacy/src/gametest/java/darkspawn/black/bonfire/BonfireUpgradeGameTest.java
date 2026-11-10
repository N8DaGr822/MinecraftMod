package darkspawn.black.bonfire;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class BonfireUpgradeGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "bonfire-test"), false);
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
	@GameTest public void bonfireUpgradesChargeInOrderAndStopAtFinalTier(GameTestHelper helper) {
		var player = player(helper); var level=helper.getLevel();
		var pos=helper.absolutePos(new BlockPos(3,3,3));
		var core=Bonfires.CORE;
		var range=BonfireBlock.RANGE;
		var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
		try {
			level.setBlockAndUpdate(pos,core.defaultBlockState()); player.setShiftKeyDown(true);
			level.getBlockState(pos).useWithoutItem(level,player,hit);
			helper.assertTrue(level.getBlockState(pos).getValue(range)==1,"Missing payment leaves starter tier intact");
			var materials=new net.minecraft.world.item.Item[]{Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND};
			int[] costs={8,4,2};
			for(int i=0;i<3;i++) {
				player.getInventory().setItem(9,new ItemStack(materials[i],costs[i]+1));
				level.getBlockState(pos).useWithoutItem(level,player,hit);
				helper.assertTrue(level.getBlockState(pos).getValue(range)==i+2 && player.getInventory().getItem(9).getCount()==1,"Upgrade consumes exactly its cost");
			}
			level.getBlockState(pos).useWithoutItem(level,player,hit);
			helper.assertTrue(level.getBlockState(pos).getValue(range)==4 && player.getInventory().getItem(9).getCount()==1,"Final tier neither wraps nor charges");
		} finally { level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); remove(player); }
		helper.succeed();
	}
}
