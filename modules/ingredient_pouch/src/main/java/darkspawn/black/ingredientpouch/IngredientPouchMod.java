package darkspawn.black.ingredientpouch;

import net.fabricmc.api.ModInitializer;

public final class IngredientPouchMod implements ModInitializer {
	@Override
	public void onInitialize() {
		IngredientPouchItem.register();
	}
}
