package darkspawn.black.boss;

import java.util.UUID;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class Endborn extends Monster {
	private int blinkCooldown;
	private UUID encounterOwner;
	private long expiresAt;
	private int missingOwnerTicks;

	public void bindToEncounter(BiomeBoss boss) {
		encounterOwner = boss.getUUID();
		expiresAt = level().getGameTime() + 2400;
		xpReward = 0;
		setPersistenceRequired();
	}

	public Endborn(EntityType<? extends Endborn> type, Level level) { super(type, level); xpReward = 12; }
	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.ARMOR, 6).add(Attributes.FOLLOW_RANGE, 48);
	}
	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 48));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (encounterOwner != null) {
			if (level.getGameTime() >= expiresAt) { discard(); return; }
			if (!(level.getEntity(encounterOwner) instanceof BiomeBoss boss)) {
				if (++missingOwnerTicks > 40) { discard(); }
				return;
			} else if (!boss.isAlive() || distanceToSqr(boss) > 160 * 160) { discard(); return; }
			missingOwnerTicks = 0;
		}
		if (blinkCooldown > 0) { blinkCooldown--; }
		if (blinkCooldown == 0 && getTarget() instanceof Player player && BiomeBoss.eligible(player) && distanceToSqr(player) > 12 * 12) {
			blink(level, player.getX() + random.nextInt(9) - 4, player.getY(), player.getZ() + random.nextInt(9) - 4);
		}
	}
	private boolean blink(ServerLevel level, double x, double y, double z) {
		blinkCooldown = 100;
		// Endborn Teleport: Vanilla's placement checks require a solid landing and an unblocked body.
		if (!randomTeleport(x, y, z, true, state -> false)) { return false; }
		level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 2, getZ(), 30, 0.5, 2, 0.5, 0.1);
		return true;
	}
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof Projectile && blinkCooldown == 0
				&& blink(level, getX() + random.nextInt(13) - 6, getY(), getZ() + random.nextInt(13) - 6)) { return false; }
		return super.hurtServer(level, source, amount);
	}
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (killedByPlayer && encounterOwner == null) {
			spawnAtLocation(level, new ItemStack(BossItems.ENDBORN_SHARD));
			spawnAtLocation(level, new ItemStack(Items.ENDER_PEARL));
		}
	}
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (encounterOwner != null) { output.putString("encounter_owner", encounterOwner.toString()); }
		output.putLong("encounter_expires", expiresAt);
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try { encounterOwner = UUID.fromString(input.getStringOr("encounter_owner", "")); }
		catch (IllegalArgumentException ignored) { encounterOwner = null; }
		expiresAt = input.getLongOr("encounter_expires", level().getGameTime() + 2400);
		if (encounterOwner != null) { xpReward = 0; }
		blinkCooldown = 60;
	}
	public static void initializeSpawning() {
		SpawnPlacements.register(BossEntities.ENDBORN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> level.getLevel().getDragonFight() != null
						&& level.getLevel().getDragonFight().hasPreviouslyKilledDragon() && Monster.checkMonsterSpawnRules(type, level, reason, pos, random));
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS),
				MobCategory.MONSTER, BossEntities.ENDBORN, 8, 1, 2);
	}
}
