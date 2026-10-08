package darkspawn.black.cooking;

import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AgricultureGameTest {
	private static BlockPos plot(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
		helper.getLevel().setBlockAndUpdate(pos.above(2), Blocks.GLOWSTONE.defaultBlockState());
		return pos;
	}

	private static int count(List<ItemStack> drops, Item item) {
		return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static void plant(GameTestHelper helper, BlockPos pos, ItemStack seeds) {
		seeds.getItem().useOn(new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, seeds,
				new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
	}

	@GameTest
	public void everySeedPlantsGrowsHarvestsAndReplants(GameTestHelper helper) {
		BlockPos pos = plot(helper);
		helper.runAfterDelay(5, () -> {
			var level = helper.getLevel();
			for (var crop : Agriculture.CROPS) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				ItemStack seeds = new ItemStack(crop.seeds(), 2);
				plant(helper, pos, seeds);
				helper.assertTrue(level.getBlockState(pos).is(crop.block()) && seeds.getCount() == 1, "Planting must consume one correct seed");
				helper.assertTrue(level.getBlockState(pos).getCloneItemStack(level, pos, false).is(crop.seeds()), "Pick-block must return this crop's seeds");
				ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 8);
				for (int i = 0; i < 4; i++) { BoneMealItem.growCrop(boneMeal, level, pos); }
				helper.assertTrue(crop.block().isMaxAge(level.getBlockState(pos)), "Bone meal must mature every crop");
				int remaining = boneMeal.getCount();
				helper.assertTrue(!BoneMealItem.growCrop(boneMeal, level, pos) && boneMeal.getCount() == remaining, "Mature crops must not consume bone meal");
				var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
				helper.assertTrue(count(drops, crop.ingredient()) >= 1 && count(drops, crop.seeds()) >= 1, "A mature harvest must supply food and replanting seeds");
				level.destroyBlock(pos, false);
				ItemStack harvestedSeeds = drops.stream().filter(stack -> stack.is(crop.seeds())).findFirst().orElseThrow().copy();
				plant(helper, pos, harvestedSeeds);
				helper.assertTrue(level.getBlockState(pos).equals(crop.block().getStateForAge(0)), "Harvested seeds must restart the renewable loop at age zero");
			}
			helper.succeed();
		});
	}

	@GameTest
	public void cropLootOnlyGrantsProduceAtMaturity(GameTestHelper helper) {
		BlockPos pos = plot(helper);
		for (var crop : Agriculture.CROPS) {
			for (int age = 0; age <= 7; age++) {
				BlockState state = crop.block().getStateForAge(age);
				var drops = Block.getDrops(state, helper.getLevel(), pos, null);
				int seeds = count(drops, crop.seeds());
				int produce = count(drops, crop.ingredient());
				helper.assertTrue(age < 7 ? seeds == 1 && produce == 0 : seeds >= 2 && seeds <= 5 && produce >= 1 && produce <= 2,
						"Young crops return only the planted seed; ripe crops grant bounded produce and extra seeds");
				helper.assertTrue(drops.stream().allMatch(stack -> stack.is(crop.seeds()) || stack.is(crop.ingredient())), "Crop loot must not fall back to vanilla wheat");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void naturalGrowthAndSavedAgeUseNativeCropRules(GameTestHelper helper) {
		BlockPos pos = plot(helper);
		helper.runAfterDelay(5, () -> {
			var level = helper.getLevel();
			for (var crop : Agriculture.CROPS) {
				level.setBlockAndUpdate(pos, crop.block().defaultBlockState());
				RandomSource random = RandomSource.create(4729);
				for (int i = 0; i < 256 && !crop.block().isMaxAge(level.getBlockState(pos)); i++) {
					level.getBlockState(pos).randomTick(level, pos, random);
				}
				helper.assertTrue(crop.block().isMaxAge(level.getBlockState(pos)), "Lit crops must mature through server random ticks");
				var encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, crop.block().getStateForAge(4)).getOrThrow();
				BlockState restored = BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
				level.setBlockAndUpdate(pos, restored);
				helper.assertTrue(restored.is(crop.block()) && crop.block().getAge(restored) == 4, "Saved state must retain crop identity and growth age");
				helper.assertTrue(restored.is(BlockTags.CROPS) && restored.getCollisionShape(level, pos).isEmpty(), "Crops must use native crop classification without blocking movement");
				ItemStack seeds = new ItemStack(crop.seeds());
				helper.assertTrue(seeds.is(ItemTags.CHICKEN_FOOD) && seeds.is(ItemTags.VILLAGER_PLANTABLE_SEEDS), "Seeds must integrate with farming and chickens");
				helper.assertTrue(seeds.has(DataComponents.COMPOSTABLE) && new ItemStack(crop.ingredient()).has(DataComponents.COMPOSTABLE), "Seeds and produce must remain compostable");
			}
			helper.succeed();
		});
	}

	@GameTest
	public void invalidSoilAndDarknessRejectPlantingWithoutConsumingSeeds(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos pos = plot(helper);
		// Light Updates: Enclose the plot and wait for the server light engine before testing darkness.
		for (BlockPos wall : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (!wall.equals(pos)) { level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState()); }
		}
		level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(level.getRawBrightness(pos, 0) < 8, "Darkness fixture must be unlit");
			for (var crop : Agriculture.CROPS) {
				ItemStack seeds = new ItemStack(crop.seeds(), 2);
				plant(helper, pos, seeds);
				helper.assertTrue(level.getBlockState(pos).isAir() && seeds.getCount() == 2, "Unlit planting must fail without losing seeds");
				level.setBlock(pos, crop.block().defaultBlockState(), 2);
				for (int i = 0; i < 64; i++) { level.getBlockState(pos).randomTick(level, pos, level.getRandom()); }
				helper.assertTrue(crop.block().getAge(level.getBlockState(pos)) == 0, "Dark crops must not random-grow");
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}
			level.setBlockAndUpdate(pos.above(), Blocks.GLOWSTONE.defaultBlockState());
			level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
			helper.runAfterDelay(5, () -> {
				helper.assertTrue(level.getRawBrightness(pos, 0) >= 9, "Soil fixture must be lit");
				for (var crop : Agriculture.CROPS) {
					ItemStack seeds = new ItemStack(crop.seeds());
					plant(helper, pos, seeds);
					helper.assertTrue(level.getBlockState(pos).isAir() && seeds.getCount() == 1, "Stone must reject planting without consuming the seed");
				}
				helper.succeed();
			});
		});
	}

	@GameTest
	public void farmerSeedTradesPreserveVanillaOffersRestockAndSave(GameTestHelper helper) {
		var level = helper.getLevel();
		Villager farmer = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
		farmer.setVillagerData(farmer.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FARMER));
		var registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
		var pool = registry.getOrThrow(VillagerTradeTags.FARMER_LEVEL_1);
		helper.assertTrue(pool.contains(registry.getOrThrow(VillagerTrades.FARMER_1_WHEAT_EMERALD))
				&& pool.contains(registry.getOrThrow(VillagerTrades.FARMER_1_EMERALD_BREAD)), "Seed additions must preserve vanilla farmer trades");
		LootContext context = new LootContext.Builder(new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, farmer.position())
				.withParameter(LootContextParams.THIS_ENTITY, farmer)
				.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
				.create(LootContextParamSets.VILLAGER_TRADE)).create(Optional.empty());
		helper.assertTrue(level.registryAccess().getOrThrow(TradeSets.FARMER_LEVEL_1).value().calculateNumberOfTrades(context) == 2
				&& farmer.getOffers().size() == 2, "Native novice offer selection must remain two trades");
		farmer.getOffers().clear();
		for (var crop : Agriculture.CROPS) {
			String name = BuiltInRegistries.ITEM.getKey(crop.seeds()).getPath();
			var trade = registry.getOrThrow(ResourceKey.create(Registries.VILLAGER_TRADE, Darkspawn.id("farmer/1/emerald_" + name)));
			helper.assertTrue(pool.contains(trade), "Every seed must be obtainable from the novice trade pool");
			var offer = trade.value().getOffer(context);
			helper.assertTrue(offer != null && offer.getResult().is(crop.seeds()) && offer.getResult().getCount() == 4
					&& offer.getMaxUses() == 12 && offer.getBaseCostA().getCount() == 2, "Seed offers must provide four seeds for two emeralds with twelve uses");
			helper.assertTrue(!offer.take(new ItemStack(Items.EMERALD), ItemStack.EMPTY), "Insufficient payment must fail");
			ItemStack payment = new ItemStack(Items.EMERALD, 2);
			helper.assertTrue(offer.take(payment, ItemStack.EMPTY) && payment.isEmpty(), "A successful trade must consume its emeralds");
			offer.setToOutOfStock();
			farmer.getOffers().add(offer);
		}
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		farmer.saveWithoutId(output);
		farmer.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
		helper.assertTrue(farmer.getOffers().size() == 6 && farmer.getOffers().stream().allMatch(offer -> offer.isOutOfStock()), "Reload must preserve purchased seed offers and depleted stock");
		farmer.restock();
		helper.assertTrue(farmer.getOffers().stream().allMatch(offer -> !offer.isOutOfStock()), "Normal farmer restocking must renew seed supplies");
		farmer.discard();
		helper.succeed();
	}

	@GameTest
	public void nativeFarmerHarvestsAndReplantsNewCrops(GameTestHelper helper) {
		BlockPos pos = plot(helper);
		helper.runAfterDelay(5, () -> {
			var level = helper.getLevel();
			Villager farmer = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
			farmer.setVillagerData(farmer.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FARMER));
			farmer.getBrain().setMemory(MemoryModuleType.SECONDARY_JOB_SITE, List.of(GlobalPos.of(level.dimension(), pos.below())));
			for (var crop : Agriculture.CROPS) {
				farmer.getInventory().clearContent();
				farmer.getInventory().setItem(0, new ItemStack(crop.seeds(), 2));
				helper.assertTrue(farmer.hasFarmSeeds() && farmer.wantsToPickUp(level, new ItemStack(crop.seeds())), "Farmers must recognize and collect every new seed");
				level.setBlockAndUpdate(pos, crop.block().getStateForAge(7));
				HarvestFarmland harvest = new HarvestFarmland();
				long time = level.getGameTime() + 1;
				helper.assertTrue(harvest.tryStart(level, farmer, time), "Native farmer behavior must find a ripe custom crop");
				harvest.tickOrStop(level, farmer, time + 1);
				helper.assertTrue(level.getBlockState(pos).isAir(), "Farmer must harvest the ripe crop");
				harvest.tickOrStop(level, farmer, time + 2);
				helper.assertTrue(level.getBlockState(pos).equals(crop.block().getStateForAge(0))
						&& farmer.getInventory().getItem(0).getCount() == 1, "Farmer must replant exactly one seed");
				harvest.doStop(level, farmer, time + 3);
			}
			farmer.discard();
			helper.succeed();
		});
	}
}
