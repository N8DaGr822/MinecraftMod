package darkspawn.black.storage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class StorageBlockEntity extends BlockEntity implements Container, MenuProvider {
	private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
	private final NonNullList<ItemStack> filters = NonNullList.withSize(3, ItemStack.EMPTY);
	private final List<BankContents.Entry> stored = new ArrayList<>();
	private final List<BankContents.Entry> trash = new ArrayList<>();
	private int tier;
	private int cursor;
	private int resortRemaining;
	private int resortCursor;
	private long revision;

	public StorageBlockEntity(BlockPos pos, BlockState state) { super(Storage.ENTITY, pos, state); }
	public boolean isBank() { return getBlockState().is(Storage.BANK); }
	public boolean isDump() { return getBlockState().is(Storage.DUMP); }
	public int tier() { return tier; }
	public int capacity() { return 256 << tier; }
	public int variants() { return stored.size() + trash.size(); }
	public long revision() { return revision; }
	public int resortRemaining() { return resortRemaining; }
	public List<BankContents.Entry> entries(boolean trashView) { return List.copyOf(trashView ? trash : stored); }
	public ItemStack filter(int index) { return filters.get(index); }
	public void setFilter(int index, ItemStack stack) {
		// Ghost Filters: Record the item type only; no physical item enters or leaves this slot.
		filters.set(index, stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem()));
		setChanged();
	}
	public boolean matches(ItemStack stack) { return filters.stream().anyMatch(filter -> !filter.isEmpty() && filter.is(stack.getItem())); }
	public boolean hasBankData() { return !stored.isEmpty() || !trash.isEmpty() || tier != 0 || filters.stream().anyMatch(filter -> !filter.isEmpty()); }

	public int insert(ItemStack offered, boolean routed) {
		if (offered.isEmpty() || offered.is(Storage.BANK.asItem())) { return 0; }
		if (isBank()) { return offered.getItem().canFitInsideContainerItems() ? insertBank(offered, routed && matches(offered)) : 0; }
		if (routed && (isDump() || !matches(offered))) { return 0; }
		int remaining = offered.getCount();
		// Stack Conservation: Merge compatible partial stacks before allocating empty slots.
		for (int pass = 0; pass < 2 && remaining > 0; pass++) {
			for (int i = 0; i < items.size() && remaining > 0; i++) {
				ItemStack existing = items.get(i);
				if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, offered)) {
					int moved = Math.min(remaining, Math.max(0, existing.getMaxStackSize() - existing.getCount()));
					existing.grow(moved);
					remaining -= moved;
				} else if (pass == 1 && existing.isEmpty()) {
					int moved = Math.min(remaining, offered.getMaxStackSize());
					items.set(i, offered.copyWithCount(moved));
					remaining -= moved;
				}
			}
		}
		if (remaining != offered.getCount()) { setChanged(); }
		return offered.getCount() - remaining;
	}

	private int insertBank(ItemStack offered, boolean trashDestination) {
		List<BankContents.Entry> destination = trashDestination ? trash : stored;
		int index = find(destination, offered);
		if (index < 0 && variants() >= capacity()) { return 0; }
		int existing = index < 0 ? 0 : destination.get(index).count();
		int moved = Math.min(offered.getCount(), BankContents.MAX_COUNT - existing);
		if (moved <= 0) { return 0; }
		var entry = new BankContents.Entry(offered, existing + moved);
		if (index < 0) { destination.add(entry); } else { destination.set(index, entry); }
		setChanged();
		return moved;
	}
	private static int find(List<BankContents.Entry> entries, ItemStack item) {
		for (int i = 0; i < entries.size(); i++) {
			if (ItemStack.isSameItemSameComponents(entries.get(i).item(), item)) { return i; }
		}
		return -1;
	}
	public int count(ItemStack item, boolean trashView) {
		List<BankContents.Entry> entries = trashView ? trash : stored;
		int index = find(entries, item);
		return index < 0 ? 0 : entries.get(index).count();
	}
	public ItemStack take(ItemStack item, int amount, boolean trashView) {
		List<BankContents.Entry> entries = trashView ? trash : stored;
		int index = find(entries, item);
		if (index < 0 || amount <= 0) { return ItemStack.EMPTY; }
		var entry = entries.get(index);
		int taken = Math.min(amount, entry.count());
		if (taken == entry.count()) { entries.remove(index); }
		else { entries.set(index, new BankContents.Entry(entry.item(), entry.count() - taken)); }
		setChanged();
		return entry.item().copyWithCount(taken);
	}
	public void emptyTrash() { if (!trash.isEmpty()) { trash.clear(); setChanged(); } }
	public boolean upgrade() { if (tier == 3) { return false; } tier++; setChanged(); return true; }
	public void startResort() { resortRemaining = stored.size(); resortCursor = 0; }

	public void tick() {
		if (level == null || level.isClientSide() || Math.floorMod(level.getGameTime() + worldPosition.asLong(), 5) != 0) { return; }
		if (isDump()) {
			for (int checked = 0; checked < items.size(); checked++) {
				int slot = cursor++ % items.size();
				if (cursor >= items.size()) { cursor = 0; }
				ItemStack source = items.get(slot);
				if (source.isEmpty()) { continue; }
				int moved = StorageRouting.transfer(this, source.copyWithCount(Math.min(64, source.getCount())), false);
				if (moved > 0) { source.shrink(moved); setChanged(); }
				break;
			}
		} else if (isBank() && resortRemaining > 0) {
			if (stored.isEmpty()) { resortRemaining = 0; return; }
			resortCursor %= stored.size();
			var entry = stored.get(resortCursor);
			int moved = StorageRouting.transfer(this, entry.item().copyWithCount(Math.min(64, entry.count())), true);
			if (moved > 0) { take(entry.item(), moved, false); }
			// Re-sort Work Budget: Continue a successful entry across ticks; skip a blocked entry once per pass.
			if (moved == 0 || count(entry.item(), false) == 0) {
				resortRemaining--;
				if (moved == 0) { resortCursor++; }
			}
		}
	}

	public BankContents snapshot() { return new BankContents(stored, trash, filters, tier); }
	private void restore(BankContents data) {
		stored.clear(); stored.addAll(data.stored());
		trash.clear(); trash.addAll(data.trash());
		filters.clear();
		for (int i = 0; i < data.filters().size(); i++) { filters.set(i, data.filters().get(i).copy()); }
		tier = data.tier();
	}
	@Override protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.store("StorageData", BankContents.CODEC, snapshot());
	}
	@Override protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear(); ContainerHelper.loadAllItems(input, items);
		restore(input.read("StorageData", BankContents.CODEC).orElse(BankContents.EMPTY));
	}
	@Override protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		if (isBank()) { restore(components.getOrDefault(Storage.BANK_CONTENTS, BankContents.EMPTY)); }
	}
	@Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (isBank()) { components.set(Storage.BANK_CONTENTS, snapshot()); }
	}
	@Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (!isBank()) { super.preRemoveSideEffects(pos, state); }
	}
	@Override public void setChanged() { revision++; super.setChanged(); }
	@Override public int getContainerSize() { return isBank() ? 0 : items.size(); }
	@Override public boolean isEmpty() { return isBank() ? stored.isEmpty() && trash.isEmpty() : items.stream().allMatch(ItemStack::isEmpty); }
	@Override public ItemStack getItem(int slot) { return items.get(slot); }
	@Override public ItemStack removeItem(int slot, int count) {
		ItemStack result = ContainerHelper.removeItem(items, slot, count); if (!result.isEmpty()) { setChanged(); } return result;
	}
	@Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
	@Override public void setItem(int slot, ItemStack item) { items.set(slot, item); item.limitSize(item.getMaxStackSize()); setChanged(); }
	@Override public void clearContent() { items.clear(); stored.clear(); trash.clear(); setChanged(); }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return !stack.is(Storage.BANK.asItem()) && (isDump() || matches(stack)); }
	@Override public boolean stillValid(Player player) { return !isRemoved() && Container.stillValidBlockEntity(this, player); }
	@Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new StorageMenu(id, inventory, this); }
}
