package darkspawn.black.legendaryarmor;

import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegendaryArmorAssetsTest {
	@BeforeAll static void initialize() { LegendaryArmorTestBootstrap.initialize(); }

	@Test void trimmedLegendaryModelsKeepAllSpritesOnTheEquipmentAtlas() throws Exception {
		var sources = json("/assets/minecraft/atlases/blocks.json").getAsJsonArray("sources");
		assertTrue(java.util.stream.StreamSupport.stream(sources.spliterator(), false).map(com.google.gson.JsonElement::getAsJsonObject)
			.anyMatch(source -> "minecraft:directory".equals(source.get("type").getAsString())
				&& "equipment".equals(source.get("source").getAsString()) && "equipment/".equals(source.get("prefix").getAsString())),
			"Equipment inventory sprites must be stitched into the blocks atlas");
		var families = LegendaryArmor.NAMES;
		for (String family : families) for (String slot : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
			String id = family + "_" + slot;
			var base = json("/assets/darkspawn/models/item/" + id + ".json").getAsJsonObject("textures");
			var trim = json("/assets/darkspawn/models/item/" + id + "_trim.json").getAsJsonObject("textures");
			assertTrue(base.get("armor").getAsString().startsWith("darkspawn:equipment/"), id);
			assertTrue(trim.get("trim").getAsString().startsWith("darkspawn:equipment/"), id + " must not mix the items and blocks atlases");
			try (var texture = getClass().getResourceAsStream("/assets/darkspawn/textures/" + trim.get("trim").getAsString().substring(10) + ".png")) {
				assertNotNull(texture, id);
			}
		}
	}

	private com.google.gson.JsonObject json(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test void legendaryAssetsHaveAllSlotsAndValidTrimBounds() throws Exception {
		assertEquals(5, LegendaryArmor.SETS.size());
		for (String family : LegendaryArmor.NAMES) {
			var geometry = json("/assets/darkspawn/geometry/armor/" + family + ".json");
			var slots = new java.util.HashSet<String>();
			for (var part : geometry.getAsJsonArray("parts")) for (var value : part.getAsJsonObject().getAsJsonArray("cubes")) {
				var cube = value.getAsJsonObject(); slots.add(cube.get("slot").getAsString());
				if (!cube.has("trim_uv")) continue;
				var uv = cube.getAsJsonArray("trim_uv"); var size = cube.getAsJsonArray("size");
				assertTrue(uv.get(0).getAsDouble() + 2 * (size.get(0).getAsDouble() + size.get(2).getAsDouble()) <= 64, family);
				assertTrue(uv.get(1).getAsDouble() + size.get(1).getAsDouble() + size.get(2).getAsDouble() <= 32, family);
			}
			assertEquals(java.util.Set.of("helmet", "chestplate", "leggings", "boots"), slots);
			for (String slot : slots) {
				assertFalse(json("/assets/darkspawn/models/item/" + family + "_" + slot + ".json").getAsJsonArray("elements").isEmpty());
				assertEquals("minecraft:select", json("/assets/darkspawn/items/" + family + "_" + slot + ".json").getAsJsonObject("model").get("type").getAsString());
			}
		}
	}
}
