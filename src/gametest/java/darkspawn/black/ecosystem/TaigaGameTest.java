package darkspawn.black.ecosystem;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import darkspawn.black.boss.BossItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class TaigaGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		var level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "taiga-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.teleportTo(pos.x, pos.y, pos.z);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return player;
	}

	private static void removePlayer(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}

	private static TaigaWolf wolf(GameTestHelper helper, TaigaWolf.Kind kind, Vec3 position) {
		var wolf = new TaigaWolf(TaigaEcosystem.WOLVES.get(kind), helper.getLevel(), kind);
		wolf.setPos(position);
		helper.getLevel().addFreshEntity(wolf);
		return wolf;
	}

	private static void tick(GameTestHelper helper, TaigaWolf wolf, int ticks) {
		for (int i = 0; i < ticks; i++) { wolf.commonTick(); wolf.customServerAiStep(helper.getLevel()); }
	}

	private static CompoundTag save(GameTestHelper helper, TaigaWolf wolf) {
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		wolf.saveWithoutId(output);
		return output.buildResult();
	}

	private static void load(GameTestHelper helper, TaigaWolf wolf, CompoundTag saved) {
		wolf.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
	}

	private static void floor(GameTestHelper helper, Vec3 center) {
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				helper.getLevel().setBlockAndUpdate(BlockPos.containing(center).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				// Test Arena: Clear headroom outside the small default template for the Alpha's full collision box.
				for (int y = 0; y < 5; y++) {
					helper.getLevel().setBlockAndUpdate(BlockPos.containing(center).offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	@GameTest(skyAccess = true)
	public void direWolvesAcquireRabbitsSheepAndPlayersThroughNormalAi(GameTestHelper helper) {
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		TaigaWolf dire = wolf(helper, TaigaWolf.Kind.DIRE, origin);
		dire.setNoGravity(true); dire.getRandom().setSeed(12);
		var rabbit = helper.spawn(EntityTypes.RABBIT, new BlockPos(4, 2, 1));
		var sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(4, 2, 3));
		rabbit.setNoAi(true); rabbit.setNoGravity(true);
		sheep.setNoAi(true); sheep.setNoGravity(true);
		sheep.setPos(origin.add(40, 0, 0));
		try {
			for (int i = 0; i < 100 && dire.getTarget() != rabbit; i++) { dire.commonTick(); dire.tick(); }
			helper.assertTrue(dire.getTarget() == rabbit, "Dire Wolf must acquire nearby rabbits through its target goals");
			rabbit.discard(); dire.setTarget(null);
			sheep.setPos(origin.add(3, 0, 1));
			for (int i = 0; i < 100 && dire.getTarget() != sheep; i++) { dire.commonTick(); dire.tick(); }
			helper.assertTrue(dire.getTarget() == sheep, "Dire Wolf must acquire sheep when its rabbit prey is gone");
			sheep.discard(); dire.setTarget(null);
			ServerPlayer player = player(helper);
			try {
				for (int i = 0; i < 100 && dire.getTarget() != player; i++) { dire.commonTick(); dire.tick(); }
				helper.assertTrue(dire.getTarget() == player, "Dire Wolf must acquire Survival players through normal AI");
			} finally { removePlayer(player); }
		} finally { dire.discard(); rabbit.discard(); sheep.discard(); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void packSpeedIsBoundedAndRemovedWhenSeparatedOrReloaded(GameTestHelper helper) {
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		TaigaWolf dire = wolf(helper, TaigaWolf.Kind.DIRE, pos);
		TaigaWolf friend = wolf(helper, TaigaWolf.Kind.FROSTFANG, pos.add(2, 0, 0));
		try {
			double base = dire.getAttributeValue(Attributes.MOVEMENT_SPEED);
			tick(helper, dire, 80);
			helper.assertTrue(dire.packed() && Math.abs(dire.getAttributeValue(Attributes.MOVEMENT_SPEED) - base * 1.1) < 0.0001, "Pack proximity must add ten percent once, not every tick");
			load(helper, dire, save(helper, dire));
			helper.assertTrue(!dire.packed() && Math.abs(dire.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < 0.0001, "Reload must rebuild temporary proximity bonuses");
			tick(helper, dire, 20);
			friend.setPos(pos.add(30, 0, 0));
			tick(helper, dire, 20);
			helper.assertTrue(!dire.packed() && Math.abs(dire.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < 0.0001, "Separating the pack must remove its bonus");
		} finally { dire.discard(); friend.discard(); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void frostfangBiteSlowsAndItsTagAllowsPowderSnowWalking(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		TaigaWolf frost = wolf(helper, TaigaWolf.Kind.FROSTFANG, player.position().add(1, 0, 0));
		TaigaWolf dire = wolf(helper, TaigaWolf.Kind.DIRE, player.position().add(3, 0, 0));
		try {
			helper.assertTrue(PowderSnowBlock.canEntityWalkOnPowderSnow(frost), "Frostfang must use vanilla powder-snow collision support");
			helper.assertTrue(!PowderSnowBlock.canEntityWalkOnPowderSnow(dire), "Ordinary Dire Wolf must not inherit snow walking");
			helper.assertTrue(frost.doHurtTarget(helper.getLevel(), player), "Frostfang bite must hit");
			helper.assertTrue(player.getEffect(MobEffects.SLOWNESS).getDuration() == 60 && player.getEffect(MobEffects.SLOWNESS).getAmplifier() == 0, "Bite must apply Slowness I for three seconds");
			player.removeAllEffects(); player.getAbilities().invulnerable = true;
			helper.assertTrue(!frost.doHurtTarget(helper.getLevel(), player) && !player.hasEffect(MobEffects.SLOWNESS), "Rejected damage must not apply bite effects");
		} finally { frost.discard(); dire.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void ravagedFrenzyExpiresAndCannotBeResetByReloading(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		TaigaWolf ravaged = wolf(helper, TaigaWolf.Kind.RAVAGED, player.position().add(2, 0, 0));
		try {
			ravaged.setTarget(player); ravaged.setHealth(10);
			double speed = ravaged.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double damage = ravaged.getAttributeValue(Attributes.ATTACK_DAMAGE);
			tick(helper, ravaged, 1);
			helper.assertTrue(ravaged.frenzy() == 100 && ravaged.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage + 2, "Low health must trigger a five-second frenzy");
			helper.assertTrue(Math.abs(ravaged.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.35) < 0.0001, "Frenzy must add thirty-five percent speed");
			CompoundTag active = save(helper, ravaged);
			tick(helper, ravaged, 100);
			helper.assertTrue(ravaged.frenzy() == 0 && ravaged.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage, "Frenzy must expire without stacking damage");
			load(helper, ravaged, active); ravaged.setTarget(player);
			tick(helper, ravaged, 50);
			helper.assertTrue(ravaged.frenzy() == 0 && Math.abs(ravaged.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 0.0001, "Reload must retain cooldown and remove canceled frenzy modifiers");
		} finally { ravaged.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void alphaHowlCallsOneBoundedPackThatExpiresWithoutRewards(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		floor(helper, center);
		TaigaWolf alpha = wolf(helper, TaigaWolf.Kind.ALPHA, center);
		try {
			alpha.getRandom().setSeed(7); alpha.setTarget(player);
			tick(helper, alpha, 60);
			var helpers = helper.getLevel().getEntitiesOfClass(TaigaWolf.class, alpha.getBoundingBox().inflate(10), TaigaWolf::reinforcement);
			helper.assertTrue(helpers.size() == 2, "Alpha must call at most two helpers into available safe space");
			helper.assertTrue(alpha.hasEffect(MobEffects.SPEED), "Howl must grant a temporary speed effect");
			load(helper, alpha, save(helper, alpha)); alpha.setTarget(player);
			tick(helper, alpha, 220);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(TaigaWolf.class, alpha.getBoundingBox().inflate(10), TaigaWolf::reinforcement).size() == 2, "Reload and repeated howls must not call a second pack");
			for (TaigaWolf called : helpers) {
				load(helper, called, save(helper, called));
				called.dropCustomDeathLoot(helper.getLevel(), called.damageSources().playerAttack(player), true);
				helper.assertTrue(called.reinforcement() && called.getExperienceReward(helper.getLevel(), player) == 0, "Called pack must retain ownership and zero XP across reload");
			}
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, alpha.getBoundingBox().inflate(10)).isEmpty(), "Called pack must not drop farmable rewards");
			CompoundTag expired = save(helper, helpers.getFirst());
			expired.putLong("darkspawn_pack_expires", helper.getLevel().getGameTime());
			load(helper, helpers.getFirst(), expired);
			tick(helper, helpers.getFirst(), 1);
			helper.assertTrue(helpers.getFirst().isRemoved(), "Expired helper must be removed after reload");
			alpha.discard();
			tick(helper, helpers.getLast(), 41);
			helper.assertTrue(helpers.getLast().isRemoved(), "Helper must not outlive a missing Alpha beyond its short unload grace");
		} finally {
			helper.getLevel().getEntitiesOfClass(TaigaWolf.class, alpha.getBoundingBox().inflate(12), TaigaWolf::reinforcement).forEach(TaigaWolf::discard);
			alpha.discard(); removePlayer(player);
		}
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void crowdedPacksPreventAdditionalAlphaReinforcements(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		TaigaWolf alpha = wolf(helper, TaigaWolf.Kind.ALPHA, player.position().add(2, 0, 0));
		var pack = new java.util.ArrayList<TaigaWolf>();
		try {
			for (int i = 0; i < 5; i++) { pack.add(wolf(helper, TaigaWolf.Kind.DIRE, alpha.position().add(0, 0, i + 1))); }
			alpha.setTarget(player); tick(helper, alpha, 60);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(TaigaWolf.class, alpha.getBoundingBox().inflate(12), TaigaWolf::reinforcement).isEmpty(), "Six existing wolves must prevent additional summoned pack members");
			helper.assertTrue(pack.getFirst().hasEffect(MobEffects.SPEED), "Crowded packs must still receive the howl buff");
		} finally { pack.forEach(TaigaWolf::discard); alpha.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void alphaLeapCanBeDodgedAndReloadCancelsItsWarning(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 4)));
		floor(helper, origin.add(3, 0, 0));
		TaigaWolf alpha = wolf(helper, TaigaWolf.Kind.ALPHA, origin);
		try {
			CompoundTag saved = save(helper, alpha); saved.putBoolean("darkspawn_called_pack", true);
			load(helper, alpha, saved); tick(helper, alpha, 60);
			Vec3 landing = origin.add(6, 0, 0);
			player.setPos(landing); alpha.setTarget(player); alpha.setOnGround(true);
			helper.assertTrue(alpha.beginLeap(), "In-range grounded Alpha must begin its pounce warning");
			float health = player.getHealth();
			tick(helper, alpha, 19);
			helper.assertTrue(alpha.windup() == 1 && player.getHealth() == health && !alpha.doHurtTarget(helper.getLevel(), player), "Warning must suppress immediate melee damage");
			player.setPos(landing.add(0, 0, 4));
			for (int i = 0; i < 30; i++) {
				tick(helper, alpha, 1);
				if (alpha.leaping()) { alpha.move(MoverType.SELF, alpha.getDeltaMovement()); }
			}
			helper.assertTrue(player.getHealth() == health && alpha.position().subtract(landing).horizontalDistanceSqr() < 1,
					"Pounce must land at the original marker and let moving players dodge; displacement=" + alpha.position().subtract(origin)
							+ ", health=" + player.getHealth() + ", leaping=" + alpha.leaping() + ", collision=" + alpha.horizontalCollision);
			alpha.setTarget(null); tick(helper, alpha, 120);
			alpha.setPos(origin); alpha.setOnGround(true); player.setPos(landing); alpha.setTarget(player);
			helper.assertTrue(alpha.beginLeap(), "Cooldown must permit a fresh warning");
			load(helper, alpha, save(helper, alpha));
			helper.assertTrue(alpha.windup() == 0 && !alpha.leaping(), "Reload must cancel unseen pounces");
			tick(helper, alpha, 60); alpha.setTarget(player); alpha.setOnGround(true);
			helper.assertTrue(alpha.beginLeap(), "Reload recovery must eventually permit a new warning");
			for (int i = 0; i < 50; i++) {
				tick(helper, alpha, 1);
				if (alpha.leaping()) { alpha.move(MoverType.SELF, alpha.getDeltaMovement()); }
			}
			helper.assertTrue(player.getHealth() < health, "Standing in the landing marker must take pounce damage; displacement="
					+ alpha.position().subtract(origin) + ", grounded=" + alpha.onGround() + ", collision=" + alpha.horizontalCollision
					+ ", visible=" + alpha.hasLineOfSight(player) + ", invulnerable=" + player.isInvulnerableTo(helper.getLevel(), alpha.damageSources().mobAttack(alpha)));
		} finally { alpha.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void alphaPounceUsesCollisionAndHeraldDropsPreserveBossProgression(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 4)));
		floor(helper, origin.add(3, 0, 0));
		TaigaWolf alpha = wolf(helper, TaigaWolf.Kind.ALPHA, origin);
		try {
			CompoundTag saved = save(helper, alpha); saved.putBoolean("darkspawn_called_pack", true);
			load(helper, alpha, saved); tick(helper, alpha, 60);
			player.setPos(origin.add(6, 0, 0)); alpha.setTarget(player); alpha.setOnGround(true);
			helper.assertTrue(alpha.beginLeap(), "Open lane must permit a pounce warning");
			tick(helper, alpha, 20);
			for (int y = 0; y < 5; y++) {
				for (int z = -2; z <= 2; z++) { helper.getLevel().setBlockAndUpdate(BlockPos.containing(origin.add(3, y, z)), Blocks.STONE.defaultBlockState()); }
			}
			float health = player.getHealth();
			for (int i = 0; i < 25; i++) {
				alpha.move(MoverType.SELF, alpha.getDeltaMovement());
				tick(helper, alpha, 1);
			}
			helper.assertTrue(alpha.getX() < origin.x + 3 && player.getHealth() == health && !alpha.leaping(), "An obstructed pounce must stop without phasing or damaging through cover");
			alpha.dropCustomDeathLoot(helper.getLevel(), alpha.damageSources().generic(), false);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, alpha.getBoundingBox().inflate(2)).isEmpty(), "Environmental kills must not produce herald rewards");
			alpha.dropCustomDeathLoot(helper.getLevel(), alpha.damageSources().playerAttack(player), true);
			var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, alpha.getBoundingBox().inflate(2));
			helper.assertTrue(drops.size() == 2 && drops.stream().anyMatch(item -> item.getItem().is(BossItems.MOONLIT_FANG))
					&& drops.stream().noneMatch(item -> item.getItem().is(BossItems.ALPHA_FANG)), "Herald must grant the summon, without boss smithing materials or hearts");
			drops.forEach(ItemEntity::discard);
		} finally { alpha.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void taigaSpawnsUseTheGlobalDragonGateAndRejectInvalidSites(GameTestHelper helper) {
		var level = helper.getLevel();
		var end = level.getServer().getLevel(Level.END);
		var originalFight = end.getDragonFight();
		var difficulty = level.getDifficulty();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		var biome = level.getBiome(pos).unwrapKey().orElseThrow();
		try {
			helper.setBiome(Biomes.SNOWY_TAIGA);
			level.setBlockAndUpdate(pos.below(), Blocks.SNOW_BLOCK.defaultBlockState());
			end.setDragonFight(EnderDragonFight.createDefault());
			for (var entry : TaigaEcosystem.WOLVES.entrySet()) {
				helper.assertTrue(!TaigaEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Pre-dragon taigas must reject ecosystem spawning");
			}
			end.setDragonFight(EnderDragonFight.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"previously_killed\":true}")).getOrThrow());
			for (var entry : TaigaEcosystem.WOLVES.entrySet()) {
				helper.assertTrue(TaigaEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Snowy taiga must permit unlocked wolves on its surface");
				helper.assertTrue(!TaigaEcosystem.canSpawn(entry.getKey(), entry.getValue(), level, EntitySpawnReason.NATURAL, pos, RandomSource.create(1)), "Daylight must still reject ordinary hostile spawning");
				helper.assertTrue(!TaigaEcosystem.canSpawn(entry.getKey(), entry.getValue(), end, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Other dimensions must reject taiga spawning");
			}
			var type = TaigaEcosystem.WOLVES.get(TaigaWolf.Kind.DIRE);
			for (var region : List.of(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
				helper.setBiome(region);
				helper.assertTrue(TaigaEcosystem.canSpawn(TaigaWolf.Kind.DIRE, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "All existing taiga variants must support the ecosystem");
			}
			level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
			helper.assertTrue(!TaigaEcosystem.canSpawn(TaigaWolf.Kind.DIRE, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Peaceful must reject hostile wolves");
			level.getServer().setDifficulty(difficulty, true);
			helper.setBiome(Biomes.FOREST);
			helper.assertTrue(!TaigaEcosystem.canSpawn(TaigaWolf.Kind.DIRE, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Forest must not inherit the taiga ecosystem");
			helper.setBiome(Biomes.TAIGA);
			level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!TaigaEcosystem.canSpawn(TaigaWolf.Kind.DIRE, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Stone floors must not spawn wolves");
			level.setBlockAndUpdate(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(pos.above(4), Blocks.STONE.defaultBlockState());
			helper.assertTrue(!TaigaEcosystem.canSpawn(TaigaWolf.Kind.DIRE, type, level, EntitySpawnReason.TRIAL_SPAWNER, pos, RandomSource.create(1)), "Underground moss must not bypass the surface rule");
		} finally { end.setDragonFight(originalFight); level.getServer().setDifficulty(difficulty, true); helper.setBiome(biome); }
		helper.succeed();
	}
}
