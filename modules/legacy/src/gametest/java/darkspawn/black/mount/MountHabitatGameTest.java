package darkspawn.black.mount;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

public final class MountHabitatGameTest {
    private static void clearing(ServerLevel level, BlockPos pos, ResourceKey<Biome> biome) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            var column = pos.offset(x, 0, z);
            level.getChunkAt(column);
            level.setBlockAndUpdate(column.below(), Blocks.STONE.defaultBlockState());
            for (int y = pos.getY(); y < level.getMaxY(); y++) level.setBlockAndUpdate(new BlockPos(column.getX(), y, column.getZ()), Blocks.AIR.defaultBlockState());
        }
        biome(level, pos, biome);
    }
    private static void biome(ServerLevel level, BlockPos pos, ResourceKey<Biome> biome) {
        FillBiomeCommand.fill(level, pos.offset(-3, -3, -3), pos.offset(3, 3, 3), level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome));
    }
    @GameTest(skyAccess = true)
    public void surfaceMountsMatchBiomesHeightAndClearance(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 3, 3)).atY(140);
        clearing(level, pos, Biomes.STONY_PEAKS);
        helper.assertTrue(SpawnPlacements.checkSpawnRules(Mounts.GRIFFIN, level, EntitySpawnReason.NATURAL, pos, level.getRandom()), "Registered griffin rule accepts an exposed summit");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.FLYING_SQUIRREL, level, pos), "Squirrels do not spawn on bare peaks");
        level.setBlockAndUpdate(pos.above(8), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.GRIFFIN, level, pos), "Covered peaks reject griffins");
        level.setBlockAndUpdate(pos.above(8), Blocks.AIR.defaultBlockState());
        biome(level, pos, Biomes.FOREST);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.FLYING_SQUIRREL, level, pos), "Forest floor allows squirrels");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.GRIFFIN, level, pos), "High forests are not griffin peaks");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.SASQUATCH, level, pos), "Warm forests reject Sasquatch");
        biome(level, pos, Biomes.SNOWY_PLAINS);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.WOOLLY_RHINO, level, pos), "Snow plains allow rhinos");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.SASQUATCH, level, pos), "Treeless tundra rejects Sasquatch");
        biome(level, pos, Biomes.SNOWY_TAIGA);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.SASQUATCH, level, pos), "Snowy taiga allows Sasquatch");
        level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.SASQUATCH, level, pos), "Tall mount needs full headroom");
        clearing(level, pos.atY(100), Biomes.STONY_PEAKS);
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.GRIFFIN, level, pos.atY(100)), "Low mountain terrain rejects griffins");
        helper.succeed();
    }
    @GameTest(skyAccess = true)
    public void caveMountsRequireCoverAndDragonRequiresNearbyLava(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 3, 3)).atY(20);
        clearing(level, pos, Biomes.PLAINS);
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.RIDING_GOLEM, level, pos), "Low open ground is not a cave");
        level.setBlockAndUpdate(pos.above(10), Blocks.STONE.defaultBlockState());
        helper.assertTrue(MountSpawning.canSpawn(Mounts.RIDING_GOLEM, level, pos), "Covered cave supports golems");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.DRAGON_MOUNT, level, pos), "Dry cave has no dragon habitat");
        level.setBlockAndUpdate(pos.offset(3, -1, 0), Blocks.LAVA.defaultBlockState());
        helper.assertTrue(MountSpawning.canSpawn(Mounts.DRAGON_MOUNT, level, pos), "Cave lava beside safe ground supports dragons");
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.DRAGON_MOUNT, level, pos), "Dragons spawn beside lava, never inside it");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var dragon = Mounts.DRAGON_MOUNT.create(level, EntitySpawnReason.NATURAL);
        dragon.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        level.addFreshEntity(dragon);
        try { helper.assertTrue(!MountSpawning.canSpawn(Mounts.DRAGON_MOUNT, level, pos), "Nearby mount blocks a duplicate encounter"); }
        finally { dragon.discard(); }
        level.setBlockAndUpdate(pos.above(10), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.DRAGON_MOUNT, level, pos), "Surface lava does not attract cave dragons");
        helper.succeed();
    }
    @GameTest(skyAccess = true)
    public void jungleDesertSwampAndOceanMountsMatchTheirHabitats(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 3, 3)).atY(80);
        clearing(level, pos, Biomes.JUNGLE);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.LEOPARD, level, pos), "Jungle supports leopards");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.GIANT_SCORPION, level, pos), "Jungle rejects desert scorpions");
        biome(level, pos, Biomes.DESERT);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.GIANT_SCORPION, level, pos), "Desert supports giant scorpions");
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.LEOPARD, level, pos), "Desert rejects leopards");
        biome(level, pos, Biomes.SWAMP);
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.CROCODILE, level, pos), "Dry swamp ground rejects crocodiles");
        level.setBlockAndUpdate(pos.offset(3, -1, 0), Blocks.WATER.defaultBlockState());
        helper.assertTrue(MountSpawning.canSpawn(Mounts.CROCODILE, level, pos), "Swamp shore supports crocodiles");
        biome(level, pos, Biomes.MANGROVE_SWAMP);
        helper.assertTrue(MountSpawning.canSpawn(Mounts.CROCODILE, level, pos), "Mangrove shore supports crocodiles");
        for (var water : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 3, 2)))
            level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.SEAHORSE, level, pos), "Swamp water rejects seahorses");
        biome(level, pos, Biomes.OCEAN);
        helper.assertTrue(SpawnPlacements.checkSpawnRules(Mounts.SEAHORSE, level, EntitySpawnReason.NATURAL, pos, level.getRandom()), "Registered seahorse rule accepts open ocean water");
        var seahorse = Mounts.SEAHORSE.create(level, EntitySpawnReason.NATURAL);
        seahorse.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
        helper.assertTrue(seahorse.checkSpawnObstruction(level), "Aquatic mount obstruction check permits water");
        seahorse.discard();
        level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!MountSpawning.canSpawn(Mounts.SEAHORSE, level, pos), "Shallow water rejects the tall seahorse");
        helper.succeed();
    }
    @GameTest(skyAccess = true)
    public void netherAndEndMountsStayInTheirDimensions(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(3, 3, 3)).atY(80);
        for (var dimension : java.util.List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            clearing(level, origin, dimension == Level.NETHER ? Biomes.NETHER_WASTES : dimension == Level.END ? Biomes.END_HIGHLANDS : Biomes.PLAINS);
            helper.assertTrue(MountSpawning.canSpawn(Mounts.CERBERUS, level, origin) == (dimension == Level.NETHER), "Cerberus belongs only in the Nether");
            helper.assertTrue(MountSpawning.canSpawn(Mounts.WINGED_UNICORN, level, origin) == (dimension == Level.END), "Unicorn belongs only in the End");
            helper.assertTrue(!MountSpawning.canSpawn(Mounts.POPTART_CAT, level, origin), "Sky cats keep their dedicated spawn system");
            helper.assertTrue(!MountSpawning.canSpawn(Mounts.GIANT_MOLE, level, origin), "Mole rejects every non-valley habitat");
            if (dimension == Level.NETHER) {
                biome(level, origin, Biomes.SOUL_SAND_VALLEY);
                helper.assertTrue(MountSpawning.canSpawn(Mounts.GIANT_MOLE, level, origin), "Soul Sand Valley supports mole habitat");
                clearing(level, origin.atY(128), Biomes.NETHER_WASTES);
                helper.assertTrue(!MountSpawning.canSpawn(Mounts.CERBERUS, level, origin.atY(128)), "No Cerberus on the Nether roof");
                biome(level, origin.atY(128), Biomes.SOUL_SAND_VALLEY);
                helper.assertTrue(!MountSpawning.canSpawn(Mounts.GIANT_MOLE, level, origin.atY(128)), "No mole on the Nether roof");
            }
        }
        helper.succeed();
    }
}
