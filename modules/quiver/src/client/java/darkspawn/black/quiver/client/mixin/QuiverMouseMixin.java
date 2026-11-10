package darkspawn.black.quiver.client.mixin;

import darkspawn.black.quiver.QuiverItem;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleMouseActions.class)
public abstract class QuiverMouseMixin {
	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private void darkspawn_quiver$selection(Slot slot, CallbackInfoReturnable<Boolean> callback) {
		// Pouch/Quiver Controls: Reuse scrolling without joining the bundle dye-recipe tag.
		if (slot.getItem().is(QuiverItem.ITEM)) callback.setReturnValue(true);
	}
}
