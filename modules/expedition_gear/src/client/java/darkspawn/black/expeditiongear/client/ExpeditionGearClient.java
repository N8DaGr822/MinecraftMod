package darkspawn.black.expeditiongear.client;

import darkspawn.black.core.client.ArmorGeometryRenderer;
import darkspawn.black.expeditiongear.ExpeditionArmor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;

public final class ExpeditionGearClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ArmorGeometryRenderer.register("mountaineering", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.MOUNTAINEERING.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("spelunking", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.SPELUNKING.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("diver", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.DIVER.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("desert_wanderer", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.DESERT_WANDERER.values().toArray(ItemLike[]::new));
		ArmorGeometryRenderer.register("ashwalker", ArmorGeometryRenderer.IRON_TRIM, ExpeditionArmor.ASHWALKER.values().toArray(ItemLike[]::new));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.is(ExpeditionArmor.DIVER.get(ArmorType.HELMET))) {
				lines.add(Component.translatable("tooltip.darkspawn.diver_helmet").withStyle(ChatFormatting.GRAY));
			}
			if (ExpeditionArmor.DESERT_WANDERER.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.desert_wanderer_armor").withStyle(ChatFormatting.GRAY));
			}
			if (ExpeditionArmor.ASHWALKER.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.ashwalker_armor").withStyle(ChatFormatting.GRAY));
			}
			if (ExpeditionArmor.MOUNTAINEERING.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.mountaineering_armor").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(ExpeditionArmor.SPELUNKING.get(ArmorType.HELMET))) {
				lines.add(Component.translatable("tooltip.darkspawn.spelunking_helmet").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(ExpeditionArmor.SPELUNKING.get(ArmorType.BOOTS))) {
				lines.add(Component.translatable("tooltip.darkspawn.spelunking_boots").withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
