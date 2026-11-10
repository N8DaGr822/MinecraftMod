package darkspawn.black.storage;

import net.fabricmc.api.ModInitializer;

public final class StorageMod implements ModInitializer {
	@Override
	public void onInitialize() {
		Storage.initialize();
	}
}
