package darkspawn.black.quiver;

import net.fabricmc.api.ModInitializer;

public final class QuiverMod implements ModInitializer {
	@Override
	public void onInitialize() {
		QuiverItem.register();
	}
}
