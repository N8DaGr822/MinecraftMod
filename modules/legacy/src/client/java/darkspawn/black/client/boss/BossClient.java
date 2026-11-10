package darkspawn.black.client.boss;

import darkspawn.black.boss.BossEmpowerment;
import darkspawn.black.boss.BossEntities;
import darkspawn.black.boss.BossItems;
import darkspawn.black.boss.BossProfile;
import darkspawn.black.boss.CreatureAssets;
import darkspawn.black.client.ecosystem.CreatureGeoRenderer;
import darkspawn.black.health.BossHeartItem;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;

public final class BossClient {
	private BossClient() {
	}

	public static void initialize() {
		EntityRenderers.register(BossEntities.TREE_SPIRIT, AncientTreeSpiritRenderer::new);
		if (CreatureAssets.MINIONS.contains("heartwood_sapling")) { EntityRenderers.register(BossEntities.HEARTWOOD_SAPLING, context -> new CreatureGeoRenderer<>(context, BossEntities.HEARTWOOD_SAPLING, .6F, "forest_creatures")); }
		else { EntityRenderers.register(BossEntities.HEARTWOOD_SAPLING, TreeSpiritRenderer::new); }
		EntityRenderers.register(BossEntities.SPIRIT_SEED, context -> new ThrownItemRenderer<>(context, 2, true));
		EntityRenderers.register(BossEntities.MUTANT_WOLF, MutantWolfGeoRenderer::new);
		if (CreatureAssets.MINIONS.contains("frost_wolf")) { EntityRenderers.register(BossEntities.FROST_WOLF, context -> new CreatureGeoRenderer<>(context, BossEntities.FROST_WOLF, .6F, "mutant_wolf")); }
		else { EntityRenderers.register(BossEntities.FROST_WOLF, MutantWolfRenderer::new); }
		EntityRenderers.register(BossEntities.FROST_SHARD, context -> new ThrownItemRenderer<>(context, 2, true));
		BossEntities.BIOME_BOSSES.forEach((profile, type) -> {
			if (profile == BossProfile.MUTANT_ZOMBIE) { EntityRenderers.register(type, MutantZombieRenderer::new); }
			else if (CreatureAssets.BOSSES.contains(profile.id())) { EntityRenderers.register(type, context -> new BiomeBossGeoRenderer<>(context, profile)); }
			else { EntityRenderers.register(type, context -> new BiomeBossRenderer(context, profile)); }
		});
		BossEntities.MINIONS.forEach((profile, type) -> {
			if (CreatureAssets.MINIONS.contains(profile.id() + "_minion")) { EntityRenderers.register(type, context -> new CreatureGeoRenderer<>(context, type, .6F, profile.id())); }
			else { EntityRenderers.register(type, context -> new BiomeBossRenderer(context, profile)); }
		});
		EntityRenderers.register(BossEntities.BOSS_BOLT, context -> new ThrownItemRenderer<>(context, 2, true));
		EntityRenderers.register(BossEntities.BOSS_HAZARD, BossHazardRenderer::new);
		EntityRenderers.register(BossEntities.VOID_PLATFORM, VoidPlatformRenderer::new);
		EntityRenderers.register(BossEntities.ENDBORN, EndbornRenderer::new);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof BossHeartItem) {
				lines.add(Component.translatable("tooltip.darkspawn.boss_heart"));
			} else if (stack.is(BossItems.ANCIENT_HEARTWOOD)) {
				lines.add(Component.translatable("tooltip.darkspawn.ancient_heartwood"));
			} else if (stack.is(BossItems.MOONLIT_FANG)) {
				lines.add(Component.translatable("tooltip.darkspawn.moonlit_fang"));
			} else if (stack.is(BossItems.ALPHA_FANG)) {
				lines.add(Component.translatable("tooltip.darkspawn.alpha_fang"));
			} else if (stack.is(BossItems.LIVING_HEARTWOOD)) {
				lines.add(Component.translatable("tooltip.darkspawn.living_heartwood"));
			}
			if (stack.getItem() instanceof darkspawn.black.boss.BiomeBossSummonItem summon) {
				lines.add(Component.translatable("tooltip.darkspawn." + summon.profile().id() + "_sigil"));
			}
			for (var entry : BossItems.ESSENCES.entrySet()) {
				if (stack.is(entry.getValue())) { lines.add(Component.translatable("tooltip.darkspawn." + entry.getKey().id() + "_essence")); }
			}
			if (stack.is(BossItems.ENDBORN_SHARD)) { lines.add(Component.translatable("tooltip.darkspawn.endborn_shard")); }
			String power = stack.get(BossEmpowerment.POWER);
			if (power != null && BossEmpowerment.isKnownPower(power) && !BossEmpowerment.ROOTBOUND.equals(power) && !BossEmpowerment.PREDATORS_RUSH.equals(power)) {
				lines.add(Component.translatable("tooltip.darkspawn." + power));
			}
			if (BossEmpowerment.ROOTBOUND.equals(stack.get(BossEmpowerment.POWER))) {
				lines.add(Component.translatable("tooltip.darkspawn.rootbound"));
			} else if (BossEmpowerment.PREDATORS_RUSH.equals(stack.get(BossEmpowerment.POWER))) {
				lines.add(Component.translatable("tooltip.darkspawn.predators_rush"));
			}
		});
	}
}
