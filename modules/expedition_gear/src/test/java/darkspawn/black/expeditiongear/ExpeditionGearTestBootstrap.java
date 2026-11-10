package darkspawn.black.expeditiongear;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

final class ExpeditionGearTestBootstrap {
	private static boolean initialized;

	private ExpeditionGearTestBootstrap() {
	}

	static synchronized void initialize() {
		// Fabric Tests: ServiceLoader providers must use the same Knot loader as their mod interfaces.
		Thread.currentThread().setContextClassLoader(ExpeditionGearTestBootstrap.class.getClassLoader());
		if (initialized) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		new ExpeditionGearMod().onInitialize();
		// Minecraft 26.3: Bind defaults after registering all mod items and components.
		var registries = VanillaRegistries.createWorldLookup();
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(components -> components.apply());
		initialized = true;
	}
}
