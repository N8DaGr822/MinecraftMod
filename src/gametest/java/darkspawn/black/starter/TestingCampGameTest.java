package darkspawn.black.starter;

import darkspawn.black.bonfire.BonfireBlock;
import darkspawn.black.bonfire.Bonfires;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.cooking.CookingRecipe;
import darkspawn.black.equipment.ArmorStyles;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

public final class TestingCampGameTest {
	private static BlockPos prepare(GameTestHelper helper, int height) {
		var center = helper.absolutePos(new BlockPos(0, height, 0));
		for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
			helper.getLevel().getChunkAt(center.offset(x, 0, z));
			for (int y = 0; y <= 4; y++) helper.getLevel().setBlockAndUpdate(center.offset(x, y, z),
				(y == 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
		}
		return center;
	}
	private static void clear(GameTestHelper helper, BlockPos center) {
		for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) for (int y = 0; y <= 4; y++)
			helper.getLevel().setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
	}
	@GameTest
	public void campBuildsStationsAndSuppliesWithoutActivatingWard(GameTestHelper helper) {
		var center = prepare(helper, 60);
		try {
			// Natural clearings can contain replaceable vegetation between the table and shelves.
			helper.getLevel().setBlockAndUpdate(center.offset(4, 1, -3), Blocks.SHORT_GRASS.defaultBlockState());
			var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
			var kit = new ItemStack(TestingCamp.ITEM);
			var context = new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player,
				net.minecraft.world.InteractionHand.MAIN_HAND, kit, new net.minecraft.world.phys.BlockHitResult(
					net.minecraft.world.phys.Vec3.atCenterOf(center).add(0, .5, 0), net.minecraft.core.Direction.UP, center, false));
			helper.assertTrue(kit.getItem().useOn(context).consumesAction(), "Actual item use places the camp");
			helper.assertTrue(kit.isEmpty(), "Successful survival placement consumes one kit");
			helper.assertTrue(helper.getLevel().getBlockState(center.offset(-4, 1, -6)).is(Cooking.STATION), "Cooking station installed");
			helper.assertTrue(helper.getLevel().getBlockState(center.offset(-5, 1, -6)).is(Blocks.CRAFTING_TABLE), "Crafting table installed");
			var core = helper.getLevel().getBlockState(center.offset(-4, 2, -2));
			helper.assertTrue(core.is(Bonfires.CORE) && core.getValue(BonfireBlock.PHASE) == 0, "Ward starts off for hostile tests");
			helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.STONE), "Existing ground preserved");
			var table = center.offset(5, 1, -3);
			long shelves = net.minecraft.world.level.block.EnchantingTableBlock.BOOKSHELF_OFFSETS.stream()
				.filter(offset -> net.minecraft.world.level.block.EnchantingTableBlock.isValidBookShelf(helper.getLevel(), table, offset)).count();
			helper.assertTrue(shelves == 15, "All fifteen bookshelves provide power despite pre-existing grass");
			for (int x = -3; x <= -1; x++) {
				var pos = center.offset(x, 1, -6);
				var furnace = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(pos);
				helper.assertTrue(furnace.getItem(1).is(Items.COAL) && furnace.getItem(1).getCount() == 32, "Furnaces are ready with fuel");
				helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
					== net.minecraft.core.Direction.SOUTH, "Workstations face the central aisle");
			}
			var brewing = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(center.offset(4, 1, -6));
			helper.assertTrue(brewing.getItem(4).is(Items.BLAZE_POWDER), "Brewing stand starts fueled");
			int expected = TestingCamp.supplies(helper.getLevel()).stream().mapToInt(s -> s.items().size()).sum();
			int actual = 0;
			for (var pos : TestingCamp.barrelPositions(center)) if (helper.getLevel().getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
				helper.assertTrue(barrel.hasCustomName(), "Every barrel is labeled");
				for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
					var stack = barrel.getItem(slot);
					if (!stack.isEmpty()) { actual++; helper.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "Legal supply stacks"); }
				}
			}
			helper.assertTrue(helper.getLevel().getBlockEntity(center.offset(-5,1,4)) instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity,"Camp provides real archaeology sample deposits");
			helper.assertTrue(actual == expected, "All supply stacks fit without loss");
			helper.assertTrue(TestingCamp.place(helper.getLevel(), center, null, new ItemStack(TestingCamp.ITEM)) != null, "Repeat placement refuses occupied camp");
		} finally { clear(helper, center); }
		helper.succeed();
	}
	@GameTest
	public void blockedCampLeavesWorldAndContainerContentsUntouched(GameTestHelper helper) {
		var center = prepare(helper, 70);
		try {
			var obstruction = center.offset(10, 4, 10);
			helper.getLevel().setBlockAndUpdate(obstruction, Blocks.BARREL.defaultBlockState());
			var barrel = (BarrelBlockEntity) helper.getLevel().getBlockEntity(obstruction);
			barrel.setItem(0, new ItemStack(Items.DIAMOND, 7));
			helper.assertTrue(TestingCamp.place(helper.getLevel(), center, null, new ItemStack(TestingCamp.ITEM)) != null, "Far corner obstruction refuses whole camp");
			helper.assertTrue(barrel.getItem(0).getCount() == 7, "Original contents preserved");
			helper.assertTrue(helper.getLevel().getBlockState(center.offset(-5, 1, -6)).isAir(), "No partial crafting station placed");
			helper.assertTrue(helper.getLevel().getBlockState(TestingCamp.barrelPositions(center).getFirst()).isAir(), "No partial supplies placed");
			helper.getLevel().setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(center, Blocks.OAK_SLAB.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP)
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
			helper.assertTrue(TestingCamp.place(helper.getLevel(), center, null, new ItemStack(TestingCamp.ITEM)) != null, "Waterlogged supporting blocks also refuse placement");
			helper.assertTrue(helper.getLevel().getBlockState(center.offset(-5, 1, -6)).isAir(), "Waterlogged refusal leaves stations unplaced");
		} finally { clear(helper, center); }
		helper.succeed();
	}
	@GameTest
	public void pantryCoversLoadedCookingRecipesAndAllStyles(GameTestHelper helper) {
		var supplies = TestingCamp.supplies(helper.getLevel());
		var all = supplies.stream().flatMap(s -> s.items().stream()).toList();
		var guide = all.stream().filter(stack -> stack.is(Items.WRITTEN_BOOK)).findFirst().orElseThrow()
			.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(guide.pages().stream().anyMatch(page -> page.raw().getString().contains("FACTION SKIRMISHES"))
			&& guide.pages().stream().anyMatch(page -> page.raw().getString().contains("Cinderjaw")), "Camp guide covers new islands and skirmish testing");
		for (var item : java.util.List.of(darkspawn.black.worldgen.DiscoveryCharts.ITEM, darkspawn.black.boss.AwakenedRematches.SEAL, Items.BRUSH, darkspawn.black.pirate.Pirate.GATE_ITEM, darkspawn.black.pirate.LogboundCompass.ITEM,
			darkspawn.black.pirate.Ships.SHIPYARD_ITEM, darkspawn.black.pirate.Ships.HELM_ITEM, darkspawn.black.pirate.Ships.CANNON_ITEM,
			darkspawn.black.pirate.Ships.SHOT, darkspawn.black.pirate.Ships.FIRE_SHOT, darkspawn.black.pirate.PirateDrinks.BEER,
			darkspawn.black.pirate.PirateDrinks.RUM, darkspawn.black.worldgen.LandmarkCompass.ITEM,
			darkspawn.black.forsaken.ForsakenContent.BLADE, darkspawn.black.forsaken.ForsakenContent.SCANNER,
			darkspawn.black.forsaken.ForsakenContent.BEACON_ITEM, darkspawn.black.forsaken.ForsakenContent.CORE,
			darkspawn.black.mount.MountCompanions.CAPSULE, darkspawn.black.mount.MountCompanions.VET_KIT))
			helper.assertTrue(all.stream().anyMatch(stack -> stack.is(item)), "Expansion testing item supplied: " + item);
		for (var item : darkspawn.black.boss.BossItems.HEARTS.values()) helper.assertTrue(all.stream().anyMatch(stack -> stack.is(item)), "Every current heart supplied");
		for (var item : darkspawn.black.boss.BossItems.SUMMONS.values()) helper.assertTrue(all.stream().anyMatch(stack -> stack.is(item)), "Every regional summon supplied");
		for (var item : darkspawn.black.forsaken.ForsakenContent.ARMOR.values()) helper.assertTrue(all.stream().anyMatch(stack -> stack.is(item)), "Complete Quarantine armor supplied");
		var positions = TestingCamp.barrelPositions(BlockPos.ZERO);
		helper.assertTrue(positions.size() == 72 && positions.stream().distinct().count() == 72, "Expanded perimeter positions are unique");
		helper.assertTrue(supplies.stream().mapToInt(s -> (s.items().size() + 26) / 27).sum() <= positions.size(), "All expansion supplies fit");
		var weapons = supplies.stream().filter(s -> s.name().equals("10 Combat tools")).findFirst().orElseThrow().items();
		for (var item : java.util.List.of(Items.DIAMOND_AXE, Items.DIAMOND_SPEAR, Items.TRIDENT, Items.MACE, Items.CROSSBOW))
			helper.assertTrue(weapons.stream().anyMatch(stack -> stack.is(item)), "Style-specific weapons are supplied");
		var pantry = supplies.stream().filter(s -> s.name().contains("Cooking ingredients")).findFirst().orElseThrow().items();
		helper.getLevel().getServer().getRecipeManager().getRecipes().forEach(holder -> {
			if (holder.value() instanceof CookingRecipe recipe) recipe.ingredients().forEach(ingredient ->
				helper.assertTrue(pantry.stream().anyMatch(ingredient::test), "Every recipe ingredient can be supplied"));
		});
		for (String style : ArmorStyles.STYLES) {
			var sample = supplies.stream().filter(s -> s.name().equals("Style: " + style.replace('_', ' '))).findFirst().orElseThrow();
			helper.assertTrue(sample.items().size() == 8 && sample.items().stream().allMatch(s -> ArmorStyles.style(s).equals(style)), "Both complete style sets included");
		}
		helper.succeed();
	}
}
