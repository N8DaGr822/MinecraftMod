package darkspawn.black.cooking;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.TestBootstrap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuisineTest {
	@BeforeAll static void init() { TestBootstrap.initialize(); }
	@Test void varietyExpiresWithRepetitionAndSaveRestoreKeepsDiscoveries() {
		var progress = new CulinaryProgress();
		for (int i = 0; i < 8; i++) progress.eat("food" + i, true);
		assertEquals(8, progress.variety());
		assertEquals(0.1F, progress.saturationBonus());
		progress.unlock("shadow_stew");
		for (int i = 0; i < 8; i++) progress.eat("food0", true);
		assertEquals(1, progress.variety());
		assertEquals(0, progress.saturationBonus());
		var loaded = new CulinaryProgress();
		loaded.restore(progress.recent(), progress.meals(), progress.recipes());
		assertEquals(8, loaded.meals().size());
		assertTrue(loaded.knows("shadow_stew"));
		assertFalse(loaded.unlock("shadow_stew"));
		assertEquals(1, loaded.variety());
	}
	@Test void everyNewDishDecodesMatchesAndDoesNotCollideWithAnotherRecipe() throws Exception {
		var lookup = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		var recipes = new ArrayList<CookingRecipe>();
		var inputs = new ArrayList<CraftingInput>();
		for (var entry : Cuisine.DISHES.entrySet()) {
			try (var stream = getClass().getResourceAsStream("/data/darkspawn/recipe/" + entry.getKey() + ".json")) {
				assertNotNull(stream, entry.getKey());
				var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
				CookingRecipe recipe = assertInstanceOf(CookingRecipe.class, Recipe.DIRECT_CODEC.parse(lookup, json).getOrThrow());
				var items = new ArrayList<ItemStack>();
				for (var ingredient : recipe.ingredients()) items.add(new ItemStack(ingredient.items().findFirst().orElseThrow()));
				while (items.size() < 4) items.add(ItemStack.EMPTY);
				var input = CraftingInput.of(4, 1, items);
				assertTrue(recipe.matches(input, null), entry.getKey());
				assertTrue(recipe.assemble(input).is(entry.getValue().item()), entry.getKey());
				recipes.add(recipe); inputs.add(input);
			}
		}
		for (int i = 0; i < recipes.size(); i++) for (int j = 0; j < recipes.size(); j++) {
			assertEquals(i == j, recipes.get(i).matches(inputs.get(j), null), "New dishes must have unambiguous ingredients");
		}
	}
}
