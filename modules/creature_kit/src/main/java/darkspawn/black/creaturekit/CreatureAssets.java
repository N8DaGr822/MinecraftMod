package darkspawn.black.creaturekit;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Completed rig registrations: each creature mod enables a rig only after its resources and clips are validated. */
public final class CreatureAssets {
	private static final Set<String> ANIMATED = ConcurrentHashMap.newKeySet();
	private CreatureAssets() { }
	/** Enable the standard clip set ({@code animation.<id>.idle}, walk, warning, active, hide, death, attack). */
	public static void register(Collection<String> ids) { ANIMATED.addAll(ids); }
	public static void register(String... ids) { register(Set.of(ids)); }
	public static boolean animated(String id) { return ANIMATED.contains(id); }
	public static Set<String> registered() { return Set.copyOf(ANIMATED); }
}
