package darkspawn.black.boss;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class HeartwoodSapling extends Monster implements AnimatedCreature {
	private final com.geckolib.animatable.instance.AnimatableInstanceCache animationCache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);
	@Override public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
	private UUID owner;
	private int lifeTicks;

	public HeartwoodSapling(EntityType<? extends HeartwoodSapling> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 0;
	}

	public static AttributeSupplier.Builder attributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (++lifeTicks > 2400 || owner == null || !(level.getEntity(owner) instanceof AncientTreeSpirit boss) || !boss.isAlive()) {
			discard();
			return;
		}
		if (tickCount % 40 == 0 && distanceToSqr(boss) < 48 * 48) {
			boss.heal(boss.forestVariant() == 1 ? 6 : 4);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 12, 0.5, 1, 0.5, 0.1);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, boss.getX(), boss.getY() + 10 * AncientTreeSpirit.MODEL_SCALE, boss.getZ(),
					15, AncientTreeSpirit.MODEL_SCALE, 2 * AncientTreeSpirit.MODEL_SCALE, AncientTreeSpirit.MODEL_SCALE, 0.1);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) {
			output.putString("darkspawn_owner", owner.toString());
		}
		output.putInt("darkspawn_life_ticks", lifeTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			owner = UUID.fromString(input.getStringOr("darkspawn_owner", ""));
		} catch (IllegalArgumentException ignored) {
			owner = null;
		}
		lifeTicks = input.getIntOr("darkspawn_life_ticks", 0);
	}
}
