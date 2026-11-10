package darkspawn.black.mount;

import darkspawn.black.creaturemounts.CreatureMounts;

import darkspawn.black.creaturemounts.Cerberus;
import darkspawn.black.creaturemounts.DragonMount;
import darkspawn.black.creaturemounts.GiantScorpion;
import darkspawn.black.creaturemounts.Leopard;
import darkspawn.black.creaturemounts.PoptartCat;
import darkspawn.black.creaturemounts.SkyCatSpawning;
import darkspawn.black.mountkit.PrototypeMount;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class TravelMountGameTest {
    private static final List<EntityType<? extends PrototypeMount>> TYPES = List.of(
        CreatureMounts.WOOLLY_RHINO, CreatureMounts.FLYING_SQUIRREL, CreatureMounts.POPTART_CAT, CreatureMounts.DRAGON_MOUNT,
        CreatureMounts.WINGED_UNICORN, CreatureMounts.RIDING_GOLEM, CreatureMounts.SASQUATCH, CreatureMounts.CERBERUS,
        CreatureMounts.LEOPARD, CreatureMounts.SEAHORSE, CreatureMounts.GIANT_SCORPION, CreatureMounts.CROCODILE, CreatureMounts.GIANT_MOLE);
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "mount-test"), false);
		var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			// Mirror the local rider's authority flag to catch mounts accidentally inheriting client ownership.
			@Override protected boolean isLocalClientAuthoritative() { return true; }
		};
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

    private static PrototypeMount spawn(GameTestHelper helper, EntityType<? extends PrototypeMount> type) {
        var origin=helper.absolutePos(new BlockPos(3,3,3));
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            helper.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<16;y++) helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var mount=type.create(helper.getLevel(),EntitySpawnReason.COMMAND);
        mount.setPos(Vec3.atBottomCenterOf(origin)); mount.setOnGround(true);
        helper.getLevel().addFreshEntity(mount); return mount;
    }
    private static void ride(PrototypeMount mount, ServerPlayer rider) {
        // Movement fixtures begin owned; lifecycle tests exercise actual feeding separately.
        var ownership = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mount.level().registryAccess());
        mount.saveWithoutId(ownership);
        ownership.store("MountOwner", net.minecraft.core.UUIDUtil.CODEC, rider.getUUID());
        mount.load(TagValueInput.create(ProblemReporter.DISCARDING, mount.level().registryAccess(), ownership.buildResult()));
        rider.teleportTo(mount.getX(),mount.getY(),mount.getZ());
        mount.mobInteract(rider,InteractionHand.MAIN_HAND); mount.positionRider(rider);
    }
    @GameTest(skyAccess=true)
    public void combatRespectsWallsFacingRangeAndPeacefulWildlife(GameTestHelper helper) {
        var rider = player(helper);
        try {
            for (var type : List.of(CreatureMounts.DRAGON_MOUNT, CreatureMounts.GIANT_SCORPION, CreatureMounts.CROCODILE)) {
                for (boolean wall : List.of(false, true)) {
                    var mount = spawn(helper, type);
                    var front = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, mount.position().add(0, 0, 2.5));
                    var rear = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, mount.position().add(0, 0, -2.5));
                    var far = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, mount.position().add(0, 0, 10));
                    var friendly = combatTarget(helper, darkspawn.black.ecosystem.RegionalEcosystems.MOBS.get(
                        darkspawn.black.ecosystem.RegionalKind.STORM_FINCH), mount.position().add(0, .5, 1.5));
                    try {
                        ride(mount, rider); rider.setYRot(0); rider.setXRot(0);
                        if (wall) for (int x = -3; x <= 3; x++) for (int y = 0; y <= 4; y++)
                            helper.getLevel().setBlockAndUpdate(mount.blockPosition().offset(x, y, 1), Blocks.STONE.defaultBlockState());
                        mount.acceptControls(rider, PrototypeMount.ABILITY);
                        mount.combat.tick(helper.getLevel());
                        helper.assertTrue(wall ? front.getHealth() == 20 : front.getHealth() < 20, "Wall blocks attack: " + type);
                        helper.assertTrue(rear.getHealth() == 20 && far.getHealth() == 20, "Facing and range respected: " + type);
                        helper.assertTrue(friendly.getHealth() == friendly.getMaxHealth(), "Peaceful regional Monster is protected: " + type);
                        helper.assertTrue(rider.getHealth() == rider.getMaxHealth(), "Owner is protected");
                        helper.assertTrue(helper.getLevel().getBlockState(mount.blockPosition().below()).is(Blocks.STONE), "Attack does not destroy terrain");
                    } finally { rider.stopRiding(); mount.discard(); front.discard(); rear.discard(); far.discard(); friendly.discard(); }
                }
            }
        } finally { cleanup(rider); }
        helper.succeed();
    }
    private static net.minecraft.world.entity.Mob combatTarget(GameTestHelper helper,
            EntityType<? extends net.minecraft.world.entity.Mob> type, Vec3 position) {
        var target = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        target.setPos(position);
        target.setNoAi(true);
        helper.getLevel().addFreshEntity(target);
        return target;
    }
}
