package darkspawn.black.boss;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import darkspawn.black.health.BossHearts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class AncientTreeSpirit extends Monster implements GeoEntity {
	// Boss Scale: Keep the authored rig and UVs intact while scaling visuals and combat anchors together.
	public static final float MODEL_SCALE = 28.0F / 18.0F;
	public static final int AWAKENING_TICKS = 60;
	public static final int DEFEAT_TICKS = 60;
	private static final double ROOT_RADIUS = 3.5;
	private static final double SWEEP_RADIUS = 14;
	private static final DustParticleOptions SEED_WARNING = new DustParticleOptions(0xB7EF68, 1.4F);
	private static final DustParticleOptions ROOT_WARNING = new DustParticleOptions(0xE8BC66, 1.4F);
	private static final DustParticleOptions SWEEP_WARNING = new DustParticleOptions(0xF07838, 1.8F);
	public static final DataTicket<Integer> ANIMATION_PHASE = DataTickets.create("darkspawn_tree_phase", Integer.class);
	public static final DataTicket<Integer> ANIMATION_WINDUP = DataTickets.create("darkspawn_tree_windup", Integer.class);
	public static final DataTicket<Integer> ANIMATION_RECOVERY = DataTickets.create("darkspawn_tree_recovery", Integer.class);
	public static final DataTicket<Integer> ANIMATION_ATTACK = DataTickets.create("darkspawn_tree_attack", Integer.class);
	public static final DataTicket<Integer> ANIMATION_AWAKENING = DataTickets.create("darkspawn_tree_awakening", Integer.class);
	public static final DataTicket<Float> ANIMATION_LIFECYCLE_TIME = DataTickets.create("darkspawn_tree_lifecycle_time", Float.class);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ancient_tree_spirit.idle");
	private static final RawAnimation WRATH_IDLE = RawAnimation.begin().thenLoop("animation.ancient_tree_spirit.wrath_idle");
	private static final RawAnimation RECOVER = RawAnimation.begin().thenLoop("animation.ancient_tree_spirit.recovery");
	private static final RawAnimation AWAKEN = RawAnimation.begin().thenPlayAndHold("animation.ancient_tree_spirit.awaken");
	private static final RawAnimation DEFEAT = RawAnimation.begin().thenPlayAndHold("animation.ancient_tree_spirit.defeat");
	private static final RawAnimation[] CHARGES = {
			RawAnimation.begin().thenPlayAndHold("animation.ancient_tree_spirit.charge_seed"),
			RawAnimation.begin().thenPlayAndHold("animation.ancient_tree_spirit.charge_root"),
			RawAnimation.begin().thenPlayAndHold("animation.ancient_tree_spirit.charge_sweep")
	};
	private static final String[] ATTACK_ANIMATIONS = {"seed_volley", "root_eruption", "root_sweep"};
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ATTACK_KIND = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RECOVERY = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> AWAKENING = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DEFEAT_TIME = SynchedEntityData.defineId(AncientTreeSpirit.class, EntityDataSerializers.INT);
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
	public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<AncientTreeSpirit>("ambient", 4, test -> {
			if (test.getDataOrDefault(ANIMATION_AWAKENING, 0) > 0 || test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
				return PlayState.STOP;
			}
			return test.setAndContinue(test.getDataOrDefault(ANIMATION_PHASE, 1) == 3 ? WRATH_IDLE : IDLE);
		}));
		// Tree Telegraph: Synced state cancels charge poses immediately and restores them for arriving clients.
		var action = new AnimationController<AncientTreeSpirit>("action", 0, test -> {
			if (test.getDataOrDefault(ANIMATION_AWAKENING, 0) > 0 || test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)) {
				return PlayState.STOP;
			}
			if (test.getDataOrDefault(ANIMATION_WINDUP, 0) > 0) {
				return test.setAndContinue(CHARGES[Math.clamp(test.getDataOrDefault(ANIMATION_ATTACK, 0), 0, 2)]);
			}
			return test.getDataOrDefault(ANIMATION_RECOVERY, 0) > 0 ? test.setAndContinue(RECOVER) : PlayState.STOP;
		});
		for (String name : ATTACK_ANIMATIONS) {
			action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.ancient_tree_spirit." + name));
		}
		action.triggerableAnim("phase_change", RawAnimation.begin().thenPlay("animation.ancient_tree_spirit.phase_change"));
		controllers.add(action);
		// Lifecycle Poses: Saved server clocks keep arriving clients on the current reveal/collapse pose.
		controllers.add(new AnimationController<AncientTreeSpirit>("lifecycle", 0, test -> {
			boolean dying = test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false);
			if (!dying && test.getDataOrDefault(ANIMATION_AWAKENING, 0) == 0) { return PlayState.STOP; }
			test.setAnimation(dying ? DEFEAT : AWAKEN);
			test.controller().setAnimationTime(test.getDataOrDefault(ANIMATION_LIFECYCLE_TIME, 0F));
			return PlayState.CONTINUE;
		}));
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
		data.define(ATTACK_KIND, 0);
		data.define(WINDUP, 0);
		data.define(RECOVERY, 0);
		data.define(AWAKENING, AWAKENING_TICKS);
		data.define(DEFEAT_TIME, 0);
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

	public int attackKind() { return entityData.get(ATTACK_KIND); }
	public int awakening() { return entityData.get(AWAKENING); }
	public int defeatTime() { return entityData.get(DEFEAT_TIME); }

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
		if (isDeadOrDying()) { return; }
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
		if (awakening() > 0) {
			if (awakening() == AWAKENING_TICKS) {
				level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 4, 0.5F);
			}
			if (awakening() % 10 == 0) {
				level.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 1, getZ(), 32, 7, 0.5, 7, 0.1);
			}
			entityData.set(AWAKENING, awakening() - 1);
			// Opening Timing: The reveal occupies the first three seconds of the existing five-second startup.
			cooldown = Math.max(40, cooldown - 1);
			return;
		}
		int nextPhase = BossPhase.advance(phase(), getHealth(), getMaxHealth());
		if (nextPhase != phase()) {
			if (phase() == 1) {
				spawnHealers(level);
			}
			entityData.set(PHASE, nextPhase);
			bossBar.setName(Component.translatable("boss.darkspawn.tree.phase." + nextPhase));
			bossBar.setColor(nextPhase == 3 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
			cooldown = 60;
			entityData.set(WINDUP, 0);
			entityData.set(RECOVERY, 0);
			triggerAnim("action", "phase_change");
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), true, false,
					getX(), getY() + 25, getZ(), 96, 7, 2, 4, 0.15);
			level.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 4, 0.5F);
		}
		if (recovery() > 0) {
			entityData.set(RECOVERY, recovery() - 1);
			if (tickCount % 4 == 0) {
				for (int side = 0; side < 4; side++) {
					double angle = side * Math.PI / 2;
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX() + Math.cos(angle) * 3 * MODEL_SCALE,
							getY() + 11 * MODEL_SCALE, getZ() + Math.sin(angle) * 3 * MODEL_SCALE, 2, 0.2, 2 * MODEL_SCALE, 0.2, 0);
				}
			}
		}
		List<ServerPlayer> nearby = level.players().stream()
				.filter(player -> eligible(player) && distanceToSqr(player) <= 128 * 128).toList();
		if (nearby.isEmpty()) {
			entityData.set(WINDUP, 0);
			return;
		}
		lastActiveTick = level.getGameTime();
		if (windup() > 0) {
			telegraph(level, nearby);
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
			entityData.set(ATTACK_KIND, attack);
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

	private void telegraph(ServerLevel level, List<ServerPlayer> players) {
		if (tickCount % 4 != 0) {
			return;
		}
		// Aerial Warning: A dashed flight path and three-axis reticle distinguish seeds from ground attacks.
		if (attack == 0 || phase() == 3) {
			Vec3 target = attack == 0 ? markedTarget : markedTarget.add(0, 8, 0);
			Vec3 origin = seedOrigin(target);
			for (int i = 0; i <= 16; i++) {
				warningParticle(level, players, SEED_WARNING, origin.lerp(target, i / 16.0));
			}
			for (int sign = -1; sign <= 1; sign += 2) {
				warningParticle(level, players, SEED_WARNING, target.add(sign * 1.5, 0, 0));
				warningParticle(level, players, SEED_WARNING, target.add(0, sign * 1.5, 0));
				warningParticle(level, players, SEED_WARNING, target.add(0, 0, sign * 1.5));
			}
			if (attack == 0) {
				return;
			}
		}
		Vec3 center = attack == 2 ? position() : markedTarget;
		// Fair Warning: The fixed outer ring and the damage check share the exact same radius.
		double radius = attack == 2 ? SWEEP_RADIUS : ROOT_RADIUS;
		int points = attack == 2 ? 64 : 32;
		for (int i = 0; i < points; i++) {
			double angle = i * Math.PI * 2 / points;
			warningParticle(level, players, attack == 2 ? SWEEP_WARNING : ROOT_WARNING,
					center.add(Math.cos(angle) * radius, 0.4, Math.sin(angle) * radius));
		}
	}

	private void warningParticle(ServerLevel level, List<ServerPlayer> players, ParticleOptions particle, Vec3 pos) {
		// Combat Readability: Critical warnings reach arena participants even while flying beyond the usual particle range.
		for (ServerPlayer player : players) {
			level.sendParticles(player, particle, true, true, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	private void executeAttack(ServerLevel level, List<ServerPlayer> players) {
		// Tree Impact: The server starts the release gesture only when the existing attack executes.
		triggerAnim("action", ATTACK_ANIMATIONS[attack]);
		if (attack == 0) {
			seedVolley(level, markedTarget);
		} else if (attack == 1) {
			rootEruption(level, players, markedTarget, ROOT_RADIUS);
		} else {
			rootEruption(level, players, position(), SWEEP_RADIUS);
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
		Vec3 origin = seedOrigin(target);
		level.playSound(null, blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 3, 0.7F);
		for (int i = 0; i < count; i++) {
			SpiritSeed seed = new SpiritSeed(BossEntities.SPIRIT_SEED, level);
			seed.setOwner(this);
			seed.setPos(origin);
			Vec3 aim = target.add((i - (count - 1) / 2.0) * 2.5, 0, 0).subtract(origin);
			seed.shoot(aim.x, aim.y, aim.z, 1.3F, 0);
			level.addFreshEntity(seed);
		}
	}

	private Vec3 seedOrigin(Vec3 target) {
		Vec3 direction = target.subtract(position().add(0, 12 * MODEL_SCALE, 0)).normalize();
		return position().add(direction.x * 6 * MODEL_SCALE, 12 * MODEL_SCALE, direction.z * 6 * MODEL_SCALE);
	}

	private void rootEruption(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
		level.sendParticles(ParticleTypes.COMPOSTER, center.x, center.y + 1, center.z, 100, radius / 2, 2, radius / 2, 0.15);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
				center.x, center.y + 1, center.z, 48, radius / 2, 1, radius / 2, 0.2);
		level.playSound(null, BlockPos.containing(center), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 4, 0.5F);
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
		// Awakening Protection: The unfolding model cannot be farmed for damage; administrative kills still work.
		if (awakening() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { return false; }
		// Participation: Register before lethal damage so the final attacker receives a heart too.
		if (damage > 0 && source.getEntity() instanceof ServerPlayer player && eligible(player)) {
			participants.add(player.getUUID());
		}
		Entity direct = source.getDirectEntity();
		double hitHeight = direct instanceof Projectile ? direct.getY() - getY()
				: direct instanceof Player player ? player.getEyeY() - getY() : -1;
		if (phase() >= 2 && recovery() > 0 && hitHeight >= 8 * MODEL_SCALE && hitHeight <= 14 * MODEL_SCALE) {
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
		if (level() instanceof ServerLevel server && !dead && !isRemoved()) {
			entityData.set(AWAKENING, 0);
			entityData.set(WINDUP, 0);
			entityData.set(RECOVERY, 0);
			stopTriggeredAnim("action", null);
			bossBar.setVisible(false);
			removeHealers(server);
			server.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 4, 0.5F);
		}
		super.die(source);
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (level() instanceof ServerLevel server && !isRemoved()) {
			entityData.set(DEFEAT_TIME, Math.min(deathTime, DEFEAT_TICKS));
			if (deathTime == 20 || deathTime == 45) {
				server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
						true, false, getX(), getY() + (deathTime == 20 ? 22 : 14), getZ(), 80, 6, 2, 5, 0.15);
				server.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 4, 0.5F);
			}
			// Defeat Removal: Only extend the corpse lifetime; vanilla die() still awards loot and XP once.
			if (deathTime >= DEFEAT_TICKS) {
				server.broadcastEntityEvent(this, (byte)60);
				remove(Entity.RemovalReason.KILLED);
			}
		}
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
		output.putInt("darkspawn_awakening", awakening());
		output.putInt("darkspawn_forest_variant", forestVariant);
		output.putLong("darkspawn_last_active_tick", lastActiveTick);
		output.store("darkspawn_participants", Codec.STRING.listOf(), participants.stream().map(UUID::toString).toList());
		output.store("darkspawn_healers", Codec.STRING.listOf(), healers.stream().map(UUID::toString).toList());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		// Existing Saves: Missing awakening data means an already-active encounter, never a fresh reveal.
		entityData.set(AWAKENING, Math.clamp(input.getIntOr("darkspawn_awakening", 0), 0, AWAKENING_TICKS));
		entityData.set(DEFEAT_TIME, Math.clamp(deathTime, 0, DEFEAT_TICKS));
		if (isDeadOrDying()) { bossBar.setVisible(false); }
		entityData.set(PHASE, Math.clamp(input.getIntOr("darkspawn_phase", 1), 1, 3));
		forestVariant = Math.clamp(input.getIntOr("darkspawn_forest_variant", 0), 0, 2);
		lastActiveTick = input.getLongOr("darkspawn_last_active_tick", level().getGameTime());
		bossBar.setName(Component.translatable("boss.darkspawn.tree.phase." + phase()));
		bossBar.setColor(phase() == 3 ? BossEvent.BossBarColor.RED : phase() == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
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
