package darkspawn.black.boss;

import java.util.Set;

/** Completed asset registrations: enable each rig only after its resources and clips are validated. */
public final class CreatureAssets {
	public static final Set<String> BOSSES = Set.of("fossil_tyrant", "thunder_bird", "titan_boa", "baba_yaga", "mountain_titan", "ice_wyrm", "kraken", "cave_crawler", "shadow_creeper_queen", "mycelial_sovereign", "netherborn", "soulbound_colossus", "void_eye");
	public static final Set<String> MOBS = Set.of("barkling", "hollowed", "rootcrawler", "ancient_ent", "frostfang", "dire_wolf", "ravaged_wolf", "alpha_dire_wolf", "rotted_zombie");
	public static final Set<String> MINIONS = Set.of("mutant_zombie_minion", "fossil_tyrant_minion", "thunder_bird_minion", "titan_boa_minion", "baba_yaga_minion", "mountain_titan_minion", "ice_wyrm_minion", "kraken_minion", "cave_crawler_minion", "shadow_creeper_queen_minion", "mycelial_sovereign_minion", "netherborn_minion", "soulbound_colossus_minion", "void_eye_minion", "heartwood_sapling", "frost_wolf");
	public static String texture(String id, String region) {
		return id.startsWith("warped_") || id.equals("riftling") ? "netherborn_warped" : region;
	}
	private CreatureAssets() { }
}
