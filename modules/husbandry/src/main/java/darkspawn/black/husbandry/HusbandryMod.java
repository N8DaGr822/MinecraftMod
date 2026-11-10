package darkspawn.black.husbandry;

import net.fabricmc.api.ModInitializer;

public final class HusbandryMod implements ModInitializer {
	@Override
	public void onInitialize() {
		AnimalUtility.initialize();
	}
}
