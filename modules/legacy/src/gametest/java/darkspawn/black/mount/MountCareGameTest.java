package darkspawn.black.mount;

import darkspawn.black.noah.NoahMod;

import darkspawn.black.creaturemounts.CreatureMounts;

import darkspawn.black.creaturemounts.Seahorse;
import darkspawn.black.mountkit.PrototypeMount;
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

public final class MountCareGameTest {
	private static ServerPlayer player(GameTestHelper helper) { return player(helper, UUID.randomUUID()); }
	private static ServerPlayer player(GameTestHelper helper, UUID id) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "mount-test"), false);
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
        // Test Isolation: Keep live companions outside habitat tests' 96-block population checks.
        var origin=helper.absolutePos(new BlockPos(3,3,3)).atY(260);
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            helper.getLevel().setBlockAndUpdate(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<16;y++) helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var mount=type.create(helper.getLevel(),EntitySpawnReason.COMMAND);
        mount.setPos(Vec3.atBottomCenterOf(origin)); mount.setOnGround(true);
        helper.getLevel().addFreshEntity(mount); return mount;
    }

    private static void own(PrototypeMount mount, ServerPlayer owner) {
        mount.assignOwner(owner.getUUID()); owner.teleportTo(mount.getX()+3, mount.getY(), mount.getZ());
    }
    private static net.minecraft.nbt.CompoundTag save(PrototypeMount mount) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mount.level().registryAccess());
        mount.saveWithoutId(output); return output.buildResult();
    }
    private static void load(PrototypeMount mount, net.minecraft.nbt.CompoundTag tag) {
        mount.load(TagValueInput.create(ProblemReporter.DISCARDING, mount.level().registryAccess(), tag));
    }
    @GameTest(skyAccess=true)
    public void aquaticCapsulesRequireWaterAndRespectOneBlockBoundary(GameTestHelper helper) {
        var owner=player(helper);var mount=spawn(helper,CreatureMounts.SEAHORSE);String ticket="";
        try {
            own(mount,owner);var pos=mount.blockPosition();var uuid=mount.getUUID();var capsule=new net.minecraft.world.item.ItemStack(MountCompanions.CAPSULE);
            helper.assertTrue(MountCompanions.capture(owner,mount,capsule),"Seahorse stores normally");ticket=MountCompanions.token(capsule);
            helper.assertTrue(!MountCompanions.release(owner,capsule,pos),"Seahorse cannot be released on land");
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<4;y++)helper.getLevel().setBlockAndUpdate(pos.offset(x,y,z),Blocks.WATER.defaultBlockState());
            var registry=MountRegistry.get(helper.getLevel());var saved=registry.get(ticket);
            var foreign=registry.store(new MountRegistry.Entry(saved.owner(),saved.mountId(),saved.type(),saved.entity(),false,true));
            var copy=capsule.copy();var tag=new net.minecraft.nbt.CompoundTag();tag.putString("MountToken",foreign);
            copy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            helper.assertTrue(!MountCompanions.release(owner,copy,pos),"OneBlock snapshot cannot export to Overworld");registry.remove(foreign);
            helper.assertTrue(MountCompanions.release(owner,capsule,pos),"Fully submerged safe placement succeeds");
            var restored=(PrototypeMount)helper.getLevel().getEntity(uuid);helper.assertTrue(restored!=null,"Same aquatic identity restored");restored.discard();
        }finally{if(!ticket.isEmpty())MountRegistry.get(helper.getLevel()).remove(ticket);mount.discard();cleanup(owner);}
        helper.succeed();
    }
    @GameTest public void genericCareMenuRejectsUniqueCompanions(GameTestHelper helper){
        var p=player(helper);var m=NoahMod.THALASSAR.create(p.level(),EntitySpawnReason.COMMAND);
        try{
            m.bond(p);m.setPos(p.position());p.level().addFreshEntity(m);
            var menu=new MountMenu(1,p.getInventory(),m);
            helper.assertTrue(!menu.stillValid(p)&&!menu.clickMenuButton(p,MountMenu.FOLLOW),"Generic care menu rejects unique companions even if constructed directly");
        }finally{p.closeContainer();m.discard();cleanup(p);}helper.succeed();
    }
}
