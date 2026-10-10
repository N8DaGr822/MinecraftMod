package darkspawn.black.cooking;

import com.google.common.collect.ImmutableSet;
import darkspawn.black.Darkspawn;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

public final class VillageKitchen {
	public static final ResourceKey<VillagerProfession> CHEF = ResourceKey.create(Registries.VILLAGER_PROFESSION, Darkspawn.id("chef"));
	private VillageKitchen() {}
	public static void initialize() {
		var site = PoiHelper.register(Darkspawn.id("chef"), 1, 1, Cooking.STATION);
		var trades = new Int2ObjectOpenHashMap<ResourceKey<TradeSet>>();
		for (int i = 1; i <= 5; i++) trades.put(i, ResourceKey.create(Registries.TRADE_SET, Darkspawn.id("chef/level_" + i)));
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, CHEF, new VillagerProfession(
			Component.translatable("entity.darkspawn.chef"), holder -> holder.value() == site, holder -> holder.value() == site,
			ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_BUTCHER, trades));
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (!source.isBuiltin()) return;
			String scroll = switch (key.identifier().toString()) {
				case "minecraft:chests/ancient_city" -> "shadow_stew";
				case "minecraft:chests/bastion_treasure" -> "infernal_roast";
				case "minecraft:chests/shipwreck_supply" -> "captains_chowder";
				case "minecraft:chests/jungle_temple" -> "jungle_pepper_skewer";
				case "minecraft:chests/abandoned_mineshaft" -> "shadow_stew";
				case "minecraft:chests/igloo_chest" -> "frost_garlic_soup";
				case "minecraft:chests/simple_dungeon" -> "marsh_rice_bowl";
				case "minecraft:gameplay/fishing/treasure" -> "captains_chowder";
				default -> null;
			};
			if (scroll != null) builder.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(Cuisine.SCROLLS.get(scroll)))
				.when(LootItemRandomChanceCondition.randomChance(0.5F)));
		});
	}
}
