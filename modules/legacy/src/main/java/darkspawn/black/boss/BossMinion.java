package darkspawn.black.boss;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class BossMinion extends Monster implements AnimatedCreature {
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
	public enum Role { MELEE, HEALER, TURRET, EGG, GUARD, SOUL_CAGE, TENTACLE }
	private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(BossMinion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(BossMinion.class, EntityDataSerializers.INT);
	private final BossProfile profile;
	private UUID owner;
	private UUID captive;
	private long expiresAt = -1;
	private int missingOwnerTicks;
	private int cooldown = 60;
	private Vec3 targetPoint = Vec3.ZERO;

	public BossMinion(EntityType<? extends BossMinion> type, Level level, BossProfile profile) {
		super(type, level);
		this.profile = profile;
		setPersistenceRequired();
		xpReward = 0;
	}
	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 45).add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 64);
	}
	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true) {
			@Override public boolean canUse() { return role() == Role.MELEE && super.canUse(); }
			@Override public boolean canContinueToUse() { return role() == Role.MELEE && super.canContinueToUse(); }
		});
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(ROLE, 0);
		data.define(WINDUP, 0);
	}
	public BossProfile profile() { return profile; }
	public Role role() { return Role.values()[Math.clamp(entityData.get(ROLE), 0, Role.values().length - 1)]; }
	public int windup() { return entityData.get(WINDUP); }
	public void configure(BiomeBoss boss, Role role, UUID captive) {
		owner = boss.getUUID();
		this.captive = captive;
		entityData.set(ROLE, role.ordinal());
		expiresAt = level().getGameTime() + (role == Role.EGG ? 120 : 2400);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(role == Role.TENTACLE ? 120 : role == Role.GUARD ? 90 : 45);
		setHealth(getMaxHealth());
		setNoGravity(role != Role.MELEE);
	}
	@Override
	protected Component getTypeName() { return Component.translatable("minion.darkspawn." + role().name().toLowerCase(java.util.Locale.ROOT)); }
	@Override
	public boolean canBreatheUnderwater() { return profile == BossProfile.KRAKEN || super.canBreatheUnderwater(); }
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (owner == null) { discard(); return; }
		if (!(level.getEntity(owner) instanceof BiomeBoss boss)) {
			if (++missingOwnerTicks > 40) { discard(); }
			return;
		}
		missingOwnerTicks = 0;
		if (!boss.isAlive() || distanceToSqr(boss) > 160 * 160) { discard(); return; }
		if (level.getGameTime() >= expiresAt) {
			if (role() == Role.EGG) {
				entityData.set(ROLE, Role.MELEE.ordinal());
				expiresAt = level.getGameTime() + 1800;
				setNoGravity(false);
			} else { discard(); }
			return;
		}
		if (role() == Role.MELEE || role() == Role.EGG || role() == Role.GUARD || role() == Role.SOUL_CAGE) { return; }
		getNavigation().stop();
		if (role() == Role.HEALER) {
			if (tickCount % 40 == 0) {
				boss.heal(6);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 12, 1, 1, 1, 0);
			}
			return;
		}
		if (windup() > 0) {
			if (tickCount % 5 == 0) { BiomeBoss.ring(level, targetPoint, role() == Role.TENTACLE ? 5 : 2, boss.boltKind()); }
			entityData.set(WINDUP, windup() - 1);
			if (windup() == 0) {
				animateAttack();
				if (role() == Role.TENTACLE) { boss.strikeArea(level, targetPoint, 5, 8, 14, BossBolt.Kind.PHYSICAL, true); }
				else { boss.fireFrom(level, position().add(0, 1.5, 0), targetPoint, 1, boss.boltKind()); }
				cooldown = 80;
			}
		} else if (--cooldown <= 0) {
			var player = level.players().stream().filter(p -> BiomeBoss.eligible(p) && distanceToSqr(p) < 64 * 64)
					.min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			if (player != null) {
				targetPoint = player.position().add(0, role() == Role.TENTACLE ? 0 : 1, 0);
				entityData.set(WINDUP, 40);
			} else { cooldown = 40; }
		}
	}
	@Override
	public void die(DamageSource source) {
		if (role() == Role.SOUL_CAGE && captive != null && level() instanceof ServerLevel server) {
			var player = server.getServer().getPlayerList().getPlayer(captive);
			if (player != null) { BossEffects.returnHeart(player); }
		}
		super.die(source);
	}
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) { output.putString("owner", owner.toString()); }
		if (captive != null) { output.putString("captive", captive.toString()); }
		output.putInt("role", role().ordinal());
		output.putLong("expires", expiresAt);
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try { owner = UUID.fromString(input.getStringOr("owner", "")); } catch (IllegalArgumentException ignored) { owner = null; }
		try { captive = UUID.fromString(input.getStringOr("captive", "")); } catch (IllegalArgumentException ignored) { captive = null; }
		entityData.set(ROLE, Math.clamp(input.getIntOr("role", 0), 0, Role.values().length - 1));
		entityData.set(WINDUP, 0);
		expiresAt = input.getLongOr("expires", level().getGameTime() + 2400);
		setNoGravity(role() != Role.MELEE);
		cooldown = 60;
	}
}
