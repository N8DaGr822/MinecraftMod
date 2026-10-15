package darkspawn.black.mixin;

import darkspawn.black.equipment.ExpeditionArmor;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ExpeditionArmorMixin {
	// Fire Protection: Worn plates reduce fire damage, including lava, but never grant immunity.
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float darkspawn$ashwalkerFire(float amount, ServerLevel level, DamageSource source, float originalAmount) {
		return source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
			? amount * (1 - 0.1F * ExpeditionArmor.ashwalkerPieces((LivingEntity) (Object) this)) : amount;
	}

	@Inject(method = "canFreeze", at = @At("RETURN"), cancellable = true)
	private void darkspawn$insulatedSuit(CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValue() && ExpeditionArmor.wearsFullMountaineering((LivingEntity) (Object) this)) callback.setReturnValue(false);
	}

	// Headlamp Ownership: Consult equipped gear without adding, refreshing, or removing potion/meal effects.
	@Inject(method = "hasEffect", at = @At("RETURN"), cancellable = true)
	private void darkspawn$headlampVisible(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> callback) {
		if (effect.equals(MobEffects.NIGHT_VISION) && (ExpeditionArmor.hasHeadlamp((LivingEntity) (Object) this) || (Object)this instanceof net.minecraft.world.entity.player.Player p && darkspawn.black.inventory.Accessories.vision(p))) callback.setReturnValue(true);
	}

	@Inject(method = "getEffect", at = @At("RETURN"), cancellable = true)
	private void darkspawn$headlampVision(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> callback) {
		if (effect.equals(MobEffects.NIGHT_VISION) && (ExpeditionArmor.hasHeadlamp((LivingEntity) (Object) this) || (Object)this instanceof net.minecraft.world.entity.player.Player p && darkspawn.black.inventory.Accessories.vision(p))) {
			callback.setReturnValue(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
		}
	}
}
