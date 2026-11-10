package darkspawn.black.legendaryarmor;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

final class LegendaryArmorTestBootstrap {
	private static boolean initialized;

	private LegendaryArmorTestBootstrap() {
	}

	static synchronized void initialize() {
		// Fabric Tests: ServiceLoader providers must use the same Knot loader as their mod interfaces.
		Thread.currentThread().setContextClassLoader(LegendaryArmorTestBootstrap.class.getClassLoader());
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		new LegendaryArmorMod().onInitialize();
		// Minecraft 26.3: Bind defaults after registering all mod items and components.
		var registries = VanillaRegistries.createWorldLookup();
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(components -> components.apply());
		initialized = true;
	}
}
