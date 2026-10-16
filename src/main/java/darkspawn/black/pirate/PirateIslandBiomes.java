package darkspawn.black.pirate;

import darkspawn.black.Darkspawn;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Assign native biome data only to the chunk currently being generated. */
public final class PirateIslandBiomes {
	private PirateIslandBiomes() { }
	public static ResourceKey<Biome> biome(int kind) {
		return switch(kind) {
			case 9 -> Biomes.PLAINS;
			case 10 -> Biomes.JUNGLE;
			case 11 -> Biomes.NETHER_WASTES;
			case 12 -> Biomes.END_HIGHLANDS;
			case 13 -> Biomes.DESERT;
			case 14 -> Biomes.SNOWY_TAIGA;
			default -> ResourceKey.create(Registries.BIOME, Darkspawn.id("pirate_seas"));
		};
	}
	public static void apply(ChunkAccess chunk, StructureManager structures) {
		var starts=structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),s->s instanceof PirateIslandStructure);
		var registry=structures.registryAccess().lookupOrThrow(Registries.BIOME);
		// Generation Ownership: Never edit neighboring or previously generated chunks, including player builds.
		for(var start:starts) {
			int kind=IslandDiscoveries.kind(start);if(kind<9)continue;
			var center=start.getChunkPos();var biome=registry.getOrThrow(biome(kind));
			chunk.fillBiomesFromNoise((x,y,z)->{
				int dx=(x<<2)-center.getMinBlockX(),dz=(z<<2)-center.getMinBlockZ();
				// Keep ocean outside the coastline; quart-sized biome cells blend naturally along the shore.
				return dx*dx+dz*dz<=58*58?biome:chunk.getNoiseBiome(x,y,z);
			});
		}
	}
}
