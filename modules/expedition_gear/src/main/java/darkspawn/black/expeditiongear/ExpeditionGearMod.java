package darkspawn.black.expeditiongear;

import darkspawn.black.core.hook.ClimateProtection;
import net.fabricmc.api.ModInitializer;

public final class ExpeditionGearMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ExpeditionArmor.initialize();
		// Optional Climate: Without darkspawn_climate nobody queries the provider and the suits keep their other effects.
		ClimateProtection.register(ExpeditionArmor::climateProtection);
	}
}
