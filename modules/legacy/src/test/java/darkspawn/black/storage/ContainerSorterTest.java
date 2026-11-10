package darkspawn.black.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerSorterTest {
	@BeforeAll
	static void initializeMinecraftRegistries() {
		darkspawn.black.TestBootstrap.initialize();
	}

	@Test
	void sortsByRegistryIdAndMovesEmptySlotsToEnd() {
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.STONE, 10));
		chest.setItem(3, new ItemStack(Items.APPLE, 2));
		chest.setItem(26, new ItemStack(Items.DIRT, 3));

		assertTrue(ContainerSorter.sortContainer(chest));

		assertStack(chest, 0, Items.APPLE, 2);
		assertStack(chest, 1, Items.DIRT, 3);
		assertStack(chest, 2, Items.STONE, 10);
		for (int slot = 3; slot < 27; slot++) {
			assertTrue(chest.getItem(slot).isEmpty());
		}
	}

	@Test
	void combinesMatchingStacksWithoutExceedingTheirLimits() {
		SimpleContainer chest = new SimpleContainer(27);
		chest.setItem(0, new ItemStack(Items.STONE, 40));
		chest.setItem(1, new ItemStack(Items.STONE, 40));
		chest.setItem(2, new ItemStack(Items.ENDER_PEARL, 12));
		chest.setItem(3, new ItemStack(Items.ENDER_PEARL, 12));

		ContainerSorter.sortContainer(chest);

		assertStack(chest, 0, Items.ENDER_PEARL, 16);
		assertStack(chest, 1, Items.ENDER_PEARL, 8);
		assertStack(chest, 2, Items.STONE, 64);
		assertStack(chest, 3, Items.STONE, 16);
	}

	@Test
	void slotSubsetLeavesOtherSlotsUntouched() {
		SimpleContainer container = new SimpleContainer(12);
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			container.setItem(slot, new ItemStack(slot % 2 == 0 ? Items.STONE : Items.APPLE, slot + 1));
		}
		List<ItemStack> originalReferences = new ArrayList<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			originalReferences.add(container.getItem(slot));
		}

		ContainerSorter.sortSlots(container, new int[] { 4, 5, 6, 7 });

		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (slot < 4 || slot > 7) {
				assertSame(originalReferences.get(slot), container.getItem(slot));
				assertEquals(slot + 1, container.getItem(slot).getCount());
			}
		}
	}

	@Test
	void preservesNamesDamageAndCustomDataWithoutMergingDifferentComponents() {
		SimpleContainer chest = new SimpleContainer(27);
		ItemStack named = new ItemStack(Items.STONE, 20);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		ItemStack tagged = new ItemStack(Items.STONE, 15);
		CompoundTag data = new CompoundTag();
		data.putString("owner", "darkspawn-test");
		tagged.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
		damaged.setDamageValue(25);
		chest.setItem(0, named);
		chest.setItem(1, new ItemStack(Items.STONE, 30));
		chest.setItem(2, tagged);
		chest.setItem(3, damaged);
		chest.setItem(4, new ItemStack(Items.IRON_PICKAXE));
		chest.setItem(5, named.copyWithCount(10));
		List<ItemStack> before = snapshot(chest);

		ContainerSorter.sortContainer(chest);

		assertSameContents(before, snapshot(chest));
		assertEquals(5, snapshot(chest).stream().filter(stack -> !stack.isEmpty()).count());
		assertEquals(20, named.getCount(), "Original item objects must not be modified during planning.");
		assertEquals(25, damaged.getDamageValue());
		assertFalse(ContainerSorter.sortContainer(chest), "Sorting a second time must be a no-op.");
	}

	@Test
	void emptyAndAlreadySortedContainersAreUnchanged() {
		SimpleContainer chest = new SimpleContainer(27);
		assertFalse(ContainerSorter.sortContainer(chest));
		ItemStack apple = new ItemStack(Items.APPLE, 64);
		chest.setItem(0, apple);
		chest.setItem(1, new ItemStack(Items.STONE, 64));

		assertFalse(ContainerSorter.sortContainer(chest));
		assertSame(apple, chest.getItem(0));
	}

	@Test
	void fullContainerKeepsEveryItem() {
		SimpleContainer chest = new SimpleContainer(27);
		for (int slot = 0; slot < 27; slot++) {
			ItemStack stack = new ItemStack(slot % 2 == 0 ? Items.STONE : Items.APPLE, 64);
			stack.set(DataComponents.CUSTOM_NAME, Component.literal("Slot " + slot));
			chest.setItem(slot, stack);
		}
		List<ItemStack> before = snapshot(chest);

		ContainerSorter.sortContainer(chest);

		assertSameContents(before, snapshot(chest));
		assertEquals(27, snapshot(chest).stream().filter(stack -> !stack.isEmpty()).count());
	}

	@Test
	void componentDefinedStackLimitsAreRespected() {
		SimpleContainer chest = new SimpleContainer(27);
		ItemStack limited = new ItemStack(Items.STONE, 6);
		limited.set(DataComponents.MAX_STACK_SIZE, 8);
		chest.setItem(0, limited);
		chest.setItem(1, limited.copy());

		ContainerSorter.sortContainer(chest);

		assertStack(chest, 0, Items.STONE, 8);
		assertStack(chest, 1, Items.STONE, 4);
		assertEquals(8, chest.getItem(0).getMaxStackSize());
	}

	@Test
	void randomizedContainersPreserveCountsComponentsAndOrdering() {
		Random random = new Random(263);
		Item[] items = { Items.APPLE, Items.STONE, Items.DIRT, Items.ENDER_PEARL, Items.IRON_PICKAXE };
		for (int iteration = 0; iteration < 200; iteration++) {
			SimpleContainer chest = new SimpleContainer(27);
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				if (random.nextInt(4) == 0) {
					continue;
				}
				ItemStack stack = new ItemStack(items[random.nextInt(items.length)]);
				stack.setCount(1 + random.nextInt(stack.getMaxStackSize()));
				if (random.nextBoolean()) {
					stack.set(DataComponents.CUSTOM_NAME, Component.literal("Group " + random.nextInt(3)));
				}
				chest.setItem(slot, stack);
			}
			List<ItemStack> before = snapshot(chest);

			ContainerSorter.sortContainer(chest);

			assertSameContents(before, snapshot(chest));
			String previousId = "";
			boolean foundEmpty = false;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				ItemStack stack = chest.getItem(slot);
				if (stack.isEmpty()) {
					foundEmpty = true;
				} else {
					assertFalse(foundEmpty, "Empty slots must follow all items.");
					String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
					assertTrue(previousId.compareTo(id) <= 0);
					assertTrue(stack.getCount() <= stack.getMaxStackSize());
					previousId = id;
				}
			}
			assertFalse(ContainerSorter.sortContainer(chest));
		}
	}

	private static List<ItemStack> snapshot(SimpleContainer container) {
		List<ItemStack> result = new ArrayList<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			result.add(container.getItem(slot).copy());
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

	private static void assertStack(SimpleContainer container, int slot, Item item, int count) {
		assertSame(item, container.getItem(slot).getItem());
		assertEquals(count, container.getItem(slot).getCount());
	}
}
