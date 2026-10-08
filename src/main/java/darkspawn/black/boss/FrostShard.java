package darkspawn.black.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class FrostShard extends ThrowableItemProjectile {
	public FrostShard(EntityType<? extends FrostShard> type, Level level) { super(type, level); }

	@Override
	protected Item getDefaultItem() { return Items.PRISMARINE_SHARD; }

	@Override
	protected double getDefaultGravity() { return 0.001; }

	@Override
	protected float getAirDrag() { return 1.0F; }

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !(entity instanceof MutantWolf) && !(entity instanceof FrostWolf) && super.canHitEntity(entity);
	}

	@Override
	public void tick() {
		super.tick();
		if (tickCount > 160) { discard(); }
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity target
				&& target.hurtServer(server, damageSources().thrown(this, getOwner()), 10)) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.05);
			discard();
		}
	}
}
