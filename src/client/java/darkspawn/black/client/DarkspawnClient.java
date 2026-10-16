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
		ArmorLoadoutControls.initialize();
        InventoryHubControls.initialize();
        for(int i=0;i<darkspawn.black.cooking.RegionalFishing.TYPES.size();i++){
            String id=darkspawn.black.cooking.RegionalFishing.IDS[i];
            net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.cooking.RegionalFishing.TYPES.get(i),context->new net.minecraft.client.renderer.entity.CodRenderer(context){
                @Override public net.minecraft.resources.Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state){return Darkspawn.id("textures/entity/fish/"+id+".png");}
            });
        }
		TemperatureHud.initialize();
		OneBlockControls.initialize();
		JournalControls.initialize();
		ExpeditionArmorRenderer.initialize();
		ArmorStyleRenderer.initialize();
		darkspawn.black.client.legacy.NoahClient.initialize();
		darkspawn.black.client.mount.GriffinClient.initialize();
		darkspawn.black.client.pirate.ShipClient.initialize();
		darkspawn.black.client.boss.BossClient.initialize();
		darkspawn.black.client.audio.EncounterMusic.initialize();
		darkspawn.black.ecosystem.ForestEcosystem.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "forest_creatures") : new darkspawn.black.client.ecosystem.ForestMobRenderer(context, kind)));
		darkspawn.black.ecosystem.TaigaEcosystem.WOLVES.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "mutant_wolf") : new darkspawn.black.client.ecosystem.TaigaWolfRenderer(context, kind)));
		MenuScreens.register(Cooking.MENU, CookingScreen::new);
		MenuScreens.register(darkspawn.black.storage.Storage.MENU, StorageScreen::new);
		MenuScreens.register(darkspawn.black.forsaken.Forsaken.MENU, QuarantineScreen::new);
		darkspawn.black.forsaken.ForsakenContent.MOBS.forEach((kind, type) ->
			net.minecraft.client.renderer.entity.EntityRenderers.register(type, context -> new CreatureGeoRenderer<>(context, type, kind.width / 3, kind.id)));
		net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.forsaken.ForsakenContent.SPORE,
			net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		darkspawn.black.ecosystem.RegionalEcosystems.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureAssets.MOBS.contains(kind.id()) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, CreatureAssets.texture(kind.id(), kind.region.id())) : new darkspawn.black.client.ecosystem.RegionalMobRenderer(context, kind)));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.BLADE)) lines.add(Component.translatable("tooltip.darkspawn.pulse_blade").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.BEACON_ITEM)) lines.add(Component.translatable("tooltip.darkspawn.containment_beacon").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.SCANNER)) lines.add(Component.translatable("message.darkspawn.forsaken_scan_mode", Component.translatable("scanner.darkspawn.mode_" + darkspawn.black.forsaken.NestScannerItem.mode(stack))).withStyle(ChatFormatting.AQUA));
			if (darkspawn.black.forsaken.ForsakenContent.ARMOR.containsValue(stack.getItem())) lines.add(Component.translatable("tooltip.darkspawn.quarantine").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.SCANNER)) lines.add(Component.translatable("tooltip.darkspawn.bio_scanner").withStyle(ChatFormatting.GREEN));
			String legendary = darkspawn.black.equipment.LegendaryArmor.family(stack);
			if (!legendary.isEmpty()) lines.add(Component.translatable("tooltip.darkspawn.legendary." + legendary).withStyle(ChatFormatting.GOLD));
			if (darkspawn.black.equipment.ArmorStyles.supports(stack)) {
				lines.add(Component.translatable("tooltip.darkspawn.armor_style", Component.translatable("armor_style.darkspawn." + darkspawn.black.equipment.ArmorStyles.style(stack))).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.darkspawn.style_bonus." + darkspawn.black.equipment.ArmorStyles.style(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			if (stack.is(darkspawn.black.equipment.ExpeditionArmor.DIVER.get(net.minecraft.world.item.equipment.ArmorType.HELMET))) {
				lines.add(Component.translatable("tooltip.darkspawn.diver_helmet").withStyle(ChatFormatting.GRAY));
			}
			if (darkspawn.black.equipment.ExpeditionArmor.DESERT_WANDERER.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.desert_wanderer_armor").withStyle(ChatFormatting.GRAY));
			}
			if (darkspawn.black.equipment.ExpeditionArmor.ASHWALKER.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.ashwalker_armor").withStyle(ChatFormatting.GRAY));
			}
			if (darkspawn.black.equipment.ExpeditionArmor.MOUNTAINEERING.containsValue(stack.getItem())) {
				lines.add(Component.translatable("tooltip.darkspawn.mountaineering_armor").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(darkspawn.black.equipment.ExpeditionArmor.SPELUNKING.get(net.minecraft.world.item.equipment.ArmorType.HELMET))) {
				lines.add(Component.translatable("tooltip.darkspawn.spelunking_helmet").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(darkspawn.black.equipment.ExpeditionArmor.SPELUNKING.get(net.minecraft.world.item.equipment.ArmorType.BOOTS))) {
				lines.add(Component.translatable("tooltip.darkspawn.spelunking_boots").withStyle(ChatFormatting.GRAY));
			}
			if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
					&& blockItem.getBlock() instanceof darkspawn.black.storage.SortingTubeBlock) {
				lines.add(Component.translatable(blockItem.getBlock() == darkspawn.black.storage.Storage.TUBE
					? "storage.darkspawn.tube_hint" : "storage.darkspawn.block_hint").withStyle(ChatFormatting.GRAY));
				var contents = stack.get(darkspawn.black.storage.Storage.BANK_CONTENTS);
				if (contents != null) {
					long count = java.util.stream.Stream.concat(contents.stored().stream(), contents.trash().stream())
						.mapToLong(darkspawn.black.storage.BankContents.Entry::count).sum();
					lines.add(Component.translatable("storage.darkspawn.bank_item_hint", count, contents.tier() + 1).withStyle(ChatFormatting.GRAY));
				}
			}
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
				Screens.getWidgets(screen).add(darkspawn.black.client.UiTheme.button(Component.translatable("button.darkspawn.expanded_inventory"),
						button -> client.gui.setScreen(new ExpandedInventoryScreen(client.player)))
						.bounds(width - 116, 6, 110, 20).build());
			}
			if (!(screen instanceof InventoryScreen)) {
				return;
			}

			var trash = UiTheme.button(Component.translatable("button.darkspawn.empty_trash"), button -> {
				if (client.player != null) ClientPlayNetworking.send(new darkspawn.black.inventory.TrashInventoryPayload(client.player.inventoryMenu.getStateId()));
			}).bounds(width - 68, 30, 62, 20)
				.tooltip(Tooltip.create(Component.translatable("tooltip.darkspawn.empty_trash"))).build();
			Runnable updateTrash = () -> trash.active = canSort(client)
				&& ClientPlayNetworking.canSend(darkspawn.black.inventory.TrashInventoryPayload.TYPE)
				&& client.player.inventoryMenu.getSlot(InventoryExpansion.TRASH_MENU_SLOT).hasItem();
			updateTrash.run(); Screens.getWidgets(screen).add(trash);
			ScreenEvents.afterTick(screen).register(current -> updateTrash.run());

			// Inventory UI: Anchor outside the inventory so opening the recipe book cannot cover slots.
			Button sortButton = darkspawn.black.client.UiTheme.button(Component.translatable("button.darkspawn.sort_inventory"),
					button -> requestSort(client))
					.bounds(width - 68, 6, 62, 20)
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
