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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class TravelMountGameTest {
    private static final List<EntityType<? extends PrototypeMount>> TYPES = List.of(
        Mounts.WOOLLY_RHINO, Mounts.FLYING_SQUIRREL, Mounts.POPTART_CAT, Mounts.DRAGON_MOUNT,
        Mounts.WINGED_UNICORN, Mounts.RIDING_GOLEM, Mounts.SASQUATCH, Mounts.CERBERUS);
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "mount-test"), false);
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
        rider.teleportTo(mount.getX(),mount.getY(),mount.getZ());
        mount.mobInteract(rider,InteractionHand.MAIN_HAND); mount.positionRider(rider);
    }
    @GameTest(skyAccess=true)
    public void everyPrototypeRidesRejectsOtherPilotsAndPreservesIdentity(GameTestHelper helper) {
        var rider=player(helper); var stranger=player(helper);
        try {
            for(var type:TYPES) {
                var mount=spawn(helper,type);
                try {
                    ride(mount,rider);
                    helper.assertTrue(rider.getVehicle()==mount,"Empty-hand ride: "+type);
                    helper.assertTrue(!stranger.startRiding(mount),"Reject second rider: "+type);
                    helper.assertTrue(!mount.acceptControls(stranger,PrototypeMount.FORWARD),"Reject stranger controls: "+type);
                    helper.assertTrue(mount.acceptControls(rider,PrototypeMount.FORWARD),"Accept rider: "+type);
                    mount.setCustomName(Component.literal("Trail companion")); mount.setHealth(12);
                    var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,helper.getLevel().registryAccess());
                    mount.saveWithoutId(saved);
                    var restored=type.create(helper.getLevel(),EntitySpawnReason.LOAD);
                    restored.load(TagValueInput.create(ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved.buildResult()));
                    helper.assertTrue(restored.getHealth()==12 && restored.isPersistenceRequired() && restored.getCustomName().getString().equals("Trail companion"),"Retain health/name/persistence: "+type);
                    helper.assertTrue(restored.getRiddenInput(rider,Vec3.ZERO).x==0 && restored.getRiddenInput(rider,Vec3.ZERO).z==0,"No stale input after reload: "+type);
                    restored.discard();
                    rider.stopRiding();
                    helper.assertTrue(!mount.acceptControls(rider,PrototypeMount.FORWARD),"Reject former rider: "+type);
                } finally { rider.stopRiding(); mount.discard(); }
            }
        } finally { cleanup(rider);cleanup(stranger); }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void newFlyersTakeOffHoverAndLand(GameTestHelper helper) {
        var rider=player(helper);
        try {
            for(var type:List.of(Mounts.POPTART_CAT,Mounts.DRAGON_MOUNT,Mounts.WINGED_UNICORN)) {
                var mount=spawn(helper,type);
                try {
                    ride(mount,rider); double ground=mount.getY();
                    for(int i=0;i<12;i++){mount.travel(new Vec3(0,1,0));mount.positionRider(rider);}
                    helper.assertTrue(mount.flying() && mount.getY()>ground+2,"Flyer takes off: "+type);
                    for(int i=0;i<20;i++){mount.travel(Vec3.ZERO);mount.positionRider(rider);}
                    helper.assertTrue(Math.abs(mount.getDeltaMovement().y)<.001,"Flyer hovers: "+type);
                    for(int i=0;i<40;i++){mount.travel(new Vec3(0,-1,0));mount.positionRider(rider);}
                    helper.assertTrue(!mount.flying() && mount.onGround(),"Flyer lands: "+type);
                } finally {rider.stopRiding();mount.discard();}
            }
        } finally {cleanup(rider);}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void squirrelGlidesWithoutClimbingOrHoveringAndLands(GameTestHelper helper) {
        var rider=player(helper);var mount=spawn(helper,Mounts.FLYING_SQUIRREL);
        try {
            ride(mount,rider);double ground=mount.getY();
            mount.setPos(mount.position().add(0,5,0));mount.setOnGround(false);mount.positionRider(rider);
            double previous=mount.getY();
            for(int i=0;i<20;i++) {
                mount.travel(new Vec3(0,1,0));mount.positionRider(rider);
                helper.assertTrue(mount.getY()<previous,"Holding jump never gains altitude while gliding");previous=mount.getY();
            }
            helper.assertTrue(mount.flying() && mount.getY()>ground,"Glider remains airborne during gentle descent");
            for(int i=0;i<30 && !mount.onGround();i++){mount.travel(new Vec3(0,-1,0));mount.positionRider(rider);}
            helper.assertTrue(mount.onGround() && !mount.flying() && mount.getHealth()==24,
                "Dive lands safely: ground="+mount.onGround()+", gliding="+mount.flying()+", health="+mount.getHealth()+", y="+mount.getY()+", floor="+ground);
            mount.setPos(mount.position().add(0,1,0));mount.setOnGround(false);mount.positionRider(rider);
            for(int i=0;i<30 && !mount.onGround();i++){mount.travel(new Vec3(0,1,0));mount.positionRider(rider);}
            helper.assertTrue(mount.onGround() && !mount.flying(),"Holding jump during touchdown still exits gliding");
        } finally {cleanup(rider);mount.discard();}
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void groundMountsCannotFlyAndHeadroomBlocksEveryMount(GameTestHelper helper) {
        var rider=player(helper);
        try {
            for(var type:TYPES) {
                var mount=spawn(helper,type);
                try {
                    int roofY=(int)Math.floor(mount.getPassengerRidingPosition(rider).y+1);
                    helper.getLevel().setBlockAndUpdate(new BlockPos(mount.blockPosition().getX(),roofY,mount.blockPosition().getZ()),Blocks.STONE.defaultBlockState());
                    ride(mount,rider);helper.assertTrue(!rider.isPassenger(),"Low roof rejects mounting: "+type);
                    helper.getLevel().setBlockAndUpdate(new BlockPos(mount.blockPosition().getX(),roofY,mount.blockPosition().getZ()),Blocks.AIR.defaultBlockState());
                    ride(mount,rider);
                    if(!mount.canFly() && !mount.canGlide()) {
                        mount.setPos(mount.position().add(0,3,0));mount.setOnGround(false);mount.positionRider(rider);
                        mount.travel(new Vec3(0,1,0));
                        helper.assertTrue(!mount.flying() && !mount.isNoGravity(),"Ground mount retains gravity: "+type);
                    }
                } finally {rider.stopRiding();mount.discard();}
            }
        } finally {cleanup(rider);}
        helper.succeed();
    }
}
