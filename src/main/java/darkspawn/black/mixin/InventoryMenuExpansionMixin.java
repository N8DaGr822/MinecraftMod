package darkspawn.black.mixin;

import darkspawn.black.inventory.ExpandedInventorySlot;
import darkspawn.black.inventory.InventoryExpansion;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuExpansionMixin extends AbstractContainerMenu {
	protected InventoryMenuExpansionMixin(MenuType<?> type, int containerId) {
		super(type, containerId);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void darkspawn$addExtraSlots(Inventory inventory, boolean active, Player owner, CallbackInfo callback) {
		// Menu Sync: Keep slot IDs stable on both sides; locked rows reject item interaction.
		for (int slot = 0; slot < InventoryExpansion.EXTRA_SLOTS; slot++) {
			addSlot(new ExpandedInventorySlot(inventory, slot));
		}
	}

	@Redirect(method = "quickMoveStack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/InventoryMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
	private boolean darkspawn$shiftClickOverflow(InventoryMenu menu, ItemStack stack, int start, int end,
			boolean backwards, Player player, int sourceSlot) {
		boolean moved = moveItemStackTo(stack, start, end, backwards);
		if (!stack.isEmpty() && sourceSlot < InventoryExpansion.FIRST_EXTRA_MENU_SLOT && (start == 9 || start == 36)) {
			moved |= moveItemStackTo(stack, InventoryExpansion.FIRST_EXTRA_MENU_SLOT,
					InventoryExpansion.FIRST_EXTRA_MENU_SLOT + InventoryExpansion.unlockedSlots(player.getInventory()), false);
		}
		return moved;
	}
}
