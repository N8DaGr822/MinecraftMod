package darkspawn.black.client;

import darkspawn.black.core.client.ArmorGeometryRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.world.level.ItemLike;

final class LegacyArmorRenderers {
	private LegacyArmorRenderers() { }

	static void initialize() {
		ArmorGeometryRenderer.register("quarantine", ArmorGeometryRenderer.IRON_TRIM, darkspawn.black.forsaken.ForsakenContent.ARMOR.values().toArray(ItemLike[]::new));
	}
}
