package darkspawn.black.client;

import darkspawn.black.equipment.ArmorStyles;
import java.util.HashMap;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class ArmorStyleRenderer {
	private ArmorStyleRenderer() { }

	public static void initialize() {
		register("iron", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		register("diamond", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
	}

	private static void register(String metal, ItemLike... items) {
		ArmorRenderer.register(context -> {
			var styles = new HashMap<String, ExpeditionArmorRenderer>();
			for (String style : ArmorStyles.STYLES) styles.put(style, new ExpeditionArmorRenderer(context, metal + "_" + style));
			return (pose, collector, stack, state, slot, light, vanillaModel) -> {
				String style = ArmorStyles.style(stack);
				// Shadow Cloak: Skip the whole armor submission, including trim and glint, while the wearer is invisible.
				if (style.equals("shadowblade") && state.isInvisible) return;
				styles.get(style).render(pose, collector, stack, state, slot, light, vanillaModel);
			};
		}, items);
	}
}
