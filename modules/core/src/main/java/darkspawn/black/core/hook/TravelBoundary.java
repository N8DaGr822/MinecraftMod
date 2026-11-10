package darkspawn.black.core.hook;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Profile-isolated travel (for example OneBlock islands) registered by the owning mod and queried by mounts, events
 * and relics that must not cross or run inside those boundaries. With no boundary registered nothing is isolated.
 */
public final class TravelBoundary {
	public interface Boundary {
		/** The player is inside this boundary's isolated space or is mid-transfer into or out of it. */
		boolean isolated(ServerPlayer player);
		/** A stable zone key for {@code level}: levels with different keys are on opposite sides of this boundary. */
		String zone(Level level);
		/** {@code level} keeps its own possessions (saved companions and ledgers) instead of sharing the overworld's. */
		default boolean separatePossessions(Level level) { return false; }
	}
	private static final List<Boundary> BOUNDARIES = new CopyOnWriteArrayList<>();
	private TravelBoundary() { }
	public static void register(Boundary boundary) { BOUNDARIES.add(boundary); }
	public static boolean isolated(ServerPlayer player) {
		for (var boundary : BOUNDARIES) if (boundary.isolated(player)) return true;
		return false;
	}
	/** True when travelling between {@code from} and {@code to} crosses no registered boundary. */
	public static boolean sameZone(Level from, Level to) {
		for (var boundary : BOUNDARIES) if (!boundary.zone(from).equals(boundary.zone(to))) return false;
		return true;
	}
	/** Where possessions saved from {@code level} live: the level itself if a boundary isolates them, else the overworld. */
	public static ServerLevel possessionsLevel(ServerLevel level) {
		for (var boundary : BOUNDARIES) if (boundary.separatePossessions(level)) return level;
		return level.getServer().overworld();
	}
	/** {@code level} is inside a boundary zone that still shares the overworld's possessions (for example a shared island). */
	public static boolean sharedIsland(Level level) {
		for (var boundary : BOUNDARIES) if (!boundary.zone(level).isEmpty() && !boundary.separatePossessions(level)) return true;
		return false;
	}
}
