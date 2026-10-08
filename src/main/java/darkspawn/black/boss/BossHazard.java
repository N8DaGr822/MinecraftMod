package darkspawn.black.boss;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class BossHazard extends Entity {
	private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(BossHazard.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(BossHazard.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> WARMUP = SynchedEntityData.defineId(BossHazard.class, EntityDataSerializers.INT);
	private UUID owner;
	private long expiresAt;
	private BossBolt.Kind kind = BossBolt.Kind.POISON;

	public BossHazard(EntityType<? extends BossHazard> type, Level level) { super(type, level); }
	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		data.define(ATTACK, BossAttack.SPORES.ordinal());
		data.define(RADIUS, 5F);
		data.define(WARMUP, 30);
	}
	public void configure(BiomeBoss boss, BossAttack attack, BossBolt.Kind kind, float radius, int lifetime) {
		owner = boss.getUUID();
		this.kind = kind;
		entityData.set(ATTACK, attack.ordinal());
		entityData.set(RADIUS, radius);
		expiresAt = level().getGameTime() + lifetime;
	}
	public BossAttack attack() { return BossAttack.values()[Math.clamp(entityData.get(ATTACK), 0, BossAttack.values().length - 1)]; }
	public float radius() { return entityData.get(RADIUS); }
	public int warmup() { return entityData.get(WARMUP); }

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) { return; }
		if (owner == null || !(server.getEntity(owner) instanceof BiomeBoss boss) || !boss.isAlive() || server.getGameTime() >= expiresAt) {
			discard();
			return;
		}
		if (warmup() > 0) { entityData.set(WARMUP, warmup() - 1); }
		if (tickCount % 5 == 0) {
			for (int i = 0; i < 16; i++) {
				double angle = i * Math.PI / 8;
				server.sendParticles(BossBolt.particle(kind), getX() + Math.cos(angle) * radius(), getY() + 0.2,
						getZ() + Math.sin(angle) * radius(), 1, 0, 0, 0, 0);
			}
		}
		if (warmup() > 0 || tickCount % 20 != 0) { return; }
		for (var player : server.players()) {
			Vec3 delta = player.position().subtract(position());
			if (!BiomeBoss.eligible(player) || delta.horizontalDistanceSqr() > radius() * radius() || delta.y < -2 || delta.y > 6) { continue; }
			if (player.hurtServer(server, damageSources().mobAttack(boss), 4)) { BossEffects.apply(player, kind); }
			if (attack() == BossAttack.WHIRLPOOL || attack() == BossAttack.VOID_RIFTS) {
				player.knockback(0.4, delta.x, delta.z, damageSources().mobAttack(boss), 4);
			}
		}
	}
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (owner != null) { output.putString("owner", owner.toString()); }
		output.putLong("expires", expiresAt);
		output.putInt("attack", attack().ordinal());
		output.putInt("kind", kind.ordinal());
		output.putFloat("radius", radius());
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		try { owner = UUID.fromString(input.getStringOr("owner", "")); } catch (IllegalArgumentException ignored) { owner = null; }
		expiresAt = input.getLongOr("expires", 0);
		entityData.set(ATTACK, Math.clamp(input.getIntOr("attack", 0), 0, BossAttack.values().length - 1));
		entityData.set(RADIUS, Math.clamp(input.getFloatOr("radius", 5), 1, 12));
		kind = BossBolt.Kind.values()[Math.clamp(input.getIntOr("kind", 0), 0, BossBolt.Kind.values().length - 1)];
		// Reload Telegraph: A saved hazard must warn returning players again before applying damage.
		entityData.set(WARMUP, 30);
	}
}
