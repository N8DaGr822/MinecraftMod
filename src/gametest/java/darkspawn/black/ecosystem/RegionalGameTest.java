package darkspawn.black.ecosystem;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.boss.BossBolt;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class RegionalGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "region-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = origin(helper); player.teleportTo(pos.x, pos.y, pos.z);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}
	private static void remove(ServerPlayer player) { player.level().getServer().getPlayerList().remove(player); player.discard(); }
	private static Vec3 origin(GameTestHelper helper) { return Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))); }
	private static RegionalMob mob(GameTestHelper helper, RegionalKind kind, Vec3 pos) {
		var mob = new RegionalMob(RegionalEcosystems.MOBS.get(kind), helper.getLevel(), kind);
		mob.setPos(pos); helper.getLevel().addFreshEntity(mob); return mob;
	}
	private static void tick(GameTestHelper helper, RegionalMob mob, int ticks) {
		for (int i = 0; i < ticks && !mob.isRemoved(); i++) { mob.commonTick(); mob.customServerAiStep(helper.getLevel()); }
	}
	private static CompoundTag save(GameTestHelper helper, RegionalMob mob) {
		var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		mob.saveWithoutId(out); return out.buildResult();
	}
	private static void load(GameTestHelper helper, RegionalMob mob, CompoundTag saved) {
		mob.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
	}
	private static void floor(GameTestHelper helper, Vec3 center, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				BlockPos pos = BlockPos.containing(center).offset(x, 0, z);
				helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
				for (int y = 0; y < 6; y++) { helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState()); }
			}
		}
	}
	private static void cleanup(GameTestHelper helper) {
		var box = new net.minecraft.world.phys.AABB(origin(helper), origin(helper)).inflate(40);
		helper.getLevel().getEntitiesOfClass(RegionalMob.class, box).forEach(RegionalMob::discard);
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
		helper.getLevel().getEntitiesOfClass(BossBolt.class, box).forEach(BossBolt::discard);
	}

	@GameTest(skyAccess = true)
	public void allRegionalCreaturesRunNormalAiAndRoundTripSavedState(GameTestHelper helper) {
		Vec3 pos = origin(helper); floor(helper, pos, 5);
		try {
			for (var kind : RegionalKind.values()) {
				RegionalMob mob = mob(helper, kind, pos); mob.setNoGravity(true);
				for (int i = 0; i < 8; i++) { mob.commonTick(); mob.tick(); }
				helper.assertTrue(mob.isAlive(), "Creature must survive ordinary initialization: " + kind);
				helper.assertTrue(mob.getMaxHealth() == kind.health, "Registered attributes must match " + kind);
				load(helper, mob, save(helper, mob));
				helper.assertTrue(mob.kind() == kind && mob.warning() == 0, "Reload must preserve identity and cancel warnings: " + kind);
				mob.discard();
			}
		} finally { cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void eachHeraldDropsOnlyItsDiscoveryAndHelpersNeverPayRewards(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			for (var kind : RegionalKind.values()) {
				if (!kind.herald() || kind == RegionalKind.TYRANT_SKULL || kind == RegionalKind.BABAS_FAMILIAR) { continue; }
				RegionalMob mob = mob(helper, kind, player.position().add(3, 0, 0));
				var area = mob.getBoundingBox().inflate(3);
				mob.dropCustomDeathLoot(helper.getLevel(), player.damageSources().playerAttack(player), false);
				helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "Environmental death must not yield herald reward: " + kind);
				mob.dropCustomDeathLoot(helper.getLevel(), player.damageSources().playerAttack(player), true);
				var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
				helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(RegionalEcosystems.REWARDS.get(kind)), "Only the discovery material is earned: " + kind);
				drops.forEach(ItemEntity::discard);
				var saved = save(helper, mob); saved.putBoolean("darkspawn_helper", true); saved.putLong("darkspawn_expires", helper.getLevel().getGameTime() + 600);
				load(helper, mob, saved);
				mob.dropCustomDeathLoot(helper.getLevel(), player.damageSources().playerAttack(player), true);
				helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty() && mob.getExperienceReward(helper.getLevel(), player) == 0,
						"Saved helper must grant no items or XP: " + kind);
				mob.discard();
			}
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void telegraphedSlamCanBeDodgedAndReloadRequiresAnotherWarning(GameTestHelper helper) {
		ServerPlayer player = player(helper); floor(helper, player.position(), 6);
		RegionalMob brute = mob(helper, RegionalKind.BRUTE_ZOMBIE, player.position().add(3, 0, 0));
		try {
			tick(helper, brute, 40); float health = player.getHealth();
			helper.assertTrue(brute.beginAttack(player) && brute.warning() == 20, "Slam must begin with one-second warning");
			helper.assertTrue(player.getHealth() == health, "Warning must not deal instant damage");
			Vec3 mark = player.position(); player.teleportTo(mark.x, mark.y, mark.z + 5);
			tick(helper, brute, 20);
			helper.assertTrue(player.getHealth() == health, "Moving outside the fixed marker must dodge");
			tick(helper, brute, 70); player.teleportTo(mark.x, mark.y, mark.z);
			helper.assertTrue(brute.beginAttack(player), "Next slam can start after cooldown");
			load(helper, brute, save(helper, brute)); tick(helper, brute, 25);
			helper.assertTrue(player.getHealth() == health && brute.warning() == 0, "Reload must cancel pending damage");
			tick(helper, brute, 20); helper.assertTrue(brute.beginAttack(player), "Reload allows a fresh warned attack");
			tick(helper, brute, 20); helper.assertTrue(player.getHealth() < health, "Standing in the warned area must take damage");
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void poisonFrostAndHexRequireSuccessfulBites(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			for (var kind : List.of(RegionalKind.TREE_VIPER, RegionalKind.FOSSIL_SCORPION, RegionalKind.ICEFANG, RegionalKind.HEXED_FROG)) {
				RegionalMob mob = mob(helper, kind, player.position().add(1, 0, 0));
				player.removeAllEffects(); player.setHealth(20); player.damageCooldownTime = 0;
				helper.assertTrue(mob.doHurtTarget(helper.getLevel(), player), "Bite should hit: " + kind);
				helper.assertTrue(player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.SLOWNESS) || player.hasEffect(MobEffects.WEAKNESS), "Bite must apply regional effect");
				player.removeAllEffects(); player.getAbilities().invulnerable = true;
				helper.assertTrue(!mob.doHurtTarget(helper.getLevel(), player) && player.getActiveEffects().isEmpty(), "Rejected bite must not apply effects");
				player.getAbilities().invulnerable = false; mob.discard();
			}
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void summonWavesAreFiniteSavedAndExpireWithoutTheirOwner(GameTestHelper helper) {
		ServerPlayer player = player(helper); floor(helper, player.position(), 7);
		try {
			for (var kind : List.of(RegionalKind.SOUL_KEEPER, RegionalKind.VOID_SENTINEL)) {
				RegionalMob herald = mob(helper, kind, player.position().add(5, 0, 0));
				herald.getRandom().setSeed(7); herald.setTarget(player); tick(helper, herald, 1);
				var helpers = helper.getLevel().getEntitiesOfClass(RegionalMob.class, herald.getBoundingBox().inflate(15), RegionalMob::helper);
				helper.assertTrue(helpers.size() == 2, "Herald must call at most two placed helpers: " + kind + " got " + helpers.size());
				load(helper, herald, save(helper, herald)); herald.setTarget(player); tick(helper, herald, 121);
				helper.assertTrue(helper.getLevel().getEntitiesOfClass(RegionalMob.class, herald.getBoundingBox().inflate(15), RegionalMob::helper).size() == 2, "Reload must not reset lifetime wave limit");
				for (var child : helpers) { load(helper, child, save(helper, child)); }
				herald.discard(); for (var child : helpers) { tick(helper, child, 41); helper.assertTrue(child.isRemoved(), "Orphan helpers expire after load grace"); }
				cleanup(helper);
			}
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void broodHatchingAndLarvaGrowthCannotMultiplyTemporaryMinions(GameTestHelper helper) {
		Vec3 pos = origin(helper); floor(helper, pos, 5);
		try {
			RegionalMob egg = mob(helper, RegionalKind.SHADOW_EGG, pos); egg.getRandom().setSeed(7);
			egg.die(egg.damageSources().generic());
			var larvae = helper.getLevel().getEntitiesOfClass(RegionalMob.class, egg.getBoundingBox().inflate(10), RegionalMob::helper);
			helper.assertTrue(larvae.size() == 2, "Egg should hatch two bounded larvae");
			for (var larva : larvae) {
				var saved = save(helper, larva); saved.putInt("darkspawn_growth", 1200); load(helper, larva, saved); tick(helper, larva, 1);
				helper.assertTrue(larva.kind() == RegionalKind.SHADOW_LARVA && !larva.isRemoved(), "Temporary larvae cannot mature");
				saved.putLong("darkspawn_expires", helper.getLevel().getGameTime() - 1); load(helper, larva, saved); tick(helper, larva, 1);
				helper.assertTrue(larva.isRemoved(), "Saved temporary expiry must be enforced");
			}
			egg.discard();
			RegionalMob natural = mob(helper, RegionalKind.SHADOW_LARVA, pos);
			var grown = save(helper, natural); grown.putInt("darkspawn_growth", 1199); load(helper, natural, grown); tick(helper, natural, 1);
			helper.assertTrue(natural.isRemoved() && helper.getLevel().getEntitiesOfClass(RegionalMob.class,
					new net.minecraft.world.phys.AABB(pos, pos).inflate(4), m -> m.kind() == RegionalKind.SHADOW_DRONE).size() == 1, "Natural larva must mature once");
		} finally { cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void bonewalkerRevivesOnceEvenAcrossReload(GameTestHelper helper) {
		RegionalMob walker = mob(helper, RegionalKind.BONEWALKER, origin(helper));
		try {
			walker.hurtServer(helper.getLevel(), walker.damageSources().generic(), 100);
			helper.assertTrue(walker.isAlive() && walker.getHealth() == 14, "First lethal hit must reconstruct at half health");
			load(helper, walker, save(helper, walker)); walker.damageCooldownTime = 0;
			walker.hurtServer(helper.getLevel(), walker.damageSources().generic(), 100);
			helper.assertTrue(!walker.isAlive(), "Reload cannot restore a spent reconstruction");
		} finally { cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void soulPreviewNeverStacksAndExpiresWithoutChangingPermanentHealth(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			float base = player.getMaxHealth(); EcosystemEffects.fracture(player, false);
			helper.assertTrue(player.getMaxHealth() == base - 1, "Lost Soul temporarily borrows half a heart");
			EcosystemEffects.fracture(player, true); helper.assertTrue(player.getMaxHealth() == base - 1, "Repeated previews cannot stack");
			for (int i = 0; i < 101; i++) { player.commonTick(); player.baseTick(); }
			helper.assertTrue(player.getMaxHealth() == base && !player.hasEffect(EcosystemEffects.FADING_SOUL), "Natural expiry must restore full maximum");
			EcosystemEffects.fracture(player, true); helper.assertTrue(player.getMaxHealth() == base - 2, "Keeper previews one heart");
			player.removeAllEffects(); helper.assertTrue(player.getMaxHealth() == base, "Cleansing must restore only temporary capacity");
		} finally { remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void fossilExcavationIsPlayerDrivenAndCannotBeClaimedTwice(GameTestHelper helper) {
		ServerPlayer player = player(helper); RegionalMob skull = mob(helper, RegionalKind.TYRANT_SKULL, player.position().add(3, 0, 0));
		try {
			helper.assertTrue(!skull.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 100), "Fossil discovery is not a combat reward");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
			skull.mobInteract(player, InteractionHand.MAIN_HAND); skull.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(skull.isRemoved() && player.getInventory().countItem(RegionalEcosystems.REWARDS.get(RegionalKind.TYRANT_SKULL)) == 1,
					"Repeated excavation must yield exactly one fossil");
			helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Accepted excavation costs one durability");
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void familiarFollowingRewardIsSavedAndBelongsToItsFollower(GameTestHelper helper) {
		ServerPlayer player = player(helper); ServerPlayer other = player(helper);
		RegionalMob familiar = mob(helper, RegionalKind.BABAS_FAMILIAR, player.position().add(1, 0, 0));
		try {
			var saved = save(helper, familiar);
			var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
			out.store("darkspawn_guide_destination", Vec3.CODEC, familiar.position());
			out.store("darkspawn_guide_start", Vec3.CODEC, player.position().add(10, 0, 0));
			saved.merge(out.buildResult()); saved.putString("darkspawn_guide_player", player.getUUID().toString());
			load(helper, familiar, saved); tick(helper, familiar, 10);
			var token = RegionalEcosystems.REWARDS.get(RegionalKind.BABAS_FAMILIAR);
			helper.assertTrue(familiar.claimed() && player.getInventory().countItem(token) == 1 && other.getInventory().countItem(token) == 0,
					"Following discovery must reward only the saved follower");
			load(helper, familiar, save(helper, familiar)); tick(helper, familiar, 20);
			helper.assertTrue(player.getInventory().countItem(token) == 1, "Reload cannot repeat the discovery");
			familiar.dropCustomDeathLoot(helper.getLevel(), player.damageSources().playerAttack(player), true);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, familiar.getBoundingBox().inflate(4)).isEmpty(), "Killing the familiar must not grant its token");
		} finally { cleanup(helper); remove(player); remove(other); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void scavengedItemsRemainSavedAndReturnWithoutDuplication(GameTestHelper helper) {
		Vec3 pos = origin(helper); RegionalMob bogling = mob(helper, RegionalKind.BOGLING, pos);
		var dropsRule = net.minecraft.world.level.gamerules.GameRules.MOB_DROPS;
		boolean mobDrops = helper.getLevel().getGameRules().get(dropsRule);
		ItemEntity food = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.APPLE, 3));
		helper.getLevel().addFreshEntity(food);
		try {
			tick(helper, bogling, 10); helper.assertTrue(food.getItem().getCount() == 2, "Bogling may take one loose food item");
			load(helper, bogling, save(helper, bogling)); tick(helper, bogling, 30);
			helper.assertTrue(food.getItem().getCount() == 2, "Loaded carried item prevents a second theft");
			helper.getLevel().getGameRules().set(dropsRule, false, helper.getLevel().getServer());
			bogling.die(bogling.damageSources().generic());
			int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bogling.getBoundingBox().inflate(4)).stream()
					.filter(item -> item.getItem().is(Items.APPLE)).mapToInt(item -> item.getItem().getCount()).sum();
			helper.assertTrue(count == 3, "Death returns the original food even when ordinary mob drops are disabled");
		} finally { helper.getLevel().getGameRules().set(dropsRule, mobDrops, helper.getLevel().getServer()); cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void webAndDarkPatchesWarnExpireAndNeverReplaceBlocks(GameTestHelper helper) {
		ServerPlayer player = player(helper); floor(helper, player.position(), 4);
		try {
			for (var kind : List.of(RegionalKind.WEB_SPITTER, RegionalKind.SHADOW_SPITTER)) {
				RegionalMob caster = mob(helper, kind, player.position().add(3, 0, 0));
				caster.onProjectileHit(player); tick(helper, caster, 19);
				helper.assertTrue(player.getActiveEffects().isEmpty(), "Patch must warn before applying its first effect");
				tick(helper, caster, 1);
				helper.assertTrue(player.hasEffect(kind == RegionalKind.WEB_SPITTER ? MobEffects.SLOWNESS : MobEffects.DARKNESS), "Active patch applies its regional effect");
				tick(helper, caster, 81); player.removeAllEffects(); tick(helper, caster, 40);
				helper.assertTrue(player.getActiveEffects().isEmpty(), "Expired patch cannot keep refreshing effects");
				helper.assertTrue(helper.getLevel().getBlockState(player.blockPosition()).isAir(), "Temporary web/shadow must preserve blocks");
				caster.discard();
			}
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void mushroomRegenerationRequiresMyceliumAndClimbingUsesCollision(GameTestHelper helper) {
		Vec3 pos = origin(helper); floor(helper, pos, 4);
		RegionalMob guardian = mob(helper, RegionalKind.MYCELIAL_GUARDIAN, pos);
		RegionalMob spider = mob(helper, RegionalKind.CAVE_SKITTERER, pos.add(3, 0, 0));
		try {
			guardian.setHealth(50); tick(helper, guardian, 40); helper.assertTrue(guardian.getHealth() == 50, "Stone must not heal the guardian");
			helper.getLevel().setBlockAndUpdate(BlockPos.containing(pos).below(), Blocks.MYCELIUM.defaultBlockState());
			tick(helper, guardian, 40); helper.assertTrue(guardian.getHealth() == 51, "Mycelium heals one health per two seconds");
			spider.horizontalCollision = true; tick(helper, spider, 1); helper.assertTrue(spider.onClimbable(), "Spider must climb at a collided wall");
			spider.horizontalCollision = false; tick(helper, spider, 1); helper.assertTrue(!spider.onClimbable(), "Climbing must stop away from walls");
		} finally { cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void stormFeatherRequiresExposedCopperAndThunderAndConsumesOnce(GameTestHelper helper) {
		var level = helper.getLevel(); ServerPlayer player = player(helper);
		float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
		BlockPos pos = player.blockPosition().below();
		try {
			level.setBlockAndUpdate(pos, Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RegionalEcosystems.REWARDS.get(RegionalKind.THUNDER_ROC), 2));
			var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
			level.setRainLevel(0); level.setThunderLevel(0);
			player.getMainHandItem().getItem().useOn(context);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "Clear weather must preserve feathers");
			level.setRainLevel(1); level.setThunderLevel(1);
			player.getMainHandItem().getItem().useOn(context);
			helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getInventory().countItem(RegionalEcosystems.STORMFORGED_FEATHER) == 1,
					"One accepted storm ritual exchanges exactly one feather");
			level.setBlockAndUpdate(pos.above(4), Blocks.STONE.defaultBlockState());
			player.getMainHandItem().getItem().useOn(context);
			helper.assertTrue(player.getMainHandItem().getCount() == 1, "Covered copper must reject storm forging");
			helper.assertTrue(level.getBlockState(pos).is(Blocks.COPPER_BLOCK.weathering().unaffected()), "Ritual preserves copper");
		} finally { level.setRainLevel(rain); level.setThunderLevel(thunder); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void pounceUsesPhysicalCollisionAndCannotStrikeThroughAWall(GameTestHelper helper) {
		ServerPlayer player = player(helper); Vec3 start = player.position().add(-6, 0, 0); floor(helper, player.position(), 8);
		RegionalMob raptor = mob(helper, RegionalKind.BONE_RAPTOR, start);
		try {
			tick(helper, raptor, 40); helper.assertTrue(raptor.beginAttack(player), "Raptor starts a warned pounce");
			tick(helper, raptor, 20);
			BlockPos wall = BlockPos.containing(start).offset(2, 0, 0);
			for (int y = 0; y < 4; y++) { for (int z = -2; z <= 2; z++) { helper.getLevel().setBlockAndUpdate(wall.offset(0, y, z), Blocks.STONE.defaultBlockState()); } }
			float health = player.getHealth();
			for (int i = 0; i < 25; i++) { tick(helper, raptor, 1); raptor.move(MoverType.SELF, raptor.getDeltaMovement()); }
			helper.assertTrue(raptor.getX() < wall.getX() + .5, "Leap must stop before passing through a wall");
			helper.assertTrue(player.getHealth() == health, "Covered target must not receive landing damage");
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void passiveWildlifeAndNeutralCreaturesDoNotHuntUnprovokedPlayers(GameTestHelper helper) {
		ServerPlayer player = player(helper); floor(helper, player.position(), 5);
		try {
			for (var kind : List.of(RegionalKind.STORM_FINCH, RegionalKind.STONEBACK_GOAT, RegionalKind.SPORELING, RegionalKind.END_GRAZER, RegionalKind.VOID_RAY)) {
				RegionalMob creature = mob(helper, kind, player.position().add(3, 0, 0)); creature.setNoGravity(true);
				for (int i = 0; i < 50; i++) { creature.commonTick(); creature.tick(); }
				helper.assertTrue(creature.getTarget() == null, "Unprovoked wildlife must stay neutral/passive: " + kind);
				creature.discard();
			}
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void surfaceNightAndCaveSpawningUseTheirDistinctHabitats(GameTestHelper helper) {
		var level = helper.getLevel(); var end = level.getServer().getLevel(Level.END); var fight = end.getDragonFight();
		BlockPos pos = BlockPos.containing(origin(helper)); var biome = level.getBiome(pos).unwrapKey().orElseThrow();
		long time = level.getOverworldClockTime(); var difficulty = level.getDifficulty();
		try {
			floor(helper, origin(helper), 2);
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			helper.setTime(18000); level.updateSkyBrightness();
			for (var region : List.of(BossProfile.MUTANT_ZOMBIE, BossProfile.FOSSIL_TYRANT, BossProfile.THUNDER_BIRD,
					BossProfile.TITAN_BOA, BossProfile.BABA_YAGA, BossProfile.MOUNTAIN_TITAN, BossProfile.ICE_WYRM)) {
				var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(region.biomes()).stream().findFirst().orElseThrow();
				helper.setBiome(holder.unwrapKey().orElseThrow());
				for (var kind : RegionalKind.values()) {
					if (kind.region == region && kind.temper == RegionalKind.Temper.HOSTILE) {
						helper.assertTrue(RegionalEcosystems.canSpawn(kind, level, pos), "Night surface must allow " + kind);
					}
				}
			}
			helper.setBiome(Biomes.PLAINS); helper.setTime(6000); level.updateSkyBrightness();
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.ROTTED_ZOMBIE, level, pos), "Daylight rejects hostile plains spawning");
			helper.setTime(18000); level.updateSkyBrightness(); level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.ROTTED_ZOMBIE, level, pos), "Peaceful rejects hostile ecosystem mobs");
			level.getServer().setDifficulty(difficulty, true);
			for (int x = -4; x <= 4; x++) { for (int z = -4; z <= 4; z++) { level.setBlockAndUpdate(pos.offset(x, 4, z), Blocks.STONE.defaultBlockState()); } }
			helper.assertTrue(RegionalEcosystems.canSpawn(RegionalKind.CAVE_SKITTERER, level, pos), "Ordinary underground plains join the cave ecosystem");
			helper.setBiome(Biomes.DEEP_DARK);
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.CAVE_SKITTERER, level, pos), "Deep Dark belongs to its own colony");
			helper.assertTrue(RegionalEcosystems.canSpawn(RegionalKind.SHADOW_DRONE, level, pos), "Deep Dark colony spawns underground");
		} finally {
			end.setDragonFight(fight); helper.setBiome(biome); helper.setTime(time); level.updateSkyBrightness(); level.getServer().setDifficulty(difficulty, true);
		}
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void netherVariantsAndEndWildlifeSpawnOnlyInTheirNativeRegions(GameTestHelper helper) {
		var server = helper.getLevel().getServer(); var end = server.getLevel(Level.END); var fight = end.getDragonFight();
		BlockPos pos = new BlockPos(8, 80, 8);
		try {
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (var dimension : List.of(Level.NETHER, Level.END)) {
				var level = server.getLevel(dimension); level.getChunkAt(pos);
				level.setBlockAndUpdate(pos.below(), (dimension == Level.END ? Blocks.END_STONE : Blocks.NETHERRACK).defaultBlockState());
				for (int y = 0; y < 180; y++) { level.setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState()); }
				var biomes = dimension == Level.END ? List.of(Biomes.END_HIGHLANDS)
						: List.of(Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST, Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY);
				for (var biome : biomes) {
					net.minecraft.server.commands.FillBiomeCommand.fill(level, pos.offset(-3, -3, -3), pos.offset(3, 3, 3),
							level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome));
					for (var kind : RegionalKind.values()) {
						if (kind.region.dimension() != dimension) { continue; }
						boolean expected = dimension == Level.END || kind.region == BossProfile.SOULBOUND_COLOSSUS && biome == Biomes.SOUL_SAND_VALLEY
								|| kind.region == BossProfile.NETHERBORN && switch (RegionalEcosystems.netherVariant(kind)) {
									case 0 -> biome == Biomes.CRIMSON_FOREST; case 1 -> biome == Biomes.WARPED_FOREST; default -> biome == Biomes.NETHER_WASTES;
								};
						helper.assertTrue(RegionalEcosystems.canSpawn(kind, level, pos) == expected, "Dimension/variant habitat: " + kind + " in " + biome.identifier());
					}
				}
			}
		} finally { end.setDragonFight(fight); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void oceanCreaturesRequireWaterAndCrabsStayNearTheSeabed(GameTestHelper helper) {
		var level = helper.getLevel(); var end = level.getServer().getLevel(Level.END); var fight = end.getDragonFight();
		BlockPos pos = BlockPos.containing(origin(helper)); var biome = level.getBiome(pos).unwrapKey().orElseThrow(); long time = level.getOverworldClockTime();
		try {
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			helper.setBiome(Biomes.DEEP_OCEAN); helper.setTime(18000); level.updateSkyBrightness();
			for (int y = -1; y <= 8; y++) { level.setBlockAndUpdate(pos.above(y), Blocks.WATER.defaultBlockState()); }
			level.setBlockAndUpdate(pos.below(2), Blocks.SAND.defaultBlockState());
			for (var kind : List.of(RegionalKind.ABYSSAL_FISH, RegionalKind.GIANT_CRAB, RegionalKind.SIREN, RegionalKind.LEVIATHAN_SPAWN)) {
				helper.assertTrue(RegionalEcosystems.canSpawn(kind, level, pos), "Deep ocean must permit " + kind);
				RegionalMob creature = mob(helper, kind, Vec3.atBottomCenterOf(pos));
				helper.assertTrue(creature.canBreatheUnderwater() && creature.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation,
						"Ocean creature needs actual aquatic movement and breathing: " + kind);
				creature.discard();
			}
			level.setBlockAndUpdate(pos.below(2), Blocks.WATER.defaultBlockState());
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.GIANT_CRAB, level, pos), "Crabs cannot naturally spawn suspended far above the seabed");
			level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.SIREN, level, pos), "Shallow puddles cannot spawn ocean predators");
		} finally { end.setDragonFight(fight); helper.setBiome(biome); helper.setTime(time); level.updateSkyBrightness(); cleanup(helper); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void sentinelShieldFacetsProtectTheirAnglesAndLeaveAttackableGaps(GameTestHelper helper) {
		ServerPlayer player = player(helper); Vec3 pos = player.position();
		RegionalMob sentinel = mob(helper, RegionalKind.VOID_SENTINEL, pos);
		try {
			sentinel.yBodyRot = 0;
			player.teleportTo(pos.x, pos.y, pos.z + 4);
			sentinel.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 8);
			float protectedLoss = sentinel.getMaxHealth() - sentinel.getHealth();
			sentinel.setHealth(sentinel.getMaxHealth()); sentinel.damageCooldownTime = 0;
			player.teleportTo(pos.x + Math.sin(Math.PI / 3) * 4, pos.y, pos.z + 2);
			sentinel.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 8);
			float gapLoss = sentinel.getMaxHealth() - sentinel.getHealth();
			helper.assertTrue(protectedLoss > 0 && gapLoss > protectedLoss * 2, "Gap must take substantially more damage than a shield facet");
			tick(helper, sentinel, 10); helper.assertTrue(sentinel.shieldAngle() == 20, "Rendered shield angle must come from synchronized server state");
		} finally { cleanup(helper); remove(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void allRegionsRejectPreDragonWrongDimensionAndInvalidBiomes(GameTestHelper helper) {
		var level = helper.getLevel(); var end = level.getServer().getLevel(Level.END); var fight = end.getDragonFight();
		BlockPos pos = BlockPos.containing(origin(helper)); var biome = level.getBiome(pos).unwrapKey().orElseThrow();
		try {
			end.setDragonFight(EnderDragonFight.createDefault());
			for (var kind : RegionalKind.values()) { helper.assertTrue(!RegionalEcosystems.canSpawn(kind, level, pos), "Global pre-dragon gate: " + kind); }
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			helper.setBiome(Biomes.FOREST);
			for (var kind : RegionalKind.values()) {
				helper.assertTrue(!RegionalEcosystems.canSpawn(kind, level, pos), "Wrong region must reject natural spawns: " + kind);
				if (kind.region.dimension() != Level.END) { helper.assertTrue(!RegionalEcosystems.canSpawn(kind, end, pos), "Wrong dimension: " + kind); }
			}
			helper.setBiome(Biomes.BADLANDS); level.setBlockAndUpdate(pos.below(), Blocks.RED_SAND.defaultBlockState());
			helper.assertTrue(RegionalEcosystems.canSpawn(RegionalKind.TYRANT_SKULL, level, pos), "Unlocked surface badlands fossil is discoverable");
			level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.TYRANT_SKULL, level, pos), "Fossil cannot spawn on an arbitrary stone floor");
			helper.setBiome(Biomes.MUSHROOM_FIELDS); level.setBlockAndUpdate(pos.below(), Blocks.MYCELIUM.defaultBlockState());
			helper.assertTrue(RegionalEcosystems.canSpawn(RegionalKind.SPORELING, level, pos), "Passive mushroom wildlife must use its own daylight rule");
			helper.assertTrue(RegionalEcosystems.canSpawn(RegionalKind.SPOREWALKER, level, pos), "Post-dragon mushroom hostiles are explicitly allowed on mycelium");
			level.setBlockAndUpdate(pos.above(5), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!RegionalEcosystems.canSpawn(RegionalKind.SPOREWALKER, level, pos), "Surface region cannot spawn underground");
		} finally { end.setDragonFight(fight); helper.setBiome(biome); }
		helper.succeed();
	}
}
