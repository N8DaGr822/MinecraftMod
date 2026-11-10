package darkspawn.black.creaturemounts;

import darkspawn.black.mountkit.PrototypeMount;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Natural mount habitats use the same biome and placement hooks as the regional creatures. */
public final class MountSpawning {
    private MountSpawning() { }

    public static void initialize() {
        var peaks = BiomeSelectors.includeByKey(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS);
        var forest = BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA));
        var snow = BiomeSelectors.includeByKey(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA,
            Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS);
        BiomeModifications.addSpawn(peaks, MobCategory.CREATURE, CreatureMounts.GRIFFIN, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SOUL_SAND_VALLEY), MobCategory.CREATURE, CreatureMounts.GIANT_MOLE, 1, 1, 1);
        BiomeModifications.addSpawn(forest, MobCategory.CREATURE, CreatureMounts.FLYING_SQUIRREL, 4, 1, 1);
        BiomeModifications.addSpawn(snow, MobCategory.CREATURE, CreatureMounts.WOOLLY_RHINO, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SNOWY_TAIGA, Biomes.GROVE), MobCategory.CREATURE, CreatureMounts.SASQUATCH, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_NETHER), MobCategory.CREATURE, CreatureMounts.CERBERUS, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_END), MobCategory.CREATURE, CreatureMounts.WINGED_UNICORN, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.CREATURE, CreatureMounts.DRAGON_MOUNT, 1, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.CREATURE, CreatureMounts.RIDING_GOLEM, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_JUNGLE), MobCategory.CREATURE, CreatureMounts.LEOPARD, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.CREATURE, CreatureMounts.GIANT_SCORPION, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SWAMP, Biomes.MANGROVE_SWAMP), MobCategory.CREATURE, CreatureMounts.CROCODILE, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), MobCategory.WATER_CREATURE, CreatureMounts.SEAHORSE, 2, 1, 1);
        SpawnPlacements.register(CreatureMounts.SEAHORSE, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) -> canSpawn(type, level, pos));
        for (var type : java.util.List.of(CreatureMounts.GRIFFIN, CreatureMounts.FLYING_SQUIRREL, CreatureMounts.WOOLLY_RHINO,
                CreatureMounts.SASQUATCH, CreatureMounts.CERBERUS, CreatureMounts.WINGED_UNICORN, CreatureMounts.DRAGON_MOUNT, CreatureMounts.RIDING_GOLEM,
                CreatureMounts.LEOPARD, CreatureMounts.GIANT_SCORPION, CreatureMounts.CROCODILE, CreatureMounts.GIANT_MOLE)) {
            SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) -> canSpawn(entityType, level, pos)
                    && (entityType != CreatureMounts.GIANT_MOLE || random.nextInt(100) == 0));
        }
    }

    static boolean canSpawn(EntityType<?> type, ServerLevelAccessor level, BlockPos pos) {
        var dimension = level.getLevel().dimension();
        var biome = level.getBiome(pos);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        boolean surfaceFloor = pos.getY() >= surface - 1;
        boolean habitat;
        if (type == CreatureMounts.GIANT_MOLE) {
            habitat = dimension.equals(Level.NETHER) && biome.is(Biomes.SOUL_SAND_VALLEY) && pos.getY() < 120;
        } else if (type == CreatureMounts.CERBERUS) {
            habitat = dimension.equals(Level.NETHER) && biome.is(BiomeTags.IS_NETHER) && pos.getY() < 120;
        } else if (type == CreatureMounts.WINGED_UNICORN) {
            habitat = dimension.equals(Level.END) && biome.is(BiomeTags.IS_END) && surfaceFloor && pos.getY() >= 30;
        } else if (!dimension.equals(Level.OVERWORLD)) {
            return false;
        } else if (type == CreatureMounts.GRIFFIN) {
            habitat = surfaceFloor && pos.getY() >= 128 && level.canSeeSky(pos)
                && (biome.is(Biomes.JAGGED_PEAKS) || biome.is(Biomes.FROZEN_PEAKS) || biome.is(Biomes.STONY_PEAKS));
        } else if (type == CreatureMounts.FLYING_SQUIRREL) {
            habitat = surfaceFloor && (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA));
        } else if (type == CreatureMounts.WOOLLY_RHINO) {
            habitat = surfaceFloor && (biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.SNOWY_TAIGA)
                || biome.is(Biomes.GROVE) || biome.is(Biomes.SNOWY_SLOPES) || biome.is(Biomes.FROZEN_PEAKS) || biome.is(Biomes.JAGGED_PEAKS));
        } else if (type == CreatureMounts.SASQUATCH) {
            habitat = surfaceFloor && (biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.GROVE));
        } else if (type == CreatureMounts.LEOPARD) {
            habitat = surfaceFloor && biome.is(BiomeTags.IS_JUNGLE);
        } else if (type == CreatureMounts.GIANT_SCORPION) {
            habitat = surfaceFloor && biome.is(Biomes.DESERT);
        } else if (type == CreatureMounts.CROCODILE) {
            habitat = surfaceFloor && (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP))
                && nearFluid(level, pos, FluidTags.WATER);
        } else if (type == CreatureMounts.SEAHORSE) {
            habitat = biome.is(BiomeTags.IS_OCEAN) && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).is(FluidTags.WATER)
                && level.getFluidState(pos.above(2)).is(FluidTags.WATER);
        } else if (type == CreatureMounts.RIDING_GOLEM || type == CreatureMounts.DRAGON_MOUNT) {
            // Cave Mounts: Require overhead terrain, not merely low elevation beside surface lava.
            habitat = pos.getY() < 48 && surface >= pos.getY() + 8
                && (type != CreatureMounts.DRAGON_MOUNT || nearFluid(level, pos, FluidTags.LAVA));
        } else {
            return false;
        }
        if (!habitat || !level.getWorldBorder().isWithinBounds(pos)
                || type != CreatureMounts.SEAHORSE && !level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)) return false;
        var bounds = type.getSpawnAABB(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (!level.noCollision(bounds) || type != CreatureMounts.SEAHORSE && level.containsAnyLiquid(bounds)) return false;
        // Local Encounter Cap: Claimed and named mounts also prevent nearby duplicates of their species.
        return level.getLevel().getEntitiesOfClass(PrototypeMount.class, new AABB(pos).inflate(96),
            mount -> mount.isAlive() && mount.getType() == type).isEmpty();
    }

    private static boolean nearFluid(ServerLevelAccessor level, BlockPos pos, net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> fluid) {
        for (var nearby : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 3, 6))) {
            // Spawn checks must not force neighboring chunks to load.
            if (level.hasChunk(nearby.getX() >> 4, nearby.getZ() >> 4) && level.getFluidState(nearby).is(fluid)) return true;
        }
        return false;
    }
}
