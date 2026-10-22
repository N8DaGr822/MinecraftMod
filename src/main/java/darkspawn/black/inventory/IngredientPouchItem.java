package darkspawn.black.inventory;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemLore;

public final class IngredientPouchItem extends BundleItem {
	public static final TagKey<Item> INGREDIENTS = TagKey.create(Registries.ITEM, Darkspawn.id("pouch_ingredients"));
	public static Item ITEM;

	private IngredientPouchItem(Properties properties) {
		super(properties);
	}

	public static void register() {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id("ingredient_pouch"));
		ITEM = Registry.register(BuiltInRegistries.ITEM, key, new IngredientPouchItem(new Properties().setId(key)
			.stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
			.component(DataComponents.LORE, new ItemLore(List.of(
				Component.translatable("tooltip.darkspawn.ingredient_pouch.capacity"),
				Component.translatable("tooltip.darkspawn.ingredient_pouch.controls"))))));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(ITEM));
	}

	public static boolean accepts(ItemStack stack) {
		// Pouch Eligibility: Even a widened data-pack tag cannot introduce nested storage.
		return !stack.isEmpty() && stack.is(INGREDIENTS) && stack.getItem().canFitInsideContainerItems()
			&& !stack.has(DataComponents.BUNDLE_CONTENTS) && !stack.has(DataComponents.CONTAINER);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	private boolean reject(ItemStack other, ClickAction action, Player player) {
		if (action != ClickAction.PRIMARY || other.isEmpty() || accepts(other)) return false;
		player.sendOverlayMessage(Component.translatable("message.darkspawn.ingredient_pouch.rejected"));
		return true;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
		return reject(slot.getItem(), action, player) || super.overrideStackedOnOther(self, slot, action, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action,
			Player player, SlotAccess carriedItem) {
		return reject(other, action, player) || super.overrideOtherStackedOnMe(self, other, slot, action, player, carriedItem);
	}
}
