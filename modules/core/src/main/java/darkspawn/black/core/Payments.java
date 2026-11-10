package darkspawn.black.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/** Item-count payments from a player's inventory. Creative players always pay. */
public final class Payments {
	private Payments() { }
	/** True when the player holds {@code count} of {@code item}; removes them as well when {@code consume}. */
	public static boolean pay(ServerPlayer player, Item item, int count, boolean consume) {
		if (player.getAbilities().instabuild) return true;
		int total = 0; for (int i=0;i<player.getInventory().getContainerSize();i++) if (player.getInventory().getItem(i).is(item)) total += player.getInventory().getItem(i).getCount();
		if (total < count) return false;
		if (consume) for (int i=0;i<player.getInventory().getContainerSize() && count>0;i++) { var stack=player.getInventory().getItem(i); if(stack.is(item)){int take=Math.min(count,stack.getCount());stack.shrink(take);count-=take;} }
		return true;
	}
}
