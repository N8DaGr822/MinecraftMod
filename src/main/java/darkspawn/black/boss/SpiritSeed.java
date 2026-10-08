package darkspawn.black.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpiritSeed extends ThrowableItemProjectile {
	public SpiritSeed(EntityType<? extends SpiritSeed> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.OAK_SAPLING;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.001;
	}

	@Override
	protected float getAirDrag() {
		// Aerial Reach: Keep volleys dangerous throughout the boss's 128-block engagement area.
		return 1.0F;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !(entity instanceof AncientTreeSpirit) && !(entity instanceof HeartwoodSapling) && super.canHitEntity(entity);
	}

	@Override
	public void tick() {
		super.tick();
		if (tickCount > 160) {
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server) {
			hit.getEntity().hurtServer(server, damageSources().thrown(this, getOwner()), 10);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.05);
			discard();
		}
	}
}
