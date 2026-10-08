package darkspawn.black.boss;

import java.util.Set;

/** Completed asset registrations: enable each rig only after its resources and clips are validated. */
public final class CreatureAssets {
	public static final Set<String> BOSSES = Set.of("fossil_tyrant", "thunder_bird", "titan_boa", "baba_yaga", "mountain_titan", "ice_wyrm", "kraken", "cave_crawler", "shadow_creeper_queen", "mycelial_sovereign", "netherborn", "soulbound_colossus", "void_eye");
	public static final Set<String> MOBS = Set.of();
	public static final Set<String> MINIONS = Set.of();
	private CreatureAssets() { }
}
