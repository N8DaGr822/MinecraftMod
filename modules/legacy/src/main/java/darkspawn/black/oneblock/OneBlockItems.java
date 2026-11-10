package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record OneBlockItems(List<ItemStack> inventory, List<ItemStack> ender) {
	// Vanilla Slots: 36 storage slots plus the 7 equipment indices (armor, offhand, body, saddle).
	public static final int INVENTORY_SLOTS = 43;
	public static final int ENDER_SLOTS = 27;
	public static final OneBlockItems EMPTY = new OneBlockItems(List.of(), List.of());
	public static final Codec<OneBlockItems> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ItemStack.OPTIONAL_CODEC.listOf(0, INVENTORY_SLOTS).fieldOf("inventory").forGetter(OneBlockItems::inventory),
		ItemStack.OPTIONAL_CODEC.listOf(0, ENDER_SLOTS).fieldOf("ender").forGetter(OneBlockItems::ender)
	).apply(instance, OneBlockItems::new));

	public OneBlockItems {
		inventory = inventory.stream().map(ItemStack::copy).toList();
		ender = ender.stream().map(ItemStack::copy).toList();
	}

	public static OneBlockItems capture(ServerPlayer player) {
		var items = new ArrayList<ItemStack>(); var ender = new ArrayList<ItemStack>();
		for (int i = 0; i < INVENTORY_SLOTS; i++) items.add(player.getInventory().getItem(i));
		for (int i = 0; i < ENDER_SLOTS; i++) ender.add(player.getEnderChestInventory().getItem(i));
		return new OneBlockItems(items, ender);
	}

	public void apply(ServerPlayer player) {
		// Item Ownership: Character progression stays shared; every possession changes profiles.
		for (int i = 0; i < INVENTORY_SLOTS; i++) player.getInventory().setItem(i, item(inventory, i));
		for (int i = 0; i < ENDER_SLOTS; i++) player.getEnderChestInventory().setItem(i, item(ender, i));
		player.getInventory().setChanged();
	}
	private static ItemStack item(List<ItemStack> items, int slot) { return slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY; }
}
