package darkspawn.black.creaturemounts;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

final class MountsTestBootstrap {
	private static boolean initialized;
	private MountsTestBootstrap() { }
	static synchronized void initialize() {
		// Fabric Tests: ServiceLoader providers must use the same Knot loader as their mod interfaces.
		Thread.currentThread().setContextClassLoader(MountsTestBootstrap.class.getClassLoader());
		if (initialized) return;
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		CreatureMounts.initialize();
		initialized = true;
	}
}
