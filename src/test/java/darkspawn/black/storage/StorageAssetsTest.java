package darkspawn.black.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.TestBootstrap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageAssetsTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	private JsonObject json(String path) throws Exception {
		try (var input = getClass().getResourceAsStream(path)) {
			assertNotNull(input, path);
			return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
	@Test void allDirectionsTexturesRecipesAndDropsResolve() throws Exception {
		var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		var names = json("/assets/darkspawn/lang/en_us.json");
		for (String id : List.of("dump_chest", "sorting_chest", "bank_chest", "sorting_tube")) {
			var states = json("/assets/darkspawn/blockstates/" + id + ".json").getAsJsonObject("variants");
			assertEquals(Set.of("facing=north", "facing=south", "facing=east", "facing=west", "facing=up", "facing=down"), states.keySet());
			for (var state : states.entrySet()) { assertEquals("darkspawn:block/" + id, state.getValue().getAsJsonObject().get("model").getAsString()); }
			var model = json("/assets/darkspawn/models/block/" + id + ".json");
			for (var texture : model.getAsJsonObject("textures").entrySet()) {
				String[] resource = texture.getValue().getAsString().split(":", 2);
				assertNotNull(getClass().getResource("/assets/" + resource[0] + "/textures/" + resource[1] + ".png"), texture.getValue().getAsString());
			}
			for (var element : model.getAsJsonArray("elements")) {
				for (String endpoint : List.of("from", "to")) {
					for (var coordinate : element.getAsJsonObject().getAsJsonArray(endpoint)) { assertTrue(coordinate.getAsDouble() >= -16 && coordinate.getAsDouble() <= 32); }
				}
				assertEquals(6, element.getAsJsonObject().getAsJsonObject("faces").size());
			}
			assertEquals("darkspawn:item/" + id, json("/assets/darkspawn/items/" + id + ".json").getAsJsonObject("model").get("model").getAsString());
			// Inventory Artwork: Custom materials retain the placed block's authored geometry.
			var itemModel = json("/assets/darkspawn/models/item/" + id + ".json");
			assertEquals(model.get("elements"), itemModel.get("elements"));
			for (var texture : itemModel.getAsJsonObject("textures").entrySet()) {
				String resource = texture.getValue().getAsString();
				assertTrue(resource.startsWith("darkspawn:"), resource);
				assertNotNull(getClass().getResource("/assets/darkspawn/textures/" + resource.substring(10) + ".png"), resource);
			}
			assertTrue(names.has("block.darkspawn." + id));
			assertNotNull(Recipe.CODEC.parse(ops, json("/data/darkspawn/recipe/" + id + ".json")).getOrThrow());
			var pool = json("/data/darkspawn/loot_table/blocks/" + id + ".json").getAsJsonArray("pools").get(0).getAsJsonObject();
			assertEquals("darkspawn:" + id, pool.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
			if (id.equals("bank_chest")) { assertFalse(pool.has("conditions"), "A packed Bank must not be randomly discarded by explosion decay"); }
		}
		assertTrue(json("/data/minecraft/tags/block/mineable/axe.json").getAsJsonArray("values").asList().stream().anyMatch(value -> value.getAsString().equals("darkspawn:cooking_station")), "Preserve existing axe-mining behavior");
	}
}
