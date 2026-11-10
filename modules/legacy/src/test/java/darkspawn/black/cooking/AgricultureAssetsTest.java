package darkspawn.black.cooking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import darkspawn.black.TestBootstrap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgricultureAssetsTest {
	@BeforeAll
	static void initialize() { TestBootstrap.initialize(); }

	private JsonObject json(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private void model(String id, Set<String> checked) throws Exception {
		if (!checked.add(id)) { return; }
		String[] parts = id.split(":", 2);
		JsonObject definition = json("/assets/" + parts[0] + "/models/" + parts[1] + ".json");
		if (definition.has("parent") && !definition.get("parent").getAsString().contains("builtin/")) {
			model(definition.get("parent").getAsString(), checked);
		}
		if (definition.has("textures")) {
			for (var texture : definition.getAsJsonObject("textures").entrySet()) {
				String value = texture.getValue().getAsString();
				if (value.startsWith("#")) { continue; }
				String[] textureId = value.split(":", 2);
				assertNotNull(getClass().getResource("/assets/" + textureId[0] + "/textures/" + textureId[1] + ".png"), value);
			}
		}
	}

	@Test
	void everyGrowthStageAndInventoryIconResolvesPackagedAssets() throws Exception {
		Set<String> checked = new HashSet<>();
		JsonObject names = json("/assets/darkspawn/lang/en_us.json");
		for (var crop : Agriculture.CROPS) {
			String block = BuiltInRegistries.BLOCK.getKey(crop.block()).getPath();
			JsonObject definition = json("/assets/darkspawn/blockstates/" + block + ".json");
			Set<Integer> ages = new HashSet<>();
			for (var entry : definition.getAsJsonArray("multipart")) {
				JsonObject part = entry.getAsJsonObject();
				ages.add(part.getAsJsonObject("when").get("age").getAsInt());
				model(part.getAsJsonObject("apply").get("model").getAsString(), checked);
			}
			assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7), ages, block);
			assertTrue(names.has("block.darkspawn." + block));
			for (var item : new net.minecraft.world.item.Item[]{crop.seeds(), crop.ingredient()}) {
				String name = BuiltInRegistries.ITEM.getKey(item).getPath();
				model(json("/assets/darkspawn/items/" + name + ".json").getAsJsonObject("model").get("model").getAsString(), checked);
				assertTrue(names.has("item.darkspawn." + name));
			}
		}
		model(json("/assets/darkspawn/items/butter.json").getAsJsonObject("model").get("model").getAsString(), checked);
	}
}
