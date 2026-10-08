package darkspawn.black.client;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;

// Creative Access: Reuse the normal inventory layout without redirecting back to the item catalog.
public final class ExpandedInventoryScreen extends InventoryScreen {
	public ExpandedInventoryScreen(Player player) {
		super(player);
		player.containerMenu = player.inventoryMenu;
	}
}
