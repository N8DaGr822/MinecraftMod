package darkspawn.black.pirate;

import com.mojang.serialization.MapCodec;
import darkspawn.black.Darkspawn;
import darkspawn.black.worldgen.BossLandmarkStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Chunk generation owns islands and finite caches; revisiting never rebuilds them. */
public final class PirateIslandStructure extends Structure {
	public static final MapCodec<PirateIslandStructure> CODEC = simpleCodec(PirateIslandStructure::new);
	public static final StructureType<PirateIslandStructure> TYPE = () -> CODEC;
	public PirateIslandStructure(StructureSettings settings) { super(settings); }
	public static void initialize() { Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Darkspawn.id("pirate_island"), TYPE); }
	@Override public StructureType<?> type() { return TYPE; }
	@Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!(context.chunkGenerator() instanceof FlatLevelSource) || !context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
		int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
		// Harbor Reserve: Keep all random island footprints outside the shared arrival island.
		if (Math.abs((long)x) < 128 && Math.abs((long)z) < 128) return Optional.empty();
		if (Math.abs((long)x - 192) < 128 && Math.abs((long)z) < 128) return Optional.empty();
		var origin = new BlockPos(x - 24, 40, z - 24);
		int kind = context.random().nextInt(IslandDiscoveries.NAMES.length);
		var template = Darkspawn.id("pirate/island_" + kind);
		int radius = kind >= 9 ? 64 : 56;
		return Optional.of(new GenerationStub(origin, pieces -> {
			pieces.addPiece(new BossLandmarkStructure.LandmarkPiece(context.structureTemplateManager(), template, origin, Rotation.NONE));
			pieces.addPiece(new BossLandmarkStructure.LandmarkPiece(context.structureTemplateManager(), Darkspawn.id("pirate/terrain_" + kind), new BlockPos(x - radius, 40, z - radius), Rotation.NONE));
		}));
	}
}
