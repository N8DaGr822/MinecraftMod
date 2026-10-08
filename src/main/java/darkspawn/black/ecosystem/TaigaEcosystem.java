package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.MutantWolfSummonItem;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class TaigaEcosystem {
	public static final Map<TaigaWolf.Kind, EntityType<TaigaWolf>> WOLVES = createWolves();

	private TaigaEcosystem() { }

	private static Map<TaigaWolf.Kind, EntityType<TaigaWolf>> createWolves() {
		var result = new EnumMap<TaigaWolf.Kind, EntityType<TaigaWolf>>(TaigaWolf.Kind.class);
		for (TaigaWolf.Kind kind : TaigaWolf.Kind.values()) {
			var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(kind.id));
			var builder = EntityType.Builder.<TaigaWolf>of((type, level) -> new TaigaWolf(type, level, kind), MobCategory.MONSTER)
					.sized(kind.width, kind.height).clientTrackingRange(10).notInPeaceful().noLootTable();
			result.put(kind, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key)));
		}
		return Map.copyOf(result);
	}

	public static void initialize() {
		var snowy = BiomeSelectors.includeByKey(Biomes.SNOWY_TAIGA);
		WOLVES.forEach((kind, type) -> {
			FabricDefaultAttributeRegistry.register(type, TaigaWolf.attributes(kind));
			SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, level, reason, pos, random) -> canSpawn(kind, entityType, level, reason, pos, random));
			BiomeModifications.addSpawn(BiomeSelectors.tag(MutantWolfSummonItem.TAIGAS).and(snowy.negate()), MobCategory.MONSTER,
					type, kind.weight, kind == TaigaWolf.Kind.DIRE ? 2 : 1, kind == TaigaWolf.Kind.DIRE ? 3 : kind == TaigaWolf.Kind.ALPHA ? 1 : 2);
			BiomeModifications.addSpawn(BiomeSelectors.tag(MutantWolfSummonItem.TAIGAS).and(snowy), MobCategory.MONSTER,
					type, kind == TaigaWolf.Kind.FROSTFANG ? 12 : kind.weight, 1, kind == TaigaWolf.Kind.ALPHA ? 1 : 2);
		});
	}

	static boolean canSpawn(TaigaWolf.Kind kind, EntityType<TaigaWolf> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		var end = level.getLevel().getServer().getLevel(Level.END);
		if (level.getLevel().dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
				|| end == null || end.getDragonFight() == null || !end.getDragonFight().hasPreviouslyKilledDragon()
				|| !level.getBiome(pos).is(MutantWolfSummonItem.TAIGAS)) { return false; }
		var ground = level.getBlockState(pos.below());
		boolean surface = ground.is(BlockTags.DIRT) || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.SNOW_BLOCK)
				|| ground.is(Blocks.SNOW) || kind == TaigaWolf.Kind.FROSTFANG && ground.is(Blocks.POWDER_SNOW);
		return surface && pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
	}
}
