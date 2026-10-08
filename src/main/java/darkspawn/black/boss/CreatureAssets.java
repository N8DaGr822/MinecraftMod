package darkspawn.black.boss;

import java.util.Set;

/** Completed asset registrations: enable each rig only after its resources and clips are validated. */
public final class CreatureAssets {
	public static final Set<String> BOSSES = Set.of("fossil_tyrant", "thunder_bird", "titan_boa");
	public static final Set<String> MOBS = Set.of();
	public static final Set<String> MINIONS = Set.of();
	private CreatureAssets() { }
}
