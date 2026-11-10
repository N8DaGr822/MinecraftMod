package darkspawn.black.fishing;

import net.fabricmc.api.ModInitializer;

public final class FishingMod implements ModInitializer {
	@Override
	public void onInitialize() {
		RegionalFishing.initialize();
	}
}
