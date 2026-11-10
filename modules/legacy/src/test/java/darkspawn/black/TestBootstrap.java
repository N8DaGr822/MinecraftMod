package darkspawn.black;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

public final class TestBootstrap {
	private static boolean initialized;

	private TestBootstrap() {
	}

	public static synchronized void initialize() {
		// Fabric Tests: ServiceLoader providers must use the same Knot loader as their mod interfaces.
		Thread.currentThread().setContextClassLoader(TestBootstrap.class.getClassLoader());
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		// Extracted modules are separate mods at runtime; initialize the ones legacy code still uses.
		new darkspawn.black.storage.StorageMod().onInitialize();
		new darkspawn.black.bonfire.BonfireMod().onInitialize();
		new darkspawn.black.quiver.QuiverMod().onInitialize();
		new darkspawn.black.ingredientpouch.IngredientPouchMod().onInitialize();
		new darkspawn.black.explorerpack.ExplorerPackMod().onInitialize();
		new darkspawn.black.enchantments.EnchantmentsMod().onInitialize();
		new darkspawn.black.armorstyles.ArmorStylesMod().onInitialize();
		new darkspawn.black.expeditiongear.ExpeditionGearMod().onInitialize();
		new darkspawn.black.climate.ClimateMod().onInitialize();
		new darkspawn.black.legendaryarmor.LegendaryArmorMod().onInitialize();
		new darkspawn.black.farming.FarmingMod().onInitialize();
		new darkspawn.black.fishing.FishingMod().onInitialize();
		new Darkspawn().onInitialize();
		// Minecraft 26.3: Bind defaults after registering all mod items and components.
		var registries = VanillaRegistries.createWorldLookup();
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(components -> components.apply());
		initialized = true;
	}
}
