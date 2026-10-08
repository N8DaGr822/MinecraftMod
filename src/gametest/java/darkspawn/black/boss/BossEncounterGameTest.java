package darkspawn.black.boss;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class BossEncounterGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		return player(helper, new Connection(PacketFlow.SERVERBOUND));
	}
	private static ServerPlayer player(GameTestHelper helper, Connection connection) {
		ServerLevel level = helper.getLevel();
		var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "boss-test"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override public GameType gameMode() { return GameType.SURVIVAL; }
			@Override public boolean isClientAuthoritative() { return false; }
		};
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
	public void treeAnimationsPreserveAttackWarningsRecoveryAndReload(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		boss.setPos(player.position().add(20, 0, 0));
		level.addFreshEntity(boss);
		try {
			boolean checkedReload = false;
			for (int phase = 1; phase <= 3; phase++) {
				boss.setHealth(boss.getMaxHealth() * (phase == 1 ? 1 : phase == 2 ? 0.6F : 0.3F));
				boss.customServerAiStep(level);
				var released = new java.util.HashSet<Integer>();
				for (int tick = 0; tick < 600; tick++) {
					int previousWindup = boss.windup();
					boss.commonTick(); boss.customServerAiStep(level);
					if (previousWindup == 0 && boss.windup() > 0) {
						helper.assertTrue(boss.windup() == 40, "Animation integration shortened the attack warning");
					}
					if (previousWindup > 0) {
						helper.assertTrue(boss.windup() == previousWindup - 1, "Warning countdown changed");
						if (boss.windup() == 0) {
							released.add(boss.attackKind());
							helper.assertTrue(boss.recovery() == (phase >= 2 ? 40 : 0), "Weak point no longer matches the recovery window");
						}
					}
					if (!checkedReload && boss.windup() > 0) {
						var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
						boss.saveWithoutId(output);
						AncientTreeSpirit restored = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
						restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
						helper.assertTrue(restored.windup() == 0 && restored.recovery() == 0, "Reload resumed an unfinished attack pose");
						restored.discard(); checkedReload = true;
					}
				}
				helper.assertTrue(boss.phase() == phase && released.size() == 3, "Tree did not exercise all attack animations in phase " + phase);
			}
			// Boss Scale: Seed volleys must originate at the enlarged canopy, not the old 12-block height.
			var seeds = level.getEntitiesOfClass(SpiritSeed.class, boss.getBoundingBox().inflate(16), seed -> seed.getOwner() == boss);
			helper.assertTrue(!seeds.isEmpty(), "Tree did not release any seeds");
			for (SpiritSeed seed : seeds) {
				helper.assertTrue(Math.abs(seed.getY() - boss.getY() - 18.666667) < 0.001, "Seed did not launch from the enlarged model");
				seed.discard();
			}
			// Client Data: The newly synced attack identity must not keep a charge active after everyone leaves.
			player.teleportTo(player.getX() + 200, player.getY(), player.getZ());
			boss.customServerAiStep(level);
			helper.assertTrue(boss.windup() == 0, "Charge remained active without a nearby player");
		} finally {
			boss.die(level.damageSources().generic()); boss.discard(); removePlayer(player);
		}
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void treeWarningsMatchTheirDamageAreasAndReachFlyingPlayers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var particles = new ArrayList<ClientboundLevelParticlesPacket>();
		ServerPlayer player = player(helper, new Connection(PacketFlow.SERVERBOUND) {
			@Override public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
				if (packet instanceof ClientboundLevelParticlesPacket particle && particle.particle() instanceof DustParticleOptions) {
					particles.add(particle);
				}
				super.send(packet, listener, flush);
			}
		});
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		// Target Isolation: Other concurrently scheduled encounters must not supply this test's aim point.
		boss.setPos(player.position().add(0, 0, 512));
		level.addFreshEntity(boss);
		player.teleportTo(boss.getX() + 60, boss.getY() + 40, boss.getZ());
		try {
			var warnings = new java.util.HashSet<Integer>();
			for (int tick = 0; tick < 600 && warnings.size() < 3; tick++) {
				particles.clear();
				boss.commonTick(); boss.customServerAiStep(level);
				if (particles.isEmpty()) { continue; }
				int attack = boss.attackKind();
				warnings.add(attack);
				helper.assertTrue(particles.stream().allMatch(p -> p.overrideLimiter() && p.alwaysShow()), "Flight or particle settings can hide a critical warning");
				if (attack == 0) {
					helper.assertTrue(particles.size() == 23, "Flying player did not receive the full seed path and reticle");
					var target = particles.get(16);
					helper.assertTrue(new Vec3(target.x(), target.y(), target.z()).distanceTo(player.getEyePosition()) < 0.001,
							"Seed warning does not end at its fixed aim point");
				} else {
					helper.assertTrue(particles.size() == (attack == 2 ? 64 : 32), "Flying player lost part of the ground warning");
					Vec3 center = attack == 2 ? boss.position() : player.position();
					for (var particle : particles) {
						double radius = Math.hypot(particle.x() - center.x, particle.z() - center.z);
						helper.assertTrue(Math.abs(radius - (attack == 2 ? 14 : 3.5)) < 0.001, "Warning edge differs from the damage radius");
					}
				}
			}
			helper.assertTrue(warnings.size() == 3, "Not all warning shapes were exercised");
			boss.setHealth(boss.getMaxHealth() * 0.3F);
			boolean combinedWarning = false;
			for (int tick = 0; tick < 220 && !combinedWarning; tick++) {
				particles.clear();
				boss.commonTick(); boss.customServerAiStep(level);
				if (!particles.isEmpty() && boss.attackKind() != 0) {
					helper.assertTrue(particles.size() == 23 + (boss.attackKind() == 2 ? 64 : 32), "Wrath's extra seed volley has no aerial warning");
					combinedWarning = true;
				}
			}
			helper.assertTrue(combinedWarning, "No combined phase-three warning was exercised");
		} finally {
			level.getEntitiesOfClass(SpiritSeed.class, boss.getBoundingBox().inflate(128), seed -> seed.getOwner() == boss).forEach(SpiritSeed::discard);
			boss.die(level.damageSources().generic()); boss.discard(); removePlayer(player);
		}
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void treeHeartwoodClosesOnPhaseChangeAndRewardsElevatedHits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		boss.setPos(player.position().add(20, 0, 0));
		boss.getAttribute(Attributes.ARMOR).setBaseValue(0);
		boss.setHealth(boss.getMaxHealth() * 0.6F);
		level.addFreshEntity(boss);
		SpiritSeed probe = new SpiritSeed(BossEntities.SPIRIT_SEED, level);
		try {
			for (int tick = 0; tick < 160 && boss.recovery() == 0; tick++) {
				boss.commonTick(); boss.customServerAiStep(level);
			}
			helper.assertTrue(boss.phase() == 2 && boss.recovery() > 0, "Heartwood never opened");
			probe.setPos(boss.position().add(0, 8, 0));
			float health = boss.getHealth();
			boss.hurtServer(level, level.damageSources().thrown(probe, player), 4);
			helper.assertTrue(Math.abs(health - boss.getHealth() - 4) < 0.001, "The old low weak point still grants bonus damage");
			// Damage Probe: Measure separate hits without Minecraft's repeated-hit damage reduction.
			boss.damageCooldownTime = 0;
			probe.setPos(boss.position().add(0, 18, 0));
			health = boss.getHealth();
			boss.hurtServer(level, level.damageSources().thrown(probe, player), 4);
			helper.assertTrue(Math.abs(health - boss.getHealth() - 6) < 0.001, "Exposed elevated heartwood did not grant 50% extra damage");
			boss.setHealth(boss.getMaxHealth() * 0.3F);
			boss.customServerAiStep(level);
			helper.assertTrue(boss.phase() == 3 && boss.recovery() == 0 && boss.windup() == 0, "Phase change retained the previous attack's warning or weak point");
			boss.damageCooldownTime = 0;
			health = boss.getHealth();
			boss.hurtServer(level, level.damageSources().thrown(probe, player), 4);
			helper.assertTrue(Math.abs(health - boss.getHealth() - 4) < 0.001, "Closed heartwood still granted bonus damage during phase change");
		} finally {
			probe.discard(); boss.die(level.damageSources().generic()); boss.discard(); removePlayer(player);
		}
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void treeAwakeningPersistsAndKeepsTheFullOpeningWarning(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		boss.setPos(player.position().add(20, 0, 0));
		level.addFreshEntity(boss);
		try {
			float health = boss.getHealth();
			helper.assertTrue(boss.awakening() == 60, "New encounter skipped its reveal");
			helper.assertTrue(!boss.hurtServer(level, level.damageSources().playerAttack(player), 50), "Unfolding boss accepted ordinary damage");
			for (int tick = 0; tick < 20; tick++) { boss.commonTick(); boss.customServerAiStep(level); }
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			boss.saveWithoutId(output);
			var saved = output.buildResult();
			AncientTreeSpirit restored = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(restored.awakening() == 40, "Reload restarted or skipped a partial reveal");
			for (int tick = 0; tick < 40; tick++) {
				boss.commonTick(); boss.customServerAiStep(level);
				helper.assertTrue(boss.windup() == 0 && boss.recovery() == 0, "Boss attacked while awakening");
			}
			helper.assertTrue(boss.awakening() == 0 && boss.getHealth() == health, "Reveal failed to finish safely");
			for (int tick = 0; tick < 39; tick++) { boss.commonTick(); boss.customServerAiStep(level); }
			helper.assertTrue(boss.windup() == 0, "Opening attack started before the original five-second delay");
			boss.commonTick(); boss.customServerAiStep(level);
			helper.assertTrue(boss.windup() == 40, "Awakening shortened the first attack's warning");
			helper.assertTrue(boss.hurtServer(level, level.damageSources().generic(), 5), "Boss remained protected after awakening");
			// Save Compatibility: Old active encounters have no awakening field.
			saved.remove("darkspawn_awakening");
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(restored.awakening() == 0, "Old save replayed the reveal");
			restored.discard();
			AncientTreeSpirit commanded = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
			helper.assertTrue(commanded.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE), "Awakening blocked administrative removal");
			commanded.discard();
		} finally { boss.discard(); removePlayer(player); }
		helper.succeed();
	}

	@GameTest(skyAccess = true)
	public void treeDefeatLastsThreeSecondsWithoutRepeatingRewards(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		AncientTreeSpirit boss = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
		boss.setPos(player.position().add(20, 0, 0));
		level.addFreshEntity(boss);
		try {
			for (int tick = 0; tick < 60; tick++) { boss.commonTick(); boss.customServerAiStep(level); }
			var area = boss.getBoundingBox().inflate(32);
			var previousItems = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
			boss.hurtServer(level, level.damageSources().playerAttack(player), 10000);
			helper.assertTrue(boss.isDeadOrDying() && !boss.isRemoved(), "Defeat did not begin with a visible corpse");
			var drops = level.getEntitiesOfClass(ItemEntity.class, area, item -> !previousItems.contains(item.getUUID()));
			helper.assertTrue(drops.stream().filter(item -> item.getItem().is(BossItems.LIVING_HEARTWOOD)).count() == 1, "Death must award one Heartwood drop");
			helper.assertTrue(drops.stream().filter(item -> item.getItem().is(BossItems.HEARTS.get(BossKind.ANCIENT_TREE_SPIRIT))).count() == 1, "Death must reserve one boss heart");
			boss.die(level.damageSources().playerAttack(player));
			for (int tick = 0; tick < 20; tick++) { boss.tickDeath(); }
			helper.assertTrue(!boss.isRemoved() && boss.defeatTime() == 20, "Vanilla removal cut the collapse short");
			var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			boss.saveWithoutId(output);
			AncientTreeSpirit restored = new AncientTreeSpirit(BossEntities.TREE_SPIRIT, level);
			restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
			helper.assertTrue(restored.isDeadOrDying() && restored.defeatTime() == 20, "Reload restarted the collapse");
			for (int tick = 20; tick < 59; tick++) { boss.tickDeath(); restored.tickDeath(); }
			helper.assertTrue(!boss.isRemoved() && !restored.isRemoved(), "Collapse ended before its final pose");
			boss.tickDeath(); restored.tickDeath();
			helper.assertTrue(boss.isRemoved() && restored.isRemoved(), "Completed collapse left a corpse behind");
			helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area, item -> !previousItems.contains(item.getUUID())).size() == drops.size(),
					"Repeated death, collapse, or reload duplicated rewards");
			drops.forEach(ItemEntity::discard);
		} finally { boss.discard(); removePlayer(player); }
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
