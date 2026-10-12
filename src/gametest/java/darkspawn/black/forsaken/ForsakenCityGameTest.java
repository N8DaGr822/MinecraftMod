package darkspawn.black.forsaken;

import darkspawn.black.Darkspawn;
import java.util.ArrayList;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class ForsakenCityGameTest {
	@GameTest public void cityTemplateHasTwoDistrictsFiniteCachesAndClearReturnStreet(GameTestHelper helper) {
		var template = helper.getLevel().getStructureTemplateManager().getOrCreate(Darkspawn.id("forsaken/city_slice"));
		var settings = new StructurePlaceSettings().setRotation(Rotation.NONE);
		helper.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(64, 24, 80)), "City stays within its authored footprint");
		var chests = template.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
		helper.assertTrue(chests.size() == 10, "Ten finite native loot caches are authored: " + chests.size() + " " + chests.stream().map(info -> info.pos()).toList());
		for (var chest : chests) {
			var table = chest.nbt().getStringOr("LootTable", "");
			helper.assertTrue(table.startsWith("darkspawn:chests/forsaken_"), "All caches use dedicated data-driven loot tables");
			if (table.endsWith("medical") || table.endsWith("research") || table.endsWith("rooftop")) helper.assertTrue(chest.pos().getZ() >= 42, "Better facility loot is beyond the marked quarantine boundary");
		}
		var air = template.filterBlocks(BlockPos.ZERO, settings, Blocks.AIR).stream().map(info -> info.pos()).collect(java.util.stream.Collectors.toSet());
		for (int z = 0; z < 80; z++) helper.assertTrue(air.contains(new BlockPos(31, 4, z)) && air.contains(new BlockPos(31, 5, z)), "Main return street has continuous headroom");
		helper.assertTrue(air.contains(new BlockPos(49, 15, 38)), "The rooftop broken span remains a deliberate one-block gap");
		helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, Blocks.LADDER).size() > 40, "Ground, upper floors, roofs and tunnel have vertical access");
		helper.assertTrue(!template.filterBlocks(BlockPos.ZERO, settings, Blocks.SCULK).isEmpty() && !template.filterBlocks(BlockPos.ZERO, settings, Blocks.NETHER_WART_BLOCK).isEmpty(), "Infested district has visible biological growth");
		helper.succeed();
	}
	@GameTest public void nativeCityStartsAreDeterministicAndSurviveSaveLoad(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL); var access = level.registryAccess();
		var generator = level.getChunkSource().getGenerator();
		var holder = access.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Darkspawn.id("forsaken_city")));
		var structure = holder.value();
		var set = access.lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, Darkspawn.id("forsaken_city"))).value();
		var placement = (RandomSpreadStructurePlacement) set.placement();
		helper.assertTrue(placement.spacing() == 32 && placement.separation() == 31, "City anchors repeat on a fixed 512-block grid");
		for (long seed : new long[] {1, 42, -7319}) for (int region : new int[] {-2, 0, 2}) {
			var chunk = placement.getPotentialStructureChunk(seed, region * 32, region * 32);
			helper.assertTrue(chunk.equals(new ChunkPos(region * 32, region * 32)), "Grid handles positive and negative coordinates consistently");
			var normal = (NoiseBasedChunkGenerator) WorldPresets.getNormalOverworld(access).generator();
			var random = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, normal.generatorSettings().value());
			var climate = random.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
			var start = structure.generate(holder, Forsaken.LEVEL, access, generator, generator.getBiomeSource(), climate, random, level.getStructureTemplateManager(), seed, chunk, 0, level, structure.biomes()::contains);
			helper.assertTrue(start.isValid(), "Forsaken flat generator and biome produce a city start");
			var bounds = start.getBoundingBox();
			helper.assertTrue(bounds.minX() == chunk.getMinBlockX() - 32 && bounds.minZ() == chunk.getMinBlockZ() + 16 && bounds.minY() == 76, "City aligns south of landing and with stone surface height");
			var saved = start.createTag(StructurePieceSerializationContext.fromLevel(level), chunk);
			var loaded = net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(StructurePieceSerializationContext.fromLevel(level), saved, seed);
			helper.assertTrue(loaded != null && loaded.isValid() && loaded.getBoundingBox().equals(bounds), "Saved native structure start preserves layout");
		}
		helper.succeed();
	}
	@GameTest public void nativePiecePlacementGeneratesLootOnceAndLandingVisitsPreserveEdits(GameTestHelper helper) {
		var level = helper.getLevel().getServer().getLevel(Forsaken.LEVEL);
		// Fresh Region: Keep native generation away from the shared landing and every other test fixture.
		int anchor = (Math.floorDiv(helper.absolutePos(BlockPos.ZERO).getX(), 512) + 20) * 512;
		var origin = new BlockPos(anchor - 32, 76, 16);
		// GameTestServer disables structure generation globally; exercise the same native piece placement used by terrain generation.
		var piece = new darkspawn.black.worldgen.BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(), Darkspawn.id("forsaken/city_slice"), origin, Rotation.NONE);
		var bounds = piece.getBoundingBox();
		for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) level.getChunk(x, z);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(1), bounds, new ChunkPos(anchor >> 4, 0), origin);
		var street = origin.offset(31, 3, 0); level.getChunk(street);
		helper.assertTrue(level.getBlockState(street).is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_concrete"))), "Native structure piece placement builds the city streets");
		var chestPos = origin.offset(7, 4, 24); level.getChunk(chestPos);
		var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
		helper.assertTrue(chest != null, "City generation installs functional native loot containers");
		chest.unpackLootTable(null);
		var original = new ArrayList<ItemStack>(); for (int i = 0; i < chest.getContainerSize(); i++) original.add(chest.getItem(i).copy());
		helper.assertTrue(original.stream().anyMatch(stack -> !stack.isEmpty()), "Ordinary scavenging supplies are obtainable");
		chest.clearContent(); ForsakenLanding.prepare(level); chest.unpackLootTable(null);
		helper.assertTrue(chest.isEmpty(), "Revisits do not restock a looted cache");
		for (int i = 0; i < original.size(); i++) chest.setItem(i, original.get(i));
		var old = level.getBlockState(street); level.setBlockAndUpdate(street, Blocks.GOLD_BLOCK.defaultBlockState());
		ForsakenLanding.prepare(level); level.getChunk(street);
		helper.assertTrue(level.getBlockState(street).is(Blocks.GOLD_BLOCK), "Native city does not overwrite player edits on revisit"); level.setBlockAndUpdate(street, old);
		var signPos = origin.offset(28, 4, 40); level.getChunk(signPos);
		var sign = (net.minecraft.world.level.block.entity.SignBlockEntity) level.getBlockEntity(signPos);
		helper.assertTrue(sign != null && sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false).getFirst().getString().equals("QUARANTINE"), "Native sign text clearly labels the district boundary");
		helper.succeed();
	}
}
