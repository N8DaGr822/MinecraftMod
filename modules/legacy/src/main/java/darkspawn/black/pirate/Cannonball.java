package darkspawn.black.pirate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Naval ammunition damages vessels only; no terrain explosions or direct player damage. */
public final class Cannonball extends ThrowableItemProjectile {
	public Cannonball(EntityType<? extends Cannonball> type,Level level) { super(type,level); }
	@Override protected Item getDefaultItem() { return Ships.SHOT; }
	@Override protected double getDefaultGravity() { return .003; }
	@Override protected boolean canHitEntity(Entity entity) {
		// Naval Targeting: Docked ships ignore building clicks but remain valid cannon targets.
		return entity instanceof PirateShip && entity!=getOwner() && entity.isAlive();
	}
	@Override public void tick() { super.tick();if(!level().isClientSide()&&tickCount>100)discard(); }
	@Override protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if(getOwner() instanceof PirateShip attacker && hit.getEntity() instanceof PirateShip victim)victim.cannonDamage(attacker,getItem().getItem());
	}
	@Override protected void onHit(HitResult hit) {
		super.onHit(hit);
		if(level() instanceof ServerLevel server){server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,getX(),getY(),getZ(),12,.3,.3,.3,.03);discard();}
	}
}
