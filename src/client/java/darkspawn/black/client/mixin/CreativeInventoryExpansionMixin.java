package darkspawn.black.client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryExpansionMixin {
	@Redirect(method = { "selectTab", "slotClicked" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
	private int darkspawn$keepCatalogSlotsUnchanged(NonNullList<?> slots) {
		// Creative Catalog: Extra slots use the normal inventory screen, avoiding overlapping hotbar slots.
		return Math.min(slots.size(), 46);
	}
}
