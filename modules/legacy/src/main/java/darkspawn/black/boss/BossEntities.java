package darkspawn.black.boss;

import darkspawn.black.Darkspawn;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class BossEntities {
	// Boss Health: A separate synced attribute keeps large encounters above vanilla's 1,024 HP limit without changing other entities.
	public static final Holder<Attribute> MAX_HEALTH = Registry.registerForHolder(
			BuiltInRegistries.ATTRIBUTE, Darkspawn.id("boss_max_health"),
			new RangedAttribute("attribute.darkspawn.boss_max_health", 20, 1, 100000).setSyncable(true));
	public static final EntityType<AncientTreeSpirit> TREE_SPIRIT = register("ancient_tree_spirit",
			EntityType.Builder.of(AncientTreeSpirit::new, MobCategory.MONSTER).sized(16, 28).eyeHeight(12 * AncientTreeSpirit.MODEL_SCALE)
					.clientTrackingRange(12).updateInterval(2).notInPeaceful());
	public static final EntityType<HeartwoodSapling> HEARTWOOD_SAPLING = register("heartwood_sapling",
			EntityType.Builder.of(HeartwoodSapling::new, MobCategory.MONSTER).sized(1.6F, 2.88F)
					.clientTrackingRange(10).notInPeaceful().noLootTable());
	public static final EntityType<SpiritSeed> SPIRIT_SEED = register("spirit_seed",
			EntityType.Builder.<SpiritSeed>of(SpiritSeed::new, MobCategory.MISC).sized(0.5F, 0.5F)
					.clientTrackingRange(10).updateInterval(2).noLootTable());
	public static final EntityType<MutantWolf> MUTANT_WOLF = register("mutant_wolf",
			EntityType.Builder.of(MutantWolf::new, MobCategory.MONSTER).sized(10, 9).eyeHeight(6)
					.clientTrackingRange(12).updateInterval(1).notInPeaceful());
	public static final EntityType<FrostWolf> FROST_WOLF = register("frost_wolf",
			EntityType.Builder.of(FrostWolf::new, MobCategory.MONSTER).sized(1.5F, 1.35F)
					.clientTrackingRange(10).notInPeaceful().noLootTable());
	public static final EntityType<FrostShard> FROST_SHARD = register("frost_shard",
			EntityType.Builder.<FrostShard>of(FrostShard::new, MobCategory.MISC).sized(0.5F, 0.5F)
					.clientTrackingRange(10).updateInterval(2).noLootTable());

	public static final EntityType<Endborn> ENDBORN = register("endborn",
			EntityType.Builder.of(Endborn::new, MobCategory.MONSTER).sized(1.4F, 4.6F).clientTrackingRange(10).notInPeaceful().noLootTable());
	public static final Map<BossProfile, EntityType<BiomeBoss>> BIOME_BOSSES = createBosses();
	public static final Map<BossProfile, EntityType<BossMinion>> MINIONS = createMinions();
	public static final EntityType<BossBolt> BOSS_BOLT = register("boss_bolt",
			EntityType.Builder.<BossBolt>of(BossBolt::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(12).updateInterval(2).noLootTable());
	public static final EntityType<BossHazard> BOSS_HAZARD = register("boss_hazard",
			EntityType.Builder.<BossHazard>of(BossHazard::new, MobCategory.MISC).sized(12, 1).clientTrackingRange(12).noLootTable());
	public static final EntityType<VoidPlatform> VOID_PLATFORM = register("void_platform",
			EntityType.Builder.<VoidPlatform>of(VoidPlatform::new, MobCategory.MISC).sized(8, 1).clientTrackingRange(12).noLootTable());

	private static Map<BossProfile, EntityType<BiomeBoss>> createBosses() {
		var result = new EnumMap<BossProfile, EntityType<BiomeBoss>>(BossProfile.class);
		for (BossProfile profile : BossProfile.values()) {
			var builder = EntityType.Builder.<BiomeBoss>of((type, level) -> new BiomeBoss(type, level, profile), MobCategory.MONSTER)
					.sized(profile.width, profile.height).eyeHeight(profile.height * 0.65F).clientTrackingRange(12).updateInterval(2).notInPeaceful();
			if (profile.fireproof()) { builder.fireImmune(); }
			result.put(profile, register(profile.id(), builder));
		}
		return Map.copyOf(result);
	}
	private static Map<BossProfile, EntityType<BossMinion>> createMinions() {
		var result = new EnumMap<BossProfile, EntityType<BossMinion>>(BossProfile.class);
		for (BossProfile profile : BossProfile.values()) {
			var builder = EntityType.Builder.<BossMinion>of((type, level) -> new BossMinion(type, level, profile), MobCategory.MONSTER)
					.sized(profile == BossProfile.KRAKEN ? 3 : profile.width * 0.18F, profile == BossProfile.KRAKEN ? 12 : profile.height * 0.18F)
					.clientTrackingRange(12).notInPeaceful().noLootTable();
			if (profile.fireproof()) { builder.fireImmune(); }
			result.put(profile, register(profile.id() + "_minion", builder));
		}
		return Map.copyOf(result);
	}

	private BossEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(TREE_SPIRIT, AncientTreeSpirit.attributes());
		FabricDefaultAttributeRegistry.register(HEARTWOOD_SAPLING, HeartwoodSapling.attributes());
		FabricDefaultAttributeRegistry.register(MUTANT_WOLF, MutantWolf.attributes());
		FabricDefaultAttributeRegistry.register(FROST_WOLF, FrostWolf.attributes());
		FabricDefaultAttributeRegistry.register(ENDBORN, Endborn.attributes());
		BIOME_BOSSES.forEach((profile, type) -> FabricDefaultAttributeRegistry.register(type, BiomeBoss.attributes(profile)));
		MINIONS.forEach((profile, type) -> FabricDefaultAttributeRegistry.register(type, BossMinion.attributes()));
	}
}
