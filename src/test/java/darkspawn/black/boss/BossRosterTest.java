package darkspawn.black.boss;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import darkspawn.black.Darkspawn;
import darkspawn.black.TestBootstrap;
import darkspawn.black.health.BossHeartProgress;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossRosterTest {
	private static HolderLookup.Provider registries;
	@BeforeAll
	static void initialize() { TestBootstrap.initialize(); registries = VanillaRegistries.createWorldLookup(); }

	private JsonElement resource(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream(path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
	}

	@Test
	void oldOceanHeartsMigrateWithoutGrantingADuplicateUpgrade() {
		var progress = new BossHeartProgress();
		progress.restore(List.of("leviathan", "kraken", "ancient_tree_spirit"));
		assertEquals(2, progress.savedBosses().size());
		assertEquals(4, progress.bonusHealth());
		assertTrue(progress.hasConsumed(BossKind.KRAKEN));
		assertFalse(progress.consume(BossKind.KRAKEN));
		assertFalse(progress.savedBosses().contains("leviathan"));
		assertNotNull(BossItems.LEGACY_OCEAN_HEART);
	}

	@Test
	void allFamiliesHaveRegisteredEncountersHeartsAndUsableItemAssets() throws Exception {
		assertEquals(16, BossKind.values().length);
		assertEquals(16, BossItems.HEARTS.size());
		assertEquals(14, BossEntities.BIOME_BOSSES.size());
		var language = resource("/assets/darkspawn/lang/en_us.json").getAsJsonObject();
		for (BossProfile profile : BossProfile.values()) {
			assertEquals(profile.id(), BuiltInRegistries.ENTITY_TYPE.getKey(BossEntities.BIOME_BOSSES.get(profile)).getPath());
			assertTrue(BossEmpowerment.isKnownPower(profile.power));
			for (String suffix : List.of("_sigil", "_essence", "_heart")) {
				String id = profile.id() + suffix;
				assertNotNull(BuiltInRegistries.ITEM.get(Darkspawn.id(id)).orElseThrow());
				var definition = resource("/assets/darkspawn/items/" + id + ".json").getAsJsonObject();
				assertEquals("darkspawn:item/" + id, definition.getAsJsonObject("model").get("model").getAsString());
				assertNotNull(resource("/assets/darkspawn/models/item/" + id + ".json"));
				assertTrue(language.has("item.darkspawn." + id));
			}
			for (int phase = 1; phase <= 3; phase++) { assertTrue(language.has("boss.darkspawn." + profile.id() + ".phase." + phase)); }
		}
	}

	@Test
	void packagedRecipesUnlocksAndBiomesResolveAgainstMinecraftRegistries() throws Exception {
		var ops = registries.createSerializationContext(JsonOps.INSTANCE);
		var recipes = new MappedRegistry<Recipe<?>>(Registries.RECIPE, Lifecycle.stable());
		var names = new ArrayList<String>();
		for (BossProfile profile : BossProfile.values()) {
			names.add(profile.id() + "_sigil");
			names.add(profile.power + "_empowerment");
			for (var biome : resource("/data/darkspawn/tags/worldgen/biome/" + profile.id() + "_biomes.json").getAsJsonObject().getAsJsonArray("values")) {
				assertNotNull(registries.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(biome.getAsString()))));
			}
		}
		for (String name : names) {
			recipes.register(ResourceKey.create(Registries.RECIPE, Darkspawn.id(name)),
					Recipe.DIRECT_CODEC.parse(ops, resource("/data/darkspawn/recipe/" + name + ".json")).getOrThrow(), RegistrationInfo.BUILT_IN);
		}
		recipes.freeze();
		var advancementOps = HolderLookup.Provider.create(Stream.concat(registries.listRegistries(), Stream.of(recipes))).createSerializationContext(JsonOps.INSTANCE);
		for (String name : names) {
			assertNotNull(Advancement.CODEC.parse(advancementOps, resource("/data/darkspawn/advancement/recipes/misc/" + name + ".json")).getOrThrow());
		}
	}

	@Test
	void summonRecipesConsumeDeclaredMaterialsAndRequireEndbornShardsForTheEye() throws Exception {
		var ops = registries.createSerializationContext(JsonOps.INSTANCE);
		for (BossProfile profile : BossProfile.values()) {
			var recipe = assertInstanceOf(ShapedRecipe.class, Recipe.DIRECT_CODEC.parse(ops,
					resource("/data/darkspawn/recipe/" + profile.id() + "_sigil.json")).getOrThrow());
			var outer = profile == BossProfile.VOID_EYE ? Items.OBSIDIAN : profile.outerMaterial;
			var input = new ArrayList<ItemStack>(List.of(new ItemStack(outer), new ItemStack(profile.innerMaterial), new ItemStack(outer),
					new ItemStack(profile.innerMaterial), new ItemStack(Items.DRAGON_BREATH), new ItemStack(profile.innerMaterial),
					new ItemStack(outer), new ItemStack(profile.innerMaterial), new ItemStack(outer)));
			if (profile == BossProfile.VOID_EYE) {
				input.set(1, new ItemStack(Items.ENDER_EYE)); input.set(3, new ItemStack(BossItems.ENDBORN_SHARD));
				input.set(5, new ItemStack(BossItems.ENDBORN_SHARD)); input.set(7, new ItemStack(Items.END_CRYSTAL));
			}
			var crafting = CraftingInput.of(3, 3, input);
			assertTrue(recipe.matches(crafting, null), profile.id());
			assertTrue(recipe.assemble(crafting).is(BossItems.SUMMONS.get(profile)), profile.id());
			input.set(4, ItemStack.EMPTY);
			assertFalse(recipe.matches(CraftingInput.of(3, 3, input), null));
		}
	}

	@Test
	void everyPowerRecipePreservesWeaponDataAndSurvivesNetworkSynchronization() throws Exception {
		var ops = registries.createSerializationContext(JsonOps.INSTANCE);
		var access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		for (BossProfile profile : BossProfile.values()) {
			var recipe = assertInstanceOf(BossEmpowermentRecipe.class, Recipe.DIRECT_CODEC.parse(ops,
					resource("/data/darkspawn/recipe/" + profile.power + "_empowerment.json")).getOrThrow());
			var weapon = BossEmpowerment.apply(new ItemStack(Items.NETHERITE_SWORD), BossEmpowerment.ROOTBOUND);
			weapon.setDamageValue(137);
			weapon.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
			var input = new SmithingRecipeInput(new ItemStack(Items.AMETHYST_SHARD), weapon, new ItemStack(BossItems.ESSENCES.get(profile)));
			var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
			try {
				// Unit Registry: This harness has no loaded item tags; the headless server tests cover the packaged tagged recipe.
				var networkRecipe = new BossEmpowermentRecipe(recipe.templateIngredient().orElseThrow(), Ingredient.of(Items.NETHERITE_SWORD),
						recipe.additionIngredient().orElseThrow(), recipe.assemble(input).get(BossEmpowerment.POWER));
				BossEmpowermentRecipe.STREAM_CODEC.encode(buffer, networkRecipe);
				var decoded = BossEmpowermentRecipe.STREAM_CODEC.decode(buffer);
				assertTrue(decoded.matches(input, null), profile.id());
				ItemStack result = decoded.assemble(input);
				var expected = weapon.copy(); expected.set(BossEmpowerment.POWER, profile.power);
				assertTrue(ItemStack.matches(expected, result));
				assertEquals(BossEmpowerment.ROOTBOUND, weapon.get(BossEmpowerment.POWER));
				assertFalse(decoded.matches(new SmithingRecipeInput(input.template(), result, input.addition()), null));
				assertEquals(0, buffer.readableBytes());
			} finally { buffer.release(); }
		}
		var invalid = resource("/data/darkspawn/recipe/tidecaller_empowerment.json").getAsJsonObject();
		invalid.addProperty("power", "typo_power");
		assertTrue(Recipe.DIRECT_CODEC.parse(ops, invalid).error().isPresent());
	}

	@Test
	void allBossModelsBakeAndAnimateEveryMinionRoleWithoutAGameWindow() throws Exception {
		Class<?> modelClass = Class.forName("darkspawn.black.client.boss.BiomeBossModel");
		Class<?> stateClass = Class.forName("darkspawn.black.client.boss.BiomeBossRenderState");
		for (BossProfile profile : BossProfile.values()) {
			Object layer = modelClass.getMethod("createBodyLayer", BossProfile.class).invoke(null, profile);
			Object root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			Object model = modelClass.getConstructors()[0].newInstance(root);
			Object state = stateClass.getConstructor().newInstance();
			stateClass.getField("profile").set(state, profile);
			for (int phase = 1; phase <= 3; phase++) {
				stateClass.getField("phase").setInt(state, phase);
				stateClass.getField("minion").setBoolean(state, false);
				modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
				stateClass.getField("minion").setBoolean(state, true);
				for (var role : BossMinion.Role.values()) {
					stateClass.getField("role").setInt(state, role.ordinal());
					modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
				}
			}
		}
		for (String name : List.of("BiomeBossRenderer", "BossAccentLayer", "BossHazardRenderer", "VoidPlatformRenderer", "EndbornRenderer")) {
			Class.forName("darkspawn.black.client.boss." + name, false, getClass().getClassLoader()).getDeclaredMethods();
		}
	}

	@Test
	void endbornAssetsBakeWithGeckoLibAndAllAnimatedBonesExist() throws Exception {
		var loader = new GeckoLibGsonLoader();
		var model = loader.bakeGeckoLibModelFile(Darkspawn.id("entity/endborn"),
				resource("/assets/darkspawn/geckolib/models/entity/endborn.geo.json").getAsJsonObject());
		var animations = loader.bakeGeckoLibAnimationsFile(Darkspawn.id("entity/endborn"),
				resource("/assets/darkspawn/geckolib/animations/entity/endborn.animation.json").getAsJsonObject(), MathParser.create());
		assertFalse(model.isMissingno());
		assertTrue(model.getBone("head").isPresent(), "The renderer requires a head bone for look tracking");
		for (String name : List.of("idle", "walk", "attack", "blink")) {
			var animation = animations.getAnimation("animation.endborn." + name);
			assertNotNull(animation, name);
			assertTrue(animation.length() > 0, name);
			for (var bone : animation.boneAnimations()) {
				assertTrue(model.getBone(bone.boneName()).isPresent(), name + ": " + bone.boneName());
			}
		}
		try (var stream = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/endborn.png")) {
			assertNotNull(stream);
			var texture = javax.imageio.ImageIO.read(stream);
			assertNotNull(texture, "The packaged texture must decode as an image");
			assertEquals(texture.getWidth(), texture.getHeight(), "The model uses a square UV atlas");
		}
	}

	@Test
	void treeSpiritAssetsPreserveScaleWeakPointAndAnimationBones() throws Exception {
		var loader = new GeckoLibGsonLoader();
		var geometry = resource("/assets/darkspawn/geckolib/models/entity/ancient_tree_spirit.geo.json").getAsJsonObject();
		var model = loader.bakeGeckoLibModelFile(Darkspawn.id("entity/ancient_tree_spirit"), geometry);
		var animations = loader.bakeGeckoLibAnimationsFile(Darkspawn.id("entity/ancient_tree_spirit"),
				resource("/assets/darkspawn/geckolib/animations/entity/ancient_tree_spirit.animation.json").getAsJsonObject(), MathParser.create());
		assertFalse(model.isMissingno());
		for (String name : List.of("idle", "wrath_idle", "charge_seed", "charge_root", "charge_sweep",
				"seed_volley", "root_eruption", "root_sweep", "phase_change", "recovery", "awaken", "defeat")) {
			var animation = animations.getAnimation("animation.ancient_tree_spirit." + name);
			assertNotNull(animation, name);
			for (var bone : animation.boneAnimations()) {
				assertTrue(model.getBone(bone.boneName()).isPresent(), name + ": " + bone.boneName());
			}
		}
		assertEquals(AncientTreeSpirit.AWAKENING_TICKS / 20.0,
				animations.getAnimation("animation.ancient_tree_spirit.awaken").length(), 0.001);
		assertEquals(AncientTreeSpirit.DEFEAT_TICKS / 20.0,
				animations.getAnimation("animation.ancient_tree_spirit.defeat").length(), 0.001);
		double height = 0;
		int heartFaces = 0;
		for (var value : geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
			var bone = value.getAsJsonObject();
			if (!bone.has("cubes")) { continue; }
			for (var cubeValue : bone.getAsJsonArray("cubes")) {
				var cube = cubeValue.getAsJsonObject();
				double bottom = cube.getAsJsonArray("origin").get(1).getAsDouble();
				double top = bottom + cube.getAsJsonArray("size").get(1).getAsDouble();
				height = Math.max(height, top / 16 * AncientTreeSpirit.MODEL_SCALE);
				if (bone.get("name").getAsString().equals("heartwood")) {
					assertTrue(bottom / 16 * AncientTreeSpirit.MODEL_SCALE >= 8 * AncientTreeSpirit.MODEL_SCALE
							&& top / 16 * AncientTreeSpirit.MODEL_SCALE <= 14 * AncientTreeSpirit.MODEL_SCALE,
							"Heartwood must mark the raised vulnerable height band");
					heartFaces++;
				}
			}
		}
		assertEquals(BossEntities.TREE_SPIRIT.getDimensions().height(), height, 0.001);
		assertEquals(28, height, 0.001, "The Tree Spirit must render at its intended boss height");
		assertEquals(4, heartFaces, "Elytra attackers need a visible weak point from all four sides");
		try (var stream = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/ancient_tree_spirit.png")) {
			assertNotNull(stream);
			assertNotNull(javax.imageio.ImageIO.read(stream));
		}
		Class.forName("darkspawn.black.client.boss.AncientTreeSpiritRenderer", false, getClass().getClassLoader()).getDeclaredMethods();
	}

	@Test
	void mutantWolfAssetsKeepTheExistingSizeAndSupportEveryCombatPose() throws Exception {
		var loader = new GeckoLibGsonLoader();
		var geometry = resource("/assets/darkspawn/geckolib/models/entity/mutant_wolf.geo.json").getAsJsonObject();
		var model = loader.bakeGeckoLibModelFile(Darkspawn.id("entity/mutant_wolf"), geometry);
		var animations = loader.bakeGeckoLibAnimationsFile(Darkspawn.id("entity/mutant_wolf"),
				resource("/assets/darkspawn/geckolib/animations/entity/mutant_wolf.animation.json").getAsJsonObject(), MathParser.create());
		assertFalse(model.isMissingno());
		assertTrue(model.getBone("head_look").isPresent(), "Look tracking must not overwrite the animated head and jaw");
		for (String name : List.of("idle", "walk", "wrath_idle", "charge_pounce", "charge_volley", "charge_burst",
				"pounce", "frost_volley", "frost_burst", "land", "recovery", "phase_change", "awaken", "defeat")) {
			var animation = animations.getAnimation("animation.mutant_wolf." + name);
			assertNotNull(animation, name);
			assertTrue(animation.length() > 0, name);
			for (var bone : animation.boneAnimations()) {
				assertTrue(model.getBone(bone.boneName()).isPresent(), name + ": " + bone.boneName());
			}
		}
		assertEquals(MutantWolf.AWAKENING_TICKS / 20.0,
				animations.getAnimation("animation.mutant_wolf.awaken").length(), 0.001);
		assertEquals(MutantWolf.DEFEAT_TICKS / 20.0,
				animations.getAnimation("animation.mutant_wolf.defeat").length(), 0.001);
		double height = 0;
		for (var value : geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
			var bone = value.getAsJsonObject();
			if (!bone.has("cubes")) { continue; }
			for (var cubeValue : bone.getAsJsonArray("cubes")) {
				var cube = cubeValue.getAsJsonObject();
				var origin = cube.getAsJsonArray("origin");
				var size = cube.getAsJsonArray("size");
				height = Math.max(height, (origin.get(1).getAsDouble() + size.get(1).getAsDouble()) / 16);
				assertTrue(origin.get(1).getAsDouble() >= 0, "The resting paws must not sink below the ground");
				for (int axis : new int[] {0, 2}) {
					assertTrue(origin.get(axis).getAsDouble() >= -80
							&& origin.get(axis).getAsDouble() + size.get(axis).getAsDouble() <= 80,
							"The model must fit the existing ten-block ritual footprint");
				}
			}
		}
		assertEquals(BossEntities.MUTANT_WOLF.getDimensions().height(), height, 0.001);
		try (var stream = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/mutant_wolf.png")) {
			assertNotNull(stream);
			var texture = javax.imageio.ImageIO.read(stream);
			assertNotNull(texture);
			assertEquals(texture.getWidth(), texture.getHeight(), "The wolf uses a square UV atlas");
		}
		Class.forName("darkspawn.black.client.boss.MutantWolfGeoRenderer", false, getClass().getClassLoader()).getDeclaredMethods();
	}

	@Test
	void mutantZombieAssetsMatchItsSizeAndEveryPhaseAttack() throws Exception {
		var loader = new GeckoLibGsonLoader();
		var geometry = resource("/assets/darkspawn/geckolib/models/entity/mutant_zombie.geo.json").getAsJsonObject();
		var model = loader.bakeGeckoLibModelFile(Darkspawn.id("entity/mutant_zombie"), geometry);
		var animations = loader.bakeGeckoLibAnimationsFile(Darkspawn.id("entity/mutant_zombie"),
				resource("/assets/darkspawn/geckolib/animations/entity/mutant_zombie.animation.json").getAsJsonObject(), MathParser.create());
		assertFalse(model.isMissingno());
		assertTrue(model.getBone("head_look").isPresent());
		for (String name : List.of("idle", "walk", "wrath_idle", "windup_slam", "windup_boulders", "windup_brood",
				"windup_charge", "slam", "boulders", "brood", "charge", "recovery", "phase_change")) {
			var animation = animations.getAnimation("animation.mutant_zombie." + name);
			assertNotNull(animation, name);
			assertTrue(animation.length() > 0, name);
			if (name.startsWith("windup_")) { assertEquals(2, animation.length(), 0.001, "Windups must match the server's warning"); }
			for (var bone : animation.boneAnimations()) { assertTrue(model.getBone(bone.boneName()).isPresent(), name + ": " + bone.boneName()); }
		}
		double height = 0;
		for (var value : geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
			var bone = value.getAsJsonObject();
			if (!bone.has("cubes")) { continue; }
			for (var cubeValue : bone.getAsJsonArray("cubes")) {
				var cube = cubeValue.getAsJsonObject();
				var origin = cube.getAsJsonArray("origin"); var size = cube.getAsJsonArray("size");
				height = Math.max(height, (origin.get(1).getAsDouble() + size.get(1).getAsDouble()) / 16);
				assertTrue(origin.get(1).getAsDouble() >= 0, "Resting feet must stay above the ground");
				for (int axis : new int[] {0, 2}) {
					assertTrue(origin.get(axis).getAsDouble() >= -64 && origin.get(axis).getAsDouble() + size.get(axis).getAsDouble() <= 64,
							"The resting model must fit the existing eight-block ritual footprint");
				}
			}
		}
		assertEquals(BossEntities.BIOME_BOSSES.get(BossProfile.MUTANT_ZOMBIE).getDimensions().height(), height, 0.001);
		try (var stream = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/mutant_zombie.png")) {
			assertNotNull(stream); var texture = javax.imageio.ImageIO.read(stream); assertNotNull(texture);
			assertEquals(texture.getWidth(), texture.getHeight());
		}
		Class.forName("darkspawn.black.client.boss.MutantZombieRenderer", false, getClass().getClassLoader()).getDeclaredMethods();
	}

	@Test
	void beamsHaveFiniteEndpointsAndSoulTheftCannotRemoveTheLastHeart() {
		assertEquals(4, BiomeBoss.distanceToSegmentSquared(new Vec3(5, 2, 0), Vec3.ZERO, new Vec3(10, 0, 0)));
		assertEquals(25, BiomeBoss.distanceToSegmentSquared(new Vec3(15, 0, 0), Vec3.ZERO, new Vec3(10, 0, 0)));
		assertEquals(4, BiomeBoss.distanceToSegmentSquared(new Vec3(-2, 0, 0), Vec3.ZERO, new Vec3(10, 0, 0)));
		assertEquals(9, BiomeBoss.distanceToSegmentSquared(new Vec3(0, 3, 0), Vec3.ZERO, Vec3.ZERO));
		assertEquals(1, BossEffects.soulHeartsToSteal(4, 0));
		assertEquals(0, BossEffects.soulHeartsToSteal(3, 0));
		assertEquals(4, BossEffects.soulHeartsToSteal(2, 4));
		assertEquals(5, BossEffects.soulHeartsToSteal(40, 5));
	}

	@Test
	void multiplayerHealthScalingStopsAtFourPlayers() {
		assertEquals(1000, BiomeBoss.scaledHealth(1000, 0));
		assertEquals(1000, BiomeBoss.scaledHealth(1000, 1));
		assertEquals(1500, BiomeBoss.scaledHealth(1000, 2));
		assertEquals(2500, BiomeBoss.scaledHealth(1000, 4));
		assertEquals(2500, BiomeBoss.scaledHealth(1000, 100));
	}
}
