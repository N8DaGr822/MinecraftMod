package darkspawn.black.inventory;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ArmorLoadoutState {
	// Loadout Ownership: The active set lives exclusively in vanilla's four armor slots.
	final NonNullList<ItemStack> stored = NonNullList.withSize(12, ItemStack.EMPTY);
	public int unlocked = 1;
	public int active;
	public int revision;
	public int pending = -1;
	int remaining;
	int menuRevision;
	int hurtCooldown;

	public ItemStack stored(int set, int piece) { return stored.get(set * 4 + piece); }

	public void restoreItems(List<ItemStack> items, int activeSet) {
		active = Math.clamp(activeSet, 0, unlocked - 1);
		for (int i = 0; i < stored.size(); i++) stored.set(i, i < items.size() && i / 4 != active ? items.get(i).copy() : ItemStack.EMPTY);
		pending = -1; remaining = 0; revision++;
	}

	public void read(ValueInput input) {
		unlocked = Math.clamp(input.getIntOr("darkspawn_armor_sets", 1), 1, 3);
		active = Math.clamp(input.getIntOr("darkspawn_armor_active", 0), 0, unlocked - 1);
		List<ItemStack> saved = input.read("darkspawn_armor_storage", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
		for (int i = 0; i < stored.size(); i++) {
			stored.set(i, i < saved.size() && i / 4 != active && i / 4 < unlocked ? saved.get(i).copy() : ItemStack.EMPTY);
		}
		pending = -1;
		remaining = 0;
		revision++;
	}

	public void write(ValueOutput output) {
		output.putInt("darkspawn_armor_sets", unlocked);
		output.putInt("darkspawn_armor_active", active);
		output.store("darkspawn_armor_storage", ItemStack.OPTIONAL_CODEC.listOf(), stored);
	}

	public void copyFrom(ArmorLoadoutState old, boolean keepItems) {
		unlocked = old.unlocked;
		active = old.active;
		revision = old.revision + 1;
		pending = -1;
		remaining = 0;
		for (int i = 0; i < stored.size(); i++) stored.set(i, keepItems ? old.stored.get(i).copy() : ItemStack.EMPTY);
	}
}
