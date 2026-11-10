package darkspawn.black.fishing.client;

import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.fishing.RegionalFishing;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class FishingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (int i = 0; i < RegionalFishing.TYPES.size(); i++) {
			String id = RegionalFishing.IDS[i];
			EntityRenderers.register(RegionalFishing.TYPES.get(i), context -> new CodRenderer(context) {
				@Override public Identifier getTextureLocation(LivingEntityRenderState state) { return DarkspawnCore.id("textures/entity/fish/" + id + ".png"); }
			});
		}
	}
}
