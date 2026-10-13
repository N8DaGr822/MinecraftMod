package darkspawn.black.starter;

import darkspawn.black.Darkspawn;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** A consumable kit that places a normal chest; normal chest drops never refill it. */
public final class DragonStarterChest extends Item {
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("dragon_starter_chest"),
		new DragonStarterChest(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
			Darkspawn.id("dragon_starter_chest"))).stacksTo(1)));

	private DragonStarterChest(Properties properties) { super(properties); }

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(ITEM));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var placement = new BlockPlaceContext(context);
		var pos = placement.getClickedPos();
		// Delegate permissions, collision, orientation, chest joining, sound and consumption to vanilla.
		var result = ((BlockItem) Items.CHEST).place(placement);
		if (result.consumesAction() && context.getLevel() instanceof ServerLevel level
				&& level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			var contents = createContents(level);
			for (int slot = 0; slot < contents.size(); slot++) chest.setItem(slot, contents.get(slot));
			chest.setChanged();
		}
		return result;
	}

	public static List<ItemStack> createContents(ServerLevel level) {
		var kit = new ArrayList<ItemStack>(27);
		for (Item armor : List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
			var stack = enchanted(level, armor, Enchantments.PROTECTION, 4);
			if (armor == Items.DIAMOND_BOOTS) stack.enchant(level.registryAccess().getOrThrow(Enchantments.FEATHER_FALLING), 4);
			kit.add(stack);
		}
		kit.add(enchanted(level, Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 5));
		var bow = enchanted(level, Items.BOW, Enchantments.POWER, 5);
		bow.enchant(level.registryAccess().getOrThrow(Enchantments.INFINITY), 1);
		kit.add(bow);
		kit.add(enchanted(level, Items.DIAMOND_PICKAXE, Enchantments.EFFICIENCY, 4));
		kit.add(new ItemStack(Items.SHIELD));
		kit.add(new ItemStack(Items.ARROW, 64));
		kit.add(new ItemStack(Items.ARROW, 64));
		kit.add(new ItemStack(Items.COOKED_BEEF, 64));
		kit.add(new ItemStack(Items.GOLDEN_APPLE, 16));
		for (int i = 0; i < 3; i++) kit.add(new ItemStack(Items.COBBLESTONE, 64));
		kit.add(new ItemStack(Items.WATER_BUCKET));
		kit.add(new ItemStack(Items.WATER_BUCKET));
		kit.add(new ItemStack(Items.ENDER_PEARL, 16));
		kit.add(new ItemStack(Items.ENDER_EYE, 16));
		for (int i = 0; i < 3; i++) kit.add(PotionContents.createItemStack(Items.POTION, Potions.LONG_SLOW_FALLING));
		kit.add(PotionContents.createItemStack(Items.POTION, Potions.LONG_STRENGTH));
		kit.add(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_HEALING));
		kit.add(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_HEALING));
		kit.add(new ItemStack(Items.TORCH, 64));
		kit.add(new ItemStack(Items.CARVED_PUMPKIN));
		return kit;
	}

	private static ItemStack enchanted(ServerLevel level, Item item, ResourceKey<Enchantment> enchantment, int rank) {
		var stack = new ItemStack(item);
		stack.enchant(level.registryAccess().getOrThrow(enchantment), rank);
		stack.enchant(level.registryAccess().getOrThrow(Enchantments.UNBREAKING), 3);
		return stack;
	}
}
