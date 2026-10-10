package darkspawn.black.worldgen;

import darkspawn.black.Darkspawn;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

public final class BossStructureGameTest {
	private static final List<String> SITES = List.of("druid_shrine", "hunter_camp");
	@GameTest public void everyTemplateRotationKeepsChestAndRitualClearanceAndSavedIdentity(GameTestHelper helper) {
		var level=helper.getLevel(); var manager=level.getStructureTemplateManager();
		var context=StructurePieceSerializationContext.fromLevel(level);
		for (String site:SITES) for (int variant=0;variant<3;variant++) for (Rotation rotation:Rotation.values()) {
			var id=Darkspawn.id("landmarks/"+site+"_"+variant);
			var template=manager.get(id).orElseThrow();
			var origin=new BlockPos(0,80,0);
			var piece=new BossLandmarkStructure.LandmarkPiece(manager,id,origin,rotation);
			var settings=piece.placeSettings();
			int r=site.equals("druid_shrine")?15:12;
			helper.assertTrue(template.getSize().getX()==2*r+1 && template.getSize().getY()==48,"Template dimensions must match surveyed footprint");
			var chests=template.filterBlocks(origin,settings,Blocks.CHEST);
			helper.assertTrue(chests.size()==1 && chests.getFirst().nbt().getStringOr("LootTable", "").equals("darkspawn:chests/"+site),"Exactly one lazy loot chest per rotation");
			var loaded=BossLandmarkStructure.PIECE.load(context,piece.createTag(context));
			helper.assertTrue(loaded.getBoundingBox().equals(piece.getBoundingBox()) && loaded.getRotation()==rotation,"Saved pieces preserve template bounds and rotation");
			// Check explicit air for every block touched by the actual summon bounding box.
			var air=template.filterBlocks(origin,settings,Blocks.AIR).stream().map(StructureTemplate.StructureBlockInfo::pos).collect(java.util.stream.Collectors.toSet());
			int half=site.equals("druid_shrine")?10:5, height=site.equals("druid_shrine")?28:9;
			for(int x=r-half;x<=r+half;x++) for(int z=r-half;z<=r+half;z++) for(int y=4;y<4+height;y++)
				helper.assertTrue(air.contains(origin.offset(x,y,z)),"Ritual volume must stay clear: "+id+" "+rotation+" "+x+","+y+","+z);
			helper.assertTrue(template.filterBlocks(origin,settings,Blocks.STRUCTURE_BLOCK).isEmpty(),"No unresolved markers");
		}
		helper.succeed();
	}
	@GameTest public void lootAlwaysIncludesReadableClueAndNeverBossVictoryRewards(GameTestHelper helper) {
		var level=helper.getLevel();
		var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
		for(String site:SITES) {
			var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Darkspawn.id("chests/"+site)));
			for(long seed=1;seed<=30;seed++) {
				var loot=table.getRandomItems(params,seed);
				var books=loot.stream().filter(s->s.is(Items.WRITTEN_BOOK)).toList();
				helper.assertTrue(books.size()==1,"Every chest includes exactly one clue book");
				var book=books.getFirst().get(DataComponents.WRITTEN_BOOK_CONTENT);
				helper.assertTrue(book!=null && book.pages().size()==3 && book.getPages(false).getFirst().getString().contains("Ender Dragon"),"Clue must explain dragon gate");
				helper.assertTrue(loot.stream().noneMatch(s->darkspawn.black.cooking.Cuisine.TROPHIES.containsValue(s.getItem()) || darkspawn.black.cooking.Cuisine.INGREDIENTS.containsValue(s.getItem())),"Landmarks cannot grant victory rewards");
			}
		}
		helper.succeed();
	}
	@GameTest public void nativePiecePlacementCreatesAUsableChestWithoutSpawningBosses(GameTestHelper helper) {
		var level=helper.getLevel();
		// Separate from other test fixtures; only this test requests these disposable test chunks.
		var origin=new BlockPos(helper.absolutePos(BlockPos.ZERO).getX()+2048,80,helper.absolutePos(BlockPos.ZERO).getZ()+2048);
		for(String site:SITES) {
			var piece=new BossLandmarkStructure.LandmarkPiece(level.getStructureTemplateManager(),Darkspawn.id("landmarks/"+site+"_0"),origin,Rotation.CLOCKWISE_90);
			var bounds=piece.getBoundingBox();
			for(int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++) for(int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++) level.getChunk(cx,cz);
			piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(37),bounds,new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4),origin);
			var info=piece.template().filterBlocks(origin,piece.placeSettings(),Blocks.CHEST).getFirst();
			var chest=(ChestBlockEntity)level.getBlockEntity(info.pos());
			helper.assertTrue(chest!=null && chest.getLootTable()!=null,"Placement creates an unopened lazy chest");
			chest.unpackLootTable(null);
			int books=0; for(int slot=0;slot<chest.getContainerSize();slot++) if(chest.getItem(slot).is(Items.WRITTEN_BOOK)) books++;
			helper.assertTrue(books==1 && chest.getLootTable()==null,"Opening produces the clue and consumes the loot assignment once");
			chest.unpackLootTable(null); int after=0;
			for(int slot=0;slot<chest.getContainerSize();slot++) if(chest.getItem(slot).is(Items.WRITTEN_BOOK)) after++;
			helper.assertTrue(after==1,"A second opener cannot refill the chest");
			origin=origin.offset(64,0,0);
		}
		helper.succeed();
	}
	@GameTest public void nativeGenerationIsDeterministicAcrossFiveSeedsAndBothCoordinateSigns(GameTestHelper helper) {
		helper.assertTrue(BossLandmarkStructure.supportsTerrain(64,70) && !BossLandmarkStructure.supportsTerrain(64,71),"Terrain adaptation must remain bounded to three blocks above/below the midpoint");
		var level=helper.getLevel(); var access=level.registryAccess();
		var generator=(NoiseBasedChunkGenerator)WorldPresets.getNormalOverworld(access).generator();
		for(long seed:new long[]{1,42,7319,-49017,20261009}) {
			var random=RandomState.create(access.lookupOrThrow(Registries.NOISE),seed,generator.generatorSettings().value());
			var climate=random.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
			for(String site:SITES) {
				var holder=access.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Darkspawn.id(site)));
				var structure=holder.value();
				var set=access.lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET,Darkspawn.id(site))).value();
				var placement=(RandomSpreadStructurePlacement)set.placement();
				helper.assertTrue(placement.spacing()==40 && placement.separation()==16,"Rarity remains configured by the structure set");
				int found=0, eligible=0;
				for(int region=0;region<600 && found<2;region++) {
					int sign=region%2==0?1:-1;
					var chunk=placement.getPotentialStructureChunk(seed,sign*(region+1)*40,sign*(region%7+1)*40);
					helper.assertTrue(chunk.equals(placement.getPotentialStructureChunk(seed,sign*(region+1)*40,sign*(region%7+1)*40)),"Placement must be repeatable for signed regions");
					var generationContext=new Structure.GenerationContext(access,generator,generator.getBiomeSource(),climate,random,level.getStructureTemplateManager(),seed,chunk,level,structure.biomes()::contains);
					if(generationContext.couldValidBiomeExistOnTopOfChunkCenter()) eligible++;
					var start=structure.generate(holder,Level.OVERWORLD,access,generator,generator.getBiomeSource(),climate,random,level.getStructureTemplateManager(),seed,chunk,0,level,structure.biomes()::contains);
					if(!start.isValid()) continue;
					found++;
					var repeat=structure.generate(holder,Level.OVERWORLD,access,generator,generator.getBiomeSource(),climate,random,level.getStructureTemplateManager(),seed,chunk,0,level,structure.biomes()::contains);
					helper.assertTrue(repeat.isValid() && start.getBoundingBox().equals(repeat.getBoundingBox()),"Terrain and template selection must be repeatable");
					var saved=start.createTag(StructurePieceSerializationContext.fromLevel(level),chunk);
					var loaded=net.minecraft.world.level.levelgen.structure.StructureStart.loadStaticStart(StructurePieceSerializationContext.fromLevel(level),saved,seed);
					helper.assertTrue(loaded!=null && loaded.isValid() && loaded.getBoundingBox().equals(start.getBoundingBox()),"Native starts survive save/load");
					Darkspawn.LOGGER.info("Landmark sample seed={} site={} chunk={} bounds={}",seed,site,chunk,start.getBoundingBox());
				}
				Darkspawn.LOGGER.info("Landmark survey seed={} site={} validBiomeCandidates={} starts={} biomeCount={}",seed,site,eligible,found,structure.biomes().size());
				helper.assertTrue(found>0,"No usable "+site+" found in 600 candidate regions for seed "+seed+"; eligible="+eligible);
			}
		}
		helper.succeed();
	}
}
