package darkspawn.black.boss;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
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

public final class FrostWolf extends Monster {
	private UUID owner;
	private long expiresAt = -1;

	public FrostWolf(EntityType<? extends FrostWolf> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 0;
	}

	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 35).add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 64);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public void setOwner(UUID owner) { this.owner = owner; }

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (expiresAt < 0) { expiresAt = level.getGameTime() + 2400; }
		// Encounter Cleanup: Summoned pack members cannot survive their boss or an unloaded arena indefinitely.
		if (level.getGameTime() >= expiresAt || owner == null || !(level.getEntity(owner) instanceof MutantWolf boss)
				|| !boss.isAlive() || distanceToSqr(boss) > 128 * 128) {
			discard();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) { output.putString("darkspawn_owner", owner.toString()); }
		output.putLong("darkspawn_expires_at", expiresAt);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try { owner = UUID.fromString(input.getStringOr("darkspawn_owner", "")); }
		catch (IllegalArgumentException ignored) { owner = null; }
		expiresAt = input.getLongOr("darkspawn_expires_at", -1);
	}
}
