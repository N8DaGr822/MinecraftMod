package darkspawn.black.ecosystem;

import darkspawn.black.boss.BossBolt;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossItems;
import darkspawn.black.cooking.Cooking;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ForestMob extends Monster implements RangedAttackMob, darkspawn.black.boss.AnimatedCreature {
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
		BARKLING("barkling", 0.65F, 1.1F, 16, 0.24, 3, 0),
		HOLLOWED("hollowed", 0.85F, 2.3F, 36, 0.18, 6, 12),
		ROOTCRAWLER("rootcrawler", 1.2F, 0.55F, 24, 0.23, 5, 2),
		ANCIENT_ENT("ancient_ent", 1.8F, 3.8F, 100, 0.19, 10, 8);

		public final String id;
		public final float width;
		public final float height;
		private final double health;
		private final double speed;
		private final double damage;
		private final double armor;

		Kind(String id, float width, float height, double health, double speed, double damage, double armor) {
			this.id = id; this.width = width; this.height = height;
			this.health = health; this.speed = speed; this.damage = damage; this.armor = armor;
		}

		public MobCategory category() { return this == BARKLING ? MobCategory.CREATURE : MobCategory.MONSTER; }
	}

	private static final EntityDataAccessor<Boolean> HIDING = SynchedEntityData.defineId(ForestMob.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(ForestMob.class, EntityDataSerializers.INT);
	private final Kind kind;
	private long herbsReadyAt;
	private int strikeCooldown;
	private Vec3 strikePosition;

	public ForestMob(EntityType<? extends ForestMob> type, Level level, Kind kind) {
		super(type, level);
		this.kind = kind;
		xpReward = kind == Kind.BARKLING ? 1 : kind == Kind.ANCIENT_ENT ? 12 : 5;
		// Forest AI: Mob's constructor runs before kind is assigned, so install variant goals here.
		if (level instanceof ServerLevel) { initializeGoals(); }
	}

	public Kind kind() { return kind; }
	public boolean hiding() { return entityData.get(HIDING); }
	public int windup() { return entityData.get(WINDUP); }
	public boolean herbsReady() { return kind == Kind.BARKLING && level().getGameTime() >= herbsReadyAt; }

	public static AttributeSupplier.Builder attributes(Kind kind) {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.MOVEMENT_SPEED, kind.speed)
				.add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.ARMOR, kind.armor).add(Attributes.FOLLOW_RANGE, 24);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(HIDING, false);
		data.define(WINDUP, 0);
	}

	private void initializeGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		if (kind == Kind.BARKLING) {
			goalSelector.addGoal(1, new Goal() {
				{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
				@Override public boolean canUse() { return getTarget() == null && level().getNearestPlayer(ForestMob.this, 8) != null; }
				@Override public void start() { entityData.set(HIDING, true); getNavigation().stop(); }
				@Override public void stop() { entityData.set(HIDING, false); }
			});
			goalSelector.addGoal(2, new RangedAttackGoal(this, 1, 40, 10));
		} else {
			if (usesRoots()) {
				goalSelector.addGoal(1, new Goal() {
					{ setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
					@Override public boolean canUse() { return canStartStrike(getTarget()); }
					@Override public boolean canContinueToUse() { return windup() > 0; }
					@Override public void start() { beginStrike(getTarget()); getNavigation().stop(); }
				});
			}
			goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, false));
			targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		}
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	private boolean usesRoots() { return kind == Kind.ROOTCRAWLER || kind == Kind.ANCIENT_ENT; }
	private double strikeRadius() { return kind == Kind.ANCIENT_ENT ? 3 : 1.5; }
	private boolean canStartStrike(LivingEntity target) {
		return target != null && target.isAlive() && strikeCooldown == 0 && windup() == 0
				&& distanceToSqr(target) <= 36 && hasLineOfSight(target) && Math.abs(target.getY() - getY()) <= 3;
	}
	private boolean beginStrike(LivingEntity target) {
		if (!canStartStrike(target)) { return false; }
		// Root Telegraph: Lock the destination before the warning; do not track a dodging player.
		strikePosition = target.position();
		entityData.set(WINDUP, 25);
		strikeCooldown = 80;
		return true;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (strikeCooldown > 0) { strikeCooldown--; }
		if (windup() == 0 || strikePosition == null) { return; }
		getNavigation().stop();
		double radius = strikeRadius();
		if (windup() % 5 == 0) {
			for (int i = 0; i < 16; i++) {
				double angle = Math.PI * 2 * i / 16;
				level.sendParticles(ParticleTypes.COMPOSTER, strikePosition.x + Math.cos(angle) * radius,
						strikePosition.y + 0.15, strikePosition.z + Math.sin(angle) * radius, 2, 0.05, 0.1, 0.05, 0);
			}
		}
		entityData.set(WINDUP, windup() - 1);
		if (windup() != 0) { return; }
		animateAttack();
		level.sendParticles(ParticleTypes.CRIT, strikePosition.x, strikePosition.y + 0.5, strikePosition.z, 45, radius / 2, 0.6, radius / 2, 0.1);
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(strikePosition, strikePosition).inflate(radius, 2.5, radius))) {
			double dx = player.getX() - strikePosition.x;
			double dz = player.getZ() - strikePosition.z;
			if (!player.isCreative() && !player.isSpectator() && dx * dx + dz * dz <= radius * radius
					&& Math.abs(player.getY() - strikePosition.y) <= 2.5 && hasLineOfSight(player)
					&& player.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
			}
		}
		strikePosition = null;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (usesRoots()) { return target instanceof LivingEntity living && beginStrike(living); }
		boolean hit = super.doHurtTarget(level, target);
		if (hit && kind == Kind.HOLLOWED && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
		}
		return hit;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		if (kind != Kind.BARKLING || !(level() instanceof ServerLevel level)) { return; }
		animateAttack();
		BossBolt stick = new BossBolt(BossEntities.BOSS_BOLT, level);
		stick.configure(this, BossBolt.Kind.PHYSICAL, 3);
		stick.setItem(new ItemStack(Items.STICK));
		stick.setPos(getX(), getEyeY(), getZ());
		Vec3 aim = target.getEyePosition().subtract(stick.position());
		stick.shoot(aim.x, aim.y, aim.z, 1.2F, 3);
		level.addFreshEntity(stick);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack shears = player.getItemInHand(hand);
		if (kind != Kind.BARKLING || !shears.is(Items.SHEARS) || getTarget() != null || !isAlive()) {
			return super.mobInteract(player, hand);
		}
		if (!(level() instanceof ServerLevel level)) { return InteractionResult.CONSUME; }
		if (!herbsReady()) {
			if (player instanceof ServerPlayer serverPlayer) { serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.herbs_regrowing")); }
			return InteractionResult.SUCCESS_SERVER;
		}
		// Herb Harvest: Commit the cooldown before granting items so repeated interactions cannot duplicate a harvest.
		herbsReadyAt = level.getGameTime() + 6000;
		ItemStack herbs = new ItemStack(Cooking.WILD_HERBS);
		if (!player.getInventory().add(herbs)) { spawnAtLocation(level, herbs); }
		shears.hurtAndBreak(1, player, hand.asEquipmentSlot());
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 8, 0.3, 0.3, 0.3, 0);
		if (player instanceof ServerPlayer serverPlayer) { serverPlayer.sendOverlayMessage(Component.translatable("message.darkspawn.herbs_harvested")); }
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public boolean isPreventingPlayerRest(ServerLevel level, Player player) { return kind != Kind.BARKLING || getTarget() == player; }

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Wooden Weakness: Axes and fire amplify the incoming hit before vanilla armor mitigation.
		boolean axe = source.getDirectEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().is(ItemTags.AXES);
		return super.hurtServer(level, source, axe || source.is(DamageTypeTags.IS_FIRE) ? amount * 1.5F : amount);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (!killedByPlayer) { return; }
		spawnAtLocation(level, new ItemStack(kind == Kind.BARKLING ? Items.STICK : Cooking.WILD_HERBS));
		if (kind == Kind.ANCIENT_ENT) { spawnAtLocation(level, new ItemStack(BossItems.ANCIENT_HEARTWOOD)); }
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("darkspawn_herbs_ready_at", herbsReadyAt);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		herbsReadyAt = input.getLongOr("darkspawn_herbs_ready_at", 0);
		// Reload Safety: Never resume an attack whose warning was not visible to newly tracking players.
		strikePosition = null;
		entityData.set(WINDUP, 0);
		entityData.set(HIDING, false);
		strikeCooldown = 40;
	}
}
