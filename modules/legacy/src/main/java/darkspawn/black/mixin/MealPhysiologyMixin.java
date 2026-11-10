package darkspawn.black.mixin;

import darkspawn.black.cooking.MealEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MealPhysiologyMixin {
	@Inject(method = "decreaseAirSupply", at = @At("RETURN"), cancellable = true)
	private void breathe(int air, CallbackInfoReturnable<Integer> ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.hasEffect(MealEffects.DEEP_BREATH) && entity.tickCount % 2 == 0) ci.setReturnValue(air);
	}
	@ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
	private MobEffectInstance poison(MobEffectInstance effect) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (effect.getEffect().equals(MobEffects.POISON) && !effect.isInfiniteDuration() && entity.hasEffect(MealEffects.IRON_STOMACH)) {
			return new MobEffectInstance(effect.getEffect(), Math.max(1, effect.getDuration() / 2), effect.getAmplifier(),
				effect.isAmbient(), effect.isVisible(), effect.showIcon());
		}
		return effect;
	}
}
