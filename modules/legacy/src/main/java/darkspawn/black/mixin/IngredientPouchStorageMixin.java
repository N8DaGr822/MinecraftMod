package darkspawn.black.mixin;

import darkspawn.black.inventory.IngredientPouchItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class IngredientPouchStorageMixin {
	@Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
	private void darkspawn$rejectPouchNesting(int slot, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> callback) {
		// Utility Container Nesting: Hoppers bypass the native shulker menu's container-item check.
		if (stack.is(IngredientPouchItem.ITEM)||stack.is(darkspawn.black.inventory.QuiverItem.ITEM)
				||stack.is(darkspawn.black.inventory.ExplorerPackItem.ITEM)) callback.setReturnValue(false);
	}
}
