package darkspawn.black.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import darkspawn.black.Darkspawn;
import java.util.Optional;
import java.util.Map;
import darkspawn.black.boss.BossProfile;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.NoiseColumn;
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
	public static final Map<String, BossProfile> REGIONAL_SITES = Map.ofEntries(
		Map.entry("farm_excavation", BossProfile.MUTANT_ZOMBIE), Map.entry("fossil_altar", BossProfile.FOSSIL_TYRANT),
		Map.entry("storm_altar", BossProfile.THUNDER_BIRD), Map.entry("serpent_sanctuary", BossProfile.TITAN_BOA),
		Map.entry("witch_hut", BossProfile.BABA_YAGA), Map.entry("quarry_shrine", BossProfile.MOUNTAIN_TITAN),
		Map.entry("frozen_nest", BossProfile.ICE_WYRM), Map.entry("expedition_wreck", BossProfile.KRAKEN),
		Map.entry("webbed_outpost", BossProfile.CAVE_CRAWLER), Map.entry("deep_dark_hive", BossProfile.SHADOW_CREEPER_QUEEN),
		Map.entry("fungal_court", BossProfile.MYCELIAL_SOVEREIGN), Map.entry("ember_shrine", BossProfile.NETHERBORN),
		Map.entry("soul_reliquary", BossProfile.SOULBOUND_COLOSSUS), Map.entry("end_observatory", BossProfile.VOID_EYE));
	private static final Codec<String> SITE_CODEC = Codec.STRING.validate(s ->
		(s.equals("druid_shrine") || s.equals("hunter_camp") || s.equals("research_bunker") || REGIONAL_SITES.containsKey(s)) ? DataResult.success(s) : DataResult.error(() -> "Unknown landmark: " + s));
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
		if (REGIONAL_SITES.containsKey(site)) return findRegionalPoint(context);
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

	private Optional<GenerationStub> findRegionalPoint(GenerationContext context) {
		int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
		var profile = REGIONAL_SITES.get(site);
		if (!context.couldValidBiomeExistInTerrainColumn(x, z)) return Optional.empty();
		// Habitat Survey: Cache 49 noise columns once; never request live chunks or search beyond this footprint.
		NoiseColumn[] columns = new NoiseColumn[49];
		for (int ix = 0; ix < 7; ix++) for (int iz = 0; iz < 7; iz++)
			columns[ix * 7 + iz] = context.chunkGenerator().getBaseColumn(x - 12 + ix * 4, z - 12 + iz * 4, context.heightAccessor(), context.randomState());
		int min = context.heightAccessor().getMinY() + 4, max = context.heightAccessor().getMaxY() - 29;
		if (profile.underground() || profile.fireproof()) {
			max = Math.min(max, profile.fireproof() ? 96 : 40);
			min = Math.max(min, profile.fireproof() ? 32 : min);
			for (int floor = max; floor >= min; floor--) {
				if (!regionalBiome(context, x, floor + 1, z) || !columns[24].getBlock(floor).isSolidRender() || !columns[24].getBlock(floor + 1).isAir()) continue;
				if (supportsCavern(columns, floor, (int)profile.height)) return regionalStub(context, x, floor, z);
			}
			return Optional.empty();
		}
		boolean ocean = profile == BossProfile.KRAKEN;
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		for (int ix = 0; ix < 7; ix++) for (int iz = 0; iz < 7; iz++) {
			int y = context.chunkGenerator().getBaseHeight(x - 12 + ix * 4, z - 12 + iz * 4,
				ocean ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
			if (y < min || y > max || !columns[ix * 7 + iz].getBlock(y).isSolidRender()) return Optional.empty();
			low = Math.min(low, y); high = Math.max(high, y);
			if (!supportsTerrain(low, high)) return Optional.empty();
		}
		int floor = (low + high) / 2;
		if (!regionalBiome(context, x, floor + 1, z)) return Optional.empty();
		if (ocean) {
			// Wreck Access: Leave a continuous water column above every sample; reject ice-covered or shallow sites.
			int surface = context.chunkGenerator().getSeaLevel();
			if (surface - floor < 14) return Optional.empty();
			for (var column : columns) for (int y = high + 1; y < surface; y++)
				if (!column.getBlock(y).getFluidState().is(FluidTags.WATER)) return Optional.empty();
		}
		return regionalStub(context, x, floor, z);
	}

	private static boolean regionalBiome(GenerationContext context, int x, int y, int z) {
		return context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
	}

	static boolean supportsCavern(NoiseColumn[] columns, int floor, int clearance) {
		boolean entrance = false;
		for (int i = 0; i < columns.length; i++) {
			var column = columns[i];
			// Foundations may bridge only three blocks. Keep a walkable natural connection and reject fluid pockets.
			boolean support = false;
			for (int y = floor - 3; y <= floor; y++) support |= column.getBlock(y).isSolidRender();
			if (!support) return false;
			// Perimeter ruins can sit in rock, but at least one edge must connect to the existing cavern.
			if (i == 3 || i == 21 || i == 27 || i == 45) {
				boolean open = true;
				for (int y = floor + 1; y <= floor + 3; y++) open &= column.getBlock(y).isAir();
				entrance |= open;
			}
			for (int y = floor - 3; y <= floor + 24; y++) if (!column.getBlock(y).getFluidState().isEmpty()) return false;
		}
		if (!entrance) return false;
		// Rituals stay underground; require a roof above the authored clearing and the boss's natural headroom.
		for (int y = floor + 1; y <= floor + clearance; y++) if (!columns[24].getBlock(y).isAir()) return false;
		for (int y = floor + 25; y <= floor + 48; y++) if (columns[24].getBlock(y).isSolidRender()) return true;
		return false;
	}

	private Optional<GenerationStub> regionalStub(GenerationContext context, int x, int floor, int z) {
		var origin = new BlockPos(x - 12, floor - 3, z - 12);
		var rotation = Rotation.getRandom(context.random());
		var template = Darkspawn.id("landmarks/" + site + "_" + context.random().nextInt(3));
		return Optional.of(new GenerationStub(new BlockPos(x, floor + 1, z), pieces -> pieces.addPiece(
			new LandmarkPiece(context.structureTemplateManager(), template, origin, rotation))));
	}

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
