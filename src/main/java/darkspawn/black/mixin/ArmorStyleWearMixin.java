package darkspawn.black.mixin;

import darkspawn.black.equipment.ArmorStyleBonuses;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ArmorStyleWearMixin {
	@ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"), argsOnly = true)
	private int darkspawn$classicWear(int amount, int original, LivingEntity wearer, EquipmentSlot slot) {
		return ArmorStyleBonuses.armorWear(wearer, (ItemStack) (Object) this, slot, amount);
	}
}
