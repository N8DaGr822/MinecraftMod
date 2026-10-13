package darkspawn.black.starter;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DragonStarterChestGameTest {
	private static UseOnContext context(GameTestHelper helper, BlockPos pos, ItemStack stack) {
		return new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, stack,
			new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false));
	}

	@GameTest
	public void placementSuppliesFullKitWithoutChangingNeighborOrRefillingOrdinaryChests(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new BlockPos(1, 2, 1));
		level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
		var neighbor = (ChestBlockEntity) level.getBlockEntity(pos.east());
		neighbor.setItem(0, new ItemStack(Items.DIAMOND, 7));
		var kit = new ItemStack(DragonStarterChest.ITEM);
		helper.assertTrue(kit.getItem().useOn(context(helper, pos, kit)).consumesAction(), "Kit places successfully");
		helper.assertTrue(kit.isEmpty(), "Placement consumes the kit");
		var chest = (ChestBlockEntity) level.getBlockEntity(pos);
		for (int i = 0; i < 27; i++) {
			var stack = chest.getItem(i);
			helper.assertTrue(!stack.isEmpty() && stack.getCount() <= stack.getMaxStackSize(), "Every slot has a valid stack");
		}
		helper.assertTrue(chest.getItem(3).getEnchantments().getLevel(level.registryAccess().getOrThrow(Enchantments.FEATHER_FALLING)) == 4, "Boots protect against falls");
		helper.assertTrue(chest.getItem(5).getEnchantments().getLevel(level.registryAccess().getOrThrow(Enchantments.INFINITY)) == 1, "Bow has Infinity");
		helper.assertTrue(chest.getItem(19).get(DataComponents.POTION_CONTENTS).is(Potions.LONG_SLOW_FALLING), "Slow falling has the correct potion component");
		helper.assertTrue(neighbor.getItem(0).is(Items.DIAMOND) && neighbor.getItem(0).getCount() == 7, "Neighbor contents survive joining");
		helper.assertTrue(Blocks.CHEST.asItem() == Items.CHEST, "Vanilla chest item mapping is unchanged");
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		var ordinary = new ItemStack(Items.CHEST);
		Items.CHEST.useOn(context(helper, pos, ordinary));
		helper.assertTrue(((ChestBlockEntity) level.getBlockEntity(pos)).isEmpty(), "Replaced ordinary chest does not generate another kit");
		helper.succeed();
	}

	@GameTest
	public void blockedPlacementDoesNotConsumeKitOrReplaceTerrain(GameTestHelper helper) {
		var pos = helper.absolutePos(new BlockPos(1, 2, 1));
		helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
		var kit = new ItemStack(DragonStarterChest.ITEM);
		helper.assertTrue(!kit.getItem().useOn(context(helper, pos, kit)).consumesAction(), "Occupied placement fails");
		helper.assertTrue(kit.getCount() == 1 && helper.getLevel().getBlockState(pos).is(Blocks.DIAMOND_BLOCK), "Failed placement preserves kit and terrain");
		helper.succeed();
	}
}
