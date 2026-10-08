package darkspawn.black.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BossBolt extends ThrowableItemProjectile {
	public enum Kind { PHYSICAL, LIGHTNING, POISON, HEX, FROST, INK, WEB, ACID, SPORE, FIRE, SOUL, VOID }
	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BossBolt.class, EntityDataSerializers.INT);
	private float damage = 10;

	public BossBolt(EntityType<? extends BossBolt> type, Level level) { super(type, level); }
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(KIND, 0);
	}
	public Kind kind() { return Kind.values()[Math.clamp(entityData.get(KIND), 0, Kind.values().length - 1)]; }
	public void configure(LivingEntity owner, Kind kind, float damage) {
		setOwner(owner);
		entityData.set(KIND, kind.ordinal());
		setItem(new ItemStack(itemFor(kind)));
		this.damage = damage;
	}
	@Override
	protected Item getDefaultItem() {
		// Projectile Construction: SynchedEntityData is not available while vanilla creates the default stack.
		return Items.POINTED_DRIPSTONE;
	}
	private static Item itemFor(Kind kind) {
		return switch (kind) {
			case PHYSICAL -> Items.POINTED_DRIPSTONE;
			case LIGHTNING -> Items.AMETHYST_SHARD;
			case POISON, SPORE -> Items.SLIME_BALL;
			case HEX -> Items.FERMENTED_SPIDER_EYE;
			case FROST -> Items.PRISMARINE_SHARD;
			case INK -> Items.INK_SAC;
			case WEB -> Items.COBWEB;
			case ACID -> Items.ECHO_SHARD;
			case FIRE -> Items.FIRE_CHARGE;
			case SOUL -> Items.SOUL_LANTERN;
			case VOID -> Items.ENDER_PEARL;
		};
	}
	public static SimpleParticleType particle(Kind kind) {
		return switch (kind) {
			case PHYSICAL -> ParticleTypes.CRIT;
			case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
			case POISON, SPORE -> ParticleTypes.COMPOSTER;
			case HEX, VOID -> ParticleTypes.PORTAL;
			case FROST -> ParticleTypes.SNOWFLAKE;
			case INK, WEB -> ParticleTypes.SMOKE;
			case ACID -> ParticleTypes.SCULK_SOUL;
			case FIRE -> ParticleTypes.FLAME;
			case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
		};
	}
	@Override
	protected double getDefaultGravity() { return 0.001; }
	@Override
	protected float getAirDrag() { return 1; }
	@Override
	protected boolean canHitEntity(Entity entity) {
		if (getOwner() instanceof darkspawn.black.ecosystem.RegionalMob owner
				&& entity instanceof darkspawn.black.ecosystem.RegionalMob other && owner.kind().region == other.kind().region) { return false; }
		if (getOwner() instanceof BiomeBoss boss && boss.ownsSummon(entity)) { return false; }
		return !(entity instanceof BiomeBoss) && !(entity instanceof BossMinion) && !(entity instanceof AncientTreeSpirit)
				&& !(entity instanceof MutantWolf) && super.canHitEntity(entity);
	}
	@Override
	public void tick() {
		super.tick();
		if (tickCount > 180 || getOwner() instanceof LivingEntity owner && !owner.isAlive()) { discard(); }
	}
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server && hit.getEntity() instanceof LivingEntity target
				&& target.hurtServer(server, damageSources().thrown(this, getOwner()), damage)) {
			BossEffects.apply(target, kind());
			if (getOwner() instanceof darkspawn.black.ecosystem.RegionalMob owner) { owner.onProjectileHit(target); }
		}
	}
	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(particle(kind()), getX(), getY(), getZ(), 16, 0.4, 0.4, 0.4, 0.1);
			discard();
		}
	}
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("darkspawn_kind", kind().ordinal());
		output.putFloat("darkspawn_damage", damage);
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(KIND, Math.clamp(input.getIntOr("darkspawn_kind", 0), 0, Kind.values().length - 1));
		damage = Math.clamp(input.getFloatOr("darkspawn_damage", 10), 0, 40);
	}
}
