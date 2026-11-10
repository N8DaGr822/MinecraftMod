package darkspawn.black.core.hook;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;

/**
 * Creature mods that know better than the vanilla {@code Monster}/{@code Enemy} type say whether one of their mobs is
 * really hostile (peaceful or neutral wildlife can extend {@code Monster}). Mount abilities and relic powers ask before
 * picking targets. With no judge registered every entity gets the vanilla treatment.
 */
public final class Temperament {
	public interface Judge {
		/** {@code TRUE} hostile, {@code FALSE} not hostile, {@code null} when this judge has no opinion. */
		Boolean hostile(Entity entity);
	}
	private static final List<Judge> JUDGES = new CopyOnWriteArrayList<>();
	private Temperament() { }
	public static void register(Judge judge) { JUDGES.add(judge); }
	/** The first registered verdict, or {@code null} to fall back to vanilla rules. */
	public static Boolean hostile(Entity entity) {
		for (var judge : JUDGES) {
			var verdict = judge.hostile(entity);
			if (verdict != null) return verdict;
		}
		return null;
	}
}
