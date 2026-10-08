package darkspawn.black.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventorySorterTest {
	@BeforeAll
	static void initializeMinecraftRegistries() {
		darkspawn.black.TestBootstrap.initialize();
	}

	@Test
	void sortsByRegistryIdAndMovesEmptySlotsToEnd() {
		SimpleContainer inventory = new SimpleContainer(41);
		inventory.setItem(9, new ItemStack(Items.STONE, 10));
		inventory.setItem(12, new ItemStack(Items.APPLE, 2));
		inventory.setItem(35, new ItemStack(Items.DIRT, 3));

		assertTrue(InventorySorter.sortMainInventory(inventory));

		assertStack(inventory, 9, Items.APPLE, 2);
		assertStack(inventory, 10, Items.DIRT, 3);
		assertStack(inventory, 11, Items.STONE, 10);
		for (int slot = 12; slot < 36; slot++) {
			assertTrue(inventory.getItem(slot).isEmpty());
		}
	}

	@Test
	void combinesMatchingStacksWithoutExceedingTheirLimits() {
		SimpleContainer inventory = new SimpleContainer(41);
		inventory.setItem(9, new ItemStack(Items.STONE, 40));
		inventory.setItem(10, new ItemStack(Items.STONE, 40));
		inventory.setItem(11, new ItemStack(Items.ENDER_PEARL, 12));
		inventory.setItem(12, new ItemStack(Items.ENDER_PEARL, 12));

		InventorySorter.sortMainInventory(inventory);

		assertStack(inventory, 9, Items.ENDER_PEARL, 16);
		assertStack(inventory, 10, Items.ENDER_PEARL, 8);
		assertStack(inventory, 11, Items.STONE, 64);
		assertStack(inventory, 12, Items.STONE, 16);
	}

	@Test
	void preservesHotbarAndEveryEquipmentSlot() {
		SimpleContainer inventory = new SimpleContainer(43);
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			inventory.setItem(slot, new ItemStack(Items.STONE, slot + 1));
		}
		List<ItemStack> originalReferences = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			originalReferences.add(inventory.getItem(slot));
		}

		InventorySorter.sortMainInventory(inventory);

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (slot < 9 || slot >= 36) {
				assertSame(originalReferences.get(slot), inventory.getItem(slot));
				assertEquals(slot + 1, inventory.getItem(slot).getCount());
			}
		}
	}

	@Test
	void preservesNamesDamageAndCustomDataWithoutMergingDifferentComponents() {
		SimpleContainer inventory = new SimpleContainer(41);
		ItemStack named = new ItemStack(Items.STONE, 20);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		ItemStack tagged = new ItemStack(Items.STONE, 15);
		CompoundTag data = new CompoundTag();
		data.putString("owner", "darkspawn-test");
		tagged.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
		damaged.setDamageValue(25);
		inventory.setItem(9, named);
		inventory.setItem(10, new ItemStack(Items.STONE, 30));
		inventory.setItem(11, tagged);
		inventory.setItem(12, damaged);
		inventory.setItem(13, new ItemStack(Items.IRON_PICKAXE));
		inventory.setItem(14, named.copyWithCount(10));
		List<ItemStack> before = snapshot(inventory);

		InventorySorter.sortMainInventory(inventory);

		assertSameContents(before, snapshot(inventory));
		assertEquals(5, snapshot(inventory).stream().filter(stack -> !stack.isEmpty()).count());
		assertEquals(20, named.getCount(), "Original item objects must not be modified during planning.");
		assertEquals(25, damaged.getDamageValue());
		assertFalse(InventorySorter.sortMainInventory(inventory), "Sorting a second time must be a no-op.");
	}

	@Test
	void emptyAndAlreadySortedInventoriesAreUnchanged() {
		SimpleContainer inventory = new SimpleContainer(41);
		assertFalse(InventorySorter.sortMainInventory(inventory));
		ItemStack apple = new ItemStack(Items.APPLE, 64);
		inventory.setItem(9, apple);
		inventory.setItem(10, new ItemStack(Items.STONE, 64));

		assertFalse(InventorySorter.sortMainInventory(inventory));
		assertSame(apple, inventory.getItem(9));
	}

	@Test
	void fullInventoryKeepsEveryItem() {
		SimpleContainer inventory = new SimpleContainer(41);
		for (int slot = 9; slot < 36; slot++) {
			ItemStack stack = new ItemStack(slot % 2 == 0 ? Items.STONE : Items.APPLE, 64);
			stack.set(DataComponents.CUSTOM_NAME, Component.literal("Slot " + slot));
			inventory.setItem(slot, stack);
		}
		List<ItemStack> before = snapshot(inventory);

		InventorySorter.sortMainInventory(inventory);

		assertSameContents(before, snapshot(inventory));
		assertEquals(27, snapshot(inventory).stream().filter(stack -> !stack.isEmpty()).count());
	}

	@Test
	void componentDefinedStackLimitsAreRespected() {
		SimpleContainer inventory = new SimpleContainer(41);
		ItemStack limited = new ItemStack(Items.STONE, 6);
		limited.set(DataComponents.MAX_STACK_SIZE, 8);
		inventory.setItem(9, limited);
		inventory.setItem(10, limited.copy());

		InventorySorter.sortMainInventory(inventory);

		assertStack(inventory, 9, Items.STONE, 8);
		assertStack(inventory, 10, Items.STONE, 4);
		assertEquals(8, inventory.getItem(9).getMaxStackSize());
	}

	@Test
	void randomizedInventoriesPreserveCountsComponentsAndOrdering() {
		Random random = new Random(263);
		Item[] items = { Items.APPLE, Items.STONE, Items.DIRT, Items.ENDER_PEARL, Items.IRON_PICKAXE };
		for (int iteration = 0; iteration < 200; iteration++) {
			SimpleContainer inventory = new SimpleContainer(41);
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (random.nextInt(4) == 0) {
					continue;
				}
				ItemStack stack = new ItemStack(items[random.nextInt(items.length)]);
				stack.setCount(1 + random.nextInt(stack.getMaxStackSize()));
				if (random.nextBoolean()) {
					stack.set(DataComponents.CUSTOM_NAME, Component.literal("Group " + random.nextInt(3)));
				}
				inventory.setItem(slot, stack);
			}
			List<ItemStack> before = snapshot(inventory);

			InventorySorter.sortMainInventory(inventory);

			assertSameContents(before, snapshot(inventory));
			String previousId = "";
			boolean foundEmpty = false;
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (slot < 9 || slot >= 36) {
					assertTrue(ItemStack.matches(before.get(slot), stack));
				} else if (stack.isEmpty()) {
					foundEmpty = true;
				} else {
					assertFalse(foundEmpty, "Empty slots must follow all items.");
					String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
					assertTrue(previousId.compareTo(id) <= 0);
					assertTrue(stack.getCount() <= stack.getMaxStackSize());
					previousId = id;
				}
			}
			assertFalse(InventorySorter.sortMainInventory(inventory));
		}
	}

	@Test
	void rejectsContainersThatAreSmallerThanPlayerInventory() {
		SimpleContainer inventory = new SimpleContainer(27);
		inventory.setItem(9, new ItemStack(Items.DIAMOND, 3));
		assertThrows(IllegalArgumentException.class, () -> InventorySorter.sortMainInventory(inventory));
		assertStack(inventory, 9, Items.DIAMOND, 3);
	}

	@Test
	void expansionUnlocksExactlyNineSlotsPerTier() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		for (int slot = 0; slot < 36; slot++) {
			inventory.setItem(slot, new ItemStack(Items.STONE, 64));
		}
		assertEquals(-1, inventory.getFreeSlot(), "Locked rows and equipment positions are not storage.");
		for (int tier = 1; tier <= 3; tier++) {
			InventoryExpansion.setTier(inventory, tier);
			assertEquals(43 + tier * 9, inventory.getContainerSize());
			for (int slot = 43 + (tier - 1) * 9; slot < 43 + tier * 9; slot++) {
				assertEquals(slot, inventory.getFreeSlot());
				inventory.setItem(slot, new ItemStack(Items.STONE, 64));
			}
			assertEquals(-1, inventory.getFreeSlot());
		}
	}

	@Test
	void expansionKeepsEquipmentOutOfStorageBackingList() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(inventory, 3);
		for (int slot = 36; slot < 43; slot++) {
			ItemStack equipment = new ItemStack(Items.STONE, 3);
			inventory.setItem(slot, equipment);
			assertSame(equipment, inventory.getItem(slot));
			assertTrue(inventory.getNonEquipmentItems().get(slot).isEmpty());
			assertEquals(1, inventory.removeItem(slot, 1).getCount());
			assertEquals(2, inventory.removeItemNoUpdate(slot).getCount());
			assertTrue(inventory.getItem(slot).isEmpty());
		}
	}

	@Test
	void pickupsUseExtraSlotsAfterTheOriginalInventoryFills() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(inventory, 1);
		for (int slot = 0; slot < 36; slot++) {
			inventory.setItem(slot, new ItemStack(Items.STONE, 64));
		}
		ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
		tool.setDamageValue(12);
		assertTrue(inventory.add(tool));
		assertTrue(tool.isEmpty());
		assertEquals(12, inventory.getItem(43).getDamageValue());
		ItemStack pickedUp = new ItemStack(Items.STONE, 20);
		assertTrue(inventory.add(pickedUp));
		assertTrue(pickedUp.isEmpty());
		assertEquals(20, inventory.getItem(44).getCount());
		assertEquals(44, inventory.getSlotWithRemainingSpace(new ItemStack(Items.STONE)));
		assertEquals(45, inventory.getFreeSlot());
	}

	@Test
	void sortingIncludesUnlockedRowsAndProtectsEquipment() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(inventory, 3);
		ItemStack offhand = new ItemStack(Items.TORCH, 16);
		inventory.setItem(40, offhand);
		inventory.setItem(9, new ItemStack(Items.STONE, 40));
		inventory.setItem(43, new ItemStack(Items.STONE, 40));
		inventory.setItem(69, new ItemStack(Items.APPLE, 3));
		assertTrue(InventorySorter.sortMainInventory(inventory));
		assertTrue(inventory.getItem(9).is(Items.APPLE));
		assertEquals(3, inventory.getItem(9).getCount());
		assertEquals(64, inventory.getItem(10).getCount());
		assertEquals(16, inventory.getItem(11).getCount());
		assertTrue(inventory.getItem(43).isEmpty());
		assertTrue(inventory.getItem(69).isEmpty());
		assertSame(offhand, inventory.getItem(40));
		assertFalse(InventorySorter.sortMainInventory(inventory));
	}

	@Test
	void expandedItemsSurviveVanillaSaveAndLoadWithTheirComponents() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(inventory, 3);
		inventory.setItem(0, new ItemStack(Items.TORCH, 23));
		inventory.setItem(35, new ItemStack(Items.STONE, 64));
		inventory.setItem(43, new ItemStack(Items.APPLE, 12));
		ItemStack keepsake = new ItemStack(Items.DIAMOND, 7);
		keepsake.set(DataComponents.CUSTOM_NAME, Component.literal("Saved in the last slot"));
		inventory.setItem(69, keepsake);
		var registries = VanillaRegistries.createWorldLookup();
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		inventory.save(output.list("Inventory", ItemStackWithSlot.CODEC));
		var input = TagValueInput.create(ProblemReporter.DISCARDING, registries, output.buildResult());
		Inventory restored = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(restored, 3);
		restored.load(input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));
		for (int slot = 0; slot < 70; slot++) {
			assertTrue(ItemStack.matches(inventory.getItem(slot), restored.getItem(slot)), "Saved slot " + slot);
		}
	}

	@Test
	void inventoryCopyPreservesCapacityAndLastExtraSlot() {
		Inventory original = new Inventory(null, new EntityEquipment());
		InventoryExpansion.setTier(original, 3);
		original.setItem(69, new ItemStack(Items.DIAMOND, 5));
		original.setItem(39, new ItemStack(Items.IRON_HELMET));
		Inventory copy = new Inventory(null, new EntityEquipment());
		copy.replaceWith(original);
		assertEquals(3, InventoryExpansion.tier(copy));
		assertTrue(ItemStack.matches(original.getItem(69), copy.getItem(69)));
		assertTrue(ItemStack.matches(original.getItem(39), copy.getItem(39)));
		copy.clearContent();
		assertTrue(copy.isEmpty());
		assertEquals(3, InventoryExpansion.tier(copy), "Clearing items must not remove permanent capacity.");
	}

	@Test
	void inventoryMenuKeepsVanillaSlotIdsAndGatesExtraRows() {
		Inventory inventory = new Inventory(null, new EntityEquipment());
		InventoryMenu menu = new InventoryMenu(inventory, true, null);
		assertEquals(73, menu.slots.size());
		assertEquals(39, menu.getSlot(5).getContainerSlot());
		assertEquals(0, menu.getSlot(36).getContainerSlot());
		assertEquals(40, menu.getSlot(45).getContainerSlot());
		for (int tier = 0; tier <= 3; tier++) {
			InventoryExpansion.setTier(inventory, tier);
			for (int extra = 0; extra < 27; extra++) {
				var slot = menu.getSlot(46 + extra);
				assertEquals(43 + extra, slot.getContainerSlot());
				assertEquals(extra < tier * 9, slot.isActive());
				assertEquals(extra < tier * 9, slot.mayPlace(new ItemStack(Items.STONE)));
				assertEquals(extra < tier * 9, slot.mayPickup(null));
			}
		}
	}

	@Test
	void craftedUpgradesDecodeAndMatchTheirRecipes() throws Exception {
		String[] tiers = { "leather", "iron", "diamond" };
		Item[][] ingredients = {
				{ Items.STRING, Items.LEATHER, Items.STRING, Items.LEATHER, Items.CHEST, Items.LEATHER, Items.STRING, Items.LEATHER, Items.STRING },
				{ Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.LEATHER, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT },
				{ Items.AIR, Items.DIAMOND, Items.AIR, Items.DIAMOND, Items.IRON_INGOT, Items.DIAMOND, Items.AIR, Items.DIAMOND, Items.AIR }
		};
		var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		for (int tier = 0; tier < tiers.length; tier++) {
			String name = tiers[tier] + "_inventory_upgrade";
			try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/" + name + ".json")) {
				assertNotNull(stream);
				var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
				var recipe = assertInstanceOf(ShapedRecipe.class, Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow());
				var input = CraftingInput.of(3, 3, Arrays.stream(ingredients[tier]).map(ItemStack::new).toList());
				assertTrue(recipe.matches(input, null));
				assertFalse(recipe.matches(CraftingInput.EMPTY, null));
				var result = recipe.assemble(input);
				assertEquals(Darkspawn.id(name), BuiltInRegistries.ITEM.getKey(result.getItem()));
				assertEquals(1, result.getCount());
				assertEquals(1, result.getMaxStackSize());
			}
		}
	}

	@Test
	void clientInventoryMixinsApplyWithoutLaunchingTheGame() throws Exception {
		// Mixin Compatibility: Loading the screen classes validates injection targets without opening a window.
		ClassLoader loader = getClass().getClassLoader();
		Class<?> inventoryScreen = Class.forName("net.minecraft.client.gui.screens.inventory.InventoryScreen", false, loader);
		Class<?> creativeScreen = Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen", false, loader);
		Class<?> slotAccessor = Class.forName("darkspawn.black.client.mixin.SlotPositionAccessor", false, loader);
		assertTrue(slotAccessor.isAssignableFrom(Slot.class));
		assertTrue(Arrays.stream(inventoryScreen.getDeclaredMethods()).anyMatch(method -> method.getName().contains("darkspawn$drawExtraRows")));
		assertTrue(Arrays.stream(creativeScreen.getDeclaredMethods()).anyMatch(method -> method.getName().contains("darkspawn$keepCatalogSlotsUnchanged")));
	}

	private static List<ItemStack> snapshot(SimpleContainer inventory) {
		List<ItemStack> result = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			result.add(inventory.getItem(slot).copy());
		}
		return result;
	}

	private static void assertSameContents(List<ItemStack> before, List<ItemStack> after) {
		// Conservation: Match exact item/components and subtract counts, independent of slot order.
		List<ItemStack> remaining = after.stream().map(ItemStack::copy).toList();
		for (ItemStack original : before) {
			int count = original.getCount();
			for (ItemStack candidate : remaining) {
				if (ItemStack.isSameItemSameComponents(original, candidate)) {
					int matched = Math.min(count, candidate.getCount());
					count -= matched;
					candidate.shrink(matched);
				}
			}
			assertEquals(0, count, "An item or its component data was lost.");
		}
		assertTrue(remaining.stream().allMatch(ItemStack::isEmpty), "Items were duplicated.");
	}

	private static void assertStack(SimpleContainer inventory, int slot, Item item, int count) {
		assertSame(item, inventory.getItem(slot).getItem());
		assertEquals(count, inventory.getItem(slot).getCount());
	}
}
