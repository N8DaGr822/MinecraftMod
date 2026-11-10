package darkspawn.black.legendaryarmor.client;

import darkspawn.black.core.client.ArmorGeometryRenderer;
import darkspawn.black.legendaryarmor.LegendaryArmor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

public final class LegendaryArmorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LegendaryArmor.SETS.forEach((name, pieces) -> ArmorGeometryRenderer.register(name, ArmorGeometryRenderer.NETHERITE_TRIM, pieces.values().toArray(ItemLike[]::new)));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			String legendary = LegendaryArmor.family(stack);
			if (!legendary.isEmpty()) lines.add(Component.translatable("tooltip.darkspawn.legendary." + legendary).withStyle(ChatFormatting.GOLD));
		});
	}
}
