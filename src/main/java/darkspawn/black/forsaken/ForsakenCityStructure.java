package darkspawn.black.forsaken;

import com.mojang.serialization.MapCodec;
import darkspawn.black.Darkspawn;
import darkspawn.black.worldgen.BossLandmarkStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Native chunk generation owns city placement and persistence; revisiting never rebuilds streets or loot. */
public final class ForsakenCityStructure extends Structure {
	public static final MapCodec<ForsakenCityStructure> CODEC = simpleCodec(ForsakenCityStructure::new);
	public static final StructureType<ForsakenCityStructure> TYPE = () -> CODEC;
	public ForsakenCityStructure(StructureSettings settings) { super(settings); }
	public static void initialize() { Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Darkspawn.id("forsaken_city"), TYPE); }
	@Override public StructureType<?> type() { return TYPE; }
	@Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!(context.chunkGenerator() instanceof FlatLevelSource) || !context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
		int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
		int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (surface - 4 < context.heightAccessor().getMinY() || surface + 20 >= context.heightAccessor().getMaxY()) return Optional.empty();
		// Fixed Slice: The first city's north approach aligns with the landing without occupying its protected area.
		var origin = new BlockPos(x - 32, surface - 4, z + 16);
		return Optional.of(new GenerationStub(new BlockPos(x, surface, z), pieces -> pieces.addPiece(
			new BossLandmarkStructure.LandmarkPiece(context.structureTemplateManager(), Darkspawn.id("forsaken/city_slice"), origin, Rotation.NONE))));
	}
}
