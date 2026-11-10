package darkspawn.black.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class ContainerSorter {
	private ContainerSorter() {
	}

	public static boolean sortContainer(Container inventory) {
		return sortSlots(inventory, java.util.stream.IntStream.range(0, inventory.getContainerSize()).toArray());
	}

	static boolean sortSlots(Container inventory, int[] slots) {
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

		// Protected Slots: Only write the slots selected by the caller.
		for (int index = 0; index < slots.length; index++) {
			inventory.setItem(slots[index], index < sorted.size() ? sorted.get(index) : ItemStack.EMPTY);
		}
		inventory.setChanged();
		return true;
	}
}
