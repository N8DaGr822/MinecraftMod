package darkspawn.black.explorerpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ExplorerPackMenu extends AbstractContainerMenu {
	private final Inventory inventory;
	private final int source;
	private final ItemStack pack;
	private final SimpleContainer contents;

	public ExplorerPackMenu(int id, Inventory inventory) { this(id, inventory, -1); }
	public ExplorerPackMenu(int id, Inventory inventory, int source) {
		super(ExplorerPackItem.MENU, id);
		this.inventory = inventory;
		this.source = source;
		pack = source < 0 ? ItemStack.EMPTY : inventory.getItem(source);
		contents = new SimpleContainer(6) {
			@Override public void setChanged() {
				// Pack Persistence: Save every mutation, before closing, death, or a profile transition.
				if (source < 0 || inventory.getItem(source) != pack) return;
				pack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
				inventory.setChanged();
			}
		};
		if (source >= 0) pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
		for (int i = 0; i < 6; i++) addSlot(new Slot(contents, i, 62 + i % 3 * 18, 28 + i / 3 * 18) {
			@Override public boolean mayPlace(ItemStack stack) { return ExplorerPackItem.accepts(stack); }
		});
		for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
			addPlayerSlot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18);
		for (int i = 0; i < 9; i++) addPlayerSlot(inventory, i, 8 + i * 18, 142);
	}
	private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
		addSlot(new Slot(inventory, index, x, y) {
			@Override public boolean mayPickup(Player player) { return !getItem().is(ExplorerPackItem.ITEM); }
			@Override public boolean mayPlace(ItemStack stack) { return !getItem().is(ExplorerPackItem.ITEM); }
		});
	}
	@Override public boolean stillValid(Player player) {
		return player.isAlive() && !player.isSpectator() && (source < 0 || inventory.getItem(source) == pack
			&& pack.is(ExplorerPackItem.ITEM) && pack.getCount() == 1);
	}
	@Override public void clicked(int slot, int button, ContainerInput input, Player player) {
		if (!stillValid(player)) return;
		// Pack Ownership: Number-key and offhand swaps bypass the destination slot's pickup check.
		if (input == ContainerInput.SWAP && button >= 0 && button < inventory.getContainerSize()
				&& inventory.getItem(button).is(ExplorerPackItem.ITEM)) return;
		super.clicked(slot, button, input, player);
	}
	@Override public ItemStack quickMoveStack(Player player, int index) {
		if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem(), original = stack.copy();
		if (index >= 6 && !ExplorerPackItem.accepts(stack)) return ItemStack.EMPTY;
		if (index < 6 ? !moveItemStackTo(stack, 6, slots.size(), true) : !moveItemStackTo(stack, 0, 6, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}
}
