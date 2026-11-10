package darkspawn.black.magiccarpet.client;

import darkspawn.black.magiccarpet.MagicCarpetMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class MagicCarpetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(MagicCarpetMod.MAGIC_CARPET, MagicCarpetRenderer::new);
	}
}
