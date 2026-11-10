package darkspawn.black.mount;

import darkspawn.black.groundwalker.*;

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

public final class GroundWalkerGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "walker-test"), false);
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
	private static GroundWalker walker(GameTestHelper helper) {
		var walker = new GroundWalker(GroundWalkerMod.GROUND_WALKER, helper.getLevel());
		var origin = helper.absolutePos(new BlockPos(2, 3, 2));
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
			helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			for (int y = 0; y < 10; y++) helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		walker.setPos(Vec3.atBottomCenterOf(origin));
		walker.setOnGround(true);
		helper.getLevel().addFreshEntity(walker);
		return walker;
	}
	private static void mount(GroundWalker walker, ServerPlayer player) {
        // Movement fixtures begin owned; placement and repair tests exercise crafted-machine interactions.
        var ownership = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, walker.level().registryAccess());
        walker.saveWithoutId(ownership);
        ownership.store("MountOwner", net.minecraft.core.UUIDUtil.CODEC, player.getUUID());
        walker.load(TagValueInput.create(ProblemReporter.DISCARDING, walker.level().registryAccess(), ownership.buildResult()));
		player.teleportTo(walker.getX(), walker.getY(), walker.getZ());
		walker.mobInteract(player, InteractionHand.MAIN_HAND);
		walker.positionRider(player);
	}

    @GameTest(skyAccess=true)
    public void capsuleAndDefeatRecoveryKeepOneIdentity(GameTestHelper h) {
        var mech=walker(h); var owner=player(h); String token=""; var id=mech.getUUID();
        try {
            mech.assignOwner(owner.getUUID());owner.teleportTo(mech.getX()+3,mech.getY(),mech.getZ());var pos=mech.blockPosition();
            var capsule=new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE);
            mech.setOnGround(false);
            h.assertTrue(!MountCompanions.capture(owner,mech,capsule),"Airborne capture refused");mech.setOnGround(true);
            h.assertTrue(MountCompanions.capture(owner,mech,capsule),"Landed capture works");token=MountCompanions.token(capsule);var copy=capsule.copy();
            h.assertTrue(MountCompanions.release(owner,capsule,pos),"Capsule restores walker");
            var restored=(GroundWalker)h.getLevel().getEntity(id);
            h.assertTrue(restored!=null && restored.isOwnedBy(owner) && !MountCompanions.release(owner,copy,pos),"Identity and owner persist; duplicate claim fails");
            var island=h.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,darkspawn.black.core.DarkspawnCore.id("ascension")));
            h.assertTrue(!restored.canTeleport(h.getLevel(),island) && !MountCompanions.safePlacement(island,restored),"Teleport and capsule placement exclude island profiles");
            restored.die(h.getLevel().damageSources().generic());
            var claim=MountRegistry.get(h.getLevel()).entries().entrySet().stream().filter(e->e.getValue().mountId().equals(id)).findFirst().orElseThrow();token=claim.getKey();
            h.assertTrue(claim.getValue().recovery(),"Defeat creates one recovery record");
            net.minecraft.world.item.ItemStack ticket=net.minecraft.world.item.ItemStack.EMPTY;
            for(int slot=0;slot<owner.getInventory().getContainerSize();slot++)if(MountCompanions.token(owner.getInventory().getItem(slot)).equals(claim.getKey()))ticket=owner.getInventory().getItem(slot);
            h.assertTrue(!MountCompanions.release(owner,ticket,pos),"Defeat recovery requires a kit");
            owner.getInventory().add(new net.minecraft.world.item.ItemStack(MountCompanions.VET_KIT));
            h.assertTrue(MountCompanions.release(owner,ticket,pos),"Kit restores defeated machine");
            restored=(GroundWalker)h.getLevel().getEntity(id);
            h.assertTrue(restored.getHealth()==80 && restored.isOwnedBy(owner),"Recovery restores health and owner");restored.discard();
        } finally {if(!token.isEmpty())MountRegistry.get(h.getLevel()).remove(token);var live=h.getLevel().getEntity(id);if(live!=null)live.discard();cleanup(owner);mech.discard();}
        h.succeed();
    }
}
