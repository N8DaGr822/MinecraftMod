package darkspawn.black.creaturemounts.client;

import darkspawn.black.creaturemounts.CreatureMounts;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class CreatureMountsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(CreatureMounts.GRIFFIN, GriffinRenderer::new);
		EntityRenderers.register(CreatureMounts.WOOLLY_RHINO, WoollyRhinoRenderer::new);
		EntityRenderers.register(CreatureMounts.FLYING_SQUIRREL, FlyingSquirrelRenderer::new);
		EntityRenderers.register(CreatureMounts.POPTART_CAT, PoptartCatRenderer::new);
		EntityRenderers.register(CreatureMounts.DRAGON_MOUNT, DragonMountRenderer::new);
		EntityRenderers.register(CreatureMounts.WINGED_UNICORN, WingedUnicornRenderer::new);
		EntityRenderers.register(CreatureMounts.RIDING_GOLEM, RidingGolemRenderer::new);
		EntityRenderers.register(CreatureMounts.SASQUATCH, SasquatchRenderer::new);
		EntityRenderers.register(CreatureMounts.CERBERUS, CerberusRenderer::new);
		EntityRenderers.register(CreatureMounts.LEOPARD, LeopardRenderer::new);
		EntityRenderers.register(CreatureMounts.SEAHORSE, SeahorseRenderer::new);
		EntityRenderers.register(CreatureMounts.GIANT_SCORPION, GiantScorpionRenderer::new);
		EntityRenderers.register(CreatureMounts.CROCODILE, CrocodileRenderer::new);
		EntityRenderers.register(CreatureMounts.GIANT_MOLE, GiantMoleRenderer::new);
	}
}
