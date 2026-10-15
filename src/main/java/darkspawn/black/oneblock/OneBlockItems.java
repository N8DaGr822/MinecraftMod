package darkspawn.black.oneblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.inventory.ArmorLoadouts;
import darkspawn.black.inventory.InventoryExpansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record OneBlockItems(List<ItemStack> inventory, List<ItemStack> ender, List<ItemStack> armor, int active, List<ItemStack> accessories) {
	public static final OneBlockItems EMPTY = new OneBlockItems(List.of(), List.of(), List.of(), 0, List.of());
	public static final Codec<OneBlockItems> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ItemStack.OPTIONAL_CODEC.listOf(0, InventoryExpansion.STORAGE_SIZE).fieldOf("inventory").forGetter(OneBlockItems::inventory),
		ItemStack.OPTIONAL_CODEC.listOf(0, 27).fieldOf("ender").forGetter(OneBlockItems::ender),
		ItemStack.OPTIONAL_CODEC.listOf(0, 12).fieldOf("armor").forGetter(OneBlockItems::armor),
		Codec.intRange(0, 2).fieldOf("active").forGetter(OneBlockItems::active),
        ItemStack.OPTIONAL_CODEC.listOf(0,3).optionalFieldOf("accessories",List.of()).forGetter(OneBlockItems::accessories)
	).apply(instance, OneBlockItems::new));

	public OneBlockItems(List<ItemStack> inventory,List<ItemStack> ender,List<ItemStack> armor,int active){this(inventory,ender,armor,active,List.of());}

	public OneBlockItems {
        accessories=accessories.stream().map(ItemStack::copy).toList();
		inventory = inventory.stream().map(ItemStack::copy).toList();
		ender = ender.stream().map(ItemStack::copy).toList();
		armor = armor.stream().map(ItemStack::copy).toList();
	}

	public static OneBlockItems capture(ServerPlayer player) {
		var items = new ArrayList<ItemStack>(); var ender = new ArrayList<ItemStack>(); var armor = new ArrayList<ItemStack>();
		for (int i = 0; i < InventoryExpansion.STORAGE_SIZE; i++) items.add(player.getInventory().getItem(i));
		for (int i = 0; i < 27; i++) ender.add(player.getEnderChestInventory().getItem(i));
		for (int set = 0; set < 3; set++) for (int slot = 0; slot < 4; slot++) armor.add(ArmorLoadouts.state(player).stored(set, slot));
		return new OneBlockItems(items, ender, armor, ArmorLoadouts.state(player).active, darkspawn.black.inventory.Accessories.state(player).items());
	}

	public void apply(ServerPlayer player) {
		// Item Ownership: Capacity and character progression stay shared; every possession changes profiles.
		for (int i = 0; i < InventoryExpansion.STORAGE_SIZE; i++) player.getInventory().setItem(i, item(inventory, i));
		for (int i = 0; i < 27; i++) player.getEnderChestInventory().setItem(i, item(ender, i));
		ArmorLoadouts.state(player).restoreItems(armor, active);
		darkspawn.black.inventory.Accessories.state(player).restore(accessories);
        darkspawn.black.inventory.Accessories.sync(player);
		player.getInventory().setChanged();
	}
	private static ItemStack item(List<ItemStack> items, int slot) { return slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY; }
}
