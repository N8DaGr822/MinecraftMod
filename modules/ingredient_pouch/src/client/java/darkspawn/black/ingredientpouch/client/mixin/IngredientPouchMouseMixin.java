package darkspawn.black.ingredientpouch.client.mixin;

import darkspawn.black.ingredientpouch.IngredientPouchItem;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleMouseActions.class)
public abstract class IngredientPouchMouseMixin {
	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private void darkspawn_ingredient_pouch$selection(Slot slot, CallbackInfoReturnable<Boolean> callback) {
		// Pouch/Quiver Controls: Reuse scrolling without joining the bundle dye-recipe tag.
		if (slot.getItem().is(IngredientPouchItem.ITEM)) callback.setReturnValue(true);
	}
}
