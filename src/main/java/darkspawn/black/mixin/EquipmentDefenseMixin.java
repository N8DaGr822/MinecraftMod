package darkspawn.black.mixin;

import darkspawn.black.boss.EquipmentPowers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
	@Inject(method = "blockUsingItem", at = @At("HEAD"))
	private void blocked(ServerLevel level, LivingEntity attacker, DamageSource source, float amount, boolean flag, CallbackInfo ci) {
		EquipmentPowers.shieldBlock((LivingEntity) (Object) this, attacker, amount);
	}
}
