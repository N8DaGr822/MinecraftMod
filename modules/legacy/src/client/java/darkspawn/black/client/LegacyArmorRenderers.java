package darkspawn.black.client;

import darkspawn.black.core.client.ArmorGeometryRenderer;
import darkspawn.black.equipment.ExpeditionArmor;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.world.level.ItemLike;

final class LegacyArmorRenderers {
	private LegacyArmorRenderers() { }

	static void initialize() {
		ArmorRenderer.register(context -> new ArmorGeometryRenderer(context, "noah", ArmorGeometryRenderer.IRON_TRIM, "noah_pulse"), darkspawn.black.legacy.NoahLegacy.ARMOR.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("quarantine", ArmorGeometryRenderer.IRON_TRIM, darkspawn.black.forsaken.ForsakenContent.ARMOR.values().toArray(ItemLike[]::new));
		darkspawn.black.equipment.LegendaryArmor.SETS.forEach((name, pieces) -> ArmorGeometryRenderer.register(name, ArmorGeometryRenderer.NETHERITE_TRIM, pieces.values().toArray(ItemLike[]::new)));
		ArmorGeometryRenderer.register("mountaineering", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.MOUNTAINEERING.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("spelunking", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.SPELUNKING.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("diver", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.DIVER.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("desert_wanderer", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.DESERT_WANDERER.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("ashwalker", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.ASHWALKER.values().toArray(ItemLike[]::new));
	}
}
