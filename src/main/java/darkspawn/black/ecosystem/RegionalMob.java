package darkspawn.black.ecosystem;

import darkspawn.black.boss.BossBolt;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossProfile;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RegionalMob extends Monster implements RangedAttackMob {
	private static final EntityDataAccessor<Integer> WARNING = SynchedEntityData.defineId(RegionalMob.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> HIDING = SynchedEntityData.defineId(RegionalMob.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(RegionalMob.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ACTIVE = SynchedEntityData.defineId(RegionalMob.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SHIELD_ANGLE = SynchedEntityData.defineId(RegionalMob.class, EntityDataSerializers.INT);
	private final RegionalKind kind;
	private int attackCooldown = 40;
	private int blinkCooldown;
	private int pulseCooldown;
	private int leapTicks;
	private int growthTicks;
	private int missingOwnerTicks;
	private int patchTicks;
	private Vec3 patchPosition;
	private Vec3 ghostOrigin;
	private Vec3 mark;
	private Vec3 leapOrigin;
	private Vec3 guideDestination;
	private Vec3 guideStart;
	private UUID guidePlayer;
	private long guideLastSeen;
	private boolean claimed;
	private boolean called;
	private boolean reconstructed;
	private boolean helper;
	private UUID ownerId;
	private long expiresAt;
	private ItemStack carried = ItemStack.EMPTY;

	public RegionalMob(EntityType<? extends RegionalMob> type, Level level, RegionalKind kind) {
		super(type, level);
		this.kind = kind;
		xpReward = kind.temper == RegionalKind.Temper.PASSIVE ? 0 : kind.herald() ? 12 : 4;
		// Variant Construction: Install navigation/goals after kind exists; Mob invokes its own setup earlier.
		if (kind.movement == RegionalKind.Move.AIR) {
			navigation = new FlyingPathNavigation(this, level);
			moveControl = new FlyingMoveControl<>(this, 20, true);
			setNoGravity(true);
		} else if (kind.movement == RegionalKind.Move.WATER) {
			navigation = new WaterBoundPathNavigation(this, level);
			moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, .2F, .1F, false);
			setPathfindingMalus(PathType.WATER, 0);
		} else if (kind.movement == RegionalKind.Move.CLIMB) { navigation = new WallClimberNavigation(this, level); }
		if (kind.region == BossProfile.ICE_WYRM) { setPathfindingMalus(PathType.POWDER_SNOW, 0); }
		if (level instanceof ServerLevel) { initializeGoals(); }
	}

	public RegionalKind kind() { return kind; }
	public int warning() { return entityData.get(WARNING); }
	public int active() { return entityData.get(ACTIVE); }
	public int shieldAngle() { return entityData.get(SHIELD_ANGLE); }
	public boolean hiding() { return entityData.get(HIDING); }
	public boolean helper() { return helper; }
	public boolean claimed() { return claimed; }

	public static AttributeSupplier.Builder attributes(RegionalKind kind) {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.MOVEMENT_SPEED, kind.speed)
				.add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.ARMOR, kind.armor)
				.add(Attributes.FOLLOW_RANGE, 24).add(Attributes.FLYING_SPEED, kind.speed)
				.add(Attributes.KNOCKBACK_RESISTANCE, kind == RegionalKind.ROTTED_ZOMBIE ? .5 : kind.herald() ? .3 : 0);
	}

	@Override protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(WARNING, 0); data.define(HIDING, false); data.define(CLIMBING, false); data.define(ACTIVE, 0); data.define(SHIELD_ANGLE, 0);
	}

	private void initializeGoals() {
		goalSelector.addGoal(0, new Goal() {
			{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
			@Override public boolean canUse() { return warning() > 0 || leapTicks > 0 || kind.movement == RegionalKind.Move.STILL
					|| hiding() && (kind.behavior == RegionalKind.Behavior.STALK || kind.behavior == RegionalKind.Behavior.AMBUSH); }
			@Override public void start() { getNavigation().stop(); }
		});
		if (kind.movement != RegionalKind.Move.WATER) { goalSelector.addGoal(1, new FloatGoal(this)); }
		if (kind == RegionalKind.BRUTE_ZOMBIE) {
			getNavigation().setCanOpenDoors(true);
			goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
		}
		if (kind.temper != RegionalKind.Temper.PASSIVE && kind.movement != RegionalKind.Move.STILL) {
			goalSelector.addGoal(3, kind.ranged() ? new RangedAttackGoal(this, 1, 60, 16) : new MeleeAttackGoal(this, 1, false));
			targetSelector.addGoal(1, new HurtByTargetGoal(this));
			if (kind.temper == RegionalKind.Temper.HOSTILE) {
				targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
						(prey, server) -> kind != RegionalKind.BONE_VULTURE || prey.getHealth() <= prey.getMaxHealth() / 2));
			}
			if (kind == RegionalKind.BONE_RAPTOR || kind == RegionalKind.JUNGLE_STALKER || kind == RegionalKind.TREE_VIPER || kind == RegionalKind.BONE_VULTURE) {
				targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
				targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
			}
		}
		if (kind.movement == RegionalKind.Move.STILL && kind.temper == RegionalKind.Temper.HOSTILE) {
			targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		}
		if (kind.temper == RegionalKind.Temper.PASSIVE && kind.behavior != RegionalKind.Behavior.GUIDE) {
			goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6, 1, 1.3));
		}
		if (kind.movement == RegionalKind.Move.WATER) { goalSelector.addGoal(5, new RandomSwimmingGoal(this, .8, 50)); }
		else if (kind.movement == RegionalKind.Move.AIR) {
			goalSelector.addGoal(5, new RandomStrollGoal(this, .8, 40) {
				@Override protected Vec3 getPosition() {
					Vec3 candidate = position().add(random.nextInt(13) - 6, random.nextInt(7) - 2, random.nextInt(13) - 6);
					return safePosition(candidate, false) ? candidate : null;
				}
			});
		} else if (kind.movement != RegionalKind.Move.STILL) { goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .8)); }
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
	}

	@Override public boolean canBreatheUnderwater() { return kind != null && (kind.movement == RegionalKind.Move.WATER || kind == RegionalKind.BOGLING) || super.canBreatheUnderwater(); }
	@Override public boolean checkSpawnObstruction(LevelReader level) { return kind.movement == RegionalKind.Move.WATER ? level.isUnobstructed(this) : super.checkSpawnObstruction(level); }
	@Override public boolean onClimbable() { return entityData.get(CLIMBING); }
	@Override public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return kind.temper == RegionalKind.Temper.HOSTILE || getTarget() == player; }
	@Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return kind.movement != RegionalKind.Move.AIR && kind.behavior != RegionalKind.Behavior.POUNCE
				&& kind.behavior != RegionalKind.Behavior.WIDOW && super.causeFallDamage(distance, multiplier, source);
	}
	@Override public void travel(Vec3 input) {
		if (kind.movement == RegionalKind.Move.AIR) { travelFlying(input, .04F); }
		else if (kind.movement == RegionalKind.Move.WATER && isInWater()) {
			moveRelative(getSpeed(), input); move(MoverType.SELF, getDeltaMovement()); setDeltaMovement(getDeltaMovement().scale(.9));
		} else { super.travel(input); }
	}

	@Override protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (attackCooldown > 0) { attackCooldown--; }
		if (blinkCooldown > 0) { blinkCooldown--; }
		if (pulseCooldown > 0) { pulseCooldown--; }
		if (active() > 0) { entityData.set(ACTIVE, active() - 1); }
		entityData.set(CLIMBING, kind.movement == RegionalKind.Move.CLIMB && horizontalCollision);
		if (kind == RegionalKind.VOID_SENTINEL) { entityData.set(SHIELD_ANGLE, (tickCount * 2) % 360); }
		if (helper && expireHelper(level)) { return; }
		tickPatch(level);
		if (kind == RegionalKind.GIANT_CRAB && isInWater()) { setDeltaMovement(getDeltaMovement().add(0, -.03, 0)); }
		LivingEntity target = getTarget();
		if (target != null && (!validPrey(target) || level.getDifficulty() == Difficulty.PEACEFUL)) { setTarget(null); target = null; }
		if (kind.behavior == RegionalKind.Behavior.GUIDE) { tickGuide(level); }
		if (kind == RegionalKind.SHADOW_GUARDIAN) {
			if (!hasHome()) { setHomeTo(blockPosition(), 16); }
			if (!isWithinHome(blockPosition())) { setTarget(null); target = null; getNavigation().moveTo(getHomePosition().getX(), getHomePosition().getY(), getHomePosition().getZ(), 1); }
		}
		if (kind.behavior == RegionalKind.Behavior.STARE && target == null && tickCount % 10 == 0) {
			Player player = level.getNearestPlayer(this, 16);
			if (player != null && validPrey(player) && lookingAt(player) && hasLineOfSight(player)) { setTarget(player); }
		}
		if (kind.behavior == RegionalKind.Behavior.STALK || kind.behavior == RegionalKind.Behavior.AMBUSH) {
			boolean hidden = target == null || (kind.behavior == RegionalKind.Behavior.STALK ? lookingAt(target) : distanceToSqr(target) > 16);
			entityData.set(HIDING, hidden);
			if (hidden) { getNavigation().stop(); setZza(0); setXxa(0); }
		}
		if (kind == RegionalKind.FOSSIL_SCORPION || kind == RegionalKind.FROSTLING || kind == RegionalKind.BOGLING || kind == RegionalKind.TREE_VIPER) {
			entityData.set(HIDING, target == null && warning() == 0);
		}
		if (kind.behavior == RegionalKind.Behavior.LARVA && !helper && ++growthTicks >= 1200) { matureLarva(level); }
		if (kind.behavior == RegionalKind.Behavior.STATIC && level.isThundering() && tickCount % 20 == 0) {
			entityData.set(ACTIVE, 30); particles(level, BossBolt.Kind.LIGHTNING, position(), 4);
		}
		if (kind.behavior == RegionalKind.Behavior.MYCELIUM && tickCount % 40 == 0 && level.getBlockState(blockPosition().below()).is(Blocks.MYCELIUM)) {
			heal(1); particles(level, BossBolt.Kind.SPORE, position(), 8);
		}
		if (kind.behavior == RegionalKind.Behavior.GRAZE && tickCount % 100 == 0) {
			for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-2, -1, -2), blockPosition().offset(2, 1, 2))) {
				if (level.getBlockState(pos).is(Blocks.CHORUS_PLANT) || level.getBlockState(pos).is(Blocks.CHORUS_FLOWER)) {
					heal(1); entityData.set(ACTIVE, 30); particles(level, BossBolt.Kind.VOID, position(), 6); break;
				}
			}
		}
		if ((kind.behavior == RegionalKind.Behavior.SCAVENGE || kind.behavior == RegionalKind.Behavior.THIEF) && tickCount % 10 == 0) { scavenge(level); }
		if (kind == RegionalKind.LOST_SOUL && target != null && distanceToSqr(target) < 100) {
			// Soul Movement: A brief ghost pass has a bounded exit; normal flight remains collision-aware.
			noPhysics = active() > 0;
			if (horizontalCollision && blinkCooldown == 0) { ghostOrigin = position(); entityData.set(ACTIVE, 10); blinkCooldown = 100; }
			if (noPhysics) { setDeltaMovement(target.getEyePosition().subtract(position()).normalize().scale(.15)); }
		} else { noPhysics = false; }
		if (!noPhysics && ghostOrigin != null) {
			if (!level.noCollision(this) && safePosition(ghostOrigin, false)) { teleportTo(ghostOrigin.x, ghostOrigin.y, ghostOrigin.z); }
			ghostOrigin = null;
		}
		if (target != null && distanceToSqr(target) < 24 * 24) { tickSpecialMovement(level, target); }
		if (kind.movement == RegionalKind.Move.WATER && !isInWater() && tickCount % 40 == 0) { hurtServer(level, damageSources().dryOut(), 2); }
		if (leapTicks > 0) { tickLeap(level); return; }
		if (warning() > 0 && mark != null) {
			getNavigation().stop(); setZza(0); setXxa(0);
			if (warning() % 5 == 0) { warningParticles(level); }
			entityData.set(WARNING, warning() - 1);
			if (warning() == 0) { executeAttack(level); }
			return;
		}
		if (target != null && attackCooldown == 0 && distanceToSqr(target) <= specialRange() * specialRange()
				&& hasLineOfSight(target) && usesSpecialAttack()) { beginAttack(target); }
		if (target != null && pulseCooldown == 0) { supportPulse(level); }
	}

	private boolean validPrey(LivingEntity target) {
		return target.isAlive() && !(target instanceof RegionalMob ally && ally.kind.region == kind.region)
				&& (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
	}
	private boolean lookingAt(LivingEntity target) {
		return target.getLookAngle().dot(getEyePosition().subtract(target.getEyePosition()).normalize()) > .85;
	}
	private double specialRange() { return kind.movement == RegionalKind.Move.STILL ? 4 : kind.ranged() ? 16 : 7; }
	private boolean usesSpecialAttack() {
		return switch (kind.behavior) {
			case SMASH, POUNCE, DIVE, CHARGE, CONSTRICT, VINES, SIREN, SEA_GRAB, WIDOW, DARK_BURST, SHOCKWAVE, SHADOW_CLEAVE, MAGMA, MYCELIUM -> true;
			default -> kind.ranged();
		};
	}
	boolean beginAttack(LivingEntity target) {
		if (!validPrey(target) || warning() > 0 || leapTicks > 0 || attackCooldown > 0 || !hasLineOfSight(target)
				|| distanceToSqr(target) > specialRange() * specialRange()) { return false; }
		if (kind.behavior == RegionalKind.Behavior.SCAVENGE && target.getHealth() > target.getMaxHealth() / 2) { return false; }
		// Combat Telegraph: Lock a world point before warning, so movement can dodge every special strike.
		mark = target.position(); entityData.set(WARNING, kind.herald() ? 25 : 20); attackCooldown = kind.herald() ? 100 : 70;
		getNavigation().stop(); return true;
	}
	@Override public void performRangedAttack(LivingEntity target, float distanceFactor) { beginAttack(target); }
	@Override public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!(target instanceof LivingEntity living) || !validPrey(living) || warning() > 0 || leapTicks > 0 || hiding()
				|| kind.temper == RegionalKind.Temper.PASSIVE || kind.behavior == RegionalKind.Behavior.SCAVENGE && living.getHealth() > living.getMaxHealth() / 2) { return false; }
		if (usesSpecialAttack()) { return beginAttack(living); }
		boolean hit = kind.behavior == RegionalKind.Behavior.AMBUSH && pulseCooldown == 0
				? living.hurtServer(level, damageSources().mobAttack(this), (float)kind.damage * 1.5F) : super.doHurtTarget(level, target);
		if (hit) { applyBite(living); }
		return hit;
	}
	private void applyBite(LivingEntity target) {
		switch (kind.behavior) {
			case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60));
			case FROST, THAW -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60));
			case HEX -> target.addEffect(new MobEffectInstance(random.nextBoolean() ? MobEffects.WEAKNESS : MobEffects.SLOWNESS, 60));
			case SOUL_TOUCH -> EcosystemEffects.fracture(target, false);
			case FIRE -> target.igniteForSeconds(2);
			case AMBUSH -> { if (pulseCooldown == 0) { target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60)); pulseCooldown = 160; } }
			default -> { }
		}
	}
	public void onProjectileHit(LivingEntity target) {
		if (kind == RegionalKind.SOUL_KEEPER) { EcosystemEffects.fracture(target, true); }
		if (kind == RegionalKind.WEB_SPITTER || kind == RegionalKind.SHADOW_SPITTER || kind == RegionalKind.SPOREWALKER) {
			patchPosition = target.position(); patchTicks = 100;
		}
	}
	private BossBolt.Kind projectileKind() {
		return switch (kind.behavior) {
			case LIGHTNING, DIVE, STATIC -> BossBolt.Kind.LIGHTNING;
			case FROST, ICE_BREATH -> BossBolt.Kind.FROST;
			case POTION, HEX -> BossBolt.Kind.HEX;
			case WEB, WIDOW -> BossBolt.Kind.WEB;
			case ACID, DARK_BURST, SHADOW_CLEAVE -> BossBolt.Kind.ACID;
			case SPORE, MYCELIUM, BLINK_SPORE -> BossBolt.Kind.SPORE;
			case MAGMA, FIRE -> BossBolt.Kind.FIRE;
			case SOUL_BOLT, SOUL_KEEPER, SOUL_TOUCH -> BossBolt.Kind.SOUL;
			case OBSERVE, SENTINEL, BLINK, RIFT -> BossBolt.Kind.VOID;
			default -> BossBolt.Kind.PHYSICAL;
		};
	}
	private void warningParticles(ServerLevel level) {
		for (int i = 0; i < 12; i++) {
			double angle = Math.PI * 2 * i / 12;
			particles(level, projectileKind(), mark.add(Math.cos(angle) * strikeRadius(), .1, Math.sin(angle) * strikeRadius()), 1);
		}
	}
	private double strikeRadius() { return kind.herald() ? 2.5 : 1.5; }
	private void executeAttack(ServerLevel level) {
		if (mark == null) { return; }
		entityData.set(ACTIVE, 15);
		if (kind.behavior == RegionalKind.Behavior.POUNCE || kind.behavior == RegionalKind.Behavior.DIVE
				|| kind.behavior == RegionalKind.Behavior.CHARGE || kind.behavior == RegionalKind.Behavior.WIDOW) {
			leapOrigin = position(); leapTicks = 1; return;
		}
		if (kind.ranged()) {
			// Projectile Preview: Reuse existing colliding bolts, with ecosystem damage and a fixed warned aim.
			BossBolt bolt = new BossBolt(BossEntities.BOSS_BOLT, level);
			bolt.configure(this, projectileKind(), (float)kind.damage);
			bolt.setPos(getX(), getEyeY(), getZ());
			Vec3 aim = mark.add(0, .7, 0).subtract(bolt.position());
			bolt.shoot(aim.x, aim.y, aim.z, 1.1F, 0); level.addFreshEntity(bolt);
			if ((kind == RegionalKind.TITAN_SPAWN || kind == RegionalKind.MUTANT_HUSKLING) && distanceToSqr(mark) < 16) { strike(level); }
		} else { strike(level); }
		if (kind.behavior == RegionalKind.Behavior.MAGMA || kind.behavior == RegionalKind.Behavior.MYCELIUM) {
			patchPosition = mark; patchTicks = 100;
		}
		if (kind.behavior == RegionalKind.Behavior.DARK_BURST) { discard(); }
		mark = null;
	}
	private void tickLeap(ServerLevel level) {
		getNavigation().stop(); setZza(0); setXxa(0);
		if (mark == null || leapOrigin == null) { leapTicks = 0; return; }
		if (horizontalCollision || leapTicks > 22) {
			if (distanceToSqr(mark) <= 9) { strike(level); }
			leapTicks = 0; mark = null; setDeltaMovement(Vec3.ZERO); return;
		}
		double t = Math.min(1, leapTicks / 18.0);
		double arc = kind.behavior == RegionalKind.Behavior.CHARGE ? 0 : 5 * t * (1 - t);
		Vec3 next = leapOrigin.lerp(mark, t).add(0, arc, 0);
		setDeltaMovement(next.subtract(position())); leapTicks++;
	}
	private void strike(ServerLevel level) {
		if (mark == null || distanceToSqr(mark) > specialRange() * specialRange() + 16) { return; }
		particles(level, projectileKind(), mark.add(0, .4, 0), 24);
		if (kind == RegionalKind.LEVIATHAN_SPAWN) {
			entityData.set(ACTIVE, 60);
			for (var boat : level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,
					new AABB(mark, mark).inflate(strikeRadius(), 1.5, strikeRadius()))) {
				if (hasLineOfSight(boat) && boat.position().distanceToSqr(mark) < strikeRadius() * strikeRadius()) {
					boat.hurtServer(level, damageSources().mobAttack(this), 2);
				}
			}
		}
		for (LivingEntity prey : level.getEntitiesOfClass(LivingEntity.class, new AABB(mark, mark).inflate(strikeRadius(), 1.5, strikeRadius()))) {
			if (prey == this || !validPrey(prey) || !(prey instanceof Player || prey == getTarget()) || !hasLineOfSight(prey)
					|| Math.abs(prey.getY() - mark.y) > 1.5 || prey.position().multiply(1, 0, 1).distanceToSqr(mark.multiply(1, 0, 1)) > strikeRadius() * strikeRadius()) { continue; }
			if (kind == RegionalKind.SHADOW_PRAETORIAN && mark.subtract(position()).normalize().dot(prey.position().subtract(position()).normalize()) < .3) { continue; }
			if (!prey.hurtServer(level, damageSources().mobAttack(this), (float)kind.damage)) { continue; }
			switch (kind.behavior) {
				case CONSTRICT, VINES, SEA_GRAB, WIDOW -> prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
				case DARK_BURST, SHADOW_CLEAVE -> prey.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60));
				case MAGMA -> prey.igniteForSeconds(2);
				case DIVE -> prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20));
				case SIREN -> { Vec3 pull = position().subtract(prey.position()).normalize().scale(.25); prey.push(pull.x, 0, pull.z); }
				default -> prey.knockback(.7, getX() - prey.getX(), getZ() - prey.getZ(), damageSources().mobAttack(this), (float)kind.damage);
			}
		}
	}
	private void tickPatch(ServerLevel level) {
		if (patchTicks <= 0 || patchPosition == null) { return; }
		patchTicks--;
		if (patchTicks % 5 == 0) {
			for (int i = 0; i < 10; i++) {
				double angle = i * Math.PI / 5;
				particles(level, projectileKind(), patchPosition.add(Math.cos(angle) * 1.5, .15, Math.sin(angle) * 1.5), 1);
			}
		}
		// Temporary Terrain Preview: One bounded particle patch per caster, with a fresh one-second warning and no block edits.
		if (patchTicks > 80 || patchTicks % 20 != 0) { return; }
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(patchPosition, patchPosition).inflate(1.5, 1, 1.5))) {
			if (!validPrey(player) || !hasLineOfSight(player) || player.position().distanceToSqr(patchPosition) > 3) { continue; }
			if (kind == RegionalKind.WEB_SPITTER) { player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 2)); }
			else if (kind == RegionalKind.SHADOW_SPITTER) { player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30)); }
			else if (player.hurtServer(level, damageSources().mobAttack(this), 2)) {
				if (kind.behavior == RegionalKind.Behavior.MAGMA) { player.igniteForSeconds(1); }
				else { player.addEffect(new MobEffectInstance(MobEffects.POISON, 30)); }
			}
		}
	}

	private void tickSpecialMovement(ServerLevel level, LivingEntity target) {
		boolean retreat = kind == RegionalKind.YOUNG_WYRM && getHealth() < getMaxHealth() * .25
				|| kind == RegionalKind.WATCHER && distanceToSqr(target) < 25 || !carried.isEmpty()
				|| kind == RegionalKind.LEVIATHAN_SPAWN && active() > 0;
		if (retreat && tickCount % 10 == 0) {
			Vec3 away = position().subtract(target.position()).normalize().scale(6).add(position());
			if (kind.movement == RegionalKind.Move.WATER) {
				away = away.add(0, -2, 0);
				if (level.hasChunkAt(BlockPos.containing(away)) && level.getFluidState(BlockPos.containing(away)).is(net.minecraft.tags.FluidTags.WATER)) {
					getNavigation().moveTo(away.x, away.y, away.z, 1.3);
				}
			} else if (safePosition(away, kind.movement != RegionalKind.Move.AIR)) { getNavigation().moveTo(away.x, away.y, away.z, 1.3); }
		}
		if ((kind.behavior == RegionalKind.Behavior.BLINK || kind.behavior == RegionalKind.Behavior.BLINK_SPORE
				|| kind.behavior == RegionalKind.Behavior.STARE || kind.behavior == RegionalKind.Behavior.RIFT || kind.behavior == RegionalKind.Behavior.SENTINEL)
				&& blinkCooldown == 0 && warning() == 0 && leapTicks == 0) { blink(level); }
		if (kind == RegionalKind.TUNNEL_WIDOW && getHealth() < getMaxHealth() / 2 && horizontalCollision) {
			setDeltaMovement(getDeltaMovement().add(0, .12, 0));
		}
	}
	boolean blink(ServerLevel level) {
		blinkCooldown = 100;
		for (int i = 0; i < 12; i++) {
			Vec3 destination = position().add(random.nextInt(13) - 6, random.nextInt(5) - 2, random.nextInt(13) - 6);
			if (kind.movement != RegionalKind.Move.AIR) { destination = groundPosition(destination); }
			if (destination == null || !safePosition(destination, kind.movement != RegionalKind.Move.AIR)) { continue; }
			particles(level, BossBolt.Kind.VOID, position(), 16);
			teleportTo(destination.x, destination.y, destination.z); getNavigation().stop(); setDeltaMovement(Vec3.ZERO);
			particles(level, BossBolt.Kind.VOID, position(), 16); entityData.set(ACTIVE, 12); return true;
		}
		return false;
	}
	private Vec3 groundPosition(Vec3 candidate) {
		BlockPos pos = BlockPos.containing(candidate);
		if (!level().hasChunkAt(pos)) { return null; }
		// Safe Relocation: Search only nearby loaded ground; never land on the Nether roof or an End void column.
		for (int dy = 3; dy >= -4; dy--) {
			BlockPos floor = pos.offset(0, dy - 1, 0);
			if (level().getBlockState(floor).isSolidRender()) { return Vec3.atBottomCenterOf(floor.above()); }
		}
		return null;
	}
	private boolean safePosition(Vec3 destination, boolean ground) {
		BlockPos pos = BlockPos.containing(destination);
		if (!level().hasChunkAt(pos) || pos.getY() < level().getMinY() + 2 || pos.getY() + kind.height >= level().getMaxY()
				|| kind.region.fireproof() && pos.getY() >= 120) { return false; }
		AABB bounds = getBoundingBox().move(destination.subtract(position()));
		if (!level().getWorldBorder().isWithinBounds(bounds) || !level().noCollision(this, bounds) || level().containsAnyLiquid(bounds)) { return false; }
		if (ground) { return level().getBlockState(pos.below()).isSolidRender(); }
		// Even flying teleports require nearby support: movement can fly over voids, a blink cannot strand a creature there.
		for (int down = 1; down <= 12; down++) { if (level().getBlockState(pos.below(down)).isSolidRender()) { return true; } }
		return false;
	}

	private void supportPulse(ServerLevel level) {
		pulseCooldown = 120;
		if (kind.behavior == RegionalKind.Behavior.BUFF || kind.behavior == RegionalKind.Behavior.HIVE) {
			for (RegionalMob ally : level.getEntitiesOfClass(RegionalMob.class, getBoundingBox().inflate(8))) {
				if (ally.kind.region == kind.region && ally.isAlive() && hasLineOfSight(ally)) { ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 80)); }
			}
		}
		if (!called && !helper) {
			if (kind == RegionalKind.SOUL_KEEPER) { spawnHelpers(level, RegionalKind.LOST_SOUL, false); }
			if (kind == RegionalKind.VOID_SENTINEL) { spawnHelpers(level, RegionalKind.VOIDLING, false); }
		}
		if (kind == RegionalKind.THUNDER_ROC && getTarget() != null && distanceToSqr(getTarget()) < 25 && hasLineOfSight(getTarget())) {
			getTarget().knockback(.6, getX() - getTarget().getX(), getZ() - getTarget().getZ(), damageSources().mobAttack(this), 0);
			particles(level, BossBolt.Kind.LIGHTNING, position(), 18);
		}
	}
	private void spawnHelpers(ServerLevel level, RegionalKind childKind, boolean orphaned) {
		if (called || helper) { return; }
		// Population Budget: Commit the single call even if placement fails; children cannot hatch, mature, or call more.
		called = true;
		int nearby = level.getEntitiesOfClass(RegionalMob.class, getBoundingBox().inflate(12), mob -> mob.isAlive() && mob.kind.region == kind.region).size();
		for (int i = 0, spawned = 0; i < 12 && spawned < 2 && nearby + spawned < 6; i++) {
			Vec3 candidate = position().add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			if (childKind.movement != RegionalKind.Move.AIR) { candidate = groundPosition(candidate); }
			if (candidate == null || !level.hasChunkAt(BlockPos.containing(candidate))) { continue; }
			RegionalMob child = new RegionalMob(RegionalEcosystems.MOBS.get(childKind), level, childKind);
			child.setPos(candidate); child.helper = true; child.ownerId = orphaned ? null : getUUID();
			child.expiresAt = level.getGameTime() + 600; child.xpReward = 0; child.setPersistenceRequired();
			child.setTarget(getTarget());
			if (child.safePosition(candidate, childKind.movement != RegionalKind.Move.AIR) && level.addFreshEntity(child)) { spawned++; }
		}
	}
	private boolean expireHelper(ServerLevel level) {
		if (level.getGameTime() >= expiresAt || level.getDifficulty() == Difficulty.PEACEFUL) { discard(); return true; }
		if (ownerId != null) {
			Entity owner = level.getEntity(ownerId);
			if (owner == null) { if (++missingOwnerTicks > 40) { discard(); return true; } }
			else { missingOwnerTicks = 0; if (!owner.isAlive() || distanceToSqr(owner) > 32 * 32) { discard(); return true; } }
		}
		return false;
	}
	private void matureLarva(ServerLevel level) {
		RegionalMob adult = new RegionalMob(RegionalEcosystems.MOBS.get(RegionalKind.SHADOW_DRONE), level, RegionalKind.SHADOW_DRONE);
		adult.setPos(position()); adult.setTarget(getTarget());
		if (level.noCollision(adult) && level.addFreshEntity(adult)) { discard(); }
		else { growthTicks = 1100; }
	}

	private void scavenge(ServerLevel level) {
		if (!carried.isEmpty() || helper) { return; }
		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(6))) {
			ItemStack stack = drop.getItem();
			boolean useful = kind == RegionalKind.BONE_VULTURE ? stack.is(Items.BONE) : stack.get(net.minecraft.core.component.DataComponents.FOOD) != null
					|| stack.is(Items.SPIDER_EYE) || stack.is(Items.SUGAR);
			if (!useful || !hasLineOfSight(drop)) { continue; }
			if (distanceToSqr(drop) > 2) { getNavigation().moveTo(drop, 1.1); return; }
			carried = stack.split(1); if (stack.isEmpty()) { drop.discard(); } else { drop.setItem(stack); }
			setPersistenceRequired(); return;
		}
	}
	private void tickGuide(ServerLevel level) {
		if (helper || tickCount % 10 != 0) { return; }
		if (claimed) { if (level.getGameTime() >= expiresAt) { discard(); } return; }
		Player player = guidePlayer == null ? level.getNearestPlayer(this, 10) : level.getPlayerByUUID(guidePlayer);
		if (player == null || !player.isAlive() || player.isSpectator() || distanceToSqr(player) > 24 * 24) {
			if (level.getGameTime() - guideLastSeen > 600) { guidePlayer = null; guideDestination = null; guideStart = null; }
			return;
		}
		guideLastSeen = level.getGameTime();
		if (guideDestination == null) {
			for (int i = 0; i < 12; i++) {
				double angle = random.nextDouble() * Math.PI * 2;
				Vec3 candidate = groundPosition(position().add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12));
				var path = candidate == null ? null : getNavigation().createPath(BlockPos.containing(candidate), 0);
				if (candidate != null && safePosition(candidate, true) && path != null && path.canReach()
						&& level.getBiome(BlockPos.containing(candidate)).is(kind.region.biomes())) {
					guideDestination = candidate; guideStart = player.position(); guidePlayer = player.getUUID(); setPersistenceRequired(); break;
				}
			}
		}
		if (guideDestination != null) {
			getNavigation().moveTo(guideDestination.x, guideDestination.y, guideDestination.z, .7);
			particles(level, BossBolt.Kind.HEX, guideDestination.add(0, .2, 0), 4);
			if (position().distanceToSqr(guideDestination) < 9 && player.distanceToSqr(this) < 16
					&& player.position().distanceToSqr(guideStart) >= 36 && hasLineOfSight(player)) {
				claimed = true; expiresAt = level.getGameTime() + 200;
				Item reward = kind == RegionalKind.BABAS_FAMILIAR ? RegionalEcosystems.REWARDS.get(kind) : Items.GLOW_BERRIES;
				ItemStack stack = new ItemStack(reward);
				if (!player.getInventory().add(stack)) { player.spawnAtLocation(level, stack); }
				particles(level, BossBolt.Kind.HEX, guideDestination, 30);
			}
		}
	}
	@Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (kind != RegionalKind.TYRANT_SKULL || !isAlive() || player.isSpectator() || !player.getItemInHand(hand).is(ItemTags.PICKAXES)) { return super.mobInteract(player, hand); }
		if (!(level() instanceof ServerLevel level)) { return InteractionResult.CONSUME; }
		if (!claimed && !helper) {
			claimed = true;
			ItemStack fossil = new ItemStack(RegionalEcosystems.REWARDS.get(kind));
			if (!player.getInventory().add(fossil)) { player.spawnAtLocation(level, fossil); }
			player.getItemInHand(hand).hurtAndBreak(1, player, hand.asEquipmentSlot());
			particles(level, BossBolt.Kind.PHYSICAL, position(), 20); discard();
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (kind == RegionalKind.TYRANT_SKULL) { return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount); }
		boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
		if (projectile && kind == RegionalKind.SHARDLING && blinkCooldown == 0 && blink(level)) { return false; }
		if (projectile && kind == RegionalKind.STONEBACK_GOAT) { amount *= .5F; }
		if (kind == RegionalKind.FROZEN_HUSK && source.is(DamageTypeTags.IS_FIRE)) { amount *= 2; entityData.set(ACTIVE, 60); }
		if (kind == RegionalKind.GIANT_CRAB && source.getEntity() != null) {
			Vec3 incoming = source.getEntity().position().subtract(position()).normalize();
			if (getLookAngle().dot(incoming) < -.3) { amount *= 2; }
		}
		if (kind == RegionalKind.VOID_SENTINEL && source.getEntity() != null) {
			Vec3 incoming = source.getEntity().position().subtract(position()).normalize();
			// Shield Sync: All three rendered fragments and their vulnerable gaps use the same server-owned angle.
			for (int i = 0; i < 3; i++) {
				double angle = Math.toRadians(shieldAngle() - yBodyRot) + i * Math.PI * 2 / 3;
				if (incoming.dot(new Vec3(Math.sin(angle), 0, Math.cos(angle))) > .75) { amount *= .25F; break; }
			}
		}
		boolean hit = super.hurtServer(level, source, amount);
		if (hit && source.getEntity() instanceof LivingEntity attacker) {
			if (kind.behavior == RegionalKind.Behavior.HIVE || kind.behavior == RegionalKind.Behavior.EGG) {
				for (RegionalMob ally : level.getEntitiesOfClass(RegionalMob.class, getBoundingBox().inflate(12))) {
					if (ally.kind.region == kind.region && ally.kind.temper != RegionalKind.Temper.PASSIVE && ally.hasLineOfSight(attacker)) { ally.setTarget(attacker); }
				}
			}
			if (kind == RegionalKind.INFECTED_MOOSHROOM && pulseCooldown == 0 && distanceToSqr(attacker) < 9) {
				attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 40)); pulseCooldown = 80; particles(level, BossBolt.Kind.SPORE, position(), 16);
			}
			if (kind == RegionalKind.END_GRAZER && isAlive() && blinkCooldown == 0) { blink(level); }
		}
		return hit;
	}
	@Override public void die(DamageSource source) {
		if (level() instanceof ServerLevel level) {
			// Item Conservation: Return scavenged property even when ordinary mob drops are disabled.
			if (!carried.isEmpty()) { spawnAtLocation(level, carried); carried = ItemStack.EMPTY; }
			if (kind == RegionalKind.BONEWALKER && !reconstructed && !helper && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				// One Revival: Record before restoring health, and save it so reload cannot refresh the extra life.
				reconstructed = true; setHealth(getMaxHealth() / 2); entityData.set(ACTIVE, 40); attackCooldown = 40;
				particles(level, BossBolt.Kind.SOUL, position(), 24); return;
			}
			if (kind == RegionalKind.BROOD_CARRIER) { spawnHelpers(level, RegionalKind.CAVE_SKITTERER, true); }
			if (kind == RegionalKind.SHADOW_EGG) { spawnHelpers(level, RegionalKind.SHADOW_LARVA, true); }
			if (kind == RegionalKind.SPORELING || kind == RegionalKind.SOULFLAME_SKULL) { particles(level, projectileKind(), position(), 24); }
		}
		super.die(source);
	}
	@Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (!carried.isEmpty()) { spawnAtLocation(level, carried); carried = ItemStack.EMPTY; }
		if (!killedByPlayer || helper || kind == RegionalKind.TYRANT_SKULL || kind == RegionalKind.BABAS_FAMILIAR) { return; }
		if (kind.herald()) { spawnAtLocation(level, new ItemStack(RegionalEcosystems.REWARDS.get(kind))); }
		else {
			Item item = switch (kind.region) {
				case MUTANT_ZOMBIE -> Items.ROTTEN_FLESH;
				case FOSSIL_TYRANT, SOULBOUND_COLOSSUS -> Items.BONE;
				case THUNDER_BIRD -> Items.FEATHER;
				case TITAN_BOA -> Items.VINE;
				case BABA_YAGA -> Items.SPIDER_EYE;
				case MOUNTAIN_TITAN -> Items.FLINT;
				case ICE_WYRM -> Items.SNOWBALL;
				case KRAKEN -> Items.PRISMARINE_SHARD;
				case CAVE_CRAWLER -> Items.STRING;
				case SHADOW_CREEPER_QUEEN -> Items.SCULK_VEIN;
				case MYCELIAL_SOVEREIGN -> Items.BROWN_MUSHROOM;
				case NETHERBORN -> netherDrop();
				case VOID_EYE -> Items.CHORUS_FRUIT;
			};
			spawnAtLocation(level, new ItemStack(item));
		}
	}
	private Item netherDrop() {
		return switch (RegionalEcosystems.netherVariant(kind)) { case 0 -> Items.CRIMSON_FUNGUS; case 1 -> Items.WARPED_FUNGUS; default -> Items.MAGMA_CREAM; };
	}
	private static void particles(ServerLevel level, BossBolt.Kind kind, Vec3 pos, int count) {
		level.sendParticles(BossBolt.particle(kind), pos.x, pos.y, pos.z, count, .15, .2, .15, .01);
	}

	@Override protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("darkspawn_claimed", claimed); output.putBoolean("darkspawn_called", called);
		output.putBoolean("darkspawn_reconstructed", reconstructed); output.putBoolean("darkspawn_helper", helper);
		output.putLong("darkspawn_expires", expiresAt); output.putInt("darkspawn_growth", growthTicks);
		output.putInt("darkspawn_blink_cooldown", blinkCooldown); output.putInt("darkspawn_pulse_cooldown", pulseCooldown);
		if (ownerId != null) { output.putString("darkspawn_owner", ownerId.toString()); }
		if (!carried.isEmpty()) { output.store("darkspawn_carried", ItemStack.CODEC, carried); }
		if (guideDestination != null && guideStart != null && guidePlayer != null) {
			output.store("darkspawn_guide_destination", Vec3.CODEC, guideDestination);
			output.store("darkspawn_guide_start", Vec3.CODEC, guideStart);
			output.putString("darkspawn_guide_player", guidePlayer.toString());
			output.putLong("darkspawn_guide_last_seen", guideLastSeen);
		}
	}
	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		claimed = input.getBooleanOr("darkspawn_claimed", false); called = input.getBooleanOr("darkspawn_called", false);
		reconstructed = input.getBooleanOr("darkspawn_reconstructed", false); helper = input.getBooleanOr("darkspawn_helper", false);
		expiresAt = input.getLongOr("darkspawn_expires", 0); growthTicks = Math.clamp(input.getIntOr("darkspawn_growth", 0), 0, 1200);
		blinkCooldown = Math.clamp(input.getIntOr("darkspawn_blink_cooldown", 40), 40, 100);
		pulseCooldown = Math.clamp(input.getIntOr("darkspawn_pulse_cooldown", 60), 40, 120);
		ownerId = uuid(input.getStringOr("darkspawn_owner", ""));
		carried = input.read("darkspawn_carried", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		guideDestination = input.read("darkspawn_guide_destination", Vec3.CODEC).orElse(null);
		guideStart = input.read("darkspawn_guide_start", Vec3.CODEC).orElse(null);
		guidePlayer = uuid(input.getStringOr("darkspawn_guide_player", ""));
		guideLastSeen = input.getLongOr("darkspawn_guide_last_seen", level().getGameTime());
		if (helper) { xpReward = 0; }
		// Reload Safety: Resume ownership/cooldowns, never an attack whose warning was missed by a joining player.
		mark = null; leapOrigin = null; leapTicks = 0; attackCooldown = 40; noPhysics = false; ghostOrigin = null;
		patchPosition = null; patchTicks = 0;
		entityData.set(WARNING, 0); entityData.set(ACTIVE, 0); entityData.set(HIDING, false); entityData.set(CLIMBING, false);
		setDeltaMovement(Vec3.ZERO);
	}
	private static UUID uuid(String value) { try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; } }
}
