package darkspawn.black.cooking;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.TestBootstrap;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookingRecipeTest {
	@BeforeAll
	static void initialize() { TestBootstrap.initialize(); }

	private Recipe<?> recipe(String name) throws Exception {
		try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/" + name + ".json")) {
			assertNotNull(stream);
			var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			return Recipe.DIRECT_CODEC.parse(VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
		}
	}

	private void matches(String name, Item output, Item... items) throws Exception {
		CookingRecipe recipe = assertInstanceOf(CookingRecipe.class, recipe(name));
		var stacks = new ArrayList<ItemStack>();
		for (Item item : items) { stacks.add(new ItemStack(item)); }
		while (stacks.size() < 4) { stacks.add(ItemStack.EMPTY); }
		for (int i = 0; i < 4; i++) {
			Collections.rotate(stacks, 1);
			CraftingInput input = CraftingInput.of(4, 1, stacks);
			assertTrue(recipe.matches(input, null), name);
			assertTrue(recipe.assemble(input).is(output), name);
		}
		assertSame(Cooking.RECIPE_TYPE, recipe.getType());
		assertFalse(recipe.matches(CraftingInput.EMPTY, null));
	}

	@Test
	void allPackagedRecipesDecodeAndAcceptReorderedInputs() throws Exception {
		matches("salt", Cooking.SALT, Items.WATER_BUCKET);
		matches("cheese", Cooking.CHEESE, Items.MILK_BUCKET, Cooking.SALT);
		matches("herb_steak", Cooking.HERB_STEAK, Items.COOKED_BEEF, Items.DANDELION);
		matches("honey_pork", Cooking.HONEY_PORK, Items.COOKED_PORKCHOP, Items.HONEY_BOTTLE);
		matches("beef_wellington", Cooking.BEEF_WELLINGTON, Items.COOKED_BEEF, Items.WHEAT, Items.RED_MUSHROOM);
		matches("beef_wellington", Cooking.BEEF_WELLINGTON, Items.COOKED_BEEF, Items.WHEAT, Items.BROWN_MUSHROOM);
		matches("fish_chowder", Cooking.FISH_CHOWDER, Items.COOKED_COD, Items.POTATO, Items.MILK_BUCKET, Items.BOWL);
		matches("fish_chowder", Cooking.FISH_CHOWDER, Items.COOKED_SALMON, Items.POTATO, Items.MILK_BUCKET, Items.BOWL);
		matches("chicken_cordon_bleu", Cooking.CHICKEN_CORDON_BLEU, Items.COOKED_CHICKEN, Items.WHEAT, Cooking.CHEESE);
		matches("woodland_stew", Cooking.WOODLAND_STEW, Items.RED_MUSHROOM, Items.CARROT, Cooking.WILD_HERBS, Items.BOWL);
		matches("woodland_stew", Cooking.WOODLAND_STEW, Items.BROWN_MUSHROOM, Items.CARROT, Cooking.WILD_HERBS, Items.BOWL);
		assertInstanceOf(ShapedRecipe.class, recipe("cooking_station"));
	}

	@Test
	void incompleteExtraAndRawIngredientsDoNotCraft() throws Exception {
		CookingRecipe recipe = (CookingRecipe) recipe("herb_steak");
		for (var stacks : List.of(List.of(new ItemStack(Items.COOKED_BEEF), ItemStack.EMPTY),
				List.of(new ItemStack(Items.BEEF), new ItemStack(Items.DANDELION)),
				List.of(new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.DANDELION), new ItemStack(Cooking.SALT)))) {
			assertFalse(recipe.matches(CraftingInput.of(stacks.size(), 1, stacks), null));
		}
	}

	@Test
	void duplicateIngredientsRequireSeparateSlotsAndNetworkCodecPreservesResult() {
		var recipe = new CookingRecipe(List.of(Ingredient.of(Items.WHEAT), Ingredient.of(Items.WHEAT)),
				new ItemStackTemplate(Cooking.CHEESE));
		assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.WHEAT, 2))), null));
		var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT)));
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		try {
			Recipe.STREAM_CODEC.encode(buffer, recipe);
			CookingRecipe decoded = assertInstanceOf(CookingRecipe.class, Recipe.STREAM_CODEC.decode(buffer));
			assertTrue(decoded.matches(input, null));
			assertTrue(decoded.assemble(input).is(Cooking.CHEESE));
			assertEquals(0, buffer.readableBytes());
		} finally { buffer.release(); }
	}

	@Test
	void mealsCanReplaceBuffsAtFullHungerAndHavePackagedModels() {
		for (MealItem item : List.of(Cooking.HERB_STEAK, Cooking.HONEY_PORK, Cooking.BEEF_WELLINGTON, Cooking.FISH_CHOWDER, Cooking.CHICKEN_CORDON_BLEU, Cooking.WOODLAND_STEW)) {
			var stack = new ItemStack(item);
			assertTrue(stack.get(DataComponents.FOOD).canAlwaysEat());
			assertEquals(16, stack.getMaxStackSize());
			String name = BuiltInRegistries.ITEM.getKey(item).getPath();
			assertNotNull(getClass().getResource("/assets/darkspawn/items/" + name + ".json"));
			assertNotNull(getClass().getResource("/assets/darkspawn/models/item/" + name + ".json"));
		}
		assertNotNull(new ItemStack(Cooking.FISH_CHOWDER).get(DataComponents.USE_REMAINDER));
	}
}
