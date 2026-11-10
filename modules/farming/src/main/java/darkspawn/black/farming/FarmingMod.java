package darkspawn.black.farming;

import net.fabricmc.api.ModInitializer;

public final class FarmingMod implements ModInitializer {
	@Override
	public void onInitialize() {
		Agriculture.initialize();
	}
}
