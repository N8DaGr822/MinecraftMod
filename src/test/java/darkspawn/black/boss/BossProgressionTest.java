package darkspawn.black.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import darkspawn.black.TestBootstrap;
import darkspawn.black.health.BossHeartProgress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import darkspawn.black.Darkspawn;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import io.netty.buffer.Unpooled;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossProgressionTest {
	@BeforeAll
	static void initialize() {
		TestBootstrap.initialize();
	}

	@Test
	void tenUniqueBossesReachTwentyHeartsThenFiveAddAbsorption() {
		var progress = new BossHeartProgress();
		for (int i = 0; i < 15; i++) {
			assertTrue(progress.consume(BossKind.values()[i]));
			assertEquals(Math.min(i + 1, 10) * 2, progress.bonusHealth());
			assertEquals(Math.max(0, i + 1 - 10) * 2, progress.goldenCapacity());
		}
		assertFalse(progress.consume(BossKind.values()[15]));
		assertEquals(20, progress.bonusHealth());
		assertEquals(10, progress.goldenCapacity());
	}

	@Test
	void repeatBossesAndVariantsCannotIncreaseCapacityAgain() {
		var progress = new BossHeartProgress();
		assertTrue(progress.consume(BossKind.ANCIENT_TREE_SPIRIT));
		assertFalse(progress.consume(BossKind.ANCIENT_TREE_SPIRIT));
		assertEquals(2, progress.bonusHealth());
		assertEquals(1, progress.savedBosses().size());
	}

	@Test
	void goldenHeartsWaitSixtySecondsAndDamageRestartsTheDelay() {
		var progress = new BossHeartProgress();
		for (int i = 0; i < 11; i++) {
			progress.consume(BossKind.values()[i]);
		}
		for (int tick = 1; tick < BossHeartProgress.REFILL_DELAY_TICKS; tick++) {
			assertFalse(progress.tickRefill());
		}
		progress.damaged();
		for (int tick = 1; tick < BossHeartProgress.REFILL_DELAY_TICKS; tick++) {
			assertFalse(progress.tickRefill());
		}
		assertTrue(progress.tickRefill());
		progress.damaged();
		assertFalse(progress.tickRefill());
	}

	@Test
	void ordinaryHeartsNeverGrantAbsorption() {
		var progress = new BossHeartProgress();
		for (int i = 0; i < 10; i++) {
			progress.consume(BossKind.values()[i]);
		}
		for (int tick = 0; tick < BossHeartProgress.REFILL_DELAY_TICKS + 2; tick++) {
			assertFalse(progress.tickRefill());
		}
	}

	@Test
	void savedProgressRoundTripsAndDoesNotRefillImmediatelyAfterReload() {
		var progress = new BossHeartProgress();
		for (int i = 0; i < 13; i++) {
			progress.consume(BossKind.values()[i]);
		}
		var registries = VanillaRegistries.createWorldLookup();
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		output.store("darkspawn_boss_hearts", Codec.STRING.listOf(), progress.savedBosses());
		var input = TagValueInput.create(ProblemReporter.DISCARDING, registries, output.buildResult());
		var restored = new BossHeartProgress();
		restored.restore(input.read("darkspawn_boss_hearts", Codec.STRING.listOf()).orElseThrow());
		assertEquals(progress.savedBosses(), restored.savedBosses());
		assertEquals(20, restored.bonusHealth());
		assertEquals(6, restored.goldenCapacity());
		assertFalse(restored.tickRefill());
		assertFalse(restored.consume(BossKind.ANCIENT_TREE_SPIRIT));
	}

	@Test
	void corruptedOrDuplicateSaveEntriesCannotExceedCaps() {
		var progress = new BossHeartProgress();
		List<String> ids = new ArrayList<>(List.of("invalid_boss", "ancient_tree_spirit", "ancient_tree_spirit"));
		ids.addAll(Arrays.stream(BossKind.values()).map(BossKind::id).toList());
		progress.restore(ids);
		assertEquals(15, progress.savedBosses().size());
		assertEquals(20, progress.bonusHealth());
		assertEquals(10, progress.goldenCapacity());
	}

	@Test
	void phasesAdvanceAtThresholdsWithoutReversingWhenHealed() {
		assertEquals(1, BossPhase.advance(1, 701, 1000));
		assertEquals(2, BossPhase.advance(1, 700, 1000));
		assertEquals(2, BossPhase.advance(2, 900, 1000));
		assertEquals(3, BossPhase.advance(2, 350, 1000));
		assertEquals(3, BossPhase.advance(1, 200, 1000));
		assertEquals(3, BossPhase.advance(3, 1000, 1000));
	}

	@Test
	void smithingPreservesItemComponentsAndReplacesItsSinglePower() {
		ItemStack original = new ItemStack(Items.NETHERITE_SWORD);
		original.setDamageValue(87);
		original.enchant(VanillaRegistries.createWorldLookup().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
		original.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
		original.set(BossEmpowerment.POWER, "previous_boss_power");
		ItemStack result = BossEmpowerment.apply(original);
		assertEquals(87, result.getDamageValue());
		assertEquals(original.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
		assertSame(original.getItem(), result.getItem());
		assertEquals("previous_boss_power", original.get(BossEmpowerment.POWER));
		assertEquals(BossEmpowerment.ROOTBOUND, result.get(BossEmpowerment.POWER));
		ItemStack expected = original.copy();
		expected.set(BossEmpowerment.POWER, BossEmpowerment.ROOTBOUND);
		assertTrue(ItemStack.matches(expected, result));
	}

	@Test
	void smithingRequiresAllInputsAndRejectsAnAlreadyRootboundWeapon() {
		var recipe = new BossEmpowermentRecipe(Ingredient.of(Items.AMETHYST_SHARD),
				Ingredient.of(Items.NETHERITE_SWORD), Ingredient.of(BossItems.LIVING_HEARTWOOD));
		ItemStack weapon = new ItemStack(Items.NETHERITE_SWORD);
		var input = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), weapon, new ItemStack(BossItems.LIVING_HEARTWOOD));
		assertTrue(recipe.matches(input, null));
		ItemStack result = recipe.assemble(input);
		assertFalse(recipe.matches(new SmithingRecipeInput(input.template(), result, input.addition()), null));
		assertFalse(recipe.matches(new SmithingRecipeInput(ItemStack.EMPTY, weapon, input.addition()), null));
		assertFalse(recipe.matches(new SmithingRecipeInput(input.template(), weapon, ItemStack.EMPTY), null));
	}

	@Test
	void registeredBossTypesAndClientModelsLoadWithoutAGameWindow() throws Exception {
		assertEquals(16, BossEntities.TREE_SPIRIT.getDimensions().width());
		assertEquals(28, BossEntities.TREE_SPIRIT.getDimensions().height());
		assertEquals(16, BossItems.HEARTS.size());
		assertEquals(10, BossEntities.MUTANT_WOLF.getDimensions().width());
		assertEquals(9, BossEntities.MUTANT_WOLF.getDimensions().height());
		for (String name : List.of("TreeSpiritRenderer", "TreeSpiritModel", "TreeSpiritHeartLayer", "MutantWolfRenderer", "MutantWolfModel", "BossClient")) {
			Class.forName("darkspawn.black.client.boss." + name, false, getClass().getClassLoader()).getDeclaredMethods();
		}
		Class<?> model = Class.forName("darkspawn.black.client.boss.TreeSpiritModel");
		Object layer = model.getMethod("createBodyLayer").invoke(null);
		assertNotNull(layer.getClass().getMethod("bakeRoot").invoke(layer));
		Class<?> wolfModel = Class.forName("darkspawn.black.client.boss.MutantWolfModel");
		Object wolfLayer = wolfModel.getMethod("createBodyLayer").invoke(null);
		assertNotNull(wolfLayer.getClass().getMethod("bakeRoot").invoke(wolfLayer));
	}

	@Test
	void packagedBossRecipesAndUnlocksDecodeUsingMinecraftCodecs() throws Exception {
		var registries = VanillaRegistries.createWorldLookup();
		var ops = registries.createSerializationContext(JsonOps.INSTANCE);
		var recipes = new MappedRegistry<Recipe<?>>(Registries.RECIPE, Lifecycle.stable());
		for (String name : List.of("ancient_heartwood", "rootbound_empowerment", "moonlit_fang", "predators_rush_empowerment")) {
			try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/" + name + ".json")) {
				assertNotNull(stream);
				var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
				recipes.register(ResourceKey.create(Registries.RECIPE, Darkspawn.id(name)),
						Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow(), RegistrationInfo.BUILT_IN);
			}
		}
		recipes.freeze();
		// Advancement Loading: Recipe predicates resolve against the loaded recipe registry in 26.3.
		var advancementOps = HolderLookup.Provider.create(Stream.concat(registries.listRegistries(), Stream.of(recipes)))
				.createSerializationContext(JsonOps.INSTANCE);
		for (String name : List.of("ancient_heartwood", "rootbound_empowerment", "moonlit_fang", "predators_rush_empowerment")) {
			try (var stream = getClass().getResourceAsStream("/data/darkspawn/advancement/recipes/misc/" + name + ".json")) {
				assertNotNull(stream);
				var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
				assertNotNull(Advancement.CODEC.parse(advancementOps, json).getOrThrow());
			}
		}
	}

	@Test
	void wolfRitualAcceptsOnlyNightAcrossMultipleDays() {
		for (long day : new long[] {0, 24000, 24000L * 100000}) {
			assertFalse(MutantWolfSummonItem.isRitualNight(day + 12999));
			assertTrue(MutantWolfSummonItem.isRitualNight(day + 13000));
			assertTrue(MutantWolfSummonItem.isRitualNight(day + 22999));
			assertFalse(MutantWolfSummonItem.isRitualNight(day + 23000));
			assertFalse(MutantWolfSummonItem.isRitualNight(day));
		}
	}

	@Test
	void wolfPowerReplacesRootboundAndCanBeReplacedBackWithoutLosingWeaponData() {
		var rootbound = new BossEmpowermentRecipe(Ingredient.of(Items.AMETHYST_SHARD),
				Ingredient.of(Items.NETHERITE_SWORD), Ingredient.of(BossItems.LIVING_HEARTWOOD));
		var rush = new BossEmpowermentRecipe(Ingredient.of(Items.AMETHYST_SHARD),
				Ingredient.of(Items.NETHERITE_SWORD), Ingredient.of(BossItems.ALPHA_FANG), BossEmpowerment.PREDATORS_RUSH);
		ItemStack weapon = new ItemStack(Items.NETHERITE_SWORD);
		weapon.setDamageValue(130);
		weapon.set(DataComponents.CUSTOM_NAME, Component.literal("The same blade"));
		weapon.enchant(VanillaRegistries.createWorldLookup().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
		ItemStack rooted = BossEmpowerment.apply(weapon);
		var input = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), rooted, new ItemStack(BossItems.ALPHA_FANG));
		assertTrue(rush.matches(input, null));
		ItemStack empowered = rush.assemble(input);
		assertEquals(BossEmpowerment.PREDATORS_RUSH, empowered.get(BossEmpowerment.POWER));
		assertEquals(BossEmpowerment.ROOTBOUND, rooted.get(BossEmpowerment.POWER));
		assertNull(weapon.get(BossEmpowerment.POWER));
		ItemStack expected = weapon.copy();
		expected.set(BossEmpowerment.POWER, BossEmpowerment.PREDATORS_RUSH);
		assertTrue(ItemStack.matches(expected, empowered));
		assertFalse(rush.matches(new SmithingRecipeInput(input.template(), empowered, input.addition()), null));
		assertFalse(rush.matches(new SmithingRecipeInput(input.template(), rooted, new ItemStack(BossItems.LIVING_HEARTWOOD)), null));
		var reverse = new SmithingRecipeInput(input.template(), empowered, new ItemStack(BossItems.LIVING_HEARTWOOD));
		assertTrue(rootbound.matches(reverse, null));
		assertTrue(ItemStack.matches(rooted, rootbound.assemble(reverse)));
	}

	@Test
	void smithingNetworkCodecPreservesBothPowerChoices() {
		var registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		for (String power : List.of(BossEmpowerment.ROOTBOUND, BossEmpowerment.PREDATORS_RUSH)) {
			var material = power.equals(BossEmpowerment.ROOTBOUND) ? BossItems.LIVING_HEARTWOOD : BossItems.ALPHA_FANG;
			var recipe = new BossEmpowermentRecipe(Ingredient.of(Items.AMETHYST_SHARD),
					Ingredient.of(Items.NETHERITE_SWORD), Ingredient.of(material), power);
			var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
			try {
				BossEmpowermentRecipe.STREAM_CODEC.encode(buffer, recipe);
				var decoded = BossEmpowermentRecipe.STREAM_CODEC.decode(buffer);
				var input = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.NETHERITE_SWORD), new ItemStack(material));
				assertTrue(decoded.matches(input, null));
				assertEquals(power, decoded.assemble(input).get(BossEmpowerment.POWER));
				assertEquals(0, buffer.readableBytes());
			} finally {
				buffer.release();
			}
		}
	}

	@Test
	void moonlitFangRecipeRequiresItsRitualMaterials() throws Exception {
		var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/moonlit_fang.json")) {
			assertNotNull(stream);
			var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			var recipe = assertInstanceOf(ShapedRecipe.class, Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow());
			var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.BONE), new ItemStack(Items.RABBIT_HIDE), new ItemStack(Items.BONE),
					new ItemStack(Items.RABBIT_HIDE), new ItemStack(Items.DRAGON_BREATH), new ItemStack(Items.RABBIT_HIDE),
					new ItemStack(Items.BONE), new ItemStack(Items.RABBIT_HIDE), new ItemStack(Items.BONE)));
			assertTrue(recipe.matches(input, null));
			assertFalse(recipe.matches(CraftingInput.EMPTY, null));
			var result = recipe.assemble(input);
			assertTrue(result.is(BossItems.MOONLIT_FANG));
			assertEquals(1, result.getCount());
		}
	}

	@Test
	void wolfPowerDataRecipeAssemblesTheDeclaredPower() throws Exception {
		var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/predators_rush_empowerment.json")) {
			assertNotNull(stream);
			var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			var recipe = assertInstanceOf(BossEmpowermentRecipe.class, Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow());
			var input = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.NETHERITE_SWORD), new ItemStack(BossItems.ALPHA_FANG));
			assertEquals(BossEmpowerment.PREDATORS_RUSH, recipe.assemble(input).get(BossEmpowerment.POWER));
		}
	}
}
