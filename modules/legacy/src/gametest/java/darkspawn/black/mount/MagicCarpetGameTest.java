package darkspawn.black.mount;

import darkspawn.black.magiccarpet.MagicCarpetMod;

import darkspawn.black.magiccarpet.MagicCarpet;

import darkspawn.black.magiccarpet.MagicCarpet;
import darkspawn.black.magiccarpet.MagicCarpetItem;
import darkspawn.black.mountkit.MountCompanions;
import darkspawn.black.mountkit.MountMenu;
import darkspawn.black.mountkit.MountRegistry;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class MagicCarpetGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "griffin-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}
	private static void cleanup(ServerPlayer player) {
		player.stopRiding();
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}
	private static MagicCarpet griffin(GameTestHelper helper) {
		var griffin = new MagicCarpet(MagicCarpetMod.MAGIC_CARPET, helper.getLevel());
		var origin = helper.absolutePos(new BlockPos(2, 3, 2));
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
			helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			for (int y = 0; y < 10; y++) helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		griffin.setPos(Vec3.atBottomCenterOf(origin));
		griffin.setOnGround(true);
		helper.getLevel().addFreshEntity(griffin);
		return griffin;
	}
	private static void mount(MagicCarpet griffin, ServerPlayer player) {
        // Movement fixtures begin owned; lifecycle tests exercise actual feeding separately.
        var ownership = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, griffin.level().registryAccess());
        griffin.saveWithoutId(ownership);
        ownership.store("MountOwner", net.minecraft.core.UUIDUtil.CODEC, player.getUUID());
        griffin.load(TagValueInput.create(ProblemReporter.DISCARDING, griffin.level().registryAccess(), ownership.buildResult()));
		player.teleportTo(griffin.getX(), griffin.getY(), griffin.getZ());
		griffin.mobInteract(player, InteractionHand.MAIN_HAND);
		griffin.positionRider(player);
	}
    @GameTest(skyAccess=true)
    public void capsuleRoundTripPreservesOwnerAndRejectsDuplicateRelease(GameTestHelper helper) {
        var rider=player(helper); var carpet=griffin(helper); String token="";
        try {
            carpet.assignOwner(rider.getUUID()); rider.teleportTo(carpet.getX()+3,carpet.getY(),carpet.getZ());
            var id=carpet.getUUID(); var pos=carpet.blockPosition();
            carpet.care.cargo.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD,5));
            var capsule=new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE);
            helper.assertTrue(MountCompanions.capture(rider,carpet,capsule),"Existing capsule safely stores the carpet"); token=MountCompanions.token(capsule); var duplicate=capsule.copy();
            helper.assertTrue(MountCompanions.release(rider,capsule,pos),"Stored carpet releases on safe ground");
            var released=(MagicCarpet)helper.getLevel().getEntity(id);
            helper.assertTrue(released!=null && released.isOwnedBy(rider) && released.care.cargo.getItem(0).getCount()==5,"Identity, owner and cargo survive storage");
            helper.assertTrue(!MountCompanions.release(rider,duplicate,pos),"Copied claim cannot duplicate the carpet");
            var ascension=helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,darkspawn.black.Darkspawn.id("ascension")));
            helper.assertTrue(!MagicCarpet.allowedLevel(ascension),"Prototype excludes island dimensions");
            released.discard();
        } finally { if(!token.isEmpty()) MountRegistry.get(helper.getLevel()).remove(token); cleanup(rider); carpet.discard(); }
        helper.succeed();
    }
}
