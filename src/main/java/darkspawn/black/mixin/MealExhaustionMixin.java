package darkspawn.black.mixin;

import darkspawn.black.cooking.MealEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class MealExhaustionMixin {
	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float exhaustion(float amount) {
		Player player = (Player) (Object) this;
		return player.hasEffect(MealEffects.SUSTAINED) || player.isSprinting() && player.hasEffect(MealEffects.ENERGIZED)
			? amount * 0.8F : amount;
	}
}
