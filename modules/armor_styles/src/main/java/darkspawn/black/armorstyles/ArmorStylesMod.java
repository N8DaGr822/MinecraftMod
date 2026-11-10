package darkspawn.black.armorstyles;

import darkspawn.black.core.hook.EquipmentChanged;
import net.fabricmc.api.ModInitializer;

public final class ArmorStylesMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ArmorStyles.initialize();
		// Profile Swaps: Worn sets replaced wholesale by another mod refresh their bonuses immediately.
		EquipmentChanged.EVENT.register(ArmorStyleBonuses::update);
	}
}
