package darkspawn.black.starter;

import darkspawn.black.Darkspawn;
import darkspawn.black.bonfire.Bonfires;
import darkspawn.black.cooking.*;
import darkspawn.black.equipment.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Opt-in, repeatable Creative/command testing supplies. Does not change progression. */
public final class TestingCamp extends Item {
	public static final int RADIUS = 10;
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, Darkspawn.id("testing_camp"),
		new TestingCamp(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Darkspawn.id("testing_camp"))).stacksTo(1)));
	public record Supply(String name, List<ItemStack> items) { }
	private TestingCamp(Properties properties) { super(properties); }
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(ITEM));
	}
	@Override public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.mayBuild() || context.getClickedFace() != Direction.UP) return InteractionResult.FAIL;
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		String error = place(level, context.getClickedPos(), player, context.getItemInHand());
		if (error != null) {
			player.sendSystemMessage(Component.literal(error));
			return InteractionResult.FAIL;
		}
		if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
		player.sendSystemMessage(Component.literal("Testing camp ready! Open the labeled barrels. Cookbook + ingredients test cooking; scrolls unlock recipes. Bonfire starts OFF. Use /summon darkspawn:griffin for flight tests."));
		return InteractionResult.SUCCESS;
	}

	private static void add(List<ItemStack> list, Item item, int count) {
		list.add(new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize())));
	}
	private static List<ItemStack> stacks(int count, Item... items) {
		var result = new ArrayList<ItemStack>();
		for (Item item : items) add(result, item, count);
		return result;
	}
	public static List<Supply> supplies(ServerLevel level) {
		var result = new ArrayList<Supply>();
		var guide = new ItemStack(Items.WRITTEN_BOOK);
		var guidePages = List.of(
			"DARKSPAWN TEST CAMP\n\nStart here: take the dragon kit, cookbook and scrolls. Switch to Survival for real food, equipment and crafting behavior. Supplies are finite; place another camp in a clear area to restock.",
			"COOKING\n\nUse scrolls to learn locked recipes. Open the cookbook, then put its ingredients into the Cooking Station beside the crafting table. Pantry barrels include every loaded cooking ingredient. Finished meals let you compare buffs immediately.",
			"FARMING AND ARMOR\n\nThe irrigated plot has all nine crops. Try seeds and bone meal. Compare iron/diamond styles, five expedition sets and five legendary sets. Use the inventory's loadouts. XP bottles and lapis support enchanting.",
			"BASE TESTS\n\nThe bonfire starts OFF. Empty-hand right-click enables it; sneak-click cycles range. Keep it OFF for nearby hostile tests. Build a sorting line using the storage barrel. Use a bed from that barrel to set your spawn in the Overworld.",
			"TRAVEL AND FLIGHT\n\n/summon darkspawn:griffin\nRide empty-handed; Space rises, X descends, Shift dismounts. OneBlock is in the inventory menu. Quarantine Gates still require dragon completion and a valid frame. This camp does not unlock progression or change gamerules.");
		guide.set(DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
			net.minecraft.server.network.Filterable.passThrough("Testing Camp Guide"), "Darkspawn", 0,
			guidePages.stream().map(text -> net.minecraft.server.network.Filterable.passThrough((Component) Component.literal(text))).toList(), true));
		result.add(new Supply("00 START HERE - testing guide", List.of(guide)));
		result.add(new Supply("01 Dragon combat kit", DragonStarterChest.createContents(level)));
		// Use the loaded recipes, including data packs, so the pantry cannot drift from cooking recipes.
		var ingredients = new TreeMap<String, Item>();
		level.getServer().getRecipeManager().getRecipes().forEach(holder -> {
			if (holder.value() instanceof CookingRecipe recipe) recipe.ingredients().forEach(ingredient ->
				ingredient.items().forEach(item -> ingredients.put(BuiltInRegistries.ITEM.getKey(item.value()).toString(), item.value())));
		});
		var pantry = new ArrayList<ItemStack>();
		ingredients.values().forEach(item -> add(pantry, item, 64));
		result.add(new Supply("02 Cooking ingredients", pantry));
		var meals = stacks(8, Cooking.HERB_STEAK, Cooking.HONEY_PORK, Cooking.BEEF_WELLINGTON, Cooking.FISH_CHOWDER,
			Cooking.CHICKEN_CORDON_BLEU, Cooking.WOODLAND_STEW);
		Cuisine.DISHES.values().forEach(dish -> add(meals, dish.item(), 8));
		result.add(new Supply("03 Meals and feasts", meals));
		var scrolls = stacks(1, Cuisine.COOKBOOK);
		Cuisine.SCROLLS.values().forEach(item -> add(scrolls, item, 4));
		result.add(new Supply("04 Cookbook and recipe scrolls", scrolls));
		var crops = stacks(64, Items.BONE_MEAL, Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS);
		Agriculture.CROPS.forEach(crop -> { add(crops, crop.seeds(), 64); add(crops, crop.ingredient(), 64); });
		add(crops, Items.DIAMOND_HOE, 1);
		result.add(new Supply("05 Farming", crops));
		var expedition = new ArrayList<ItemStack>();
		List.of(ExpeditionArmor.MOUNTAINEERING, ExpeditionArmor.SPELUNKING, ExpeditionArmor.DIVER,
			ExpeditionArmor.DESERT_WANDERER, ExpeditionArmor.ASHWALKER).forEach(set -> set.values().forEach(item -> add(expedition, item, 1)));
		result.add(new Supply("06 Climate and expedition armor", expedition));
		var legendary = new ArrayList<ItemStack>();
		LegendaryArmor.SETS.values().forEach(set -> set.values().forEach(item -> add(legendary, item, 1)));
		LegendaryArmor.CORES.values().forEach(item -> add(legendary, item, 16));
		result.add(new Supply("07 Legendary armor and cores", legendary));
		for (String style : ArmorStyles.STYLES) {
			var armor = new ArrayList<ItemStack>();
			for (Item item : new Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}) armor.add(ArmorStyles.apply(new ItemStack(item), style));
			result.add(new Supply("Style: " + style.replace('_', ' '), armor));
		}
		result.add(new Supply("08 Crafting and fuel", stacks(64, Items.COAL, Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT,
			Items.DIAMOND, Items.NETHERITE_INGOT, Items.LEATHER, Items.PAPER, Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK,
			Items.COBBLESTONE, Items.STRING, Items.FLINT, Items.LAPIS_LAZULI, Items.EXPERIENCE_BOTTLE, Items.BOOK,
			Items.BLAZE_POWDER, Items.NETHER_WART, Items.GLASS_BOTTLE, Items.REDSTONE, Items.GLOWSTONE_DUST)));
		var utilities = stacks(16, Bonfires.ITEM, Items.STONE_BRICKS, Items.WATER_BUCKET, Items.CAMPFIRE, Items.TORCH,
			Items.CHEST, Items.OBSIDIAN);
		for (String id : List.of("copper_block", "white_bed", "anvil", "smithing_table")) add(utilities, BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id)), 8);
		for (String id : List.of("dump_chest", "sorting_chest", "sorting_tube", "bank_chest", "leather_inventory_upgrade",
			"iron_inventory_upgrade", "diamond_inventory_upgrade", "quarantine_gate", "containment_key")) add(utilities, BuiltInRegistries.ITEM.getValue(Darkspawn.id(id)), 16);
		result.add(new Supply("09 Base, storage and travel", utilities));
		return result;
	}

	public static List<BlockPos> barrelPositions(BlockPos center) {
		var positions = new ArrayList<BlockPos>();
		for (int x = -9; x <= 9; x += 2) { positions.add(center.offset(x, 1, -9)); positions.add(center.offset(x, 1, 9)); }
		for (int z = -7; z <= 7; z += 2) { positions.add(center.offset(-9, 1, z)); positions.add(center.offset(9, 1, z)); }
		return positions;
	}

	/** Returns a readable refusal before changing any block. Null means placement succeeded. */
	public static String place(ServerLevel level, BlockPos center, Player player, ItemStack kit) {
		var pages = new ArrayList<Supply>();
		for (Supply supply : supplies(level)) for (int i = 0; i < supply.items().size(); i += 27)
			pages.add(new Supply(supply.name() + (supply.items().size() > 27 ? " / " + (i / 27 + 1) : ""),
				supply.items().subList(i, Math.min(i + 27, supply.items().size()))));
		var barrels = barrelPositions(center);
		if (pages.size() > barrels.size()) return "Too many testing supplies for this camp (data-pack recipes may add more ingredients).";
		var plan = new LinkedHashMap<BlockPos, BlockState>();
		for (int x = -RADIUS; x <= RADIUS; x++) for (int z = -RADIUS; z <= RADIUS; z++) for (int y = 0; y <= 4; y++) {
			var pos = center.offset(x, y, z);
			if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos))
				return "Camp needs a loaded 21 x 21 area inside the world border, with four blocks of headroom.";
			if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, kit)))
				return "You cannot build throughout this camp area.";
			var state = level.getBlockState(pos);
			if (y == 0 && state.isFaceSturdy(level, pos, Direction.UP) && level.getBlockEntity(pos) == null) continue;
			if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || level.getBlockEntity(pos) != null)
				return "Camp blocked at " + pos.toShortString() + ". Choose flat, open ground: 21 x 21 with four blocks of headroom.";
			if (y == 0) plan.put(pos, Blocks.STONE_BRICKS.defaultBlockState());
		}
		Block[] stations = {Blocks.CRAFTING_TABLE, Cooking.STATION, Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE,
			Blocks.STONECUTTER, Blocks.SMITHING_TABLE, Blocks.ANVIL, Blocks.GRINDSTONE, Blocks.BREWING_STAND};
		for (int i = 0; i < stations.length; i++) plan.put(center.offset(-5 + i, 1, -6), stations[i].defaultBlockState());
		plan.put(center.offset(5, 1, -3), Blocks.ENCHANTING_TABLE.defaultBlockState());
		for (int x = 3; x <= 7; x++) for (int z = -5; z <= -1; z++)
			if (x == 3 || x == 7 || z == -5 || z == -1) if (!(x == 5 && z == -1)) plan.put(center.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState());
		for (int x = -5; x <= -3; x++) for (int z = -3; z <= -1; z++) plan.put(center.offset(x, 1, z),
			(x == -4 && z == -2 ? BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_block")) : Blocks.STONE_BRICKS).defaultBlockState());
		plan.put(center.offset(-4, 2, -2), Bonfires.CORE.defaultBlockState());
		for (int x = 2; x <= 6; x++) for (int z = 2; z <= 6; z++) {
			plan.put(center.offset(x, 1, z), (x == 4 && z == 4 ? Blocks.WATER.defaultBlockState() : Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7)));
			if (!(x == 4 && z == 4)) plan.put(center.offset(x, 2, z), Agriculture.CROPS.get(((x - 2) * 5 + z - 2) % Agriculture.CROPS.size()).block().getStateForAge(0));
		}
		plan.put(center.offset(4, 2, 4), Blocks.SEA_LANTERN.defaultBlockState());
		for (int x : new int[] {-7, 7}) for (int z : new int[] {-7, 7}) plan.put(center.offset(x, 1, z), Blocks.SEA_LANTERN.defaultBlockState());
		for (int i = 0; i < pages.size(); i++) {
			var pos = barrels.get(i);
			plan.put(pos, Blocks.BARREL.defaultBlockState());
			int rotation = pos.getZ() == center.getZ() - 9 ? 0 : pos.getZ() == center.getZ() + 9 ? 8 : pos.getX() < center.getX() ? 12 : 4;
			plan.put(pos.above(), Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
		}
		for (var entry : plan.entrySet()) if (!entry.getValue().getCollisionShape(level, entry.getKey()).isEmpty()
			&& !level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(entry.getKey()), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty())
			return "A player or entity is in the camp's building area. Move them clear and try again.";
		plan.forEach(level::setBlockAndUpdate);
		for (int i = 0; i < pages.size(); i++) {
			var barrel = (BarrelBlockEntity) level.getBlockEntity(barrels.get(i));
			var label = new ItemStack(Items.BARREL);
			label.set(DataComponents.CUSTOM_NAME, Component.literal(pages.get(i).name()));
			barrel.applyComponentsFromItemStack(label);
			for (int slot = 0; slot < pages.get(i).items().size(); slot++) barrel.setItem(slot, pages.get(i).items().get(slot).copy());
			barrel.setChanged();
			var sign = (SignBlockEntity) level.getBlockEntity(barrels.get(i).above());
			var lines = new ArrayList<Component>();
			String line = "";
			for (String word : pages.get(i).name().split(" ")) {
				if (!line.isEmpty() && line.length() + word.length() + 1 > 15) { lines.add(Component.literal(line)); line = ""; }
				line += (line.isEmpty() ? "" : " ") + word;
			}
			lines.add(Component.literal(line));
			while (lines.size() < 4) lines.add(Component.empty());
			var text = new SignText(lines.subList(0, 4), lines.subList(0, 4), DyeColor.BLACK, false);
			for (var side : SignTextSlot.values()) sign.setText(text, side);
			sign.setWaxed(true);
		}
		return null;
	}
}
