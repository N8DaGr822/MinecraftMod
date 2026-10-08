package darkspawn.black.ecosystem;

import darkspawn.black.Darkspawn;
import darkspawn.black.boss.TreeSpiritSummonItem;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ForestEcosystem {
	public static final Map<ForestMob.Kind, EntityType<ForestMob>> MOBS = createMobs();

	private ForestEcosystem() { }

	private static Map<ForestMob.Kind, EntityType<ForestMob>> createMobs() {
		var result = new EnumMap<ForestMob.Kind, EntityType<ForestMob>>(ForestMob.Kind.class);
		for (ForestMob.Kind kind : ForestMob.Kind.values()) {
			var key = ResourceKey.create(Registries.ENTITY_TYPE, Darkspawn.id(kind.id));
			var builder = EntityType.Builder.<ForestMob>of((type, level) -> new ForestMob(type, level, kind), kind.category())
					.sized(kind.width, kind.height).clientTrackingRange(10).noLootTable();
			if (kind != ForestMob.Kind.BARKLING) { builder.notInPeaceful(); }
			result.put(kind, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key)));
		}
		return Map.copyOf(result);
	}

	public static void initialize() {
		MOBS.forEach((kind, type) -> {
			FabricDefaultAttributeRegistry.register(type, ForestMob.attributes(kind));
			SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, level, reason, pos, random) -> canSpawn(kind, entityType, level, reason, pos, random));
			BiomeModifications.addSpawn(BiomeSelectors.tag(TreeSpiritSummonItem.FORESTS), kind.category(), type,
					kind == ForestMob.Kind.ANCIENT_ENT ? 1 : kind == ForestMob.Kind.BARKLING ? 12 : 8,
					1, kind == ForestMob.Kind.ANCIENT_ENT ? 1 : kind == ForestMob.Kind.BARKLING ? 3 : 2);
		});
	}

	static boolean canSpawn(ForestMob.Kind kind, EntityType<ForestMob> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		// Ecosystem Progression: The End's saved fight state unlocks all players and existing forest chunks.
		var end = level.getLevel().getServer().getLevel(Level.END);
		if (level.getLevel().dimension() != Level.OVERWORLD || end == null || end.getDragonFight() == null
				|| !end.getDragonFight().hasPreviouslyKilledDragon() || !level.getBiome(pos).is(TreeSpiritSummonItem.FORESTS)) {
			return false;
		}
		var ground = level.getBlockState(pos.below());
		if (!(ground.is(BlockTags.DIRT) || ground.is(Blocks.MOSS_BLOCK))
				|| pos.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1) {
			return false;
		}
		return kind == ForestMob.Kind.BARKLING
				? level.getMaxLocalRawBrightness(pos) > 8 && Monster.checkMobSpawnRules(type, level, reason, pos, random)
				: level.getDifficulty() != Difficulty.PEACEFUL && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
	}
}
