package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import darkspawn.black.health.BossHeartItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class BossItems {
	public static final Item ANCIENT_HEARTWOOD = register("ancient_heartwood", TreeSpiritSummonItem::new);
	public static final Item LIVING_HEARTWOOD = register("living_heartwood", Item::new);
	public static final Item MOONLIT_FANG = register("moonlit_fang", MutantWolfSummonItem::new);
	public static final Item ALPHA_FANG = register("alpha_fang", Item::new);
	public static final Map<BossKind, Item> HEARTS = createHearts();

	// Save Compatibility: Old ocean hearts still redeem the same single Kraken reward.
	public static final Item LEGACY_OCEAN_HEART = register("leviathan_heart", properties -> new BossHeartItem(properties, BossKind.KRAKEN));
	public static final Item ENDBORN_SHARD = register("endborn_shard", Item::new);
	public static final Map<BossProfile, Item> SUMMONS = createSummons();
	public static final Map<BossProfile, Item> ESSENCES = createEssences();

	private static Map<BossProfile, Item> createSummons() {
		var result = new EnumMap<BossProfile, Item>(BossProfile.class);
		for (BossProfile profile : BossProfile.values()) {
			result.put(profile, register(profile.id() + "_sigil", properties -> new BiomeBossSummonItem(properties, profile)));
		}
		return Map.copyOf(result);
	}
	private static Map<BossProfile, Item> createEssences() {
		var result = new EnumMap<BossProfile, Item>(BossProfile.class);
		for (BossProfile profile : BossProfile.values()) { result.put(profile, register(profile.id() + "_essence", Item::new)); }
		return Map.copyOf(result);
	}

	private BossItems() {
	}

	private static Map<BossKind, Item> createHearts() {
		Map<BossKind, Item> hearts = new EnumMap<>(BossKind.class);
		for (BossKind boss : BossKind.values()) {
			hearts.put(boss, register(boss.id() + "_heart", properties -> new BossHeartItem(properties, boss)));
		}
		return Map.copyOf(hearts);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key).stacksTo(1)));
	}

	public static void register() {
	}
}
