package darkspawn.black.client;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.CreatureAssets;
import darkspawn.black.client.ecosystem.CreatureGeoRenderer;
import darkspawn.black.cooking.Cooking;
import darkspawn.black.cooking.MealItem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

public class DarkspawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        for(int i=0;i<darkspawn.black.cooking.RegionalFishing.TYPES.size();i++){
            String id=darkspawn.black.cooking.RegionalFishing.IDS[i];
            net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.cooking.RegionalFishing.TYPES.get(i),context->new net.minecraft.client.renderer.entity.CodRenderer(context){
                @Override public net.minecraft.resources.Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state){return Darkspawn.id("textures/entity/fish/"+id+".png");}
            });
        }
		TemperatureHud.initialize();
		OneBlockControls.initialize();
        net.minecraft.client.renderer.entity.EntityRenderers.register(darkspawn.black.oneblock.AscensionGuardian.TYPE,AscensionGuardianRenderer::new);
		JournalControls.initialize();
		LegacyArmorRenderers.initialize();
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
			if (stack.getItem() instanceof MealItem meal) {
				lines.add(MealItem.nutritionDescription(stack).copy().withStyle(ChatFormatting.GRAY));
				lines.add(meal.effectDescription().copy().withStyle(ChatFormatting.GREEN));
				lines.add(meal.effectDetail().copy().withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.darkspawn.meal_replaces").withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
