package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import darkspawn.black.core.Progress;
import darkspawn.black.health.BossHearts;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Boss-owned cuisine: the 16 boss ingredients, 16 trophies, defeat rewards and heart progress (moved out of cooking). */
public final class BossCuisine {
	public static final Map<BossKind, Item> INGREDIENTS = new EnumMap<>(BossKind.class);
	public static final Map<BossKind, Item> TROPHIES = new EnumMap<>(BossKind.class);
	private BossCuisine() {}

	private static Item register(String name, Function<Item.Properties, Item> factory) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	private static Item trophy(String name) {
		var block = Registry.register(BuiltInRegistries.BLOCK, Darkspawn.id(name), new TrophyBlock(BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, Darkspawn.id(name))).strength(1).noOcclusion().sound(SoundType.WOOD)));
		BlockItem item = (BlockItem) register(name, p -> new BlockItem(block, p.useBlockDescriptionPrefix()));
		item.registerBlocks(Item.BY_BLOCK, item);
		return item;
	}

	public static void initialize() {
		INGREDIENTS.put(BossKind.ANCIENT_TREE_SPIRIT, register("ancient_sap", Item::new));
		INGREDIENTS.put(BossKind.MUTANT_WOLF, register("prime_beast_meat", Item::new));
		INGREDIENTS.put(BossKind.MUTANT_ZOMBIE, register("restored_marrow", Item::new));
		INGREDIENTS.put(BossKind.FOSSIL_TYRANT, register("marrow_extract", Item::new));
		INGREDIENTS.put(BossKind.THUNDER_BIRD, register("charged_yolk", Item::new));
		INGREDIENTS.put(BossKind.TITAN_BOA, register("titan_meat", Item::new));
		INGREDIENTS.put(BossKind.BABA_YAGA, register("witch_spice", Item::new));
		INGREDIENTS.put(BossKind.MOUNTAIN_TITAN, register("mineral_salt", Item::new));
		INGREDIENTS.put(BossKind.ICE_WYRM, register("wyrm_fat", Item::new));
		INGREDIENTS.put(BossKind.KRAKEN, register("kraken_meat", Item::new));
		INGREDIENTS.put(BossKind.CAVE_CRAWLER, register("cave_truffle", Item::new));
		INGREDIENTS.put(BossKind.SHADOW_CREEPER_QUEEN, register("royal_jelly", Item::new));
		INGREDIENTS.put(BossKind.MYCELIAL_SOVEREIGN, register("sovereign_cap", Item::new));
		INGREDIENTS.put(BossKind.NETHERBORN, register("infernal_marrow", Item::new));
		INGREDIENTS.put(BossKind.SOULBOUND_COLOSSUS, register("soul_salt", Item::new));
		INGREDIENTS.put(BossKind.VOID_EYE, register("void_essence", Item::new));
		for (BossKind kind : BossKind.values()) TROPHIES.put(kind, trophy(kind.id() + "_trophy"));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> INGREDIENTS.values().forEach(entries::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> TROPHIES.values().forEach(entries::accept));
		// Winter armor blocks freeze damage (the WARMING meal adds its own check in cooking).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, damage) ->
			!source.is(DamageTypes.FREEZE) || !EquipmentPowers.wears(entity, "armor_winter"));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> hearts(handler.player));
	}

	public static void hearts(ServerPlayer player) {
		var hearts = BossHearts.progress(player).savedBosses();
		for (String boss : hearts) Progress.award(player, "heart_" + boss, "earned");
		if (hearts.size() >= 8) Progress.award(player, "heart_collector", "earned");
		if (hearts.size() >= 15) Progress.award(player, "beyond_mortal", "earned");
	}

	/** Only real bosses call this in the one-time death-loot path; helpers and abandonment never do. */
	public static void grant(LivingEntity boss, ServerLevel level, Set<UUID> participants, BossKind kind, int range) {
		for (UUID id : participants) {
			var player = level.getServer().getPlayerList().getPlayer(id);
			if (player == null || !player.isAlive() || player.isSpectator() || player.level() != level || boss.distanceToSqr(player) > range * range) continue;
			List<ItemStack> rewards = boss.entityTags().contains(AwakenedRematches.TAG)
				? List.of(AwakenedRematches.trophy(kind), AwakenedRematches.cosmetic(kind))
				: List.of(new ItemStack(INGREDIENTS.get(kind)), new ItemStack(TROPHIES.get(kind)));
			for (var stack : rewards) {
				ItemEntity reward = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
				reward.setTarget(id);
				level.addFreshEntity(reward);
			}
			if (boss.entityTags().contains(AwakenedRematches.TAG)) continue;
			Progress.award(player, "defeat_" + kind.id(), "earned");
			Progress.award(player, "conqueror", kind.id());
		}
	}
}
