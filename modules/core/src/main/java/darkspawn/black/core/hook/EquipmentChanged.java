package darkspawn.black.core.hook;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/** Fired after a mod replaces a player's worn equipment wholesale (profile swaps), so set bonuses can refresh. */
public final class EquipmentChanged {
	@FunctionalInterface public interface Listener { void changed(ServerPlayer player); }
	public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> player -> {
		for (var listener : listeners) listener.changed(player);
	});
	private EquipmentChanged() { }
	public static void fire(ServerPlayer player) { EVENT.invoker().changed(player); }
}
