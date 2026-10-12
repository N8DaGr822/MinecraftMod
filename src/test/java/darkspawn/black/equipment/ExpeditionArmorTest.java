package darkspawn.black.equipment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import darkspawn.black.TestBootstrap;
import darkspawn.black.boss.BossEmpowerment;
import java.io.InputStreamReader;
import javax.imageio.ImageIO;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.ArmorType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpeditionArmorTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	private com.google.gson.JsonObject json(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test void nativeSmithingPreservesExistingItemDataForEveryPiece() throws Exception {
		var lookup = VanillaRegistries.createWorldLookup();
		for (String family : new String[] { "mountaineering", "spelunking", "diver", "desert_wanderer", "ashwalker" }) {
			for (ArmorType type : new ArmorType[] { ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS }) {
				String id = family + "_" + type.getName();
				var recipe = assertInstanceOf(SmithingTransformRecipe.class, Recipe.DIRECT_CODEC.parse(lookup.createSerializationContext(JsonOps.INSTANCE), json("/data/darkspawn/recipe/" + id + ".json")).getOrThrow());
				var original = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("iron_" + type.getName())));
				original.set(DataComponents.CUSTOM_NAME, Component.literal("Family Expedition"));
				original.set(BossEmpowerment.POWER, "armor_highland");
				original.setDamageValue(17);
				original.enchant(lookup.getOrThrow(Enchantments.PROTECTION), 2);
				var addition = switch (family) {
					case "diver" -> Items.PRISMARINE_SHARD;
					case "desert_wanderer" -> Items.RABBIT_HIDE;
					case "ashwalker" -> Items.MAGMA_CREAM;
					case "mountaineering" -> BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("white_wool"));
					default -> type == ArmorType.HELMET ? Items.GLOWSTONE_DUST : type == ArmorType.BOOTS ? Items.SLIME_BALL : Items.COPPER_INGOT;
				};
				ItemStack result = recipe.assemble(new SmithingRecipeInput(new ItemStack(Items.LEATHER), original, new ItemStack(addition)));
				assertTrue(result.is(BuiltInRegistries.ITEM.getValue(Darkspawn.id(id))));
				assertEquals(17, result.getDamageValue());
				assertEquals(original.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
				assertEquals(original.get(DataComponents.ENCHANTMENTS), result.get(DataComponents.ENCHANTMENTS));
				assertEquals("armor_highland", result.get(BossEmpowerment.POWER));
				assertEquals(type.getSlot(), result.get(DataComponents.EQUIPPABLE).slot());
				assertEquals(Darkspawn.id(family), result.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier());
			}
		}
	}

	@Test void paddedBootsUseOneFeetScopedMultiplicativeModifier() {
		var boots = new ItemStack(ExpeditionArmor.SPELUNKING.get(ArmorType.BOOTS));
		var fall = boots.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(e -> e.attribute().equals(Attributes.FALL_DAMAGE_MULTIPLIER)).toList();
		assertEquals(1, fall.size());
		assertEquals(-0.25, fall.getFirst().modifier().amount());
		assertEquals(EquipmentSlotGroup.FEET, fall.getFirst().slot());
		assertTrue(new ItemStack(ExpeditionArmor.MOUNTAINEERING.get(ArmorType.BOOTS)).get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().noneMatch(e -> e.attribute().equals(Attributes.FALL_DAMAGE_MULTIPLIER)));
	}

	@Test void wearableAndInventoryAssetsHaveValidGeometryTexturesAndReferences() throws Exception {
		for (String family : new String[] { "mountaineering", "spelunking", "diver", "desert_wanderer", "ashwalker" }) {
			var geometry = json("/assets/darkspawn/geometry/armor/" + family + ".json");
			var layers = json("/assets/darkspawn/equipment/" + family + ".json").getAsJsonObject("layers");
			// Client Equipment Codec: Both native fallback layer lists must be nonempty, even with a custom renderer.
			assertFalse(layers.getAsJsonArray("humanoid").isEmpty());
			assertFalse(layers.getAsJsonArray("humanoid_leggings").isEmpty());
			try (var stream = getClass().getResourceAsStream("/assets/darkspawn/textures/equipment/" + family + ".png")) {
				assertNotNull(stream);
				var image = ImageIO.read(stream);
				assertEquals(256, image.getWidth()); assertEquals(128, image.getHeight());
			}
			var parts = new java.util.HashSet<String>();
			var slots = new java.util.HashSet<String>();
			for (var part : geometry.getAsJsonArray("parts")) {
				assertTrue(parts.add(part.getAsJsonObject().get("name").getAsString()));
				for (var value : part.getAsJsonObject().getAsJsonArray("cubes")) {
					var cube = value.getAsJsonObject();
					slots.add(cube.get("slot").getAsString());
					var size = cube.getAsJsonArray("size");
					for (var dimension : size) assertTrue(dimension.getAsDouble() > 0 && Double.isFinite(dimension.getAsDouble()));
					assertTrue(2 * (size.get(0).getAsDouble() + size.get(2).getAsDouble()) <= 64, "Box UV stays in material tile");
					assertTrue(size.get(1).getAsDouble() + size.get(2).getAsDouble() <= 64);
					assertTrue(cube.get("material").getAsInt() >= 0 && cube.get("material").getAsInt() < 8);
				}
			}
			assertEquals(java.util.Set.of("head", "body", "left_arm", "right_arm", "left_leg", "right_leg"), parts);
			assertEquals(java.util.Set.of("helmet", "chestplate", "leggings", "boots"), slots);
			for (String slot : slots) {
				String id = family + "_" + slot;
				assertEquals("darkspawn:item/" + id, json("/assets/darkspawn/items/" + id + ".json").getAsJsonObject("model").get("model").getAsString());
				var model = json("/assets/darkspawn/models/item/" + id + ".json");
				assertEquals("darkspawn:equipment/" + family, model.getAsJsonObject("textures").get("armor").getAsString());
				assertFalse(model.getAsJsonArray("elements").isEmpty());
				for (var element : model.getAsJsonArray("elements")) for (String point : new String[] { "from", "to" }) for (var axis : element.getAsJsonObject().getAsJsonArray(point)) assertTrue(axis.getAsDouble() >= -16 && axis.getAsDouble() <= 32);
			}
		}
	}
}
