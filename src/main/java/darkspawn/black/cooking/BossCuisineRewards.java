package darkspawn.black.cooking;

import darkspawn.black.boss.BossKind;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class BossCuisineRewards {
	private BossCuisineRewards() {}
	/** Only real bosses call this in the one-time death-loot path; helpers and abandonment never do. */
	public static void grant(LivingEntity boss, ServerLevel level, Set<UUID> participants, BossKind kind, int range) {
		for (UUID id : participants) {
			var player = level.getServer().getPlayerList().getPlayer(id);
			if (player == null || !player.isAlive() || player.isSpectator() || player.level() != level || boss.distanceToSqr(player) > range * range) continue;
			for (var item : java.util.List.of(Cuisine.INGREDIENTS.get(kind), Cuisine.TROPHIES.get(kind))) {
				ItemEntity reward = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), new ItemStack(item));
				reward.setTarget(id);
				level.addFreshEntity(reward);
			}
			Culinary.award(player, "defeat_" + kind.id(), "earned");
			Culinary.award(player, "conqueror", kind.id());
		}
	}
}
