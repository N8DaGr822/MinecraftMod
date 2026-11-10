package darkspawn.black.client;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.CreatureRoster;
import darkspawn.black.creaturekit.client.CreatureGeoRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class DarkspawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		OneBlockControls.initialize();
        net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.oneblock.AscensionGuardian.TYPE,AscensionGuardianRenderer::new);
		JournalControls.initialize();
		LegacyArmorRenderers.initialize();
		darkspawn.black.client.pirate.ShipClient.initialize();
		darkspawn.black.client.boss.BossClient.initialize();
		darkspawn.black.ecosystem.ForestEcosystem.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureRoster.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "forest_creatures") : new darkspawn.black.client.ecosystem.ForestMobRenderer(context, kind)));
		darkspawn.black.ecosystem.TaigaEcosystem.WOLVES.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureRoster.MOBS.contains(kind.id) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, "mutant_wolf") : new darkspawn.black.client.ecosystem.TaigaWolfRenderer(context, kind)));
		MenuScreens.register(darkspawn.black.forsaken.Forsaken.MENU, QuarantineScreen::new);
		darkspawn.black.forsaken.ForsakenContent.MOBS.forEach((kind, type) ->
			net.minecraft.client.renderer.entity.EntityRenderers.register(type, context -> new CreatureGeoRenderer<>(context, type, kind.width / 3, kind.id)));
		net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.forsaken.ForsakenContent.SPORE,
			net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		darkspawn.black.ecosystem.RegionalEcosystems.MOBS.forEach((kind, type) ->
				net.minecraft.client.renderer.entity.EntityRenderers.register(type,
						context -> CreatureRoster.MOBS.contains(kind.id()) ? new CreatureGeoRenderer<>(context, type, kind.width / 3, CreatureRoster.texture(kind.id(), kind.region.id())) : new darkspawn.black.client.ecosystem.RegionalMobRenderer(context, kind)));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.BLADE)) lines.add(Component.translatable("tooltip.darkspawn.pulse_blade").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.BEACON_ITEM)) lines.add(Component.translatable("tooltip.darkspawn.containment_beacon").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.SCANNER)) lines.add(Component.translatable("message.darkspawn.forsaken_scan_mode", Component.translatable("scanner.darkspawn.mode_" + darkspawn.black.forsaken.NestScannerItem.mode(stack))).withStyle(ChatFormatting.AQUA));
			if (darkspawn.black.forsaken.ForsakenContent.ARMOR.containsValue(stack.getItem())) lines.add(Component.translatable("tooltip.darkspawn.quarantine").withStyle(ChatFormatting.GREEN));
			if (stack.is(darkspawn.black.forsaken.ForsakenContent.SCANNER)) lines.add(Component.translatable("tooltip.darkspawn.bio_scanner").withStyle(ChatFormatting.GREEN));
		});
	}
}
