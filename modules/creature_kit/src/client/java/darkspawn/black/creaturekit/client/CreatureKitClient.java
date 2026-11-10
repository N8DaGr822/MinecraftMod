package darkspawn.black.creaturekit.client;

import darkspawn.black.creaturekit.CreatureKit;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class CreatureKitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(CreatureKit.BOSS_BOLT, context -> new ThrownItemRenderer<>(context, 2, true));
		EncounterMusic.initialize();
	}
}
