package darkspawn.black.forsaken;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SporeBolt extends ThrowableItemProjectile {
	public SporeBolt(EntityType<? extends SporeBolt> type, Level level) { super(type, level); }
	@Override protected Item getDefaultItem() { return ForsakenContent.BIOFIBER; }
	@Override protected double getDefaultGravity() { return .001; }
	@Override protected boolean canHitEntity(Entity entity) { return !(entity instanceof Infected) && super.canHitEntity(entity); }
	@Override public void tick() { super.tick(); if (!level().isClientSide() && tickCount > 80) discard(); }
	@Override protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity target
			&& target.hurtServer(server, damageSources().thrown(this, getOwner()), 3)) ForsakenContent.contaminate(target);
	}
	@Override protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 12, .3, .3, .3, .02); discard();
		}
	}
}
