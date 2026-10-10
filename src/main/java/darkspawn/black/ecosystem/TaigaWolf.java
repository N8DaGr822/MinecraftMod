package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossItems;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TaigaWolf extends Monster implements darkspawn.black.boss.AnimatedCreature {
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

	private final com.geckolib.animatable.instance.AnimatableInstanceCache animationCache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);
	@Override public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
	public enum Kind {
		DIRE("dire_wolf", 0.9F, 1.25F, 28, 0.29, 5, 10),
		FROSTFANG("frostfang", 0.9F, 1.25F, 30, 0.28, 5, 3),
		RAVAGED("ravaged_wolf", 1.15F, 1.5F, 44, 0.3, 7, 4),
		ALPHA("alpha_dire_wolf", 1.35F, 1.8F, 90, 0.28, 8, 1);

		public final String id;
		public final float width;
		public final float height;
		final int weight;
		private final double health;
		private final double speed;
		private final double damage;
		Kind(String id, float width, float height, double health, double speed, double damage, int weight) {
			this.id = id; this.width = width; this.height = height;
			this.health = health; this.speed = speed; this.damage = damage; this.weight = weight;
		}
	}

	private static final Identifier PACK_SPEED = Darkspawn.id("taiga_pack_speed");
	private static final Identifier FRENZY_SPEED = Darkspawn.id("taiga_frenzy_speed");
	private static final Identifier FRENZY_DAMAGE = Darkspawn.id("taiga_frenzy_damage");
	private static final EntityDataAccessor<Boolean> PACKED = SynchedEntityData.defineId(TaigaWolf.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> FRENZY = SynchedEntityData.defineId(TaigaWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(TaigaWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> LEAPING = SynchedEntityData.defineId(TaigaWolf.class, EntityDataSerializers.BOOLEAN);
	private final Kind kind;
	private int frenzyCooldown;
	private int howlCooldown = 60;
	private int leapCooldown = 60;
	private int leapTicks;
	private Vec3 markedTarget = Vec3.ZERO;
	private Vec3 leapOrigin = Vec3.ZERO;
	private boolean calledReinforcements;
	private boolean reinforcement;
	private UUID packOwner;
	private long expiresAt;
	private int missingOwnerTicks;

	public TaigaWolf(EntityType<? extends TaigaWolf> type, Level level, Kind kind) {
		super(type, level);
		this.kind = kind;
		xpReward = kind == Kind.ALPHA ? 12 : 5;
		if (kind == Kind.FROSTFANG) {
			setPathfindingMalus(PathType.POWDER_SNOW, 0);
		}
		// Variant AI: Configure after the constructor assigns kind, matching the forest ecosystem.
		if (level instanceof ServerLevel) { initializeGoals(); }
	}

	public Kind kind() { return kind; }
	public boolean packed() { return entityData.get(PACKED); }
	public int frenzy() { return entityData.get(FRENZY); }
	public int windup() { return entityData.get(WINDUP); }
	public boolean leaping() { return entityData.get(LEAPING); }
	public boolean reinforcement() { return reinforcement; }

	public static AttributeSupplier.Builder attributes(Kind kind) {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.MOVEMENT_SPEED, kind.speed)
				.add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.ARMOR, kind == Kind.ALPHA ? 4 : 0).add(Attributes.KNOCKBACK_RESISTANCE, kind == Kind.ALPHA ? 0.3 : 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(PACKED, false); data.define(FRENZY, 0); data.define(WINDUP, 0); data.define(LEAPING, false);
	}

	private void initializeGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		if (kind == Kind.ALPHA) {
			goalSelector.addGoal(1, new Goal() {
				{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
				@Override public boolean canUse() { return canStartLeap(); }
				@Override public boolean canContinueToUse() { return windup() > 0 || leaping(); }
				@Override public void start() { beginLeap(); }
			});
		}
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (reinforcement) {
			// Called Pack: Helpers are bounded, saved, and never become a second source of drops or reinforcements.
			if (packOwner == null || level.getGameTime() >= expiresAt) { discard(); return; }
			if (!(level.getEntity(packOwner) instanceof TaigaWolf alpha)) {
				if (++missingOwnerTicks > 40) { discard(); }
				return;
			} else if (!alpha.isAlive() || distanceToSqr(alpha) > 32 * 32) { discard(); return; }
			missingOwnerTicks = 0;
		}
		if (tickCount % 20 == 0) {
			boolean nearbyPack = !level.getEntitiesOfClass(TaigaWolf.class, getBoundingBox().inflate(8),
					wolf -> wolf != this && wolf.isAlive() && distanceToSqr(wolf) <= 64 && hasLineOfSight(wolf)).isEmpty();
			entityData.set(PACKED, nearbyPack);
			setModifier(Attributes.MOVEMENT_SPEED, PACK_SPEED, nearbyPack, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		}
		if (frenzyCooldown > 0) { frenzyCooldown--; }
		if (frenzy() > 0) { entityData.set(FRENZY, frenzy() - 1); }
		if (kind == Kind.RAVAGED && getTarget() != null && getHealth() <= getMaxHealth() * 0.35F && frenzyCooldown == 0) {
			entityData.set(FRENZY, 100);
			frenzyCooldown = 300;
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY(), getZ(), 8, 0.4, 0.2, 0.4, 0);
		}
		setModifier(Attributes.MOVEMENT_SPEED, FRENZY_SPEED, frenzy() > 0, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		setModifier(Attributes.ATTACK_DAMAGE, FRENZY_DAMAGE, frenzy() > 0, 2, AttributeModifier.Operation.ADD_VALUE);
		if (kind != Kind.ALPHA) { return; }
		if (howlCooldown > 0) { howlCooldown--; }
		if (leapCooldown > 0) { leapCooldown--; }
		if (getTarget() != null && getTarget().isAlive() && distanceToSqr(getTarget()) <= 24 * 24 && howlCooldown == 0) { howl(level); }
		if (windup() > 0) {
			getNavigation().stop();
			if (windup() % 4 == 0) {
				for (int i = 0; i < 16; i++) {
					double angle = i * Math.PI / 8;
					level.sendParticles(ParticleTypes.SNOWFLAKE, markedTarget.x + Math.cos(angle) * 1.75,
							markedTarget.y + 0.15, markedTarget.z + Math.sin(angle) * 1.75, 2, 0, 0.1, 0, 0);
				}
			}
			entityData.set(WINDUP, windup() - 1);
			if (windup() == 0) {
				// The warning owns its fixed destination even if the prey runs out of sight before launch.
				if (!onGround()) { finishLeap(); return; }
				leapOrigin = position(); leapTicks = 0; entityData.set(LEAPING, true);
			}
		}
		if (leaping()) { tickLeap(level); }
	}

	private void setModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			Identifier id, boolean active, double amount, AttributeModifier.Operation operation) {
		var instance = getAttribute(attribute);
		if (active && !instance.hasModifier(id)) { instance.addTransientModifier(new AttributeModifier(id, amount, operation)); }
		else if (!active && instance.hasModifier(id)) { instance.removeModifier(id); }
	}

	private boolean canStartLeap() {
		return kind == Kind.ALPHA && leapCooldown == 0 && windup() == 0 && !leaping() && onGround() && getTarget() != null && getTarget().isAlive()
				&& distanceToSqr(getTarget()) >= 9 && distanceToSqr(getTarget()) <= 64
				&& Math.abs(getTarget().getY() - getY()) <= 2 && hasLineOfSight(getTarget());
	}

	boolean beginLeap() {
		if (!canStartLeap()) { return false; }
		// Alpha Telegraph: Mark the landing before launch and retain it if the target moves.
		markedTarget = getTarget().position();
		entityData.set(WINDUP, 20);
		leapCooldown = 120;
		getNavigation().stop();
		return true;
	}

	private void tickLeap(ServerLevel level) {
		getNavigation().stop();
		setSpeed(0);
		if (++leapTicks > 1 && onGround()) {
			if (distanceToSqr(markedTarget) <= 4) {
				level.sendParticles(ParticleTypes.CRIT, markedTarget.x, markedTarget.y + 0.4, markedTarget.z, 25, 0.8, 0.3, 0.8, 0.1);
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(markedTarget, markedTarget).inflate(1.75, 1.5, 1.75))) {
					double dx = target.getX() - markedTarget.x;
					double dz = target.getZ() - markedTarget.z;
					if (prey(target) && dx * dx + dz * dz <= 1.75 * 1.75 && Math.abs(target.getY() - markedTarget.y) <= 1.5 && hasLineOfSight(target)) {
						target.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
					}
				}
			}
			finishLeap();
		} else if (leapTicks > 28 || leapTicks > 1 && horizontalCollision) { finishLeap(); }
		else if (leapTicks <= 16) {
			// Pounce Collision: Follow a fixed low arc through normal movement, never teleport through blocks.
			double t = leapTicks / 16.0;
			Vec3 next = leapOrigin.add(markedTarget.subtract(leapOrigin).scale(t)).add(0, 7.2 * t * (1 - t), 0);
			setDeltaMovement(next.subtract(position()));
		} else { setDeltaMovement(0, getDeltaMovement().y, 0); }
	}

	private void finishLeap() {
		entityData.set(WINDUP, 0); entityData.set(LEAPING, false);
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
	}

	private static boolean prey(LivingEntity entity) {
		return entity.isAlive() && (entity instanceof Rabbit || entity instanceof Sheep
				|| entity instanceof Player player && !player.isCreative() && !player.isSpectator());
	}

	private void howl(ServerLevel level) {
		howlCooldown = 200;
		playSound(darkspawn.black.audio.DarkspawnSounds.creature(this, "warning"), 1F, 1F);
		var wolves = level.getEntitiesOfClass(TaigaWolf.class, getBoundingBox().inflate(12),
				wolf -> wolf.isAlive() && distanceToSqr(wolf) <= 144);
		for (TaigaWolf wolf : wolves) {
			if (hasLineOfSight(wolf)) { wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 100)); }
		}
		level.sendParticles(ParticleTypes.CLOUD, getX(), getEyeY(), getZ(), 20, 1, 0.2, 1, 0.03);
		if (calledReinforcements || reinforcement) { return; }
		calledReinforcements = true;
		int spawned = 0;
		for (int attempt = 0; attempt < 12 && spawned < 2 && wolves.size() + spawned < 6; attempt++) {
			BlockPos pos = blockPosition().offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
			if (!level.hasChunkAt(pos)) { continue; }
			pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
			if (Math.abs(pos.getY() - getY()) > 3 || !level.getBlockState(pos.below()).isSolidRender()) { continue; }
			TaigaWolf wolf = new TaigaWolf(TaigaEcosystem.WOLVES.get(Kind.DIRE), level, Kind.DIRE);
			wolf.setPos(Vec3.atBottomCenterOf(pos));
			if (!level.getWorldBorder().isWithinBounds(wolf.getBoundingBox()) || !level.noCollision(wolf)
					|| level.containsAnyLiquid(wolf.getBoundingBox())) { continue; }
			wolf.reinforcement = true; wolf.packOwner = getUUID(); wolf.expiresAt = level.getGameTime() + 600;
			wolf.xpReward = 0;
			wolf.setPersistenceRequired();
			wolf.setTarget(getTarget());
			if (level.addFreshEntity(wolf)) { spawned++; }
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (windup() > 0 || leaping()) { return false; }
		boolean hit = super.doHurtTarget(level, target);
		if (hit && kind == Kind.FROSTFANG && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60));
		}
		return hit;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return kind != Kind.ALPHA && super.causeFallDamage(distance, multiplier, source);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (!killedByPlayer || reinforcement) { return; }
		spawnAtLocation(level, new ItemStack(kind == Kind.RAVAGED ? Items.RABBIT_HIDE : Items.BONE));
		// Herald Reward: Alpha Fang already belongs to the boss's smithing progression; grant its summon instead.
		if (kind == Kind.ALPHA) { spawnAtLocation(level, new ItemStack(BossItems.MOONLIT_FANG)); }
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("darkspawn_called_pack", calledReinforcements);
		output.putBoolean("darkspawn_reinforcement", reinforcement);
		if (packOwner != null) { output.putString("darkspawn_pack_owner", packOwner.toString()); }
		output.putLong("darkspawn_pack_expires", expiresAt);
		output.putInt("darkspawn_howl_cooldown", howlCooldown);
		output.putInt("darkspawn_frenzy_cooldown", frenzyCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		calledReinforcements = input.getBooleanOr("darkspawn_called_pack", false);
		reinforcement = input.getBooleanOr("darkspawn_reinforcement", false);
		try { packOwner = UUID.fromString(input.getStringOr("darkspawn_pack_owner", "")); }
		catch (IllegalArgumentException ignored) { packOwner = null; }
		expiresAt = input.getLongOr("darkspawn_pack_expires", 0);
		if (reinforcement) { xpReward = 0; }
		howlCooldown = Math.clamp(input.getIntOr("darkspawn_howl_cooldown", 60), 40, 200);
		frenzyCooldown = Math.clamp(input.getIntOr("darkspawn_frenzy_cooldown", 60), 40, 300);
		// Reload Safety: Cancel unsignaled pounces and rebuild proximity/frenzy modifiers from live state.
		finishLeap(); leapCooldown = 60;
		entityData.set(PACKED, false); entityData.set(FRENZY, 0);
		getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(PACK_SPEED);
		getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FRENZY_SPEED);
		getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(FRENZY_DAMAGE);
	}
}
