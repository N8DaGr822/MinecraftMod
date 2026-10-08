package darkspawn.black.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InventorySorter {
	private InventorySorter() {
	}

	public static void sort(ServerPlayer player, int expectedStateId) {
		// Inventory Safety: Only sort the player's own inventory on the server thread.
		if (!player.isAlive() || player.isSpectator()
				|| player.containerMenu != player.inventoryMenu
				|| !player.inventoryMenu.getCarried().isEmpty()) {
			return;
		}

		// State Sync: Reject requests based on an inventory the server has already changed.
		if (player.inventoryMenu.getStateId() != expectedStateId) {
			player.inventoryMenu.broadcastFullState();
			player.sendOverlayMessage(Component.translatable("message.darkspawn.inventory_changed"));
			return;
		}

		boolean changed = sortMainInventory(player.getInventory());
		player.inventoryMenu.broadcastChanges();
		player.sendOverlayMessage(Component.translatable(changed
				? "message.darkspawn.inventory_sorted"
				: "message.darkspawn.inventory_already_sorted"));
	}

	static boolean sortMainInventory(Container inventory) {
		if (inventory.getContainerSize() < Inventory.INVENTORY_SIZE) {
			throw new IllegalArgumentException("Sorting requires a full player inventory.");
		}

		int[] slots = InventoryExpansion.sortableSlots(inventory);
		List<ItemStack> sorted = new ArrayList<>(slots.length);
		// Stack Safety: Work on copies and compare every component before combining stacks.
		for (int slot : slots) {
			ItemStack remaining = inventory.getItem(slot).copy();
			if (remaining.isEmpty()) {
				continue;
			}

			if (remaining.isStackable()) {
				for (ItemStack target : sorted) {
					if (!ItemStack.isSameItemSameComponents(target, remaining)) {
						continue;
					}

					int space = inventory.getMaxStackSize(target) - target.getCount();
					int transferred = Math.min(space, remaining.getCount());
					if (transferred > 0) {
						target.grow(transferred);
						remaining.shrink(transferred);
					}
					if (remaining.isEmpty()) {
						break;
					}
				}
			}

			if (!remaining.isEmpty()) {
				sorted.add(remaining);
			}
		}

		// Sorting Order: Registry IDs are language-independent; equal IDs retain their order.
		sorted.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
		boolean changed = false;
		for (int index = 0; index < slots.length; index++) {
			ItemStack stack = index < sorted.size() ? sorted.get(index) : ItemStack.EMPTY;
			if (!ItemStack.matches(inventory.getItem(slots[index]), stack)) {
				changed = true;
				break;
			}
		}

		if (!changed) {
			return false;
		}

		// Protected Slots: Include unlocked storage while skipping hotbar, equipment, and locked rows.
		for (int index = 0; index < slots.length; index++) {
			inventory.setItem(slots[index], index < sorted.size() ? sorted.get(index) : ItemStack.EMPTY);
		}
		inventory.setChanged();
		return true;
	}
}
