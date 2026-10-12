package darkspawn.black.equipment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.TestBootstrap;
import darkspawn.black.boss.BossEmpowerment;
import java.io.InputStreamReader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArmorStylesTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }

	@Test void stylesPreserveVanillaIdentityComponentsAndCanBeReset() {
		var lookup = VanillaRegistries.createWorldLookup();
		for (var item : java.util.List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
			var original = new ItemStack(item);
			original.set(DataComponents.CUSTOM_NAME, Component.literal("Family Heirloom"));
			original.setDamageValue(31);
			original.enchant(lookup.getOrThrow(Enchantments.PROTECTION), 3);
			original.set(BossEmpowerment.POWER, "armor_highland");
			for (String style : ArmorStyles.STYLES) {
				var styled = ArmorStyles.apply(original, style);
				assertTrue(styled.is(item));
				assertEquals(style, ArmorStyles.style(styled));
				assertEquals(original.get(DataComponents.ATTRIBUTE_MODIFIERS), styled.get(DataComponents.ATTRIBUTE_MODIFIERS));
				assertEquals(original.get(DataComponents.EQUIPPABLE), styled.get(DataComponents.EQUIPPABLE));
				assertEquals(original.getMaxDamage(), styled.getMaxDamage());
				assertTrue(ItemStack.isSameItemSameComponents(original, ArmorStyles.apply(styled, "classic")));
				var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
				var restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, styled).getOrThrow()).getOrThrow();
				assertTrue(ItemStack.isSameItemSameComponents(styled, restored));
			}
			assertFalse(original.has(ArmorStyles.STYLE), "Applying a style must not mutate the smithing input");
		}
	}

	@Test void unsupportedArmorAndUnknownStylesAreRejected() {
		assertTrue(ArmorStyles.apply(new ItemStack(Items.NETHERITE_HELMET), "viking").isEmpty());
		assertTrue(ArmorStyles.apply(new ItemStack(Items.DIAMOND_SWORD), "spartan").isEmpty());
		assertTrue(ArmorStyles.apply(new ItemStack(Items.IRON_HELMET), "unknown").isEmpty());
		assertTrue(ArmorStyles.STYLE_CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("unknown")).error().isPresent());
	}

	@Test void retiredStylesMigrateWithoutLosingArmorData() {
		assertEquals(15, ArmorStyles.STYLES.size());
		var lookup = VanillaRegistries.createWorldLookup();
		var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
		for (var entry : java.util.Map.of("arbalist", "ranger", "royal_guard", "paladin", "brawler", "berserker").entrySet()) {
			var original = new ItemStack(Items.DIAMOND_HELMET);
			original.setDamageValue(17);
			original.set(DataComponents.CUSTOM_NAME, Component.literal("Heirloom"));
			original.enchant(lookup.getOrThrow(Enchantments.PROTECTION), 2);
			var json = ItemStack.CODEC.encodeStart(ops, original).getOrThrow().getAsJsonObject();
			json.getAsJsonObject("components").addProperty("darkspawn:armor_style", entry.getKey());
			var restored = ItemStack.CODEC.parse(ops, json).getOrThrow();
			assertEquals(entry.getValue(), ArmorStyles.style(restored));
			assertTrue(ItemStack.isSameItemSameComponents(original, ArmorStyles.apply(restored, "classic")));
			assertTrue(ArmorStyles.apply(original, entry.getKey()).isEmpty(), "Retired styles cannot be newly crafted");
		}
	}

	private com.google.gson.JsonObject json(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test void allStylesHaveWearableMeshesTrimCoordinatesAndInventoryModels() throws Exception {
		for (String metal : new String[] { "iron", "diamond" }) {
			var overlay = json("/assets/darkspawn/equipment/" + metal + "_style_overlay.json");
			assertEquals("minecraft:trim/" + metal + "_darker", overlay.getAsJsonArray("trim_overrides").get(0).getAsJsonObject().get("palette").getAsString());
			for (String style : ArmorStyles.STYLES) {
				String family = metal + "_" + style;
				var geometry = json("/assets/darkspawn/geometry/armor/" + family + ".json");
				var slots = new java.util.HashSet<String>();
				for (var part : geometry.getAsJsonArray("parts")) for (var value : part.getAsJsonObject().getAsJsonArray("cubes")) {
					var cube = value.getAsJsonObject();
					if (!cube.has("trim_uv")) continue;
					slots.add(cube.get("slot").getAsString());
					var uv = cube.getAsJsonArray("trim_uv"); var size = cube.getAsJsonArray("size");
					assertTrue(uv.get(0).getAsDouble() + 2 * (size.get(0).getAsDouble() + size.get(2).getAsDouble()) <= 64);
					assertTrue(uv.get(1).getAsDouble() + size.get(1).getAsDouble() + size.get(2).getAsDouble() <= 32);
				}
				assertEquals(java.util.Set.of("helmet", "chestplate", "leggings", "boots"), slots);
				for (String slot : slots) {
					String id = family + "_" + slot;
					assertFalse(json("/assets/darkspawn/models/item/" + id + ".json").getAsJsonArray("elements").isEmpty());
					assertEquals("darkspawn:item/" + id, json("/assets/darkspawn/models/item/" + id + "_trim.json").get("parent").getAsString());
					var selector = json("/assets/minecraft/items/" + metal + "_" + slot + ".json").getAsJsonObject("model");
					assertEquals("darkspawn:armor_style", selector.get("component").getAsString());
					assertEquals(ArmorStyles.STYLES.size() - 1, selector.getAsJsonArray("cases").size());
					assertEquals(11, selector.getAsJsonObject("fallback").getAsJsonArray("cases").size());
				}
			}
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
