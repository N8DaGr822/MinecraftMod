package darkspawn.black.armorstyles.mixin;

import darkspawn.black.armorstyles.ArmorStyleBonuses;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class ArmorStyleCombatMixin {
	@ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
	private float darkspawn$paladinHealing(float amount) {
		return amount > 0 && (Object) this instanceof ServerPlayer player && ArmorStyleBonuses.fullStyle(player).equals("paladin")
			? amount * 1.10F : amount;
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float darkspawn$styleDamage(float amount, ServerLevel level, DamageSource source, float original) {
		// Defense Refresh: Validate the worn set before this hit, including armor changed since the last server tick.
		if ((Object) this instanceof ServerPlayer player) ArmorStyleBonuses.update(player);
		return ArmorStyleBonuses.rangedDamage(source, ArmorStyleBonuses.tridentDamage(source, ArmorStyleBonuses.meleeDamage(source, amount)));
	}
}
