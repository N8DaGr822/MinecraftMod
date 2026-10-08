package darkspawn.black.boss;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class BossEncounterGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "boss-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		player.getAbilities().invulnerable = true;
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 3, 10)));
		player.teleportTo(pos.x, pos.y, pos.z);
		return player;
	}
	private static void removePlayer(ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
		player.discard();
	}

	@GameTest(skyAccess = true, maxTicks = 100)
	public void allBossesTickThroughPhasesAndSurviveSaveLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		try {
			for (BossProfile profile : BossProfile.values()) {
				BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), level, profile);
				boss.setPos(player.position().add(20, 0, 0));
				boss.prepareEncounter(level);
				helper.assertTrue(level.addFreshEntity(boss), "Boss failed to spawn: " + profile);
				try {
					for (int tick = 0; tick < 1400; tick++) {
						if (tick == 450) { boss.setHealth(boss.getMaxHealth() * 0.6F); }
						if (tick == 850) { boss.setHealth(boss.getMaxHealth() * 0.3F); }
						// Server Smoke: Advance the common entity clock before exercising AI callbacks with a connected mock player.
						boss.commonTick(); boss.customServerAiStep(level);
					}
					helper.assertTrue(boss.phase() == 3, "Boss did not enter its final phase: " + profile);
					var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
					boss.saveWithoutId(output);
					BiomeBoss restored = new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), level, profile);
					restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
					helper.assertTrue(restored.phase() == 3 && restored.windup() == 0, "Reload must preserve phase and cancel an unfinished attack: " + profile);
					helper.assertTrue(restored.getHealth() == boss.getHealth(), "Boss health changed during save/load: " + profile);
					restored.heal(restored.getMaxHealth());
					restored.customServerAiStep(level);
					helper.assertTrue(restored.phase() == 3, "Healing reversed a boss phase: " + profile);
					restored.discard();
				} finally { boss.discard(); }
			}
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void soulTheftChangesMaximumHealthAndReturnsIt(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		try {
			float initial = player.getMaxHealth();
			helper.assertTrue(BossEffects.stealHeart(player), "First soul strike must steal one heart");
			helper.assertTrue(player.getMaxHealth() == initial - 2, "Soul theft must change max health by exactly one heart");
			BossEffects.stealHeart(player);
			BossEffects.returnHeart(player);
			helper.assertTrue(player.getMaxHealth() == initial - 2, "Breaking one cage must release exactly one heart");
			player.removeEffect(BossEffects.SOUL_FRACTURE);
			helper.assertTrue(player.getMaxHealth() == initial, "Soul recovery must restore the original maximum");
		} finally { removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void smithingPowersTriggerOnlyForSuccessfulDirectMelee(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		BiomeBoss target = new BiomeBoss(BossEntities.BIOME_BOSSES.get(BossProfile.MUTANT_ZOMBIE), helper.getLevel(), BossProfile.MUTANT_ZOMBIE);
		target.setPos(player.position().add(2, 0, 0));
		try {
			player.setItemSlot(EquipmentSlot.MAINHAND, BossEmpowerment.apply(new ItemStack(Items.NETHERITE_SWORD), "stoneguard"));
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(target, player.damageSources().playerAttack(player), 10, 0, true);
			helper.assertTrue(!player.hasEffect(MobEffects.RESISTANCE), "Blocked hits must not trigger the weapon power");
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(target, player.damageSources().magic(), 10, 10, false);
			helper.assertTrue(!player.hasEffect(MobEffects.RESISTANCE), "Indirect damage must not trigger the weapon power");
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(target, player.damageSources().playerAttack(player), 10, 10, false);
			helper.assertTrue(player.hasEffect(MobEffects.RESISTANCE), "Successful direct melee must trigger Stoneguard");
			player.setItemSlot(EquipmentSlot.MAINHAND, BossEmpowerment.apply(player.getMainHandItem(), "armor_rend"));
			float armor = (float) target.getAttributeValue(Attributes.ARMOR);
			ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(target, player.damageSources().playerAttack(player), 10, 10, false);
			helper.assertTrue(target.getAttributeValue(Attributes.ARMOR) == armor - 4, "Armor Rend must lower armor by four");
		} finally { target.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void projectilesPreserveTheirVisualAndExpire(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(BossProfile.VOID_EYE), level, BossProfile.VOID_EYE);
		boss.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 20, 10))));
		level.addFreshEntity(boss);
		try {
			for (BossBolt.Kind kind : BossBolt.Kind.values()) {
				BossBolt bolt = new BossBolt(BossEntities.BOSS_BOLT, level);
				bolt.configure(boss, kind, 10);
				bolt.setPos(boss.position().add(0, 20, 0));
				helper.assertTrue(!bolt.getItem().isEmpty(), "Projectile is missing its visual: " + kind);
				var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				bolt.saveWithoutId(output);
				BossBolt restored = new BossBolt(BossEntities.BOSS_BOLT, level);
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				helper.assertTrue(restored.kind() == kind && ItemStack.matches(bolt.getItem(), restored.getItem()), "Projectile changed on reload: " + kind);
				for (int tick = 0; tick < 182 && !restored.isRemoved(); tick++) { restored.commonTick(); restored.tick(); }
				helper.assertTrue(restored.isRemoved(), "Missed projectile never expired: " + kind);
				bolt.discard();
			}
		} finally { boss.discard(); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void krakenTentaclesHaveFiniteWavesAndCleanUpWithTheirBoss(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(BossProfile.KRAKEN), level, BossProfile.KRAKEN);
		boss.setPos(player.position().add(20, 20, 0));
		boss.prepareEncounter(level);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) { level.getChunk((boss.blockPosition().getX() >> 4) + x, (boss.blockPosition().getZ() >> 4) + z); }
		}
		level.addFreshEntity(boss);
		// Entity Sections: Finish loading the spawn chunks before querying newly added tentacles.
		helper.runAfterDelay(3, () -> {
			try {
				boss.customServerAiStep(level);
				var arms = level.getEntitiesOfClass(BossMinion.class, boss.getBoundingBox().inflate(64), boss::ownsSummon);
				helper.assertTrue(arms.size() == 4, "Kraken should open with four destructible tentacles; found " + arms.size());
				boss.setHealth(boss.getMaxHealth() * 0.3F);
				boss.customServerAiStep(level);
				arms = level.getEntitiesOfClass(BossMinion.class, boss.getBoundingBox().inflate(64), boss::ownsSummon);
				helper.assertTrue(arms.size() == 8, "Skipping phase two should still create both finite tentacle waves");
				for (BossMinion arm : arms) { helper.assertTrue(arm.role() == BossMinion.Role.TENTACLE, "Wrong Kraken appendage role"); }
				boss.discard();
				for (BossMinion arm : arms) { helper.assertTrue(arm.isRemoved(), "Tentacle survived the end of its encounter"); }
			} finally { boss.discard(); removePlayer(player); }
			helper.succeed();
		});
	}

	@GameTest(skyAccess = true)
	public void summonedEndbornRememberTheirOwnerAndSoulCagesReturnOneHeart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(BossProfile.SOULBOUND_COLOSSUS), level, BossProfile.SOULBOUND_COLOSSUS);
		boss.setPos(player.position().add(20, 0, 0));
		level.addFreshEntity(boss);
		Endborn endborn = new Endborn(BossEntities.ENDBORN, level);
		endborn.setPos(boss.position().add(15, 0, 0));
		endborn.bindToEncounter(boss);
		BossMinion cage = new BossMinion(BossEntities.MINIONS.get(BossProfile.SOULBOUND_COLOSSUS), level, BossProfile.SOULBOUND_COLOSSUS);
		try {
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			endborn.saveWithoutId(output);
			Endborn restored = new Endborn(BossEntities.ENDBORN, level);
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			restored.customServerAiStep(level);
			helper.assertTrue(!restored.isRemoved(), "Summoned Endborn lost its living owner across reload");
			boss.discard();
			for (int tick = 0; tick < 42; tick++) { restored.customServerAiStep(level); }
			helper.assertTrue(restored.isRemoved(), "Summoned Endborn survived its missing owner");
			float health = player.getMaxHealth();
			BossEffects.stealHeart(player); BossEffects.stealHeart(player);
			cage.configure(boss, BossMinion.Role.SOUL_CAGE, player.getUUID());
			cage.die(player.damageSources().playerAttack(player));
			helper.assertTrue(player.getMaxHealth() == health - 2, "Destroying one soul cage must restore exactly one heart");
		} finally { cage.discard(); endborn.discard(); boss.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest
	public void packagedSmithingRecipesSyncAndMatchWithLoadedTags(GameTestHelper helper) throws Exception {
		ServerLevel level = helper.getLevel();
		var ops = level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
		for (BossProfile profile : BossProfile.values()) {
			try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/" + profile.power + "_empowerment.json")) {
				var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
				var recipe = (BossEmpowermentRecipe) net.minecraft.world.item.crafting.Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow();
				var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
				try {
					BossEmpowermentRecipe.STREAM_CODEC.encode(buffer, recipe);
					var decoded = BossEmpowermentRecipe.STREAM_CODEC.decode(buffer);
					var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD),
							new ItemStack(Items.NETHERITE_SWORD), new ItemStack(BossItems.ESSENCES.get(profile)));
					helper.assertTrue(decoded.matches(input, level), "Loaded recipe does not accept melee weapons: " + profile);
					helper.assertTrue(profile.power.equals(decoded.assemble(input).get(BossEmpowerment.POWER)), "Recipe lost its power across networking: " + profile);
					helper.assertTrue(!decoded.matches(new net.minecraft.world.item.crafting.SmithingRecipeInput(input.template(),
							new ItemStack(Items.BOW), input.addition()), level), "Melee recipe incorrectly accepts a bow: " + profile);
				} finally { buffer.release(); }
			}
		}
		helper.succeed();
	}

	@GameTest
	public void bossHealthScalingExceedsVanillaCapAndMigratesEarlierSaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (BossProfile profile : BossProfile.values()) {
			BiomeBoss boss = new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), level, profile);
			helper.assertTrue(boss.getMaxHealth() == profile.health && boss.getHealth() == profile.health, "Incorrect initial health: " + profile);
			double scaled = BiomeBoss.scaledHealth(profile.health, 4);
			boss.getAttribute(BossEntities.MAX_HEALTH).setBaseValue(scaled);
			boss.setHealth(boss.getMaxHealth());
			helper.assertTrue(boss.getHealth() == scaled, "Multiplayer health was capped: " + profile);
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			boss.saveWithoutId(output);
			BiomeBoss restored = new BiomeBoss(BossEntities.BIOME_BOSSES.get(profile), level, profile);
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			helper.assertTrue(restored.getHealth() == scaled && restored.getMaxHealth() == scaled, "Large health pool changed on reload: " + profile);
			restored.discard(); boss.discard();
		}
		AncientTreeSpirit original = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		original.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);
		original.setHealth(600);
		var legacy = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		original.saveWithoutId(legacy);
		var oldTag = legacy.buildResult(); oldTag.remove("darkspawn_boss_health");
		AncientTreeSpirit restored = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), oldTag));
		helper.assertTrue(restored.getMaxHealth() == 2000 && restored.getHealth() == 600, "Legacy boss health migration lost its scaled pool or current health");
		MutantWolf wolf = new MutantWolf(BossEntities.MUTANT_WOLF, level);
		wolf.getAttribute(BossEntities.MAX_HEALTH).setBaseValue(2250);
		wolf.setHealth(2250);
		helper.assertTrue(wolf.getHealth() == 2250, "Wolf multiplayer pool remained capped");
		helper.assertTrue(new Endborn(BossEntities.ENDBORN, level).getMaxHealth() == 60, "Non-boss health changed");
		restored.discard(); original.discard(); wolf.discard();
		helper.succeed();
	}
}
