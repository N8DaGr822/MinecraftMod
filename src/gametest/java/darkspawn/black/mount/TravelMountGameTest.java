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
        Mounts.WOOLLY_RHINO, Mounts.FLYING_SQUIRREL, Mounts.POPTART_CAT, Mounts.DRAGON_MOUNT,
        Mounts.WINGED_UNICORN, Mounts.RIDING_GOLEM, Mounts.SASQUATCH, Mounts.CERBERUS,
        Mounts.LEOPARD, Mounts.SEAHORSE, Mounts.GIANT_SCORPION, Mounts.CROCODILE, Mounts.GIANT_MOLE);
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
    @GameTest(skyAccess=true, maxTicks=120)
    public void allSpeciesTameWithThreeFeedingsAndKeepExclusiveOwners(GameTestHelper helper) {
        var owner = player(helper);
        var stranger = player(helper);
        var level = helper.getLevel();
        var mounts = new java.util.ArrayList<PrototypeMount>();
        var types = new java.util.ArrayList<EntityType<? extends PrototypeMount>>(TYPES);
        types.add(Mounts.GRIFFIN);
        // Lifecycle fixtures need no world tick movement; feeding still uses the real world's clock.
        var floor = spawn(helper, Mounts.GRIFFIN);
        var position = floor.position();
        floor.discard();
        for (var type : types) {
            var mount = type.create(level, EntitySpawnReason.NATURAL);
            mount.setPos(position);
            mount.finalizeSpawn(level, level.getCurrentDifficultyAt(mount.blockPosition()), EntitySpawnReason.NATURAL, null);
            if (mount instanceof PoptartCat cat) cat.setWildSkyCat();
            mounts.add(mount);
            helper.assertTrue(!owner.startRiding(mount), "Wild mount rejects a rider: " + type);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(owner.getMainHandItem().getCount() == 1 && !mount.isTamed(), "Wrong food changes nothing: " + type);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood(), 3));
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(owner.getMainHandItem().getCount() == 2 && !mount.isTamed(), "First feeding builds trust: " + type);
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            mount.saveWithoutId(saved);
            mount.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(owner.getMainHandItem().getCount() == 2, "Reload cannot bypass the feeding cooldown: " + type);
        }
        helper.runAfterDelay(40, () -> {
            for (var mount : mounts) {
                stranger.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood(), 3));
                mount.mobInteract(stranger, InteractionHand.MAIN_HAND);
                helper.assertTrue(stranger.getMainHandItem().getCount() == 3, "Another player cannot steal active trust");
                owner.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood(), 2));
                mount.mobInteract(owner, InteractionHand.MAIN_HAND);
                helper.assertTrue(!mount.isTamed() && owner.getMainHandItem().getCount() == 1, "Second feeding still needs one more");
            }
        });
        helper.runAfterDelay(80, () -> {
            try {
                for (var mount : mounts) {
                    owner.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood()));
                    mount.mobInteract(owner, InteractionHand.MAIN_HAND);
                    helper.assertTrue(mount.isOwnedBy(owner) && mount.isPersistenceRequired() && owner.getMainHandItem().isEmpty(), "Third feeding claims and preserves: " + mount.getType());
                    var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                    mount.saveWithoutId(saved);
                    mount.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
                    helper.assertTrue(mount.isOwnedBy(owner) && !stranger.startRiding(mount), "Ownership survives reload and rejects strangers");
                    owner.teleportTo(position.x, position.y, position.z);
                    mount.mobInteract(owner, InteractionHand.MAIN_HAND);
                    helper.assertTrue(owner.getVehicle() == mount, "Owner rides with no saddle: " + mount.getType());
                    owner.stopRiding();
                    stranger.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood()));
                    mount.mobInteract(stranger, InteractionHand.MAIN_HAND);
                    helper.assertTrue(mount.isOwnedBy(owner) && stranger.getMainHandItem().getCount() == 1, "Feeding cannot overwrite ownership");
                }
            } finally { cleanup(owner); cleanup(stranger); mounts.forEach(PrototypeMount::discard); }
            helper.succeed();
        });
    }
    @GameTest(skyAccess=true)
    public void ownerFoodHealsWithoutWasteAndLegacyMountsRemainUntamed(GameTestHelper helper) {
        var owner = player(helper);
        var mount = spawn(helper, Mounts.WOOLLY_RHINO);
        try {
            var legacy = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            mount.saveWithoutId(legacy);
            mount.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy.buildResult()));
            helper.assertTrue(!mount.isTamed() && !owner.startRiding(mount), "Existing ownerless mount must be tamed, not deleted or claimed free");
            ride(mount, owner);
            owner.stopRiding();
            owner.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(mount.tamingFood(), 2));
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(owner.getMainHandItem().getCount() == 2, "Full-health mount consumes no food");
            mount.setHealth(10);
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(mount.getHealth() == 14 && owner.getMainHandItem().getCount() == 1, "Owner food heals two hearts and consumes one item");
            mount.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(mount.getHealth() == 14 && owner.getMainHandItem().getCount() == 1, "Healing obeys the same cooldown");
        } finally { cleanup(owner); mount.discard(); }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void skyCatAcceptsMovementAndVanillaShiftDismount(GameTestHelper helper) {
        var cat = spawn(helper, Mounts.POPTART_CAT);
        var rider = player(helper);
        ride(cat, rider);
        double ground = cat.getY();
        double startZ = cat.getZ();
        rider.setYRot(0);
        cat.acceptControls(rider, PrototypeMount.FORWARD | PrototypeMount.ASCEND);
        helper.runAfterDelay(10, () -> {
            try {
                helper.assertTrue(cat.getY() > ground + 2 && cat.getZ() > startZ + 1, "Normal ticks move the ridden cat forward and upward");
                rider.connection.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
                rider.rideTick();
                helper.assertTrue(!rider.isPassenger() && !cat.isVehicle(), "Vanilla Shift packet releases the rider");
            } finally { cleanup(rider); cat.discard(); }
            helper.succeed();
        });
    }
    @GameTest(skyAccess=true)
    public void skyCatSpawnsOnlyInClearUpperAirAndBecomesPersistentWhenRidden(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(3, 3, 3));
        var sky = new BlockPos(origin.getX(), level.getMaxY() - 20, origin.getZ());
        boolean previous = level.getGameRules().get(GameRules.SPAWN_MOBS);
        var rider = player(helper);
        PoptartCat cat = null;
        try {
            level.getGameRules().set(GameRules.SPAWN_MOBS, false, level.getServer());
            helper.assertTrue(!SkyCatSpawning.trySpawn(level, sky), "Disabled mob spawning prevents sky encounters");
            level.getGameRules().set(GameRules.SPAWN_MOBS, true, level.getServer());
            helper.assertTrue(!SkyCatSpawning.trySpawn(level, origin), "Surface altitude is not a sky-cat habitat");
            level.setBlockAndUpdate(sky, Blocks.STONE.defaultBlockState());
            helper.assertTrue(!SkyCatSpawning.trySpawn(level, sky), "Occupied sky position is rejected");
            level.setBlockAndUpdate(sky, Blocks.AIR.defaultBlockState());
            helper.assertTrue(SkyCatSpawning.trySpawn(level, sky), "Clear upper air permits a natural cat");
            cat = level.getEntitiesOfClass(PoptartCat.class, new AABB(sky).inflate(3)).getFirst();
            helper.assertTrue(!cat.isPersistenceRequired(), "Unclaimed wild cat uses ordinary distance despawning");
            helper.assertTrue(!SkyCatSpawning.trySpawn(level, sky.offset(8, 0, 0)), "Local cap prevents a second cat");
            double y = cat.getY();
            for (int i = 0; i < 20; i++) cat.travel(Vec3.ZERO);
            helper.assertTrue(cat.flying() && Math.abs(cat.getY() - y) < .01, "Wild cat hovers at its sky habitat");
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            cat.saveWithoutId(saved);
            var restored = Mounts.POPTART_CAT.create(level, EntitySpawnReason.LOAD);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
            helper.assertTrue(!restored.isPersistenceRequired() && restored.flying(), "Wild hover identity survives reload");
            restored.discard();
            ride(cat, rider);
            cat.tick();
            helper.assertTrue(rider.getVehicle() == cat && cat.isPersistenceRequired(), "First ride keeps the companion");
            rider.stopRiding();
            helper.assertTrue(cat.isPersistenceRequired(), "Dismount does not make the cat despawnable again");
            cat.travel(Vec3.ZERO);
            helper.assertTrue(cat.getDeltaMovement().y < 0, "Claimed cat descends when left unoccupied");
        } finally {
            cleanup(rider);
            if (cat != null) cat.discard();
            level.setBlockAndUpdate(sky, Blocks.AIR.defaultBlockState());
            level.getGameRules().set(GameRules.SPAWN_MOBS, previous, level.getServer());
        }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void everyPrototypeRidesRejectsOtherPilotsAndPreservesIdentity(GameTestHelper helper) {
        var rider=player(helper); var stranger=player(helper);
        try {
            for(var type:TYPES) {
                var mount=spawn(helper,type);
                try {
                    mount.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(mount.blockPosition()), EntitySpawnReason.NATURAL, null);
                    helper.assertTrue(!mount.isPersistenceRequired(), "Unclaimed natural encounter counts toward creature caps: " + type);
                    var wildSave = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
                    mount.saveWithoutId(wildSave);
                    mount.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), wildSave.buildResult()));
                    helper.assertTrue(!mount.isPersistenceRequired(), "Wild encounter remains despawnable after reload: " + type);
                    ride(mount,rider);
                    helper.assertTrue(rider.getVehicle()==mount,"Empty-hand ride: "+type);
                    helper.assertTrue(mount.isPersistenceRequired(), "First rider preserves the mount: " + type);
                    helper.assertTrue(!mount.isClientAuthoritative() && !mount.isLocalClientAuthoritative(), "Both sides agree the server owns mount movement: " + type);
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
    public void moleDiggingRequiresRiderIntentAndPreservesProtectedBlocks(GameTestHelper helper) {
        var rider = player(helper);
        var mole = spawn(helper, Mounts.GIANT_MOLE);
        var level = helper.getLevel();
        try {
            ride(mole, rider);
            mole.setYRot(0);
            var origin = mole.blockPosition();
            var wall = origin.offset(0, 0, 1);
            var headroom = wall.above(2);
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(headroom, Blocks.NETHERRACK.defaultBlockState());
            mole.travel(new Vec3(0, 0, 1));
            helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Movement alone never excavates");
            mole.acceptControls(rider, PrototypeMount.DIG);
            mole.travel(Vec3.ZERO);
            helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Dig key while stationary never excavates");
            var bedrock = wall.offset(-1, 0, 0);
            var chest = wall.offset(1, 0, 0);
            level.setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            mole.acceptControls(rider, PrototypeMount.DIG | PrototypeMount.FORWARD);
            mole.travel(new Vec3(0, 0, 1));
            helper.assertTrue(level.getBlockState(wall).isAir() && level.getBlockState(headroom).isAir(), "Digging clears body and rider headroom");
            helper.assertTrue(level.getBlockState(wall.below()).is(Blocks.STONE), "Tunneling preserves supported floor");
            helper.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK) && level.getBlockState(chest).is(Blocks.CHEST), "Unbreakable blocks and containers survive");
            helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(wall).inflate(2),
                item -> item.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)).isEmpty(), "Stone drops cobblestone with diamond-tier claws");
        } finally { cleanup(rider); mole.discard(); }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void moleHonorsAdventureModeAndExpiredDigControls(GameTestHelper helper) {
        var rider = player(helper);
        var mole = spawn(helper, Mounts.GIANT_MOLE);
        var wall = mole.blockPosition().offset(0, 0, 1);
        var level = helper.getLevel();
        ride(mole, rider);
        mole.setYRot(0);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        rider.setGameMode(GameType.ADVENTURE);
        mole.acceptControls(rider, PrototypeMount.DIG | PrototypeMount.FORWARD);
        mole.travel(new Vec3(0, 0, 1));
        helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Adventure mode cannot excavate by riding");
        // Stop normal-tick movement while the input timeout elapses, then directly probe the digging guard.
        mole.setNoAi(true);
        helper.runAfterDelay(12, () -> {
            try {
                rider.setGameMode(GameType.SURVIVAL);
                mole.travel(new Vec3(0, 0, 1));
                helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Expired digging input cannot destroy blocks");
                rider.stopRiding();
                mole.travel(new Vec3(0, 0, 1));
                helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Unoccupied mole never digs");
            } finally { cleanup(rider); mole.discard(); }
            helper.succeed();
        });
    }
    @GameTest(skyAccess=true)
    public void swimmersControlDepthAndLeopardHasStrongerJump(GameTestHelper helper) {
        var rider = player(helper);
        try {
            for (var type : List.of(Mounts.SEAHORSE, Mounts.CROCODILE)) {
                var mount = spawn(helper, type);
                try {
                    var origin = mount.blockPosition();
                    for (var pos : BlockPos.betweenClosed(origin.offset(-3, 0, -3), origin.offset(3, 8, 3)))
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                    mount.setPos(mount.position().add(0, 2, 0));
                    mount.tick();
                    ride(mount, rider);
                    helper.assertTrue(rider.getVehicle() == mount && mount.isInWater(), "Aquatic rider stays mounted underwater: " + type);
                    double y = mount.getY();
                    for (int i = 0; i < 8; i++) mount.travel(new Vec3(0, 1, 0));
                    helper.assertTrue(mount.getY() > y + 1 && !mount.flying() && mount.canBreatheUnderwater(), "Space swims upward without flight: " + type);
                    y = mount.getY();
                    for (int i = 0; i < 12; i++) mount.travel(new Vec3(0, -1, 0));
                    helper.assertTrue(mount.getY() < y - 1, "Descend swims downward: " + type);
                    rider.stopRiding();
                    helper.assertTrue(!mount.isVehicle(), "Underwater dismount releases rider: " + type);
                } finally { rider.stopRiding(); mount.discard(); }
            }
            var leopard = spawn(helper, Mounts.LEOPARD);
            var rhino = Mounts.WOOLLY_RHINO.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            try {
                helper.assertTrue(leopard.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)
                    > rhino.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH), "Leopard has a stronger grounded jump");
            } finally { leopard.discard(); rhino.discard(); }
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
    @GameTest(skyAccess=true)
    public void combatAttacksApplyEffectsAndPersistCooldowns(GameTestHelper helper) {
        var rider = player(helper);
        var stranger = player(helper);
        try {
            for (var type : List.of(Mounts.DRAGON_MOUNT, Mounts.GIANT_SCORPION, Mounts.CROCODILE)) {
                var mount = spawn(helper, type);
                var target = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, mount.position().add(0, 0, 2.5));
                try {
                    ride(mount, rider); rider.setYRot(0); rider.setXRot(0);
                    helper.assertTrue(!mount.acceptControls(stranger, PrototypeMount.ABILITY), "Stranger cannot attack");
                    mount.combat.tick(helper.getLevel());
                    helper.assertTrue(target.getHealth() == 20, "No accepted attack input means no damage");
                    mount.acceptControls(rider, PrototypeMount.ABILITY);
                    mount.combat.tick(helper.getLevel());
                    float remaining = mount instanceof GiantScorpion ? 16 : 12;
                    helper.assertTrue(target.getHealth() == remaining, "Species attack damage: " + type);
                    helper.assertTrue(mount.attackPose(0) == 1 && mount.attackPose(.5F) < 1,
                        "Accepted attack starts synchronized, interpolated recovery: " + type);
                    helper.assertTrue(mount instanceof DragonMount ? target.isOnFire()
                        : target.hasEffect(mount instanceof GiantScorpion ? MobEffects.POISON : MobEffects.SLOWNESS), "Species effect: " + type);
                    target.setInvulnerableTime(0);
                    var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mount.level().registryAccess());
                    mount.saveWithoutId(saved);
                    mount.load(TagValueInput.create(ProblemReporter.DISCARDING, mount.level().registryAccess(), saved.buildResult()));
                    helper.assertTrue(mount.attackPose(0) == 0, "Reload clears animation without restarting the attack");
                    mount.acceptControls(rider, PrototypeMount.ABILITY);
                    mount.combat.tick(helper.getLevel());
                    helper.assertTrue(target.getHealth() == remaining, "Reload cannot bypass cooldown: " + type);
                } finally { rider.stopRiding(); mount.discard(); target.discard(); }
            }
        } finally { cleanup(rider); cleanup(stranger); }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void combatRespectsWallsFacingRangeAndPeacefulWildlife(GameTestHelper helper) {
        var rider = player(helper);
        try {
            for (var type : List.of(Mounts.DRAGON_MOUNT, Mounts.GIANT_SCORPION, Mounts.CROCODILE)) {
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
    @GameTest(skyAccess=true)
    public void rhinoChargeRequiresMovementHitsOnceAndStopsOnDismount(GameTestHelper helper) {
        var rider = player(helper);
        var mount = spawn(helper, Mounts.WOOLLY_RHINO);
        var target = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, mount.position().add(0, 0, 1.5));
        try {
            ride(mount, rider); rider.setYRot(0); mount.setYRot(0);
            mount.acceptControls(rider, PrototypeMount.ABILITY);
            mount.combat.tick(helper.getLevel());
            helper.assertTrue(!mount.combat.charging(), "Stationary input does not start a charge");
            mount.acceptControls(rider, PrototypeMount.FORWARD | PrototypeMount.ABILITY);
            mount.combat.tick(helper.getLevel());
            helper.assertTrue(mount.combat.charging() && mount.groundSpeed() > .3F, "Charge increases ground speed");
            mount.combat.tick(helper.getLevel());
            helper.assertTrue(target.getHealth() == 20, "No actual movement means no charge damage");
            for (int i = 0; i < 6; i++) {
                target.setInvulnerableTime(0);
                mount.setOnGround(true);
                // Direct travel fixtures supply the speed normally assigned by LivingEntity's ridden tick.
                mount.setSpeed(mount.groundSpeed());
                mount.setDeltaMovement(mount.getDeltaMovement().add(0, -.08, 0));
                mount.travel(new Vec3(0, 0, 1));
                mount.positionRider(rider);
                mount.combat.tick(helper.getLevel());
            }
            helper.assertTrue(target.getHealth() == 10, "One hit per charge even with damage immunity reset; health=" + target.getHealth());
            helper.assertTrue(target.getDeltaMovement().horizontalDistanceSqr() > 0, "Charge knocks target back");
            rider.stopRiding();
            helper.assertTrue(!mount.combat.charging() && mount.groundSpeed() == .3F, "Dismount removes charge speed immediately");
            rider.startRiding(mount);
            mount.acceptControls(rider, PrototypeMount.FORWARD | PrototypeMount.ABILITY);
            mount.combat.tick(helper.getLevel());
            helper.assertTrue(!mount.combat.charging(), "Remount cannot bypass charge cooldown");
        } finally { cleanup(rider); mount.discard(); target.discard(); }
        helper.succeed();
    }
    @GameTest(skyAccess=true)
    public void cerberusProtectsOnlyMountedOwnerFromFireAndLava(GameTestHelper helper) {
        var rider = player(helper);
        var mount = spawn(helper, Mounts.CERBERUS);
        try {
            ride(mount, rider);
            float health = rider.getHealth();
            helper.assertTrue(mount.fireImmune() && !mount.hurtServer(helper.getLevel(), mount.damageSources().lava(), 4), "Cerberus is lava immune");
            helper.assertTrue(!rider.hurtServer(helper.getLevel(), rider.damageSources().lava(), 4)
                && !rider.hurtServer(helper.getLevel(), rider.damageSources().onFire(), 4), "Mounted owner is fire and lava immune");
            helper.assertTrue(rider.getHealth() == health, "Protection prevents health loss");
            helper.assertTrue(rider.hurtServer(helper.getLevel(), rider.damageSources().generic(), 2), "Non-fire damage still applies");
            rider.stopRiding(); rider.setInvulnerableTime(0);
            helper.assertTrue(rider.hurtServer(helper.getLevel(), rider.damageSources().lava(), 4), "Dismounted rider is vulnerable immediately");
            rider.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
            rider.startRiding(mount); mount.tick(); rider.stopRiding();
            helper.assertTrue(rider.hasEffect(MobEffects.FIRE_RESISTANCE), "Mount protection preserves existing potion effects");
        } finally { cleanup(rider); mount.discard(); }
        helper.succeed();
    }
    @GameTest(skyAccess=true, maxTicks=60)
    public void combatUsesNormalRiddenTicksAndExpiresStaleInput(GameTestHelper helper) {
        var rider = player(helper);
        var mount = spawn(helper, Mounts.WOOLLY_RHINO);
        Vec3 start = mount.position();
        for (int z = 5; z <= 24; z++) for (int x = -2; x <= 2; x++)
            helper.getLevel().setBlockAndUpdate(mount.blockPosition().offset(x, -1, z), Blocks.STONE.defaultBlockState());
        var target = combatTarget(helper, net.minecraft.world.entity.EntityTypes.CREEPER, start.add(0, 0, 2));
        ride(mount, rider); rider.setYRot(0); rider.setXRot(0);
        mount.acceptControls(rider, PrototypeMount.FORWARD | PrototypeMount.ABILITY);
        for (int tick = 1; tick <= 6; tick++) helper.runAfterDelay(tick,
            () -> mount.acceptControls(rider, PrototypeMount.FORWARD | PrototypeMount.ABILITY));
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(mount.getZ() > start.z + 1 && target.getHealth() == 10,
                "Normal riding ticks move and damage once; distance=" + (mount.getZ() - start.z) + ", health=" + target.getHealth());
        });
        helper.runAfterDelay(18, () -> {
            try {
                helper.assertTrue(!mount.combat.charging(), "Stale input ends charge before its twenty-tick duration");
                helper.assertTrue(mount.attackPose(0) == 0, "Charge cancellation clears the synchronized pose");
                helper.succeed();
            } finally { cleanup(rider); mount.discard(); target.discard(); }
        });
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
