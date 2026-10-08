package darkspawn.black.boss;

import java.util.Set;

/** Completed asset registrations: enable each rig only after its resources and clips are validated. */
public final class CreatureAssets {
	public static final Set<String> BOSSES = Set.of("fossil_tyrant", "thunder_bird", "titan_boa", "baba_yaga", "mountain_titan", "ice_wyrm", "kraken", "cave_crawler", "shadow_creeper_queen", "mycelial_sovereign", "netherborn", "soulbound_colossus", "void_eye");
	public static final Set<String> MOBS = Set.of("barkling", "hollowed", "rootcrawler", "ancient_ent", "frostfang", "dire_wolf", "ravaged_wolf", "alpha_dire_wolf", "rotted_zombie", "brute_zombie", "leaping_zombie", "mutant_huskling", "failed_mutant", "bone_raptor", "fossil_scorpion", "bone_vulture", "fossilized_husk", "tyrant_skull", "storm_finch", "shocktalon", "stormstrider", "thunder_roc", "tree_viper", "constrictor", "jungle_stalker", "brood_serpent", "bogling", "hexed_frog", "swamp_wisp", "hexbound", "babas_familiar", "stoneback_goat", "crag_crawler", "stoneborn", "titan_spawn", "frostling", "frozen_husk", "icefang", "young_wyrm", "abyssal_fish", "giant_crab", "siren", "leviathan_spawn", "cave_skitterer", "web_spitter", "brood_carrier", "tunnel_widow", "shadow_drone", "shadow_stalker", "shadow_spitter", "shadow_guardian", "shadow_larva", "shadow_egg", "shadow_praetorian", "sporeling", "mycelium_crawler", "infected_mooshroom", "sporewalker", "mycelial_guardian", "crimson_ravager", "fungal_imp", "bloodroot", "warped_stalker", "warped_wisp", "riftling", "ash_ghoul", "magma_brute", "infernal_hoglin", "crimson_spawn", "warped_spawn", "infernal_spawn", "lost_soul", "bonewalker", "soulflame_skull", "tormented");
	public static final Set<String> MINIONS = Set.of("mutant_zombie_minion", "fossil_tyrant_minion", "thunder_bird_minion", "titan_boa_minion", "baba_yaga_minion", "mountain_titan_minion", "ice_wyrm_minion", "kraken_minion", "cave_crawler_minion", "shadow_creeper_queen_minion", "mycelial_sovereign_minion", "netherborn_minion", "soulbound_colossus_minion", "void_eye_minion", "heartwood_sapling", "frost_wolf");
	public static String texture(String id, String region) {
		return id.startsWith("warped_") || id.equals("riftling") ? "netherborn_warped" : region;
	}
	private CreatureAssets() { }
}
