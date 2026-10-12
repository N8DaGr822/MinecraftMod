package darkspawn.black.storage;

import darkspawn.black.inventory.InventoryExpansion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageMenu extends AbstractContainerMenu {
	public static final int PAGE_SIZE = 27;
	public static final int FILTER_START = 27;
	public static final int PLAYER_START = 30;
	public static final int DEPOSIT = 0, PREVIOUS = 1, NEXT = 2, SEARCH = 3, ORDER = 4, TAB = 5, RESORT = 6, TRASH = 7, UPGRADE = 8;
	private final StorageBlockEntity chest;
	private final SimpleContainer display = new SimpleContainer(PAGE_SIZE);
	private final SimpleContainer filters = new SimpleContainer(3);
	private final SimpleContainerData data = new SimpleContainerData(8 + PAGE_SIZE * 2);
	private final List<ItemStack> visibleKeys = new ArrayList<>();
	private String query = "";
	private boolean byCount;
	private long observedRevision = -1;
	private long trashConfirmUntil = -1;
	private long trashConfirmRevision = -1;

	public StorageMenu(int id, Inventory inventory) { this(id, inventory, null); }
	public StorageMenu(int id, Inventory inventory, StorageBlockEntity chest) {
		super(Storage.MENU, id);
		this.chest = chest;
		if (chest != null) { data.set(0, chest.isBank() ? 2 : chest.isDump() ? 0 : 1); }
		addDataSlots(data);
		Container contents = chest != null && !chest.isBank() ? chest : display;
		for (int i = 0; i < PAGE_SIZE; i++) {
			addSlot(new Slot(contents, i, 60 + i % 9 * 18, 64 + i / 9 * 18) {
				@Override public boolean mayPlace(ItemStack stack) { return !isBank() && !stack.is(Storage.BANK.asItem()); }
				@Override public boolean mayPickup(Player player) { return !isBank(); }
				@Override public boolean isFake() { return isBank(); }
			});
		}
		for (int i = 0; i < 3; i++) {
			addSlot(new Slot(filters, i, 18, 64 + i * 18) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
				@Override public boolean mayPickup(Player player) { return false; }
				@Override public boolean isFake() { return true; }
				@Override public boolean isActive() { return kind() != 0; }
			});
		}
		addStandardInventorySlots(inventory, 60, 158);
		if (chest != null) { rebuildPage(); refresh(); }
	}
	public int kind() { return data.get(0); }
	public boolean isBank() { return kind() == 2; }
	public boolean trashView() { return data.get(3) != 0; }
	public int page() { return data.get(1); }
	public int pages() { return data.get(2); }
	public int tier() { return data.get(4); }
	public int variants() { return data.get(5); }
	public boolean resorting() { return data.get(6) != 0; }
	public boolean trashArmed() { return data.get(7) != 0; }
	public int quantity(int slot) { return (data.get(8 + slot * 2) & 65535) | ((data.get(9 + slot * 2) & 65535) << 16); }

	private void rebuildPage() {
		if (chest == null || !isBank()) { return; }
		var entries = new ArrayList<>(chest.entries(trashView()));
		entries.removeIf(entry -> !entry.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
			&& !BuiltInRegistries.ITEM.getKey(entry.item().getItem()).toString().contains(query));
		Comparator<BankContents.Entry> name = Comparator.comparing(entry -> entry.item().getHoverName().getString().toLowerCase(Locale.ROOT));
		entries.sort(byCount ? Comparator.comparingInt(BankContents.Entry::count).reversed().thenComparing(name) : name);
		data.set(2, Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE));
		data.set(1, Math.min(page(), pages() - 1));
		visibleKeys.clear();
		for (int i = page() * PAGE_SIZE; i < Math.min(entries.size(), (page() + 1) * PAGE_SIZE); i++) {
			visibleKeys.add(entries.get(i).item().copy());
		}
	}
	private void refresh() {
		if (chest == null) { return; }
		for (int i = 0; i < 3; i++) { filters.setItem(i, chest.filter(i).copy()); }
		data.set(4, chest.tier()); data.set(5, chest.variants()); data.set(6, chest.resortRemaining() > 0 ? 1 : 0);
		data.set(7, chest.getLevel().getGameTime() <= trashConfirmUntil && chest.revision() == trashConfirmRevision ? 1 : 0);
		if (isBank()) {
			// Stable Rows: Do not remap visible item identities under a pending click from another client.
			for (int i = 0; i < PAGE_SIZE; i++) {
				ItemStack key = i < visibleKeys.size() ? visibleKeys.get(i) : ItemStack.EMPTY;
				int count = key.isEmpty() ? 0 : chest.count(key, trashView());
				display.setItem(i, count == 0 ? ItemStack.EMPTY : key.copyWithCount(1));
				data.set(8 + i * 2, count & 65535); data.set(9 + i * 2, count >>> 16);
			}
		}
		observedRevision = chest.revision();
	}
	@Override public void broadcastChanges() {
		if (chest != null && (observedRevision != chest.revision()
			|| resorting() != (chest.resortRemaining() > 0)
			|| trashArmed() && chest.getLevel().getGameTime() > trashConfirmUntil)) { refresh(); }
		super.broadcastChanges();
	}
	@Override public boolean stillValid(Player player) {
		return player.isAlive() && !player.isSpectator() && (chest == null || chest.stillValid(player));
	}
	@Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != filters && (!isBank() || slot.container != display) && super.canTakeItemForPickAll(stack, slot);
	}
	@Override public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (!stillValid(player)) { return; }
		if (slotId >= FILTER_START && slotId < PLAYER_START) {
			if (chest != null && kind() != 0 && input == ContainerInput.PICKUP && (button == 0 || button == 1)) {
				chest.setFilter(slotId - FILTER_START, button == 1 ? ItemStack.EMPTY : getCarried());
				broadcastChanges();
			}
			return;
		}
		if (isBank() && slotId >= 0 && slotId < PAGE_SIZE) {
			if (chest == null || (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE)) { return; }
			if (!getCarried().isEmpty()) {
				if (input == ContainerInput.PICKUP) { getCarried().shrink(chest.insert(getCarried(), false)); }
			} else if (slotId < visibleKeys.size()) {
				ItemStack key = visibleKeys.get(slotId);
				if (input == ContainerInput.QUICK_MOVE) { withdrawToInventory(player, key); }
				else if (button == 0 || button == 1) { setCarried(chest.take(key, button == 1 ? 1 : key.getMaxStackSize(), trashView())); }
			}
			broadcastChanges();
			return;
		}
		super.clicked(slotId, button, input, player);
	}
	private void withdrawToInventory(Player player, ItemStack key) {
		// Withdrawal Transaction: Only debit the Bank for items accepted by the player's normal inventory.
		int budget = 36 * key.getMaxStackSize();
		while (budget > 0) {
			int count = Math.min(key.getMaxStackSize(), chest.count(key, trashView()));
			if (count == 0) { break; }
			ItemStack offered = key.copyWithCount(count);
			moveItemStackTo(offered, PLAYER_START, slots.size(), false);
			int moved = count - offered.getCount();
			if (moved == 0) { break; }
			chest.take(key, moved, trashView()); budget -= moved;
		}
	}
	@Override public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size() || index >= FILTER_START && index < PLAYER_START || !stillValid(player)) { return ItemStack.EMPTY; }
		Slot slot = slots.get(index);
		if (!slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
		ItemStack stack = slot.getItem(), original = stack.copy();
		if (index < PAGE_SIZE) {
			if (isBank() || !moveItemStackTo(stack, PLAYER_START, slots.size(), true)) { return ItemStack.EMPTY; }
		} else if (chest != null) {
			stack.shrink(chest.insert(stack, false));
		} else if (!isBank()) {
			moveItemStackTo(stack, 0, PAGE_SIZE, false);
		}
		if (stack.getCount() == original.getCount()) { return ItemStack.EMPTY; }
		if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
		slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
		return original;
	}
	public void action(Player player, int action, String search) {
		if (chest == null || !stillValid(player) || !getCarried().isEmpty()) { return; }
		if (action == DEPOSIT) {
			for (int slot : InventoryExpansion.sortableSlots(player.getInventory())) {
				ItemStack stack = player.getInventory().getItem(slot);
				stack.shrink(chest.insert(stack, false));
			}
			player.getInventory().setChanged();
		} else if (action == ORDER && !isBank()) {
			darkspawn.black.inventory.InventorySorter.sortContainer(chest);
		} else if (isBank()) {
			switch (action) {
				case PREVIOUS -> data.set(1, Math.max(0, page() - 1));
				case NEXT -> data.set(1, Math.min(pages() - 1, page() + 1));
				case SEARCH -> { query = search.substring(0, Math.min(64, search.length())).strip().toLowerCase(Locale.ROOT); data.set(1, 0); }
				case ORDER -> { byCount = !byCount; data.set(1, 0); }
				case TAB -> { data.set(3, trashView() ? 0 : 1); data.set(1, 0); }
				case RESORT -> chest.startResort();
				case TRASH -> {
					if (!trashView()) { return; }
					long now = player.level().getGameTime();
					if (now <= trashConfirmUntil && chest.revision() == trashConfirmRevision) {
						chest.emptyTrash(); trashConfirmUntil = -1;
					} else { trashConfirmUntil = now + 100; trashConfirmRevision = chest.revision(); }
				}
				case UPGRADE -> upgrade(player);
				default -> { return; }
			}
		}
		if (action != TRASH) { trashConfirmUntil = -1; }
		rebuildPage(); refresh(); broadcastChanges();
	}
	private void upgrade(Player player) {
		if (chest.tier() == 3) { return; }
		int cost = 1 << chest.tier();
		Inventory inventory = player.getInventory();
		int available = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) { if (inventory.getItem(i).is(Items.DIAMOND_BLOCK)) { available += inventory.getItem(i).getCount(); } }
		if (available < cost) { return; }
		for (int i = 0; i < inventory.getContainerSize() && cost > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.DIAMOND_BLOCK)) { int taken = Math.min(cost, stack.getCount()); stack.shrink(taken); cost -= taken; }
		}
		inventory.setChanged(); chest.upgrade();
	}
}
