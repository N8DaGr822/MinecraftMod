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
			int color = magnitude < TemperatureState.WARNING ? 0xFF75C9A6 : state.exposure < 0 ? 0xFF82CFFF : 0xFFFFA565;
			// Climate HUD: Occupy the row above hunger; leave vanilla breath and mount health readable.
			int x = graphics.guiWidth() / 2 + 10;
			int y = graphics.guiHeight() - 51;
			if (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply()) y -= 10;
			if (player.isPassenger()) y -= 20;
			graphics.fill(x-2,y-2,x+82,y+9,0xB014282E);
			// Thermometer: Color and fill communicate comfort without a persistent text paragraph.
			graphics.fill(x+2,y,x+5,y+6,0xFFE1E7DE);
			graphics.fill(x+1,y+5,x+6,y+8,color);
			graphics.fill(x+3,y+1,x+4,y+6,color);
			for (int i=0;i<10;i++) {
				int segment = x+10+i*5;
				graphics.fill(segment,y+2,segment+3,y+7,i < Math.max(1,Math.round(magnitude/10)) ? color : 0xFF405B60);
			}
			int trend = state.target > state.exposure+.5F ? -1 : state.target < state.exposure-.5F ? 1 : 0;
			int arrowX=x+65;
			graphics.fill(arrowX,y+1,arrowX+1,y+7,color);
			if (trend != 0) {
				int tip = trend < 0 ? y+1 : y+6;
				for (int i=0;i<3;i++) graphics.fill(arrowX-i,tip-trend*i,arrowX+i+1,tip-trend*i+1,color);
			} else graphics.fill(arrowX-2,y+3,arrowX+3,y+4,color);
			if (state.wetSeconds > 0) {
				graphics.fill(x+75,y+1,x+76,y+3,0xFF82CFFF);
				graphics.fill(x+74,y+3,x+77,y+5,0xFF82CFFF);
				graphics.fill(x+73,y+5,x+78,y+7,0xFF82CFFF);
			}

		});
	}
}
