package darkspawn.black.mount;

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

public final class GriffinGameTest {
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
	private static Griffin griffin(GameTestHelper helper) {
		var griffin = new Griffin(Mounts.GRIFFIN, helper.getLevel());
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
	private static void mount(Griffin griffin, ServerPlayer player) {
        // Movement fixtures begin owned; lifecycle tests exercise actual feeding separately.
        var ownership = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, griffin.level().registryAccess());
        griffin.saveWithoutId(ownership);
        ownership.store("MountOwner", net.minecraft.core.UUIDUtil.CODEC, player.getUUID());
        griffin.load(TagValueInput.create(ProblemReporter.DISCARDING, griffin.level().registryAccess(), ownership.buildResult()));
		player.teleportTo(griffin.getX(), griffin.getY(), griffin.getZ());
		griffin.mobInteract(player, InteractionHand.MAIN_HAND);
		griffin.positionRider(player);
	}
	@GameTest(skyAccess = true)
	public void onlyCurrentRiderCanControlAndOnlyOnePlayerCanMount(GameTestHelper helper) {
		var griffin = griffin(helper);
		var owner = player(helper);
		var stranger = player(helper);
		try {
			mount(griffin, owner);
			helper.assertTrue(owner.getVehicle() == griffin, "Empty-hand interaction mounts griffin");
			helper.assertTrue(!stranger.startRiding(griffin), "Second rider rejected");
			helper.assertTrue(!griffin.acceptControls(stranger, Griffin.ASCEND), "Non-rider input rejected");
			helper.assertTrue(!griffin.acceptControls(owner, -1) && !griffin.acceptControls(owner, 256), "Unknown control bits rejected");
			helper.assertTrue(griffin.acceptControls(owner, Griffin.ASCEND), "Current rider accepted");
			owner.stopRiding();
			helper.assertTrue(!griffin.acceptControls(owner, Griffin.ASCEND), "Former rider rejected");
			helper.assertTrue(!griffin.isClientAuthoritative(), "Movement remains server authoritative");
		} finally { cleanup(owner); cleanup(stranger); griffin.discard(); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void actualServerTicksApplyControlsThenExpireThem(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		mount(griffin, rider);
		double ground = griffin.getY();
		griffin.acceptControls(rider, Griffin.ASCEND);
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(griffin.flying() && griffin.getY() > ground + 1, "Normal server ticks execute rider ascent");
		});
		helper.runAfterDelay(16, () -> {
			try {
				helper.assertTrue(griffin.getRiddenInput(rider, Vec3.ZERO).y < 0, "Lost input expires and requests descent");
				helper.assertTrue(griffin.getDeltaMovement().y < 0, "Normal server ticks descend after timeout");
			} finally { cleanup(rider); griffin.discard(); }
			helper.succeed();
		});
	}
	@GameTest(skyAccess = true)
	public void lowRoofPreventsMounting(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		try {
			helper.getLevel().setBlockAndUpdate(griffin.blockPosition().above(2), Blocks.STONE.defaultBlockState());
			mount(griffin, rider);
			helper.assertTrue(!rider.isPassenger(), "Cannot mount into a low roof");
		} finally { cleanup(rider); griffin.discard(); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void takeoffHoverDescendAndLandWithoutFallDamage(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		try {
			mount(griffin, rider);
			double ground = griffin.getY();
			for (int i = 0; i < 12; i++) { griffin.travel(new Vec3(0, 1, 0)); griffin.positionRider(rider); }
			helper.assertTrue(griffin.flying() && griffin.getY() > ground + 2, "Takeoff gains height");
			for (int i = 0; i < 20; i++) { griffin.travel(Vec3.ZERO); griffin.positionRider(rider); }
			helper.assertTrue(Math.abs(griffin.getDeltaMovement().y) < .001, "Released controls settle into hover");
			for (int i = 0; i < 40; i++) { griffin.travel(new Vec3(0, -1, 0)); griffin.positionRider(rider); }
			helper.assertTrue(!griffin.flying() && griffin.onGround(), "Descent lands and exits flight");
			helper.assertTrue(Math.abs(griffin.getY() - ground) < .01 && griffin.getHealth() == 40, "Landing preserves position and health");
		} finally { cleanup(rider); griffin.discard(); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void flightRespectsWallsAndRiderHeadroom(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		try {
			mount(griffin, rider);
			var start = griffin.blockPosition();
			for (int x = -2; x <= 2; x++) for (int y = 0; y <= 8; y++) {
				helper.getLevel().setBlockAndUpdate(start.offset(x, y, 3), Blocks.STONE.defaultBlockState());
			}
			for (int i = 0; i < 20; i++) { griffin.travel(new Vec3(0, .2, 1)); griffin.positionRider(rider); }
			helper.assertTrue(griffin.getBoundingBox().maxZ <= start.getZ() + 3.001, "Winged mount cannot pass through wall");
			// Put a roof above the rider, where the griffin itself still has clearance.
			int roofY = (int) Math.ceil(rider.getBoundingBox().maxY);
			for (int x = -2; x <= 2; x++) for (int z = -2; z <= 3; z++) {
				helper.getLevel().setBlockAndUpdate(new BlockPos(start.getX() + x, roofY, start.getZ() + z), Blocks.STONE.defaultBlockState());
			}
			for (int i = 0; i < 20; i++) { griffin.travel(new Vec3(0, 1, 0)); griffin.positionRider(rider); }
			helper.assertTrue(rider.getBoundingBox().maxY <= roofY + .001, "Rider's head stays below roof");
		} finally { cleanup(rider); griffin.discard(); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void dismountFindsFloorAndMidairExitGrantsSlowFalling(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		try {
			mount(griffin, rider);
			var safe = griffin.getDismountLocationForPassenger(rider);
			helper.assertTrue(Math.abs(safe.y - griffin.getY()) < .01, "Grounded dismount finds floor");
			helper.assertTrue(!rider.hasEffect(MobEffects.SLOW_FALLING), "Grounded dismount gives no effect");
			griffin.setPos(griffin.position().add(0, 20, 0));
			griffin.setOnGround(false);
			griffin.positionRider(rider);
			rider.stopRiding();
			helper.assertTrue(rider.hasEffect(MobEffects.SLOW_FALLING), "Actual airborne dismount protects rider");
		} finally { cleanup(rider); griffin.discard(); }
		helper.succeed();
	}
	@GameTest(skyAccess = true)
	public void saveReloadAndMissingControlsCannotLeaveAnUnpilotedFlyer(GameTestHelper helper) {
		var griffin = griffin(helper);
		var rider = player(helper);
		Griffin restored = null;
		try {
			mount(griffin, rider);
			griffin.acceptControls(rider, Griffin.FORWARD | Griffin.ASCEND);
			for (int i = 0; i < 12; i++) { griffin.travel(new Vec3(0, 1, 0)); griffin.positionRider(rider); }
			griffin.setHealth(27);
			var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			griffin.saveWithoutId(saved);
			restored = new Griffin(Mounts.GRIFFIN, helper.getLevel());
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved.buildResult()));
			helper.assertTrue(restored.flying() && restored.getHealth() == 27 && restored.isPersistenceRequired(), "Save retains flight, health and persistence");
			helper.assertTrue(restored.getRiddenInput(rider, Vec3.ZERO).equals(new Vec3(0, -1, 0)), "Reload discards stale pilot input");
			restored.setOnGround(false);
			restored.setDeltaMovement(Vec3.ZERO);
			double before = restored.getY();
			restored.travel(Vec3.ZERO);
			helper.assertTrue(restored.getY() < before, "Unpiloted griffin descends after reload");
		} finally { cleanup(rider); griffin.discard(); if (restored != null) restored.discard(); }
		helper.succeed();
	}
}
