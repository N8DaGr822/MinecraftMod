package darkspawn.black.enchantments;

import net.fabricmc.api.ModInitializer;

public final class EnchantmentsMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ThematicEnchantments.initialize();
	}
}
