package darkspawn.black.client;

import darkspawn.black.oneblock.OneBlock;
import darkspawn.black.oneblock.OneBlockActionPayload;
import darkspawn.black.oneblock.OneBlockProgress;
import darkspawn.black.oneblock.OneBlockStatusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class OneBlockControls {
	private static int phase, breaks, pending;
	private OneBlockControls() { }
	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(OneBlockStatusPayload.TYPE, (payload, context) -> {
			phase = Math.clamp(payload.phase(), 0, 5); breaks = payload.breaks(); pending = payload.pending();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { phase = 0; breaks = 0; pending = 0; });
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) || !ClientPlayNetworking.canSend(OneBlockActionPayload.TYPE)) return;
			// Inventory UI: The narrow left margin avoids covering slots, including the expanded inventory.
			var travel = Button.builder(Component.empty(), button -> ClientPlayNetworking.send(new OneBlockActionPayload(0)))
				.bounds(6, 6, 62, 20).build();
			var advance = Button.builder(Component.translatable("button.darkspawn.oneblock_advance"), button -> ClientPlayNetworking.send(new OneBlockActionPayload(1)))
				.bounds(6, 30, 62, 20).build();
			Runnable refresh = () -> {
				if (client.player == null) { travel.active = false; advance.active = false; return; }
				boolean inside = OneBlock.island(client.player.level());
				travel.setMessage(Component.translatable(pending > 0 ? "button.darkspawn.oneblock_wait" : inside ? "button.darkspawn.oneblock_leave" : "button.darkspawn.oneblock_enter"));
				travel.active = pending == 0 && client.player.isAlive() && client.player.onGround() && !client.player.isSpectator();
				travel.setTooltip(Tooltip.create(Component.translatable("tooltip.darkspawn.oneblock_travel")));
				advance.visible = inside;
				advance.active = inside && pending == 0 && phase < 5 && breaks >= OneBlockProgress.REQUIRED[phase];
				advance.setTooltip(Tooltip.create(Component.translatable("tooltip.darkspawn.oneblock_phase", Component.translatable("oneblock.darkspawn." + OneBlockProgress.PHASES[phase]), breaks,
					phase == 5 ? Component.translatable("oneblock.darkspawn.endless") : Component.literal(Integer.toString(OneBlockProgress.REQUIRED[phase])))));
			};
			refresh.run(); Screens.getWidgets(screen).add(travel); Screens.getWidgets(screen).add(advance);
			ScreenEvents.afterTick(screen).register(current -> refresh.run());
		});
	}
}
