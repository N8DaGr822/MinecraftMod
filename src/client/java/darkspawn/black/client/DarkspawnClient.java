package darkspawn.black.client;

import com.mojang.blaze3d.platform.InputConstants;
import darkspawn.black.Darkspawn;
import darkspawn.black.boss.CreatureAssets;
import darkspawn.black.client.ecosystem.CreatureGeoRenderer;
import darkspawn.black.inventory.SortInventoryPayload;
import darkspawn.black.inventory.InventoryExpansion;
import darkspawn.black.inventory.InventoryTierPayload;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.cooking.MealItem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class DarkspawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		darkspawn.black.client.boss.BossClient.initialize();
		darkspawn.black.client.audio.EncounterMusic.initialize();
		darkspawn.black.ecosystem.ForestEcosystem.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "forest_creatures") : new darkspawn.black.client.ecosystem.ForestMobRenderer(context, kind)));
		darkspawn.black.ecosystem.TaigaEcosystem.WOLVES.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "mutant_wolf") : new darkspawn.black.client.ecosystem.TaigaWolfRenderer(context, kind)));
		MenuScreens.register(Cooking.MENU, CookingScreen::new);
		darkspawn.black.ecosystem.RegionalEcosystems.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id()) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, CreatureAssets.texture(kind.id(), kind.region.id())) : new darkspawn.black.client.ecosystem.RegionalMobRenderer(context, kind)));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof MealItem meal) {
				lines.add(MealItem.nutritionDescription(stack).copy().withStyle(ChatFormatting.GRAY));
				lines.add(meal.effectDescription().copy().withStyle(ChatFormatting.GREEN));
				lines.add(meal.effectDetail().copy().withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.darkspawn.meal_replaces").withStyle(ChatFormatting.GRAY));
			}
		});
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientPlayNetworking.registerGlobalReceiver(InventoryTierPayload.TYPE, (payload, context) ->
				InventoryExpansion.setTier(context.player().getInventory(), payload.tier()));
		KeyMapping sortInventory = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.darkspawn.sort_inventory", InputConstants.Type.KEYBOARD, InputConstants.KEY_R,
				KeyMapping.Category.register(Darkspawn.id("inventory"))));

		// Keyboard Input: Use the shortcut during gameplay, leaving text fields and menus alone.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (sortInventory.consumeClick()) {
				if (client.gui.screen() == null && client.gui.overlay() == null) {
					requestSort(client);
				}
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof CreativeModeInventoryScreen && ClientPlayNetworking.canSend(SortInventoryPayload.TYPE)) {
				Screens.getWidgets(screen).add(Button.builder(Component.translatable("button.darkspawn.expanded_inventory"),
						button -> client.gui.setScreen(new ExpandedInventoryScreen(client.player)))
						.bounds(width - 116, 6, 110, 20).build());
			}
			if (!(screen instanceof InventoryScreen)) {
				return;
			}

			// Inventory UI: Anchor outside the inventory so opening the recipe book cannot cover slots.
			Button sortButton = Button.builder(Component.translatable("button.darkspawn.sort_inventory"),
					button -> requestSort(client))
					.bounds(width - 86, 6, 80, 20)
					.tooltip(Tooltip.create(Component.translatable("tooltip.darkspawn.sort_inventory")))
					.build();
			sortButton.active = canSort(client);
			Screens.getWidgets(screen).add(sortButton);
			ScreenEvents.afterTick(screen).register(currentScreen -> sortButton.active = canSort(client));
		});
	}

	private static boolean canSort(Minecraft client) {
		return client.player != null && client.player.isAlive() && !client.player.isSpectator()
				&& client.player.containerMenu == client.player.inventoryMenu
				&& client.player.inventoryMenu.getCarried().isEmpty()
				&& ClientPlayNetworking.canSend(SortInventoryPayload.TYPE);
	}

	private static void requestSort(Minecraft client) {
		if (canSort(client)) {
			// Server Authority: Send only the current menu revision, never client-supplied item stacks.
			ClientPlayNetworking.send(new SortInventoryPayload(client.player.inventoryMenu.getStateId()));
		}
	}
}
