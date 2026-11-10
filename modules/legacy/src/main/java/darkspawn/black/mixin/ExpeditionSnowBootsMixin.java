package darkspawn.black.mixin;

import darkspawn.black.equipment.ExpeditionArmor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class ExpeditionSnowBootsMixin {
	@Inject(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
	private static void darkspawn$snowBoots(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		var boots = ExpeditionArmor.MOUNTAINEERING.get(ArmorType.BOOTS);
		if (boots != null && entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.FEET).is(boots)) callback.setReturnValue(true);
	}
}
