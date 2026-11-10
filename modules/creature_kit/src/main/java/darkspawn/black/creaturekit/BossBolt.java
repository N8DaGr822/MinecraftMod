package darkspawn.black.creaturekit;

import darkspawn.black.core.DarkspawnCore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

/** Shared creature projectile ({@code darkspawn:boss_bolt}); owners customise it through {@link Shooter}. */
public final class BossBolt extends ThrowableItemProjectile {
	/** Optional owner behaviour, implemented by the creature that fires the bolt. */
	public interface Shooter {
		default float boltDamageScale() { return 1F; }
		/** The owner's own allies or summons that its bolts pass through. */
		default boolean boltSpares(Entity target) { return false; }
		default void onBoltHit(LivingEntity target) { }
	}
	private static final List<Predicate<Entity>> IMMUNE = new CopyOnWriteArrayList<>();
	private static final Identifier FRACTURED_ARMOR = DarkspawnCore.id("fractured_armor");
	/** Entities no bolt can hit, whoever fired it (for example bosses and their minions). */
	public static void registerImmune(Predicate<Entity> immune) { IMMUNE.add(immune); }
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
	/** The bolt's on-hit effect. Acid applies {@code darkspawn:fractured_armor} only when a mod registers that effect. */
	public static void applyEffect(LivingEntity target, Kind kind) {
		switch (kind) {
			case POISON, SPORE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
			case HEX -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
			case FROST -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
			case INK -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
			case WEB -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 4));
			case ACID -> BuiltInRegistries.MOB_EFFECT.get(FRACTURED_ARMOR).ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, 100, 0)));
			case FIRE -> target.igniteForSeconds(4);
			case SOUL -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			case VOID -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
			default -> { }
		}
	}
	@Override
	protected double getDefaultGravity() { return 0.001; }
	@Override
	protected float getAirDrag() { return 1; }
	@Override
	protected boolean canHitEntity(Entity entity) {
		if (getOwner() instanceof Shooter owner && owner.boltSpares(entity)) { return false; }
		for (var immune : IMMUNE) if (immune.test(entity)) return false;
		return super.canHitEntity(entity);
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
				&& target.hurtServer(server, damageSources().thrown(this, getOwner()), damage * (getOwner() instanceof Shooter owner ? owner.boltDamageScale() : 1F))) {
			applyEffect(target, kind());
			if (getOwner() instanceof Shooter owner) { owner.onBoltHit(target); }
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
