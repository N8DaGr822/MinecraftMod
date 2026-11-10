package darkspawn.black.climate;

import net.fabricmc.api.ModInitializer;

public final class ClimateMod implements ModInitializer {
	@Override
	public void onInitialize() {
		Temperature.initialize();
	}
}
