package darkspawn.black.bonfire.client;

import net.fabricmc.api.ClientModInitializer;

public final class BonfireClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BonfireHud.initialize();
	}
}
