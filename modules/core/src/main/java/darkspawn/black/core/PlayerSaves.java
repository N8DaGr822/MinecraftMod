package darkspawn.black.core;

import darkspawn.black.core.mixin.PlayerListInvoker;
import net.minecraft.server.level.ServerPlayer;

/** Writes a player's data file immediately, for example before a profile or dimension swap. */
public final class PlayerSaves {
	private PlayerSaves() { }
	public static void save(ServerPlayer player) {
		((PlayerListInvoker) player.level().getServer().getPlayerList()).darkspawn_core$save(player);
	}
}
