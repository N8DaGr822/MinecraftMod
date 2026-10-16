package darkspawn.black.client;

import darkspawn.black.inventory.ArmorLoadoutPayload;
import darkspawn.black.inventory.ArmorLoadoutSyncPayload;
import darkspawn.black.inventory.ArmorLoadouts;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class ArmorLoadoutControls {
	private ArmorLoadoutControls() { }
	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(ArmorLoadoutSyncPayload.TYPE, (payload, context) -> {
			var state = ArmorLoadouts.state(context.player());
			state.unlocked = payload.unlocked();
			state.active = payload.active();
			state.revision = payload.revision();
			state.pending = payload.pending();
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) || !ClientPlayNetworking.canSend(ArmorLoadoutPayload.TYPE)) return;
			// Inventory UI: Compact loadouts below Sort/Destroy, aligned to the same right margin.
			for (int set = 0; set < 3; set++) {
				final int target = set;
				Button button = darkspawn.black.client.UiTheme.button(Component.empty(), pressed -> {
					if (client.player == null) return;
					var state = ArmorLoadouts.state(client.player);
					ClientPlayNetworking.send(new ArmorLoadoutPayload(target, state.revision, client.player.inventoryMenu.getStateId()));
				}).bounds(width - 54, 54 + set * 18, 48, 16).build();
				Runnable refresh = () -> {
					if (client.player == null) { button.active = false; return; }
					var state = ArmorLoadouts.state(client.player);
					boolean locked = target >= state.unlocked;
					button.setMessage(Component.translatable(locked ? "button.darkspawn.loadout_locked"
						: state.pending == target ? "button.darkspawn.loadout_changing"
						: state.active == target ? "button.darkspawn.loadout_active" : "button.darkspawn.loadout", target + 1));
					button.setTooltip(Tooltip.create(Component.translatable(locked
						? "tooltip.darkspawn.loadout_unlock_" + (target + 1) : "tooltip.darkspawn.loadout")));
					button.active = !locked && state.active != target && state.pending < 0 && client.player.isAlive()
						&& !client.player.isSpectator() && client.player.onGround()
						&& client.player.containerMenu == client.player.inventoryMenu && client.player.inventoryMenu.getCarried().isEmpty();
				};
				refresh.run();
				Screens.getWidgets(screen).add(button);
				ScreenEvents.afterTick(screen).register(current -> refresh.run());
			}
		});
	}
}
