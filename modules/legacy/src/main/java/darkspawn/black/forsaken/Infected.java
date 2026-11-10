package darkspawn.black.forsaken;

import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class Infected extends Monster implements darkspawn.black.boss.AnimatedCreature {
	public enum Kind {
		WANDERER("infected_wanderer", .65F, 1.9F, 24, .27, 4),
		CRAWLER("infected_crawler", 1.1F, .65F, 16, .34, 3),
		SPITTER("spore_spitter", .8F, 1.7F, 22, .23, 3),
		BRUTE("assimilated_brute", 1.3F, 2.6F, 60, .18, 9);
		public final String id; public final float width, height; final double health, speed, damage;
		Kind(String id, float width, float height, double health, double speed, double damage) {
			this.id = id; this.width = width; this.height = height; this.health = health; this.speed = speed; this.damage = damage;
		}
	}
	private final Kind kind;
	private static final EntityDataAccessor<Integer> WARNING = SynchedEntityData.defineId(Infected.class, EntityDataSerializers.INT);
	private final com.geckolib.animatable.instance.AnimatableInstanceCache cache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);
	public Infected(EntityType<? extends Infected> type, Level level, Kind kind) {
		super(type, level); this.kind = kind; xpReward = kind == Kind.BRUTE ? 12 : 4;
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new Attack());
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .5));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}
	public Kind kind() { return kind; }
	public int warning() { return entityData.get(WARNING); }
	@Override protected void defineSynchedData(SynchedEntityData.Builder data) { super.defineSynchedData(data); data.define(WARNING, 0); }
	public static AttributeSupplier.Builder attributes(Kind kind) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, kind.health).add(Attributes.MOVEMENT_SPEED, kind.speed)
			.add(Attributes.ATTACK_DAMAGE, kind.damage).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, kind == Kind.BRUTE ? 10 : 0)
			.add(Attributes.ATTACK_KNOCKBACK, kind == Kind.BRUTE ? 2 : 0).add(Attributes.KNOCKBACK_RESISTANCE, kind == Kind.BRUTE ? .6 : 0);
	}
	@Override public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
	@Override public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {
		darkspawn.black.boss.CreatureAnimations.register(assetId(), controllers);
	}
	@Override public void animateAttack() { triggerAnim("action", "attack"); }
	@Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return kind == Kind.CRAWLER ? SoundEvents.SPIDER_AMBIENT : SoundEvents.ZOMBIE_AMBIENT; }
	@Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
	@Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
	@Override public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof Player player) ForsakenContent.contaminate(player);
		return hit;
	}
	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Blade Affinity: Apply to native player melee only; the clearing pulse has its own fixed damage.
		if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player
			&& player.getMainHandItem().is(ForsakenContent.BLADE)) amount *= 1.25F;
		return super.hurtServer(level, source, amount);
	}
	@Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (killedByPlayer) spawnAtLocation(level, new ItemStack(ForsakenContent.BIOFIBER, kind == Kind.BRUTE ? 3 : 1));
	}
	private final class Attack extends Goal {
		private int cooldown;
		Attack() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }
		@Override public void stop() { entityData.set(WARNING, 0); navigation.stop(); }
		@Override public void tick() {
			var target = getTarget(); if (target == null || !(level() instanceof ServerLevel server)) return;
			getLookControl().setLookAt(target, 30, 30);
			double distance = distanceToSqr(target);
			double reach = kind == Kind.SPITTER ? 225 : Math.pow(getBbWidth() + target.getBbWidth() + .5, 2);
			if (warning() > 0) {
				navigation.stop(); entityData.set(WARNING, warning() - 1);
				if (warning() == 0) {
					// Telegraph Counterplay: Range and sight are checked again at impact; escaping or using cover cancels damage.
					if (distance <= reach && getSensing().hasLineOfSight(target)) {
						animateAttack();
						if (kind == Kind.SPITTER) {
							var bolt = new SporeBolt(ForsakenContent.SPORE, server); bolt.setOwner(Infected.this);
							bolt.setPos(getX(), getEyeY() - .15, getZ());
							var aim = target.getEyePosition().subtract(bolt.position()); bolt.shoot(aim.x, aim.y, aim.z, .85F, 0);
							server.addFreshEntity(bolt);
						} else doHurtTarget(server, target);
					}
					cooldown = kind == Kind.CRAWLER ? 25 : kind == Kind.BRUTE ? 65 : 45;
				}
				return;
			}
			if (cooldown > 0) cooldown--;
			if (distance <= reach && getSensing().hasLineOfSight(target) && cooldown == 0) {
				entityData.set(WARNING, kind == Kind.BRUTE ? 24 : 16);
				playSound(kind == Kind.SPITTER ? SoundEvents.SLIME_SQUISH : SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, .7F, .8F);
			} else if (tickCount % 10 == 0) {
				if (kind == Kind.SPITTER && distance < 36) {
					var away = position().subtract(target.position()).normalize().scale(5).add(position());
					navigation.moveTo(away.x, away.y, away.z, 1);
				} else if (kind == Kind.CRAWLER && distance > 9 && distance < 100) {
					var delta = target.position().subtract(position()).normalize();
					double side = getId() % 2 == 0 ? 2 : -2;
					navigation.moveTo(target.getX() + delta.z * side, target.getY(), target.getZ() - delta.x * side, 1);
				} else if (distance > reach || !getSensing().hasLineOfSight(target)) navigation.moveTo(target, 1);
			}
		}
	}
}
