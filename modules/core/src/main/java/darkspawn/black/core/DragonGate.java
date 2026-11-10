package darkspawn.black.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/** Darkspawn's post-dragon content opens once this world's ender dragon has been defeated. */
public final class DragonGate {
	private DragonGate() { }
	public static boolean defeated(MinecraftServer server) {
		var end = server.getLevel(Level.END);
		return end != null && end.getDragonFight() != null && end.getDragonFight().hasPreviouslyKilledDragon();
	}
}
