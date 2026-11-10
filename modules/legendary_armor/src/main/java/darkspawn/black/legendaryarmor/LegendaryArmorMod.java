package darkspawn.black.legendaryarmor;

import darkspawn.black.core.hook.EquipmentChanged;
import net.fabricmc.api.ModInitializer;

public final class LegendaryArmorMod implements ModInitializer {
	@Override
	public void onInitialize() {
		LegendaryArmor.initialize();
		// Profile Swaps: Worn sets replaced wholesale by another mod refresh their bonuses immediately.
		EquipmentChanged.EVENT.register(LegendaryArmor::update);
	}
}
