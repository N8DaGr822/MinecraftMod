package darkspawn.black.mixin;

import darkspawn.black.boss.AncientTreeSpirit;
import darkspawn.black.boss.BiomeBoss;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.MutantWolf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BossHealthMixin {
	private static boolean darkspawn$isBoss(LivingEntity entity) {
		return entity instanceof BiomeBoss || entity instanceof AncientTreeSpirit || entity instanceof MutantWolf;
	}

	@Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
	private void darkspawn$bossHealth(CallbackInfoReturnable<Float> result) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (darkspawn$isBoss(entity)) { result.setReturnValue((float) entity.getAttributeValue(BossEntities.MAX_HEALTH)); }
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$saveBossHealthVersion(ValueOutput output, CallbackInfo callback) {
		if (darkspawn$isBoss((LivingEntity) (Object) this)) { output.putBoolean("darkspawn_boss_health", true); }
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void darkspawn$migrateBossHealth(ValueInput input, CallbackInfo callback) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (darkspawn$isBoss(entity) && !input.getBooleanOr("darkspawn_boss_health", false)) {
			// Save Migration: Earlier bosses stored their requested pool in the capped vanilla attribute.
			double oldMaximum = entity.getAttributeBaseValue(Attributes.MAX_HEALTH);
			if (oldMaximum > 20) {
				entity.getAttribute(BossEntities.MAX_HEALTH).setBaseValue(oldMaximum);
				entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
				entity.setHealth(input.getFloatOr("Health", entity.getMaxHealth()));
			}
		}
	}
}
