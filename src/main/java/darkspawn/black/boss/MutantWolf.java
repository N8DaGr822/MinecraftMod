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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class MutantWolf extends Monster implements GeoEntity {
	private int audioPreviousWarning;
	@Override public void baseTick() {
		super.baseTick();
		if (level() instanceof net.minecraft.server.level.ServerLevel && isAlive()) {
			int warning = darkspawn.black.audio.CreatureAudio.warning(this);
			if (!darkspawn.black.audio.CreatureAudio.boss(this) && darkspawn.black.audio.CreatureAudio.startsWarning(audioPreviousWarning, warning))
				playSound(darkspawn.black.audio.DarkspawnSounds.creature(this, "warning"), .8F, 1F);
			audioPreviousWarning = warning;
		}
	}
	@Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return darkspawn.black.audio.DarkspawnSounds.creature(this, "idle"); }
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return darkspawn.black.audio.DarkspawnSounds.creature(this, "hurt"); }
	@Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return darkspawn.black.audio.DarkspawnSounds.creature(this, "death"); }
	@Override public int getAmbientSoundInterval() { return 220; }
	@Override protected float getSoundVolume() { return darkspawn.black.audio.CreatureAudio.boss(this) ? 1.2F : .45F; }

	public static final int AWAKENING_TICKS = 60;
	public static final int DEFEAT_TICKS = 60;
	public static final DataTicket<Integer> ANIMATION_PHASE = DataTickets.create("darkspawn_wolf_phase", Integer.class);
	public static final DataTicket<Integer> ANIMATION_WINDUP = DataTickets.create("darkspawn_wolf_windup", Integer.class);
	public static final DataTicket<Integer> ANIMATION_RECOVERY = DataTickets.create("darkspawn_wolf_recovery", Integer.class);
	public static final DataTicket<Integer> ANIMATION_ATTACK = DataTickets.create("darkspawn_wolf_attack", Integer.class);
	public static final DataTicket<Boolean> ANIMATION_LEAPING = DataTickets.create("darkspawn_wolf_leaping", Boolean.class);
	public static final DataTicket<Integer> ANIMATION_AWAKENING = DataTickets.create("darkspawn_wolf_awakening", Integer.class);
	public static final DataTicket<Float> ANIMATION_LIFECYCLE_TIME = DataTickets.create("darkspawn_wolf_lifecycle_time", Float.class);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mutant_wolf.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mutant_wolf.walk");
	private static final RawAnimation WRATH_IDLE = RawAnimation.begin().thenLoop("animation.mutant_wolf.wrath_idle");
	private static final RawAnimation POUNCE = RawAnimation.begin().thenLoop("animation.mutant_wolf.pounce");
	private static final RawAnimation RECOVER = RawAnimation.begin().thenLoop("animation.mutant_wolf.recovery");
	private static final RawAnimation AWAKEN = RawAnimation.begin().thenPlayAndHold("animation.mutant_wolf.awaken");
	private static final RawAnimation DEFEAT = RawAnimation.begin().thenPlayAndHold("animation.mutant_wolf.defeat");
	private static final RawAnimation[] CHARGES = {
			RawAnimation.begin().thenPlayAndHold("animation.mutant_wolf.charge_pounce"),
			RawAnimation.begin().thenPlayAndHold("animation.mutant_wolf.charge_volley"),
			RawAnimation.begin().thenPlayAndHold("animation.mutant_wolf.charge_burst")
	};
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ATTACK_KIND = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RECOVERY = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> AWAKENING = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DEFEAT_TIME = SynchedEntityData.defineId(MutantWolf.class, EntityDataSerializers.INT);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("boss.darkspawn.wolf.phase.1"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
	private final Set<UUID> participants = new HashSet<>();
	private final List<UUID> pack = new ArrayList<>();
	private Vec3 markedTarget = Vec3.ZERO;
	private Vec3 leapOrigin = Vec3.ZERO;
	private Vec3 volleyTarget = Vec3.ZERO;
	private int leapTicks;
	private int attack;
	private int cooldown = 100;
	private long lastActiveTick = -1;

	public MutantWolf(EntityType<? extends MutantWolf> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 200;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<MutantWolf>("movement", 4, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)
					|| test.getDataOrDefault(ANIMATION_AWAKENING, 0) > 0
					|| test.getDataOrDefault(ANIMATION_WINDUP, 0) > 0
					|| test.getDataOrDefault(ANIMATION_RECOVERY, 0) > 0
					|| test.getDataOrDefault(ANIMATION_LEAPING, false)) { return PlayState.STOP; }
			return test.setAndContinue(test.isMoving() ? WALK : test.getDataOrDefault(ANIMATION_PHASE, 1) == 3 ? WRATH_IDLE : IDLE);
		}));
		// Wolf Telegraph: Synced attack identity keeps all viewers on the same crouch, inhale, or howl pose.
		var action = new AnimationController<MutantWolf>("action", 0, test -> {
			if (test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false)
					|| test.getDataOrDefault(ANIMATION_AWAKENING, 0) > 0) { return PlayState.STOP; }
			if (test.getDataOrDefault(ANIMATION_LEAPING, false)) { return test.setAndContinue(POUNCE); }
			if (test.getDataOrDefault(ANIMATION_WINDUP, 0) > 0) {
				return test.setAndContinue(CHARGES[Math.clamp(test.getDataOrDefault(ANIMATION_ATTACK, 0), 0, 2)]);
			}
			return test.getDataOrDefault(ANIMATION_RECOVERY, 0) > 0 ? test.setAndContinue(RECOVER) : PlayState.STOP;
		});
		for (String name : List.of("frost_volley", "frost_burst", "land", "phase_change")) {
			action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.mutant_wolf." + name));
		}
		controllers.add(action);
		// Wolf Lifecycle: Saved server clocks keep the reveal and collapse aligned for arriving viewers.
		controllers.add(new AnimationController<MutantWolf>("lifecycle", 0, test -> {
			boolean dying = test.getDataOrDefault(DataTickets.IS_DEAD_OR_DYING, false);
			if (!dying && test.getDataOrDefault(ANIMATION_AWAKENING, 0) == 0) { return PlayState.STOP; }
			test.setAnimation(dying ? DEFEAT : AWAKEN);
			test.controller().setAnimationTime(test.getDataOrDefault(ANIMATION_LIFECYCLE_TIME, 0F));
			return PlayState.CONTINUE;
		}));
	}

	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(BossEntities.MAX_HEALTH, 900).add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 128)
				.add(Attributes.ARMOR, 10).add(Attributes.STEP_HEIGHT, 1);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
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
		data.define(LEAPING, false);
		data.define(VARIANT, 0);
		data.define(AWAKENING, AWAKENING_TICKS);
		data.define(DEFEAT_TIME, 0);
	}

	public int phase() { return entityData.get(PHASE); }
	public int attackKind() { return entityData.get(ATTACK_KIND); }
	public int windup() { return entityData.get(WINDUP); }
	public int recovery() { return entityData.get(RECOVERY); }
	public boolean leaping() { return entityData.get(LEAPING); }
	public int variant() { return entityData.get(VARIANT); }
	public int awakening() { return entityData.get(AWAKENING); }
	public int defeatTime() { return entityData.get(DEFEAT_TIME); }

	public void prepareEncounter(ServerLevel level) {
		var biome = level.getBiome(blockPosition());
		entityData.set(VARIANT, biome.is(Biomes.SNOWY_TAIGA) ? 1
				: biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA) ? 2 : 0);
		long players = level.players().stream().filter(player -> eligible(player) && distanceToSqr(player) < 96 * 96).count();
		getAttribute(BossEntities.MAX_HEALTH).setBaseValue(900 + 450 * (Math.clamp(players, 1, 4) - 1));
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
		if (level.getGameTime() - lastActiveTick >= 1200) {
			removePack(level);
			discard();
			return;
		}
		bossBar.setProgress(getHealth() / getMaxHealth());
		if (awakening() > 0) {
			getNavigation().stop();
			setSpeed(0);
			setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
			if (awakening() == AWAKENING_TICKS - 20) {
				level.playSound(null, blockPosition(), darkspawn.black.audio.DarkspawnSounds.creature(this, "phase"), SoundSource.HOSTILE, 1.8F, 1F);
			}
			if (awakening() % 10 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2, getZ(), 24, 3, 1, 3, 0.05);
			}
			entityData.set(AWAKENING, awakening() - 1);
			// Opening Timing: The reveal occupies the first three seconds of the existing five-second startup.
			cooldown = Math.max(40, cooldown - 1);
			return;
		}
		int nextPhase = BossPhase.advance(phase(), getHealth(), getMaxHealth());
		if (nextPhase != phase()) {
			// Pack Budget: Each crossed phase adds a finite wave, including a hit that skips phase two.
			int count = (phase() == 1 ? (variant() == 2 ? 4 : 2) : 0) + (nextPhase == 3 ? 2 : 0);
			spawnPack(level, count);
			entityData.set(PHASE, nextPhase);
			bossBar.setName(Component.translatable("boss.darkspawn.wolf.phase." + nextPhase));
			entityData.set(WINDUP, 0);
			cooldown = 60;
			triggerAnim("action", "phase_change");
			level.playSound(null, blockPosition(), darkspawn.black.audio.DarkspawnSounds.creature(this, "phase"), SoundSource.HOSTILE, 1.8F, 1F);
		}
		List<ServerPlayer> nearby = level.players().stream()
				.filter(player -> eligible(player) && distanceToSqr(player) <= 128 * 128).toList();
		if (nearby.isEmpty()) {
			getNavigation().stop();
			entityData.set(WINDUP, 0);
			entityData.set(LEAPING, false);
			return;
		}
		lastActiveTick = level.getGameTime();
		if (leaping()) {
			tickPounce(level, nearby);
			return;
		}
		if (recovery() > 0) {
			getNavigation().stop();
			entityData.set(RECOVERY, recovery() - 1);
			return;
		}
		if (windup() > 0) {
			getNavigation().stop();
			telegraph(level);
			entityData.set(WINDUP, windup() - 1);
			if (windup() == 0) {
				if (attack == 0 && onGround()) {
					leapOrigin = position();
					leapTicks = 0;
					entityData.set(LEAPING, true);
				} else {
					if (attack == 2) {
						triggerAnim("action", "frost_burst");
						frostBurst(level, nearby, position(), 15);
					} else {
						triggerAnim("action", "frost_volley");
						frostVolley(level, volleyTarget);
					}
					finishAttack();
				}
			}
			return;
		}
		ServerPlayer target = nearby.stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
		if (tickCount % 20 == 0 && distanceToSqr(target) > 12 * 12) {
			getNavigation().moveTo(target, phase() == 3 ? 1.3 : 1.0);
		}
		if (--cooldown <= 0) {
			getNavigation().stop();
			volleyTarget = target.getEyePosition().add(target.getDeltaMovement().scale(10));
			// Telegraph Commitment: The pounce locks a reachable ground point before the player starts dodging.
			Vec3 delta = target.position().subtract(position()).multiply(1, 0, 1);
			if (delta.horizontalDistanceSqr() > 24 * 24) {
				delta = delta.normalize().scale(24);
			}
			markedTarget = groundPosition(level, getX() + delta.x, getZ() + delta.z);
			if (attack == 0 && Math.abs(markedTarget.y - getY()) > 8) {
				attack = 1;
			}
			entityData.set(WINDUP, 40);
			entityData.set(ATTACK_KIND, attack);
			level.playSound(null, blockPosition(), darkspawn.black.audio.DarkspawnSounds.creature(this, "warning"), SoundSource.HOSTILE, 1.8F, 1F);
		}
	}

	private Vec3 groundPosition(ServerLevel level, double x, double z) {
		return Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BlockPos.containing(x, getY(), z)));
	}

	private void telegraph(ServerLevel level) {
		if (tickCount % 4 != 0) { return; }
		Vec3 center = attack == 0 ? markedTarget : attack == 2 ? position() : volleyTarget;
		double radius = attack == 0 ? 7 : attack == 2 ? 15 : 2;
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI / 16;
			level.sendParticles(ParticleTypes.SNOWFLAKE, center.x + Math.cos(angle) * radius,
					center.y + 0.5, center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
		}
	}

	private void tickPounce(ServerLevel level, List<ServerPlayer> players) {
		getNavigation().stop();
		setSpeed(0);
		if (++leapTicks > 1 && onGround()) {
			triggerAnim("action", "land");
			frostBurst(level, players, position(), 7);
			if (phase() == 3) { frostVolley(level, volleyTarget); }
			finishAttack();
			return;
		}
		if (leapTicks > 44 || leapTicks > 1 && horizontalCollision) {
			finishAttack();
			return;
		}
		// Pounce Collision: Move along a fixed arc using normal entity physics; never teleport through blocks.
		if (leapTicks <= 24) {
			double t = leapTicks / 24.0;
			Vec3 next = leapOrigin.add(markedTarget.subtract(leapOrigin).scale(t)).add(0, 32 * t * (1 - t), 0);
			setDeltaMovement(next.subtract(position()));
		} else {
			setDeltaMovement(0, getDeltaMovement().y, 0);
		}
	}

	private void finishAttack() {
		entityData.set(LEAPING, false);
		entityData.set(RECOVERY, phase() == 3 ? 20 : 40);
		cooldown = phase() == 3 ? 40 : 60;
		attack = (attack + 1) % 3;
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
	}

	private void frostVolley(ServerLevel level, Vec3 target) {
		Vec3 aimFrom = position().add(0, 6, 0);
		Vec3 direction = target.subtract(aimFrom).normalize();
		Vec3 origin = aimFrom.add(direction.scale(8));
		Vec3 side = new Vec3(-direction.z, 0, direction.x).normalize();
		int count = 3 + (phase() - 1) * 2 + (variant() == 1 ? 2 : 0);
		for (int i = 0; i < count; i++) {
			FrostShard shard = new FrostShard(BossEntities.FROST_SHARD, level);
			shard.setOwner(this);
			shard.setPos(origin);
			Vec3 aim = target.add(side.scale((i - (count - 1) / 2.0) * 3)).subtract(origin);
			shard.shoot(aim.x, aim.y, aim.z, 1.4F, 0);
			level.addFreshEntity(shard);
		}
		level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3, 0.6F);
	}

	private void frostBurst(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 120, radius / 2, 2, radius / 2, 0.1);
		for (ServerPlayer player : players) {
			Vec3 delta = player.position().subtract(center);
			if (delta.horizontalDistanceSqr() <= radius * radius && delta.y >= -2 && delta.y <= 5 && hasLineOfSight(player)) {
				float damage = phase() == 3 ? 16 : 12;
				if (player.hurtServer(level, damageSources().mobAttack(this), damage)) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, variant() == 1 ? 80 : 40, 1));
					player.knockback(1.2, -delta.x, -delta.z, damageSources().mobAttack(this), damage);
				}
			}
		}
	}

	private void spawnPack(ServerLevel level, int count) {
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2 / count;
			FrostWolf wolf = new FrostWolf(BossEntities.FROST_WOLF, level);
			wolf.setOwner(getUUID());
			wolf.setPos(groundPosition(level, getX() + Math.cos(angle) * 12, getZ() + Math.sin(angle) * 12));
			if (level.getWorldBorder().isWithinBounds(wolf.getBoundingBox()) && level.noCollision(wolf, wolf.getBoundingBox())
					&& !level.containsAnyLiquid(wolf.getBoundingBox()) && level.addFreshEntity(wolf)) {
				pack.add(wolf.getUUID());
			}
		}
	}

	private void removePack(ServerLevel level) {
		for (UUID id : pack) {
			if (level.getEntity(id) instanceof FrostWolf wolf) { wolf.discard(); }
		}
		pack.clear();
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (awakening() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { return false; }
		// Participation: Include the final attacker before death rewards run.
		if (damage > 0 && source.getEntity() instanceof ServerPlayer player && eligible(player)) {
			participants.add(player.getUUID());
		}
		return super.hurtServer(level, source, recovery() > 0 ? damage * 1.25F : damage);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		darkspawn.black.cooking.BossCuisineRewards.grant(this, level, participants, BossKind.MUTANT_WOLF, 160);
		if (!participants.isEmpty()) { spawnAtLocation(level, new ItemStack(BossItems.ALPHA_FANG)); }
		for (UUID id : participants) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && player.level() == level && distanceToSqr(player) <= 160 * 160
					&& !BossHearts.progress(player).hasConsumed(BossKind.MUTANT_WOLF)) {
				ItemEntity heart = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(),
						new ItemStack(BossItems.HEARTS.get(BossKind.MUTANT_WOLF)));
				heart.setTarget(id);
				level.addFreshEntity(heart);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		stopTriggeredAnim("action", null);
		if (level() instanceof ServerLevel server && !dead && !isRemoved()) {
			entityData.set(AWAKENING, 0);
			entityData.set(WINDUP, 0);
			entityData.set(RECOVERY, 0);
			entityData.set(LEAPING, false);
			getNavigation().stop();
			setSpeed(0);
			// Airborne Defeat: Cancel horizontal/upward pounce momentum while retaining normal falling physics.
			setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
			bossBar.setVisible(false);
			removePack(server);
		}
		super.die(source);
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (level() instanceof ServerLevel server && !isRemoved()) {
			entityData.set(DEFEAT_TIME, Math.min(deathTime, DEFEAT_TICKS));
			if (deathTime == 20 || deathTime == 40) {
				server.sendParticles(ParticleTypes.SNOWFLAKE, true, false, getX(), getY() + 3, getZ(), 80, 3, 2, 3, 0.1);
				server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3, 0.5F);
			}
			// Defeat Removal: Vanilla die() owns rewards; extending the corpse lifetime must not award them again.
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
		output.putInt("darkspawn_wolf_variant", variant());
		output.putLong("darkspawn_last_active_tick", lastActiveTick);
		output.store("darkspawn_participants", Codec.STRING.listOf(), participants.stream().map(UUID::toString).toList());
		output.store("darkspawn_pack", Codec.STRING.listOf(), pack.stream().map(UUID::toString).toList());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		// Existing Saves: Missing awakening data means an active wolf, never a new summon animation.
		entityData.set(AWAKENING, Math.clamp(input.getIntOr("darkspawn_awakening", 0), 0, AWAKENING_TICKS));
		entityData.set(DEFEAT_TIME, Math.clamp(deathTime, 0, DEFEAT_TICKS));
		if (isDeadOrDying()) { bossBar.setVisible(false); }
		entityData.set(PHASE, Math.clamp(input.getIntOr("darkspawn_phase", 1), 1, 3));
		entityData.set(VARIANT, Math.clamp(input.getIntOr("darkspawn_wolf_variant", 0), 0, 2));
		lastActiveTick = input.getLongOr("darkspawn_last_active_tick", level().getGameTime());
		bossBar.setName(Component.translatable("boss.darkspawn.wolf.phase." + phase()));
		// Reload Safety: Cancel a saved lunge so returning players always get a fresh telegraph.
		entityData.set(WINDUP, 0);
		entityData.set(LEAPING, false);
		entityData.set(RECOVERY, 0);
		setDeltaMovement(Vec3.ZERO);
		cooldown = awakening() > 0 ? 40 + awakening() : 100;
		participants.clear();
		pack.clear();
		for (String id : input.read("darkspawn_participants", Codec.STRING.listOf()).orElse(List.of())) {
			try { participants.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
		for (String id : input.read("darkspawn_pack", Codec.STRING.listOf()).orElse(List.of())) {
			try { pack.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
		}
	}
}
