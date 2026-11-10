package darkspawn.black.creaturekit;

import darkspawn.black.core.DarkspawnCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** The 16 Darkspawn regions, named after their boss. Wildlife, farming and landmarks key off these, not boss classes. */
public enum Region {
	ANCIENT_TREE_SPIRIT, MUTANT_WOLF, MUTANT_ZOMBIE, FOSSIL_TYRANT, THUNDER_BIRD, TITAN_BOA, BABA_YAGA, MOUNTAIN_TITAN,
	ICE_WYRM, KRAKEN, CAVE_CRAWLER, SHADOW_CREEPER_QUEEN, MYCELIAL_SOVEREIGN, NETHERBORN, SOULBOUND_COLOSSUS, VOID_EYE;

	public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
	/** {@code #darkspawn:<id>_biomes}. */
	public TagKey<Biome> biomes() { return TagKey.create(Registries.BIOME, DarkspawnCore.id(id() + "_biomes")); }
	public boolean underground() { return this == CAVE_CRAWLER || this == SHADOW_CREEPER_QUEEN; }
	public ResourceKey<Level> dimension() {
		return this == NETHERBORN || this == SOULBOUND_COLOSSUS ? Level.NETHER : this == VOID_EYE ? Level.END : Level.OVERWORLD;
	}
	public static Region byId(String id) {
		for (var region : values()) if (region.id().equals(id)) return region;
		throw new IllegalArgumentException("Unknown Darkspawn region: " + id);
	}
}
