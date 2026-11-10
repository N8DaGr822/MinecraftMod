package darkspawn.black.groundwalker.client;

import darkspawn.black.groundwalker.GroundWalkerMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class GroundWalkerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(GroundWalkerMod.GROUND_WALKER, GroundWalkerRenderer::new);
	}
}
