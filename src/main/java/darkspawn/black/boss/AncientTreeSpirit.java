package darkspawn.black.boss;

import com.mojang.serialization.Codec;
import darkspawn.black.health.BossHearts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class AncientTreeSpirit extends Monster {
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RECOVERY = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("boss.darkspawn.tree.phase.1"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> participants = new HashSet<>();
	private final List<UUID> healers = new ArrayList<>();
	private Vec3 markedTarget = Vec3.ZERO;
	private int attack;
	private int cooldown = 100;
	private long lastActiveTick = -1;
	private int forestVariant;

	public AncientTreeSpirit(EntityType<? extends AncientTreeSpirit> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 200;
	}

	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(BossEntities.MAX_HEALTH, 800).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 128).add(Attributes.ARMOR, 12);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 128));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(PHASE, 1);
		data.define(WINDUP, 0);
		data.define(RECOVERY, 0);
	}

	public int phase() {
		return entityData.get(PHASE);
	}

	public int windup() {
		return entityData.get(WINDUP);
	}

	public int recovery() {
		return entityData.get(RECOVERY);
	}

	public int forestVariant() {
		return forestVariant;
	}

	public void prepareEncounter(ServerLevel level) {
		var biome = level.getBiome(blockPosition());
		forestVariant = biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.CHERRY_GROVE) ? 1
				: biome.is(Biomes.DARK_FOREST) || biome.is(Biomes.PALE_GARDEN) ? 2 : 0;
		long players = level.players().stream().filter(player -> eligible(player) && distanceToSqr(player) < 96 * 96).count();
		getAttribute(BossEntities.MAX_HEALTH).setBaseValue(800 + 400 * (Math.clamp(players, 1, 4) - 1));
		setHealth(getMaxHealth());
	}

	private static boolean eligible(Player player) {
		return player.isAlive() && !player.isSpectator() && !player.isCreative();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (lastActiveTick < 0) {
			lastActiveTick = level.getGameTime();
		}
		// Encounter Timeout: Persist game time so unloading the arena cannot pause abandonment indefinitely.
		if (level.getGameTime() - lastActiveTick >= 1200) {
			removeHealers(level);
			discard();
			return;
		}
		bossBar.setProgress(getHealth() / getMaxHealth());
		int nextPhase = BossPhase.advance(phase(), getHealth(), getMaxHealth());
		if (nextPhase != phase()) {
			if (phase() == 1) {
				spawnHealers(level);
			}
			entityData.set(PHASE, nextPhase);
			bossBar.setName(Component.translatable("boss.darkspawn.tree.phase." + nextPhase));
			cooldown = 60;
			entityData.set(WINDUP, 0);
			level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 4, 0.5F);
		}
		if (recovery() > 0) {
			entityData.set(RECOVERY, recovery() - 1);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 11, getZ() - 3, 2, 1, 2, 0.1, 0);
		}
		List<ServerPlayer> nearby = level.players().stream()
				.filter(player -> eligible(player) && distanceToSqr(player) <= 128 * 128).toList();
		if (nearby.isEmpty()) {
			entityData.set(WINDUP, 0);
			return;
		}
		lastActiveTick = level.getGameTime();
		if (windup() > 0) {
			telegraph(level);
			entityData.set(WINDUP, windup() - 1);
			if (windup() == 0) {
				executeAttack(level, nearby);
				entityData.set(RECOVERY, phase() >= 2 ? 40 : 0);
				cooldown = phase() == 3 ? 60 : 90;
			}
			return;
		}
		if (--cooldown <= 0) {
			ServerPlayer target = nearby.get(random.nextInt(nearby.size()));
			attack = (attack + 1) % 3;
			markedTarget = attack == 0 ? target.getEyePosition().add(target.getDeltaMovement().scale(12))
					: groundPosition(level, target.getX(), target.getZ());
			entityData.set(WINDUP, 40);
			level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 4, 0.5F);
		}
	}

	private Vec3 groundPosition(ServerLevel level, double x, double z) {
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, getY(), z));
		return Vec3.atBottomCenterOf(ground);
	}

	private void telegraph(ServerLevel level) {
		if (tickCount % 4 != 0) {
			return;
		}
		Vec3 center = attack == 2 ? position() : markedTarget;
		double radius = attack == 2 ? 14 : 3;
		for (int i = 0; i < 24; i++) {
			double angle = i * Math.PI / 12;
			level.sendParticles(ParticleTypes.COMPOSTER, center.x + Math.cos(angle) * radius,
					center.y + 0.4, center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
		}
	}

	private void executeAttack(ServerLevel level, List<ServerPlayer> players) {
		if (attack == 0) {
			seedVolley(level, markedTarget);
		} else if (attack == 1) {
			rootEruption(level, players, markedTarget, 3.5);
		} else {
			rootEruption(level, players, position(), 14);
		}
		// Ancient Wrath: Combine the ground attack with a dodgeable aerial volley.
		if (phase() == 3 && attack != 0) {
			seedVolley(level, markedTarget.add(0, 8, 0));
		}
	}

	private void seedVolley(ServerLevel level, Vec3 target) {
		int count = phase() == 1 ? 3 : phase() == 2 ? 5 : 7;
		if (forestVariant == 2) {
			count += 2;
		}
		for (int i = 0; i < count; i++) {
			SpiritSeed seed = new SpiritSeed(BossEntities.SPIRIT_SEED, level);
			seed.setOwner(this);
			Vec3 direction = target.subtract(position().add(0, 12, 0)).normalize();
			Vec3 origin = position().add(direction.x * 6, 12, direction.z * 6);
			seed.setPos(origin);
			Vec3 aim = target.add((i - (count - 1) / 2.0) * 2.5, 0, 0).subtract(origin);
			seed.shoot(aim.x, aim.y, aim.z, 1.3F, 0);
			level.addFreshEntity(seed);
		}
	}

	private void rootEruption(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
		level.sendParticles(ParticleTypes.COMPOSTER, center.x, center.y + 1, center.z, 100, radius / 2, 2, radius / 2, 0.15);
		for (ServerPlayer player : players) {
			Vec3 delta = player.position().subtract(center);
			if (delta.horizontalDistanceSqr() <= radius * radius && delta.y >= -2 && delta.y <= 6 && hasLineOfSight(player)) {
				if (player.hurtServer(level, damageSources().mobAttack(this), phase() == 3 ? 14 : 10)) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 2));
					player.knockback(1.2, -delta.x, -delta.z, damageSources().mobAttack(this), phase() == 3 ? 14 : 10);
				}
			}
		}
	}

	private void spawnHealers(ServerLevel level) {
		for (int i = 0; i < 4; i++) {
			double angle = i * Math.PI / 2;
			HeartwoodSapling sapling = new HeartwoodSapling(BossEntities.HEARTWOOD_SAPLING, level);
			sapling.setPos(groundPosition(level, getX() + Math.cos(angle) * 16, getZ() + Math.sin(angle) * 16));
			sapling.setOwner(getUUID());
			if (level.noCollision(sapling, sapling.getBoundingBox()) && level.addFreshEntity(sapling)) {
				healers.add(sapling.getUUID());
			}
		}
	}

	private void removeHealers(ServerLevel level) {
		for (UUID id : healers) {
			if (level.getEntity(id) instanceof HeartwoodSapling sapling) {
				sapling.discard();
			}
		}
		healers.clear();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Participation: Register before lethal damage so the final attacker receives a heart too.
		if (damage > 0 && source.getEntity() instanceof ServerPlayer player && eligible(player)) {
			participants.add(player.getUUID());
		}
		Entity direct = source.getDirectEntity();
		double hitHeight = direct instanceof Projectile ? direct.getY() - getY()
				: direct instanceof Player player ? player.getEyeY() - getY() : -1;
		if (phase() >= 2 && recovery() > 0 && hitHeight >= 8 && hitHeight <= 14) {
			damage *= 1.5F;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (!participants.isEmpty()) {
			spawnAtLocation(level, new ItemStack(BossItems.LIVING_HEARTWOOD));
		}
		for (UUID id : participants) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && player.level() == level && distanceToSqr(player) <= 160 * 160
					&& !BossHearts.progress(player).hasConsumed(BossKind.ANCIENT_TREE_SPIRIT)) {
				ItemEntity heart = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(),
						new ItemStack(BossItems.HEARTS.get(BossKind.ANCIENT_TREE_SPIRIT)));
				heart.setTarget(id);
				level.addFreshEntity(heart);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		if (level() instanceof ServerLevel server) {
			removeHealers(server);
		}
		super.die(source);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("darkspawn_phase", phase());
		output.putInt("darkspawn_forest_variant", forestVariant);
		output.putLong("darkspawn_last_active_tick", lastActiveTick);
		output.store("darkspawn_participants", Codec.STRING.listOf(), participants.stream().map(UUID::toString).toList());
		output.store("darkspawn_healers", Codec.STRING.listOf(), healers.stream().map(UUID::toString).toList());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(PHASE, Math.clamp(input.getIntOr("darkspawn_phase", 1), 1, 3));
		forestVariant = Math.clamp(input.getIntOr("darkspawn_forest_variant", 0), 0, 2);
		lastActiveTick = input.getLongOr("darkspawn_last_active_tick", level().getGameTime());
		bossBar.setName(Component.translatable("boss.darkspawn.tree.phase." + phase()));
		participants.clear();
		healers.clear();
		for (String id : input.read("darkspawn_participants", Codec.STRING.listOf()).orElse(List.of())) {
			try { participants.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
		for (String id : input.read("darkspawn_healers", Codec.STRING.listOf()).orElse(List.of())) {
			try { healers.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
	}
}
