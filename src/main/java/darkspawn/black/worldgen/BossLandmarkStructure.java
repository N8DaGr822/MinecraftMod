package darkspawn.black.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Native saved structure starts; nothing runs on chunk load or changes old terrain. */
public final class BossLandmarkStructure extends Structure {
	private static final Codec<String> SITE_CODEC = Codec.STRING.validate(s ->
		(s.equals("druid_shrine") || s.equals("hunter_camp")) ? DataResult.success(s) : DataResult.error(() -> "Unknown landmark: " + s));
	public static final MapCodec<BossLandmarkStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		settingsCodec(i), SITE_CODEC.fieldOf("site").forGetter(s -> s.site)
	).apply(i, BossLandmarkStructure::new));
	public static final StructureType<BossLandmarkStructure> TYPE = () -> CODEC;
	public static final StructurePieceType PIECE = (context, tag) -> new LandmarkPiece(context.structureTemplateManager(), tag);
	private final String site;
	public BossLandmarkStructure(StructureSettings settings, String site) { super(settings); this.site = site; }
	public String site() { return site; }
	public static void initialize() {
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Darkspawn.id("boss_landmark"), TYPE);
		Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Darkspawn.id("boss_landmark_piece"), PIECE);
	}
	@Override public StructureType<?> type() { return TYPE; }

	@Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!context.couldValidBiomeExistOnTopOfChunkCenter()) return Optional.empty();
		int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
		int radius = site.equals("druid_shrine") ? 15 : 12;
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		// 49 fixed terrain probes, no loaded-chunk access. Include footprint edges and center.
		for (int ix = 0; ix < 7; ix++) for (int iz = 0; iz < 7; iz++) {
			int px = x - radius + ix * radius / 3, pz = z - radius + iz * radius / 3;
			int y = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (y < context.heightAccessor().getMinY() + 4 || y + 48 >= context.heightAccessor().getMaxY()) return Optional.empty();
			var top = context.chunkGenerator().getBaseColumn(px, pz, context.heightAccessor(), context.randomState()).getBlock(y - 1);
			if (!top.getFluidState().isEmpty() || top.isAir()) return Optional.empty();
			low = Math.min(low, y); high = Math.max(high, y);
			if (!supportsTerrain(low, high)) return Optional.empty();
		}
		// Meet the middle of gentle slopes: at most three blocks of cut or foundation fill.
		int floor = (low + high) / 2 - 1;
		var center = new BlockPos(x, floor, z);
		var origin = new BlockPos(x - radius, floor - 3, z - radius);
		var rotation = Rotation.getRandom(context.random());
		var template = Darkspawn.id("landmarks/" + site + "_" + context.random().nextInt(3));
		return Optional.of(new GenerationStub(center, pieces -> pieces.addPiece(
			new LandmarkPiece(context.structureTemplateManager(), template, origin, rotation))));
	}
	public static boolean supportsTerrain(int lowest, int highest) { return highest >= lowest && highest - lowest <= 6; }

	public static final class LandmarkPiece extends TemplateStructurePiece {
		public LandmarkPiece(StructureTemplateManager manager, Identifier id, BlockPos origin, Rotation rotation) {
			super(PIECE, 0, manager, id, id.toString(), settings(id, rotation), origin);
		}
		public LandmarkPiece(StructureTemplateManager manager, CompoundTag tag) {
			super(PIECE, tag, manager, id -> settings(id, tag.read("LandmarkRotation", Rotation.CODEC).orElse(Rotation.NONE)));
		}
		private static StructurePlaceSettings settings(Identifier id, Rotation rotation) {
			int radius = id.getPath().contains("druid_shrine") ? 15 : 12;
			// Center pivot keeps all four rotations inside the same surveyed footprint.
			return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(radius, 0, radius)).setIgnoreEntities(true);
		}
		@Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.store("LandmarkRotation", Rotation.CODEC, getRotation());
		}
		@Override protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox bounds) {
			// Templates contain native chest block entities with lazy loot tables, and no data markers.
		}
	}
}
