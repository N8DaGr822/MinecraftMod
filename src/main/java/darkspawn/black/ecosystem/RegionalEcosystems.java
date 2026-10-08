package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.BossProfile;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RegionalEcosystems {
	public static final Map<RegionalKind, EntityType<RegionalMob>> MOBS = createMobs();
	public static final Map<RegionalKind, Item> REWARDS = createRewards();
	public static final Item STORMFORGED_FEATHER = registerItem("stormforged_feather", false);

	private RegionalEcosystems() { }

	private static Map<RegionalKind, EntityType<RegionalMob>> createMobs() {
		var result = new EnumMap<RegionalKind, EntityType<RegionalMob>>(RegionalKind.class);
		for (var kind : RegionalKind.values()) {
			var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(kind.id()));
			var builder = EntityType.Builder.<RegionalMob>of((type, level) -> new RegionalMob(type, level, kind), kind.category())
					.sized(kind.width, kind.height).clientTrackingRange(10).noLootTable();
			if (kind.temper == RegionalKind.Temper.HOSTILE) { builder.notInPeaceful(); }
			if (kind.region.fireproof()) { builder.fireImmune(); }
			result.put(kind, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key)));
		}
		return Map.copyOf(result);
	}

	private static Item registerItem(String id, boolean feather) {
		var key = ResourceKey.create(Registries.ITEM, Darkspawn.id(id));
		var properties = new Item.Properties().setId(key);
		return Registry.register(BuiltInRegistries.ITEM, key, feather ? new ChargedFeatherItem(properties) : new Item(properties));
	}

	private static Map<RegionalKind, Item> createRewards() {
		var result = new EnumMap<RegionalKind, Item>(RegionalKind.class);
		for (var kind : RegionalKind.values()) {
			if (kind.herald()) { result.put(kind, registerItem(kind.rewardId(), kind == RegionalKind.THUNDER_ROC)); }
		}
		return Map.copyOf(result);
	}

	public static void initialize() {
		MOBS.forEach((kind, type) -> {
			FabricDefaultAttributeRegistry.register(type, RegionalMob.attributes(kind));
			var placement = kind.movement == RegionalKind.Move.WATER ? SpawnPlacementTypes.IN_WATER
					: kind.movement == RegionalKind.Move.AIR ? SpawnPlacementTypes.NO_RESTRICTIONS : SpawnPlacementTypes.ON_GROUND;
			SpawnPlacements.register(type, placement, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, level, reason, pos, random) -> canSpawn(kind, level, pos));
			var selector = kind.region == BossProfile.CAVE_CRAWLER
					? BiomeSelectors.foundInOverworld().and(BiomeSelectors.includeByKey(Biomes.DEEP_DARK).negate())
					: BiomeSelectors.tag(kind.region.biomes());
			if (kind.region == BossProfile.NETHERBORN) {
				selector = switch (netherVariant(kind)) {
					case 0 -> BiomeSelectors.includeByKey(Biomes.CRIMSON_FOREST);
					case 1 -> BiomeSelectors.includeByKey(Biomes.WARPED_FOREST);
					default -> BiomeSelectors.includeByKey(Biomes.NETHER_WASTES, Biomes.BASALT_DELTAS);
				};
			}
			int group = switch (kind) { case BONE_RAPTOR, CAVE_SKITTERER, ICEFANG, VOIDLING -> 3; default -> kind.herald() ? 1 : 2; };
			BiomeModifications.addSpawn(selector, kind.category(), type, kind.weight, 1, group);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			for (var kind : RegionalKind.values()) { if (kind.herald()) { entries.accept(REWARDS.get(kind)); } }
			entries.accept(STORMFORGED_FEATHER);
		});
	}

	static int netherVariant(RegionalKind kind) {
		return switch (kind) {
			case CRIMSON_RAVAGER, FUNGAL_IMP, BLOODROOT, CRIMSON_SPAWN -> 0;
			case WARPED_STALKER, WARPED_WISP, RIFTLING, WARPED_SPAWN -> 1;
			default -> 2;
		};
	}

	static boolean canSpawn(RegionalKind kind, ServerLevelAccessor level, BlockPos pos) {
		var end = level.getLevel().getServer().getLevel(Level.END);
		// Shared Progression: Natural ecosystems unlock worldwide; command summons remain available to builders.
		if (!level.getLevel().dimension().equals(kind.region.dimension()) || end == null || end.getDragonFight() == null
				|| !end.getDragonFight().hasPreviouslyKilledDragon()
				|| kind.temper == RegionalKind.Temper.HOSTILE && level.getDifficulty() == Difficulty.PEACEFUL) { return false; }
		var biome = level.getBiome(pos);
		// Habitat Coverage: Height maps update with block placement; skylight propagation may still be queued.
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		boolean covered = pos.getY() < surface - 1;
		boolean cave = kind.region == BossProfile.CAVE_CRAWLER && pos.getY() < 48 && covered && !biome.is(Biomes.DEEP_DARK);
		if (!(biome.is(kind.region.biomes()) || cave)) { return false; }
		if (kind.region == BossProfile.NETHERBORN) {
			boolean variant = switch (netherVariant(kind)) {
				case 0 -> biome.is(Biomes.CRIMSON_FOREST);
				case 1 -> biome.is(Biomes.WARPED_FOREST);
				default -> biome.is(Biomes.NETHER_WASTES) || biome.is(Biomes.BASALT_DELTAS);
			};
			if (!variant) { return false; }
		}
		if (kind.movement == RegionalKind.Move.WATER) {
			return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).is(FluidTags.WATER)
					&& level.getFluidState(pos.below()).is(FluidTags.WATER)
					&& (kind != RegionalKind.GIANT_CRAB || level.getBlockState(pos.below(2)).isSolidRender())
					&& (kind != RegionalKind.ABYSSAL_FISH || level.getMaxLocalRawBrightness(pos) <= 7)
					&& (kind != RegionalKind.LEVIATHAN_SPAWN || deepWater(level, pos));
		}
		if (!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) { return false; }
		var ground = level.getBlockState(pos.below());
		if (kind.movement != RegionalKind.Move.AIR && !(ground.isSolidRender() || ground.is(Blocks.SNOW))) { return false; }
		if (kind.region.underground()) {
			return covered && level.getMaxLocalRawBrightness(pos) <= 7;
		}
		if (kind.region.dimension().equals(Level.NETHER)) { return pos.getY() < 120; }
		if (pos.getY() < surface - 1 || pos.getY() > surface + (kind.movement == RegionalKind.Move.AIR ? 12 : 1)) { return false; }
		if (kind.region.dimension().equals(Level.END)) { return surface > level.getMinY() && pos.getY() >= 30; }
		if (kind == RegionalKind.TYRANT_SKULL) { return ground.is(BlockTags.SAND) || ground.is(BlockTags.TERRACOTTA); }
		if (kind.region == BossProfile.MYCELIAL_SOVEREIGN) { return ground.is(Blocks.MYCELIUM) || ground.is(BlockTags.DIRT); }
		if (kind == RegionalKind.SWAMP_WISP || kind.temper == RegionalKind.Temper.HOSTILE) { return level.getMaxLocalRawBrightness(pos) <= 7; }
		return level.getMaxLocalRawBrightness(pos) > 8;
	}
	private static boolean deepWater(ServerLevelAccessor level, BlockPos pos) {
		// Ocean Depth: Measure water overhead, so custom sea levels and flooded terrain use the same habitat rule.
		for (int depth = 1; depth <= 8; depth++) { if (!level.getFluidState(pos.above(depth)).is(FluidTags.WATER)) { return false; } }
		return true;
	}
}
