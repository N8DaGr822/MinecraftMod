package darkspawn.black.cooking.client;

import darkspawn.black.cooking.Cooking;
import darkspawn.black.cooking.MealItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public final class CookingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(Cooking.MENU, CookingScreen::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof MealItem meal) {
				lines.add(MealItem.nutritionDescription(stack).copy().withStyle(ChatFormatting.GRAY));
				lines.add(meal.effectDescription().copy().withStyle(ChatFormatting.GREEN));
				lines.add(meal.effectDetail().copy().withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.darkspawn.meal_replaces").withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
