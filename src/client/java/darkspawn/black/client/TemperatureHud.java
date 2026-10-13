package darkspawn.black.client;

import darkspawn.black.Darkspawn;
import darkspawn.black.equipment.Temperature;
import darkspawn.black.equipment.TemperaturePayload;
import darkspawn.black.equipment.TemperatureState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TemperatureHud {
	private TemperatureHud() { }
	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(TemperaturePayload.TYPE, (payload, context) -> {
			var state = Temperature.state(context.player());
			state.restore(payload.exposure(), payload.wetSeconds(), 0); state.target = payload.target();
		});
		// Dedicated HUD: Keep climate warnings away from loadout and other action-bar messages.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Darkspawn.id("temperature"), (graphics, delta) -> {
			var client = Minecraft.getInstance(); var player = client.player;
			if (player == null || player.isCreative() || player.isSpectator()) return;
			var state = Temperature.state(player);
			float magnitude = Math.abs(state.exposure);
			String status = magnitude >= TemperatureState.DANGER ? (state.exposure < 0 ? "freezing" : "overheating")
				: magnitude >= TemperatureState.STRAIN ? (state.exposure < 0 ? "shivering" : "heat_strain")
				: magnitude >= TemperatureState.WARNING ? (state.exposure < 0 ? "cold" : "hot") : "comfortable";
			String trend = state.target > state.exposure + .5F ? "warming" : state.target < state.exposure - .5F ? "cooling" : "steady";
			var label = Component.translatable("temperature.darkspawn.reading", Component.translatable("temperature.darkspawn." + status), Component.translatable("temperature.darkspawn." + trend));
			if (state.wetSeconds > 0) label.append(Component.translatable("temperature.darkspawn.wet"));
			int color = magnitude < TemperatureState.WARNING ? 0xFFB8D8AC : state.exposure < 0 ? 0xFF82CFFF : 0xFFFFA565;
			// HUD Layout: Wrap translated/wetness labels inside the viewport and place the gauge below all lines.
			int available = Math.max(1, Math.min(240, graphics.guiWidth() - 16));
			var lines = client.font.split(label, available);
			int gaugeY = 12 + lines.size() * client.font.lineHeight;
			int panelWidth = Math.min(available, Math.max(100, lines.stream().mapToInt(client.font::width).max().orElse(0)));
			graphics.fill(5, 5, 11 + panelWidth, gaugeY + 7, 0xA0000000);
			for (int i = 0; i < lines.size(); i++) graphics.text(client.font, lines.get(i), 8, 8 + i * client.font.lineHeight, color, true);
			int gaugeWidth = Math.min(100, available);
			graphics.fill(8, gaugeY, 8 + gaugeWidth, gaugeY + 3, 0xFF444444);
			graphics.fill(8 + gaugeWidth * 32 / 100, gaugeY, 8 + gaugeWidth * 68 / 100, gaugeY + 3, 0xFF52734A);
			int marker = 8 + Math.round((state.exposure + 100) / 200 * Math.max(0, gaugeWidth - 2));
			graphics.fill(marker, gaugeY - 1, marker + Math.min(2, gaugeWidth), gaugeY + 4, color);
		});
	}
}
