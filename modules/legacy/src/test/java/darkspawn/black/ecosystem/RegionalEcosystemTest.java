package darkspawn.black.ecosystem;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import darkspawn.black.Darkspawn;
import darkspawn.black.TestBootstrap;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossProfile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionalEcosystemTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	private JsonElement resource(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path); return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}
	@Test void everyRemainingBossHasCommonCreaturesAHeraldAndSeparateDiscoveryRewards() throws Exception {
		var covered = EnumSet.noneOf(BossProfile.class);
		var language = resource("/assets/darkspawn/lang/en_us.json").getAsJsonObject();
		for (var kind : RegionalKind.values()) {
			assertEquals(Darkspawn.id(kind.id()), BuiltInRegistries.ENTITY_TYPE.getKey(RegionalEcosystems.MOBS.get(kind)));
			assertTrue(language.has("entity.darkspawn." + kind.id()));
			if (!kind.herald()) { continue; }
			covered.add(kind.region);
			var reward = RegionalEcosystems.REWARDS.get(kind);
			assertNotNull(reward); assertFalse(BossItems.ESSENCES.containsValue(reward)); assertFalse(BossItems.HEARTS.containsValue(reward));
			assertTrue(language.has("item.darkspawn." + kind.rewardId()));
			assertNotNull(resource("/assets/darkspawn/items/" + kind.rewardId() + ".json"));
			assertNotNull(resource("/assets/darkspawn/models/item/" + kind.rewardId() + ".json"));
		}
		assertEquals(EnumSet.allOf(BossProfile.class), covered);
		assertEquals(74, RegionalEcosystems.MOBS.size());
	}
	@Test void allAlternativeRecipesRequireDiscoveriesAndDragonBreathAndPreserveExistingSummons() throws Exception {
		var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
		for (var region : BossProfile.values()) {
			var recipe = assertInstanceOf(ShapelessRecipe.class, Recipe.DIRECT_CODEC.parse(ops,
					resource("/data/darkspawn/recipe/" + region.id() + "_ecosystem_sigil.json")).getOrThrow());
			var materials = new ArrayList<ItemStack>();
			for (var kind : RegionalKind.values()) {
				if (kind.region == region && kind.herald()) { materials.add(new ItemStack(kind == RegionalKind.THUNDER_ROC
						? RegionalEcosystems.STORMFORGED_FEATHER : RegionalEcosystems.REWARDS.get(kind))); }
			}
			materials.add(new ItemStack(Items.DRAGON_BREATH));
			while (materials.size() < 4) { materials.add(ItemStack.EMPTY); }
			var input = CraftingInput.of(2, 2, materials);
			assertTrue(recipe.matches(input, null), region.id());
			assertTrue(recipe.assemble(input).is(BossItems.SUMMONS.get(region)));
			materials.set(0, ItemStack.EMPTY);
			assertFalse(recipe.matches(CraftingInput.of(2, 2, materials), null), "Discovery ingredient must be required: " + region);
			assertNotNull(resource("/data/darkspawn/advancement/recipes/misc/" + region.id() + "_ecosystem_sigil.json"));
			assertNotNull(Recipe.DIRECT_CODEC.parse(ops, resource("/data/darkspawn/recipe/" + region.id() + "_sigil.json")).getOrThrow());
		}
	}
	@Test void allSeventyFourModelsBakeAndAnimateWithPackagedTextures() throws Exception {
		Class<?> modelClass = Class.forName("darkspawn.black.client.ecosystem.RegionalMobModel");
		Class<?> stateClass = Class.forName("darkspawn.black.client.ecosystem.RegionalMobRenderState");
		Class<?> rendererClass = Class.forName("darkspawn.black.client.ecosystem.RegionalMobRenderer");
		for (var kind : RegionalKind.values()) {
			Object layer = modelClass.getMethod("createBodyLayer", RegionalKind.class).invoke(null, kind);
			Object root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			Object model = modelClass.getConstructors()[0].newInstance(root, kind);
			Object state = stateClass.getConstructor().newInstance();
			stateClass.getField("walkAnimationPos").setFloat(state, 8);
			stateClass.getField("walkAnimationSpeed").setFloat(state, .8F);
			for (int warning : List.of(25, 10, 0)) {
				stateClass.getField("warning").setInt(state, warning);
				stateClass.getField("active").setInt(state, warning == 0 ? 12 : 0);
				stateClass.getField("hiding").setBoolean(state, warning == 10);
				modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			}
			var texture = (net.minecraft.resources.Identifier)rendererClass.getMethod("texture", RegionalKind.class).invoke(null, kind);
			assertNotNull(getClass().getResource("/assets/minecraft/" + texture.getPath()), kind.id() + " texture");
		}
	}
}
