package darkspawn.black.cooking.mixin;

import darkspawn.black.cooking.MealEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Keep the default priority (1000): darkspawn_expedition_gear's headlamp mixin uses 900 so its cancellable
// getEffect/hasEffect returns run first on the same targets.
@Mixin(LivingEntity.class)
public abstract class MealUtilityEffectMixin {
	// Utility Meals: Let vanilla vision and breathing checks see meals without adding or removing a potion.
	@Inject(method = "hasEffect", at = @At("RETURN"), cancellable = true)
	private void darkspawn$hasMealUtility(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValue() && MealEffects.utilityEffect((LivingEntity) (Object) this, effect, null) != null) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "getEffect", at = @At("RETURN"), cancellable = true)
	private void darkspawn$getMealUtility(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> callback) {
		MobEffectInstance result = MealEffects.utilityEffect((LivingEntity) (Object) this, effect, callback.getReturnValue());
		if (result != callback.getReturnValue()) {
			callback.setReturnValue(result);
		}
	}
}
