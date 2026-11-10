package darkspawn.black.core;

import net.minecraft.server.level.ServerPlayer;

/** Awards {@code darkspawn:progress/*} advancements. The tab root ships with core; each mod ships its own children. */
public final class Progress {
	private Progress() { }
	/** Awards {@code criterion} of {@code darkspawn:progress/<path>}, first granting the tab root once the dragon is down. */
	public static void award(ServerPlayer player, String path, String criterion) {
		if (!path.equals("root") && DragonGate.defeated(player.level().getServer())) award(player, "root", "dragon");
		advancement(player, "progress/" + path, criterion);
	}
	/** Awards one criterion of any {@code darkspawn:<path>} advancement; missing advancements are ignored. */
	public static void advancement(ServerPlayer player, String path, String criterion) {
		var advancement = player.level().getServer().getAdvancements().get(DarkspawnCore.id(path));
		if (advancement != null) player.getAdvancements().award(advancement, criterion);
	}
}
