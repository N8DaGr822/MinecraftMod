package darkspawn.black.explorerpack;

import net.fabricmc.api.ModInitializer;

public final class ExplorerPackMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ExplorerPackItem.register();
	}
}
