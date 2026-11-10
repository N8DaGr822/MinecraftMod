package darkspawn.black.climate.client;

import net.fabricmc.api.ClientModInitializer;

public final class ClimateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TemperatureHud.initialize();
	}
}
