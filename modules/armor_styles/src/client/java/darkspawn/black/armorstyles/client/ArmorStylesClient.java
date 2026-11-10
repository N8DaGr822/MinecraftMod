package darkspawn.black.armorstyles.client;

import darkspawn.black.armorstyles.ArmorStyles;
import darkspawn.black.core.client.ArmorGeometryRenderer;
import java.util.HashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class ArmorStylesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		register("iron", ArmorGeometryRenderer.IRON_TRIM, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		register("diamond", ArmorGeometryRenderer.DIAMOND_TRIM, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!ArmorStyles.supports(stack)) return;
			lines.add(Component.translatable("tooltip.darkspawn.armor_style", Component.translatable("armor_style.darkspawn." + ArmorStyles.style(stack))).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("tooltip.darkspawn.style_bonus." + ArmorStyles.style(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
		});
	}

	private static void register(String metal, String trimAsset, ItemLike... items) {
		ArmorRenderer.register(context -> {
			var styles = new HashMap<String, ArmorGeometryRenderer>();
			for (String style : ArmorStyles.STYLES) styles.put(style, new ArmorGeometryRenderer(context, metal + "_" + style, trimAsset));
			return (pose, collector, stack, state, slot, light, vanillaModel) -> {
				String style = ArmorStyles.style(stack);
				// Shadow Cloak: Skip the whole armor submission, including trim and glint, while the wearer is invisible.
				if (style.equals("shadowblade") && state.isInvisible) return;
				styles.get(style).render(pose, collector, stack, state, slot, light, vanillaModel);
			};
		}, items);
	}
}
