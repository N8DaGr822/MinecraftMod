package darkspawn.black.aircraft.client;

import darkspawn.black.aircraft.AircraftMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class AircraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(AircraftMod.AIRCRAFT, AircraftRenderer::new);
	}
}
