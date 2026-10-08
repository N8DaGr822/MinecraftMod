package darkspawn.black.boss;

import com.mojang.serialization.Codec;
import darkspawn.black.health.BossHearts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BiomeBoss extends Monster {
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(BiomeBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(BiomeBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RECOVERY = SynchedEntityData.defineId(BiomeBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(BiomeBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> AWAKENING = SynchedEntityData.defineId(BiomeBoss.class, EntityDataSerializers.INT);
	private final BossProfile profile;
	private final ServerBossEvent bossBar;
	private final Set<UUID> participants = new HashSet<>();
	private final Set<UUID> drainedPlayers = new HashSet<>();
	private final List<UUID> children = new ArrayList<>();
	private final Set<UUID> chargeHits = new HashSet<>();
	private Vec3 arena;
	private Vec3 marked = Vec3.ZERO;
	private Vec3 aimed = Vec3.ZERO;
	private Vec3 chargeStart = Vec3.ZERO;
	private Vec3 chargeEnd = Vec3.ZERO;
	private BossAttack attack = BossAttack.SLAM;
	private UUID selectedPlayer;
	private int sequence;
	private int cooldown = 100;
	private int chargeTicks;
	private boolean pouncing;
	private boolean initialized;
	private int minionsSpawned;
	private long lastActive = -1;

	public BiomeBoss(EntityType<? extends BiomeBoss> type, Level level, BossProfile profile) {
		super(type, level);
		this.profile = profile;
		bossBar = new ServerBossEvent(UUID.randomUUID(), phaseName(), profile.color(), BossEvent.BossBarOverlay.PROGRESS);
		setPersistenceRequired();
		setNoGravity(profile.flying() || profile == BossProfile.KRAKEN);
		xpReward = 250;
	}
	public static AttributeSupplier.Builder attributes(BossProfile profile) {
		return createMonsterAttributes().add(BossEntities.MAX_HEALTH, profile.health)
				.add(Attributes.MOVEMENT_SPEED, profile == BossProfile.MOUNTAIN_TITAN ? 0.15 : 0.24)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 160)
				.add(Attributes.ARMOR, profile == BossProfile.SHADOW_CREEPER_QUEEN ? 18 : 10).add(Attributes.STEP_HEIGHT, 1.5);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(PHASE, 1);
		data.define(WINDUP, 0);
		data.define(RECOVERY, 0);
		data.define(VARIANT, 0);
		data.define(AWAKENING, 0);
	}
	public BossProfile profile() { return profile; }
	public int phase() { return entityData.get(PHASE); }
	public int windup() { return entityData.get(WINDUP); }
	public int recovery() { return entityData.get(RECOVERY); }
	public int variant() { return entityData.get(VARIANT); }
	public int awakening() { return entityData.get(AWAKENING); }
	private Component phaseName() { return Component.translatable("boss.darkspawn." + profile.id() + ".phase." + phase()); }
	public static boolean eligible(Player player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }
	public static double scaledHealth(double base, long players) { return base * (1 + 0.5 * (Math.clamp(players, 1, 4) - 1)); }

	public void prepareEncounter(ServerLevel level) {
		arena = position();
		long players = level.players().stream().filter(p -> eligible(p) && distanceToSqr(p) <= 96 * 96).count();
		getAttribute(BossEntities.MAX_HEALTH).setBaseValue(scaledHealth(profile.health, players));
		setHealth(getMaxHealth());
		chooseVariant(level);
	}
	private void chooseVariant(ServerLevel level) {
		var biome = level.getBiome(blockPosition());
		boolean enhanced = switch (profile) {
			case MUTANT_ZOMBIE -> biome.is(Biomes.MEADOW);
			case FOSSIL_TYRANT -> !biome.is(Biomes.DESERT);
			case THUNDER_BIRD -> biome.is(Biomes.WINDSWEPT_SAVANNA);
			case TITAN_BOA -> biome.is(Biomes.BAMBOO_JUNGLE);
			case BABA_YAGA -> biome.is(Biomes.MANGROVE_SWAMP);
			case MOUNTAIN_TITAN -> biome.is(Biomes.JAGGED_PEAKS);
			case ICE_WYRM -> biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.FROZEN_PEAKS);
			case KRAKEN -> biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN);
			case CAVE_CRAWLER -> biome.is(Biomes.LUSH_CAVES);
			case NETHERBORN -> biome.is(Biomes.WARPED_FOREST);
			case VOID_EYE -> biome.is(Biomes.SMALL_END_ISLANDS);
			default -> false;
		};
		entityData.set(VARIANT, profile == BossProfile.NETHERBORN && (biome.is(Biomes.NETHER_WASTES) || biome.is(Biomes.BASALT_DELTAS)) ? 2 : enhanced ? 1 : 0);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (arena == null) { arena = position(); chooseVariant(level); }
		if (lastActive < 0) { lastActive = level.getGameTime(); }
		if (!initialized) {
			initialized = true;
			if (profile == BossProfile.MYCELIAL_SOVEREIGN) { entityData.set(AWAKENING, 100); }
			if (profile == BossProfile.KRAKEN) { spawnWave(level, BossMinion.Role.TENTACLE, 4); }
		}
		if (level.getGameTime() - lastActive >= 1200) { discard(); return; }
		bossBar.setProgress(getHealth() / getMaxHealth());
		var players = level.players().stream().filter(p -> eligible(p) && p.position().distanceToSqr(arena) <= 160 * 160).toList();
		if (players.isEmpty()) {
			getNavigation().stop();
			entityData.set(WINDUP, 0);
			chargeTicks = 0;
			return;
		}
		lastActive = level.getGameTime();
		if (awakening() > 0) {
			entityData.set(AWAKENING, awakening() - 1);
			if (tickCount % 10 == 0) { ring(level, position(), profile.width / 2, BossBolt.Kind.SPORE); }
			return;
		}
		int nextPhase = BossPhase.advance(phase(), getHealth(), getMaxHealth());
		if (nextPhase > phase()) {
			for (int entered = phase() + 1; entered <= nextPhase; entered++) { enterPhase(level, entered); }
			entityData.set(PHASE, nextPhase);
			bossBar.setName(phaseName());
			entityData.set(WINDUP, 0);
			cooldown = 60;
			level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2, 0.7F);
		}
		if (chargeTicks > 0) { tickCharge(level); return; }
		if (recovery() > 0) {
			getNavigation().stop();
			entityData.set(RECOVERY, recovery() - 1);
			return;
		}
		if (windup() > 0) {
			getNavigation().stop();
			if (profile.flying()) { setDeltaMovement(Vec3.ZERO); }
			telegraph(level);
			entityData.set(WINDUP, windup() - 1);
			if (windup() == 0) {
				executeAttack(level);
				sequence++;
				entityData.set(RECOVERY, phase() == 3 ? 25 : 40);
				cooldown = phase() == 3 ? 40 : 65;
			}
			return;
		}
		ServerPlayer target = players.get(Math.floorMod(sequence, players.size()));
		moveInArena(level, target);
		if (--cooldown <= 0) {
			var attacks = profile.attacks(phase(), variant());
			attack = attacks.get(Math.floorMod(sequence, attacks.size()));
			selectedPlayer = target.getUUID();
			aimed = target.getEyePosition().add(target.getDeltaMovement().scale(8));
			marked = floor(level, target.getX(), target.getZ(), target.getY());
			if (profile == BossProfile.KRAKEN) { marked = target.position(); }
			entityData.set(WINDUP, profile == BossProfile.SHADOW_CREEPER_QUEEN && phase() == 3 ? 35 : 40);
			level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 3, 0.5F);
		}
	}

	private void enterPhase(ServerLevel level, int phase) {
		switch (profile) {
			case FOSSIL_TYRANT -> { if (phase == 2) { spawnWave(level, BossMinion.Role.GUARD, 3); } }
			case BABA_YAGA -> { if (phase == 2) { spawnWave(level, BossMinion.Role.HEALER, 2); } }
			case SHADOW_CREEPER_QUEEN -> spawnWave(level, phase == 2 ? BossMinion.Role.GUARD : BossMinion.Role.TURRET, 4);
			case MYCELIAL_SOVEREIGN -> spawnWave(level, phase == 2 ? BossMinion.Role.HEALER : BossMinion.Role.TURRET, 4);
			case KRAKEN -> spawnWave(level, BossMinion.Role.TENTACLE, 2);
			case THUNDER_BIRD -> spawnWave(level, BossMinion.Role.TURRET, 2);
			case NETHERBORN -> spawnWave(level, variant() == 2 ? BossMinion.Role.TURRET : BossMinion.Role.MELEE, 3);
			default -> { }
		}
	}

	private void moveInArena(ServerLevel level, Player target) {
		if (profile.flying()) {
			double angle = tickCount * 0.012;
			Vec3 destination = arena.add(Math.cos(angle) * 18, 8 + Math.sin(angle * 2) * 4, Math.sin(angle) * 18);
			Vec3 delta = destination.subtract(position());
			setDeltaMovement(delta.lengthSqr() > 1 ? delta.normalize().scale(0.3) : delta.scale(0.3));
		} else if (profile == BossProfile.KRAKEN) {
			setDeltaMovement(Vec3.ZERO);
		} else if (tickCount % 20 == 0) {
			if (position().distanceToSqr(arena) > 80 * 80) { getNavigation().moveTo(arena.x, arena.y, arena.z, 1.2); }
			else if (target.position().distanceToSqr(arena) <= 70 * 70 && distanceToSqr(target) > 14 * 14) { getNavigation().moveTo(target, phase() == 3 ? 1.3 : 1); }
			else { getNavigation().stop(); }
		}
		getLookControl().setLookAt(target, 20, 20);
	}

	public BossBolt.Kind boltKind() {
		return switch (profile) {
			case THUNDER_BIRD -> BossBolt.Kind.LIGHTNING;
			case TITAN_BOA -> BossBolt.Kind.POISON;
			case BABA_YAGA -> BossBolt.Kind.HEX;
			case ICE_WYRM -> BossBolt.Kind.FROST;
			case KRAKEN -> BossBolt.Kind.INK;
			case CAVE_CRAWLER -> BossBolt.Kind.WEB;
			case SHADOW_CREEPER_QUEEN -> BossBolt.Kind.ACID;
			case MYCELIAL_SOVEREIGN -> BossBolt.Kind.SPORE;
			case NETHERBORN -> variant() == 1 ? BossBolt.Kind.HEX : BossBolt.Kind.FIRE;
			case SOULBOUND_COLOSSUS -> BossBolt.Kind.SOUL;
			case VOID_EYE -> BossBolt.Kind.VOID;
			default -> BossBolt.Kind.PHYSICAL;
		};
	}

	private void telegraph(ServerLevel level) {
		if (tickCount % 4 != 0) { return; }
		switch (attack) {
			case BEAM, SOUL_STORM, WATER_JET, CHARGE, POUNCE, FAULTLINE -> {
				Vec3 origin = attack == BossAttack.CHARGE || attack == BossAttack.FAULTLINE || attack == BossAttack.POUNCE ? position().add(0, 0.5, 0) : getEyePosition();
				Vec3 end = attack == BossAttack.CHARGE || attack == BossAttack.POUNCE || attack == BossAttack.FAULTLINE ? marked.add(0, 0.5, 0) : aimed;
				for (int i = 0; i <= 24; i++) {
					Vec3 point = origin.add(end.subtract(origin).scale(i / 24.0));
					level.sendParticles(BossBolt.particle(boltKind()), point.x, point.y, point.z, 1, 0, 0, 0, 0);
				}
			}
			case STORM, SHADOW_STORM -> { for (Vec3 point : stormPoints()) { ring(level, point, 3, boltKind()); } }
			case SWEEP -> ring(level, position(), 16, boltKind());
			case BITE, SOUL_DRAIN -> ring(level, position(), 18, boltKind());
			case WAVE -> ring(level, position().add(0, 5, 0), 30, boltKind());
			case WIND -> ring(level, position(), 48, boltKind());
			case SHADOW, BURROW -> ring(level, marked, 7, boltKind());
			case ERUPTION -> ring(level, marked, 8, boltKind());
			case LIGHTNING -> ring(level, aimed, 3, BossBolt.Kind.LIGHTNING);
			case VOLLEY, BOULDERS, VENOM, HEX, FROST, WEB, ACID, FLAMES -> ring(level, aimed, 2, boltKind());
			default -> ring(level, marked, 6, boltKind());
		}
	}
	static void ring(ServerLevel level, Vec3 point, double radius, BossBolt.Kind kind) {
		for (int i = 0; i < 24; i++) {
			double angle = i * Math.PI / 12;
			level.sendParticles(BossBolt.particle(kind), point.x + Math.cos(angle) * radius, point.y + 0.3,
					point.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
		}
	}
	private List<Vec3> stormPoints() {
		return List.of(aimed, aimed.add(8, 0, 0), aimed.add(-8, 0, 0), aimed.add(0, 0, 8), aimed.add(0, 0, -8));
	}

	private void executeAttack(ServerLevel level) {
		int count = 3 + (phase() - 1) * 2 + (variant() > 0 ? 2 : 0);
		float damage = profile == BossProfile.SHADOW_CREEPER_QUEEN ? 16 + phase() * 2 : 10 + phase() * 2;
		switch (attack) {
			case VOLLEY, BOULDERS -> fireFrom(level, getEyePosition(), aimed, count, boltKind());
			case VENOM -> fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.POISON);
			case HEX -> fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.HEX);
			case FROST -> fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.FROST);
			case WEB -> { fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.WEB); if (phase() >= 2) { hazard(level, marked, BossAttack.WEB, BossBolt.Kind.WEB, 5); } }
			case ACID -> { fireFrom(level, getEyePosition(), aimed, count + 2, BossBolt.Kind.ACID); if (phase() >= 2) { hazard(level, marked, BossAttack.ACID, BossBolt.Kind.ACID, 5); } }
			case FLAMES -> fireFrom(level, getEyePosition(), aimed, count + (variant() == 2 ? 4 : 0), boltKind());
			case SLAM, ERUPTION, ROOTS -> {
				strikeArea(level, marked, attack == BossAttack.ERUPTION ? 8 : 6, 6, damage, boltKind(), false);
				if (attack == BossAttack.ROOTS) { hazard(level, marked, BossAttack.ROOTS, BossBolt.Kind.WEB, 5); }
			}
			case SWEEP -> strikeArea(level, position(), 16, 6, damage, boltKind(), false);
			case BITE -> {
				Vec3 direction = aimed.subtract(getEyePosition()).normalize();
				for (ServerPlayer player : level.players()) {
					Vec3 delta = player.getEyePosition().subtract(getEyePosition());
					if (eligible(player) && delta.lengthSqr() < 18 * 18 && delta.normalize().dot(direction) > 0.4 && hasLineOfSight(player)) {
						hit(level, player, damage + 4, boltKind());
					}
				}
			}
			case CHARGE, POUNCE -> startCharge(attack == BossAttack.POUNCE);
			case FAULTLINE -> {
				Vec3 delta = marked.subtract(position()).multiply(1, 0, 1).normalize();
				for (int i = 1; i <= 6; i++) { hazard(level, floor(level, getX() + delta.x * i * 6, getZ() + delta.z * i * 6, getY()), BossAttack.FAULTLINE, BossBolt.Kind.PHYSICAL, 3); }
			}
			case LIGHTNING -> strikeArea(level, aimed.add(0, -64, 0), 3, 128, damage, BossBolt.Kind.LIGHTNING, false);
			case STORM, SHADOW_STORM -> {
				for (Vec3 point : stormPoints()) { strikeArea(level, point.add(0, -64, 0), 3, 128, damage, boltKind(), false); }
			}
			case WIND -> {
				for (ServerPlayer player : level.players()) {
					if (eligible(player) && distanceToSqr(player) <= 48 * 48 && hasLineOfSight(player)) {
						Vec3 delta = player.position().subtract(position());
						player.knockback(1.5, -delta.x, -delta.z, damageSources().mobAttack(this), 0);
					}
				}
				fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.LIGHTNING);
			}
			case CONSTRICT -> { strikeArea(level, marked, 6, 5, damage, BossBolt.Kind.WEB, false); hazard(level, marked, attack, BossBolt.Kind.WEB, 6); }
			case MIASMA, SPORES -> { hazard(level, marked, attack, BossBolt.Kind.POISON, 7); fireFrom(level, getEyePosition(), aimed, count, boltKind()); }
			case BLINK -> {
				Vec3 departed = position();
				if (safeBlink(level, marked) && profile == BossProfile.NETHERBORN) {
					hazard(level, marked, BossAttack.CONSTRICT, BossBolt.Kind.WEB, 5);
					if (phase() == 3) { hazard(level, departed, BossAttack.CONSTRICT, BossBolt.Kind.WEB, 5); }
				}
			}
			case BURROW -> {
				if (safeBlink(level, marked)) {
					strikeArea(level, position(), 7, 8, damage, BossBolt.Kind.FROST, false);
					if (phase() == 3) { hazard(level, position(), BossAttack.BLIZZARD, BossBolt.Kind.FROST, 7); }
				}
			}
			case BLIZZARD -> { hazard(level, marked, attack, BossBolt.Kind.FROST, 9); fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.FROST); }
			case WAVE -> { strikeArea(level, position().add(0, -2, 0), 30, 12, damage, BossBolt.Kind.PHYSICAL, true); fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.PHYSICAL); }
			case INK -> { hazard(level, marked, attack, BossBolt.Kind.INK, 8); fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.INK); }
			case WHIRLPOOL -> { hazard(level, marked, attack, BossBolt.Kind.INK, variant() == 1 ? 10 : 8); fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.INK); }
			case WATER_JET, BEAM -> beam(level, aimed, damage);
			case SHADOW -> {
				strikeArea(level, marked, 7, 8, damage, BossBolt.Kind.ACID, false);
				for (ServerPlayer player : level.players()) {
					if (eligible(player) && player.position().distanceToSqr(marked) < 24 * 24 && hasLineOfSight(player)) {
						player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
					}
				}
			}
			case BROOD -> {
				if (profile == BossProfile.VOID_EYE) { spawnEndWave(level); }
				else { spawnWave(level, profile == BossProfile.CAVE_CRAWLER || profile == BossProfile.SHADOW_CREEPER_QUEEN ? BossMinion.Role.EGG : BossMinion.Role.MELEE, 3); }
			}
			case FUNGAL_GROWTH -> { hazard(level, marked, attack, BossBolt.Kind.SPORE, 8); spawnWave(level, BossMinion.Role.TURRET, 1); }
			case MAGMA -> {
				for (int i = -1; i <= 1; i++) { hazard(level, marked.add(i * 6, 0, 0), attack, BossBolt.Kind.FIRE, 4); }
			}
			case SOUL_DRAIN -> {
				for (ServerPlayer player : level.players()) {
					if (eligible(player) && distanceToSqr(player) <= 18 * 18 && hasLineOfSight(player)
							&& hit(level, player, damage, BossBolt.Kind.SOUL) && BossEffects.stealHeart(player)) {
						drainedPlayers.add(player.getUUID());
						spawnMinion(level, BossMinion.Role.SOUL_CAGE, floor(level, player.getX() + 4, player.getZ() + 4, player.getY()), player.getUUID());
					}
				}
				fireFrom(level, getEyePosition(), aimed, count, BossBolt.Kind.SOUL);
			}
			case SOUL_CAGES -> spawnWave(level, BossMinion.Role.TURRET, 2);
			case SOUL_STORM -> { beam(level, aimed, damage); hazard(level, marked, attack, BossBolt.Kind.SOUL, 8); }
			case PORTAL -> {
				var player = selectedPlayer == null ? null : level.getServer().getPlayerList().getPlayer(selectedPlayer);
				if (player != null && player.level() == level && eligible(player) && distanceToSqr(player) < 128 * 128) { displaceToPlatform(level, player); }
			}
			case VOID_RIFTS -> {
				hazard(level, marked, attack, BossBolt.Kind.VOID, 7);
				fireFrom(level, getEyePosition(), aimed, count + 2, BossBolt.Kind.VOID);
			}
		}
		// Final Phase: Ground specialists add visible projectiles so flight remains an active fight.
		if (phase() == 3 && (attack == BossAttack.SLAM || attack == BossAttack.ERUPTION || attack == BossAttack.CHARGE || attack == BossAttack.ROOTS)) {
			fireFrom(level, getEyePosition(), aimed, 3, boltKind());
		}
	}

	public void fireFrom(ServerLevel level, Vec3 origin, Vec3 target, int count, BossBolt.Kind kind) {
		Vec3 forward = target.subtract(origin).normalize();
		Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
		for (int i = 0; i < count; i++) {
			BossBolt bolt = new BossBolt(BossEntities.BOSS_BOLT, level);
			bolt.configure(this, kind, profile == BossProfile.SHADOW_CREEPER_QUEEN ? 14 : 10);
			bolt.setPos(origin);
			Vec3 delta = target.add(side.scale((i - (count - 1) / 2.0) * 3)).subtract(origin);
			bolt.shoot(delta.x, delta.y, delta.z, 1.4F, 0);
			level.addFreshEntity(bolt);
		}
	}
	private boolean hit(ServerLevel level, ServerPlayer player, float damage, BossBolt.Kind kind) {
		if (!player.hurtServer(level, damageSources().mobAttack(this), damage)) { return false; }
		BossEffects.apply(player, kind);
		return true;
	}
	void strikeArea(ServerLevel level, Vec3 center, double radius, double height, float damage, BossBolt.Kind kind, boolean ships) {
		ring(level, center, radius, kind);
		for (ServerPlayer player : level.players()) {
			Vec3 delta = player.position().subtract(center);
			if (eligible(player) && delta.horizontalDistanceSqr() <= radius * radius && delta.y >= -2 && delta.y <= height && hasLineOfSight(player)) {
				if (hit(level, player, damage, kind)) { player.knockback(1, -delta.x, -delta.z, damageSources().mobAttack(this), damage); }
			}
		}
		if (ships) {
			for (AbstractBoat boat : level.getEntitiesOfClass(AbstractBoat.class, AABB.ofSize(center, radius * 2, 20, radius * 2))) {
				if (boat.position().subtract(center).horizontalDistanceSqr() <= radius * radius) {
					// Ship Combat: A warned tentacle strike damages boats; no terrain-breaking explosion is involved.
					boat.hurtServer(level, damageSources().mobAttack(this), 2);
				}
			}
		}
	}
	public static double distanceToSegmentSquared(Vec3 point, Vec3 from, Vec3 to) {
		Vec3 line = to.subtract(from);
		double length = line.lengthSqr();
		if (length < 0.0001) { return point.distanceToSqr(from); }
		double t = Math.clamp(point.subtract(from).dot(line) / length, 0, 1);
		return point.distanceToSqr(from.add(line.scale(t)));
	}
	private void beam(ServerLevel level, Vec3 target, float damage) {
		Vec3 origin = getEyePosition();
		Vec3 delta = target.subtract(origin);
		Vec3 end = origin.add(delta.lengthSqr() > 144 * 144 ? delta.normalize().scale(144) : delta);
		for (int i = 0; i <= 64; i++) {
			Vec3 point = origin.add(end.subtract(origin).scale(i / 64.0));
			level.sendParticles(BossBolt.particle(boltKind()), point.x, point.y, point.z, 3, 0.3, 0.3, 0.3, 0);
		}
		for (ServerPlayer player : level.players()) {
			if (eligible(player) && distanceToSegmentSquared(player.getEyePosition(), origin, end) <= 2.5 * 2.5 && hasLineOfSight(player)) {
				hit(level, player, damage, boltKind());
			}
		}
	}
	private void hazard(ServerLevel level, Vec3 center, BossAttack attack, BossBolt.Kind kind, float radius) {
		if (level.getEntitiesOfClass(BossHazard.class, getBoundingBox().inflate(128)).size() >= 24) { return; }
		BossHazard hazard = new BossHazard(BossEntities.BOSS_HAZARD, level);
		hazard.setPos(center);
		hazard.configure(this, attack, kind, radius, 200);
		level.addFreshEntity(hazard);
	}

	private void startCharge(boolean leap) {
		pouncing = leap;
		chargeStart = position();
		Vec3 delta = marked.subtract(position());
		if (delta.horizontalDistanceSqr() > 28 * 28) { delta = delta.multiply(1, 0, 1).normalize().scale(28); }
		chargeEnd = position().add(delta);
		if (chargeEnd.distanceToSqr(arena) > 96 * 96) { return; }
		chargeTicks = 1;
		chargeHits.clear();
		getNavigation().stop();
	}
	private void tickCharge(ServerLevel level) {
		getNavigation().stop();
		setSpeed(0);
		if (chargeTicks > 28 || chargeTicks > 1 && horizontalCollision) {
			chargeTicks = 0;
			setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
			return;
		}
		double t = Math.min(1, chargeTicks / 24.0);
		Vec3 next = chargeStart.add(chargeEnd.subtract(chargeStart).scale(t));
		if (pouncing) { next = next.add(0, 28 * t * (1 - t), 0); }
		Vec3 delta = next.subtract(position());
		setDeltaMovement(delta.x, pouncing ? delta.y : getDeltaMovement().y, delta.z);
		for (ServerPlayer player : level.players()) {
			if (eligible(player) && getBoundingBox().inflate(1).intersects(player.getBoundingBox()) && hasLineOfSight(player) && chargeHits.add(player.getUUID())) {
				hit(level, player, profile == BossProfile.NETHERBORN && variant() == 0 ? 20 : 16, boltKind());
			}
		}
		chargeTicks++;
	}
	private boolean safeBlink(ServerLevel level, Vec3 target) {
		if (target.distanceToSqr(arena) > 80 * 80) { return false; }
		AABB box = getBoundingBox().move(target.subtract(position()));
		if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(this, box) || level.containsAnyLiquid(box)) { return false; }
		level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 3, getZ(), 60, 3, 3, 3, 0.1);
		teleportTo(target.x, target.y, target.z);
		return true;
	}
	static Vec3 floor(ServerLevel level, double x, double z, double nearY) {
		BlockPos start = BlockPos.containing(x, Math.min(level.getMaxY() - 2, nearY + 8), z);
		for (int i = 0; i < 48 && start.getY() - i > level.getMinY(); i++) {
			BlockPos pos = start.below(i);
			if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && level.getBlockState(pos).isAir()) {
				return Vec3.atBottomCenterOf(pos);
			}
		}
		return new Vec3(x, nearY, z);
	}
	private void displaceToPlatform(ServerLevel level, ServerPlayer player) {
		Vec3 ground = floor(level, player.getX(), player.getZ(), player.getY());
		BlockPos floor = BlockPos.containing(ground).below();
		// Void Safety: A platform is allowed only above a broad, solid island landing; never above empty void.
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				BlockPos pos = floor.offset(x, 0, z);
				if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) { return; }
			}
		}
		if (!level.getEntitiesOfClass(VoidPlatform.class, new AABB(floor).inflate(12)).isEmpty()) { return; }
		VoidPlatform platform = new VoidPlatform(BossEntities.VOID_PLATFORM, level);
		platform.setPos(ground.add(0, 3, 0));
		AABB clearance = platform.getBoundingBox().expandTowards(0, 3, 0);
		if (!level.getWorldBorder().isWithinBounds(clearance) || !level.noCollision(platform, clearance) || !level.addFreshEntity(platform)) { return; }
		player.stopRiding();
		player.teleportTo(platform.getX(), platform.getY() + 1.1, platform.getZ());
		player.resetFallDistance();
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
		level.sendParticles(ParticleTypes.PORTAL, platform.getX(), platform.getY() + 2, platform.getZ(), 70, 3, 2, 3, 0.1);
	}

	private void spawnWave(ServerLevel level, BossMinion.Role role, int count) {
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2 / count + sequence;
			Vec3 point = position().add(Math.cos(angle) * 18, 0, Math.sin(angle) * 18);
			if (profile != BossProfile.KRAKEN && !profile.flying()) { point = floor(level, point.x, point.z, getY()); }
			spawnMinion(level, role, point, null);
		}
	}
	private void spawnMinion(ServerLevel level, BossMinion.Role role, Vec3 point, UUID captive) {
		if (!canSpawnMinion(level)) { return; }
		BossMinion minion = new BossMinion(BossEntities.MINIONS.get(profile), level, profile);
		minion.configure(this, role, captive);
		minion.setPos(point);
		if (level.getWorldBorder().isWithinBounds(minion.getBoundingBox()) && level.noCollision(minion, minion.getBoundingBox()) && level.addFreshEntity(minion)) {
			children.add(minion.getUUID());
			minionsSpawned++;
		}
	}
	public boolean ownsSummon(Entity entity) { return children.contains(entity.getUUID()); }
	private boolean canSpawnMinion(ServerLevel level) {
		return minionsSpawned < 24 && children.stream().filter(id -> level.getEntity(id) instanceof LivingEntity minion && minion.isAlive()).count() < 12;
	}
	private void spawnEndWave(ServerLevel level) {
		// End Reinforcements: Two vanilla endermites and one Endborn share the encounter's finite summon budget.
		for (int i = 0; i < 3 && canSpawnMinion(level); i++) {
			var request = new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, false);
			var minion = i == 2 ? BossEntities.ENDBORN.create(level, request) : EntityTypes.ENDERMITE.create(level, request);
			if (minion == null) { continue; }
			double angle = i * Math.PI * 2 / 3 + sequence;
			Vec3 point = floor(level, getX() + Math.cos(angle) * 18, getZ() + Math.sin(angle) * 18, getY());
			minion.setPos(point);
			if (minion instanceof Endborn endborn) { endborn.bindToEncounter(this); }
			BlockPos support = BlockPos.containing(point).below();
			if (level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
					&& level.getWorldBorder().isWithinBounds(minion.getBoundingBox()) && level.noCollision(minion, minion.getBoundingBox())
					&& level.addFreshEntity(minion)) {
				children.add(minion.getUUID());
				minionsSpawned++;
			}
		}
	}
	private boolean protectedByMinions(ServerLevel level) {
		return children.stream().anyMatch(id -> level.getEntity(id) instanceof BossMinion minion && minion.isAlive()
				&& (minion.role() == BossMinion.Role.GUARD || minion.role() == BossMinion.Role.TENTACLE));
	}
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (awakening() > 0) { return false; }
		if (damage > 0 && source.getEntity() instanceof ServerPlayer player && eligible(player)) { participants.add(player.getUUID()); }
		if (protectedByMinions(level)) { damage *= 0.35F; }
		if (profile == BossProfile.VOID_EYE && recovery() == 0 && source.getDirectEntity() instanceof Projectile projectile) {
			// Shard Alignment: Match the renderer's mirrored model coordinates and body yaw.
			double angle = Math.atan2(projectile.getZ() - getZ(), projectile.getX() - getX()) - Math.toRadians(yBodyRot) + tickCount * 0.035;
			double segment = Math.PI * 2 / (phase() == 3 ? 2 : 4);
			if (Math.abs(Math.IEEEremainder(angle, segment)) < 0.3) { damage *= 0.2F; }
		}
		return super.hurtServer(level, source, recovery() > 0 ? damage * 1.3F : damage);
	}
	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
	@Override
	public boolean canBreatheUnderwater() { return profile == BossProfile.KRAKEN || super.canBreatheUnderwater(); }
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (!participants.isEmpty()) { spawnAtLocation(level, new ItemStack(BossItems.ESSENCES.get(profile))); }
		for (UUID id : participants) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && player.level() == level && distanceToSqr(player) <= 192 * 192 && !BossHearts.progress(player).hasConsumed(profile.kind())) {
				ItemEntity heart = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), new ItemStack(BossItems.HEARTS.get(profile.kind())));
				heart.setTarget(id);
				level.addFreshEntity(heart);
			}
		}
	}
	private void cleanup(ServerLevel level) {
		for (UUID id : children) { if (level.getEntity(id) instanceof LivingEntity minion) { minion.discard(); } }
		for (UUID id : drainedPlayers) {
			var player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) { player.removeEffect(BossEffects.SOUL_FRACTURE); }
		}
		bossBar.removeAllPlayers();
	}
	@Override
	public void die(DamageSource source) {
		if (level() instanceof ServerLevel level) { cleanup(level); }
		super.die(source);
	}
	@Override
	public void remove(Entity.RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel level && bossBar != null) { cleanup(level); }
		super.remove(reason);
	}
	@Override
	public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
	@Override
	public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("phase", phase());
		output.putInt("variant", variant());
		output.putInt("awakening", awakening());
		output.putBoolean("initialized", initialized);
		output.putInt("minions_spawned", minionsSpawned);
		output.putInt("sequence", sequence);
		output.putLong("last_active", lastActive);
		if (arena != null) { output.store("arena", Vec3.CODEC, arena); }
		output.store("participants", Codec.STRING.listOf(), participants.stream().map(UUID::toString).toList());
		output.store("children", Codec.STRING.listOf(), children.stream().map(UUID::toString).toList());
		output.store("drained_players", Codec.STRING.listOf(), drainedPlayers.stream().map(UUID::toString).toList());
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(PHASE, Math.clamp(input.getIntOr("phase", 1), 1, 3));
		entityData.set(VARIANT, Math.clamp(input.getIntOr("variant", 0), 0, 2));
		entityData.set(AWAKENING, Math.clamp(input.getIntOr("awakening", 0), 0, 100));
		initialized = input.getBooleanOr("initialized", false);
		minionsSpawned = Math.clamp(input.getIntOr("minions_spawned", 0), 0, 24);
		sequence = input.getIntOr("sequence", 0);
		lastActive = input.getLongOr("last_active", level().getGameTime());
		arena = input.read("arena", Vec3.CODEC).orElse(position());
		bossBar.setName(phaseName());
		participants.clear(); children.clear(); drainedPlayers.clear();
		for (String id : input.read("participants", Codec.STRING.listOf()).orElse(List.of())) {
			try { participants.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
		for (String id : input.read("children", Codec.STRING.listOf()).orElse(List.of())) {
			try { children.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
		for (String id : input.read("drained_players", Codec.STRING.listOf()).orElse(List.of())) {
			try { drainedPlayers.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
		// Reload Safety: Save encounter progress but restart attacks with a complete warning.
		entityData.set(WINDUP, 0); entityData.set(RECOVERY, 0);
		chargeTicks = 0; cooldown = 100;
		setDeltaMovement(Vec3.ZERO);
		setNoGravity(profile.flying() || profile == BossProfile.KRAKEN);
	}
}
