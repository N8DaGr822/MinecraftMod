package darkspawn.black.cooking;

import net.fabricmc.api.ModInitializer;

public final class CookingMod implements ModInitializer {
	@Override
	public void onInitialize() {
		Cooking.initialize();
		Cuisine.initialize();
		Culinary.initialize();
		VillageKitchen.initialize();
	}
}
