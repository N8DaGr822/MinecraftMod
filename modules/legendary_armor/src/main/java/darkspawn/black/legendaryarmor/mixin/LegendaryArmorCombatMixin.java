package darkspawn.black.legendaryarmor.mixin;

import darkspawn.black.legendaryarmor.LegendaryArmor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LegendaryArmorCombatMixin {
	@Inject(method = "causeFallDamage", at = @At("HEAD"))
	private void darkspawn$refreshLegendaryFall(CallbackInfoReturnable<Boolean> callback) {
		if ((Object) this instanceof ServerPlayer player) LegendaryArmor.update(player);
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float darkspawn$legendaryDamage(float amount, ServerLevel level, DamageSource source, float original) {
		// Defense Refresh: Validate the worn set before this hit, including armor changed since the last server tick.
		if ((Object) this instanceof ServerPlayer player) LegendaryArmor.update(player);
		return LegendaryArmor.damage((LivingEntity) (Object) this, source, amount);
	}
}
