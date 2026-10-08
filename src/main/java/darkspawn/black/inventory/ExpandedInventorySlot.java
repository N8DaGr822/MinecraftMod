package darkspawn.black.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ExpandedInventorySlot extends Slot {
	private final Inventory inventory;
	private final int extraIndex;

	public ExpandedInventorySlot(Inventory inventory, int extraIndex) {
		super(inventory, InventoryExpansion.FIRST_EXTRA_SLOT + extraIndex,
				8 + extraIndex % 9 * 18, 138 + extraIndex / 9 * 18);
		this.inventory = inventory;
		this.extraIndex = extraIndex;
	}

	@Override
	public boolean isActive() {
		return extraIndex < InventoryExpansion.unlockedSlots(inventory);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return isActive();
	}

	@Override
	public boolean mayPickup(Player player) {
		return isActive() && super.mayPickup(player);
	}
}
