package darkspawn.black.bonfire;

import net.fabricmc.api.ModInitializer;

public final class BonfireMod implements ModInitializer {
	@Override
	public void onInitialize() {
		Bonfires.initialize();
	}
}
