package darkspawn.black.mixin;

import darkspawn.black.forsaken.ForsakenContent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ForsakenHoneyMixin {
	@Inject(method = "finishUsingItem", at = @At("HEAD"))
	private void cureContamination(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> result) {
		// Completed Consumption: Honey's normal food/remainder handling continues; milk uses native effect removal.
		if (!level.isClientSide() && ((ItemStack)(Object)this).is(Items.HONEY_BOTTLE)) entity.removeEffect(ForsakenContent.CONTAMINATION);
	}
}
