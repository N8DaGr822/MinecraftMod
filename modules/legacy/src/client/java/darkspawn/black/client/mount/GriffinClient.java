package darkspawn.black.client.mount;

import darkspawn.black.mount.Mounts;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class GriffinClient {
	private GriffinClient() { }
	public static void initialize() {
		EntityRenderers.register(Mounts.GRIFFIN, GriffinRenderer::new);
        EntityRenderers.register(Mounts.WOOLLY_RHINO, WoollyRhinoRenderer::new);
        EntityRenderers.register(Mounts.FLYING_SQUIRREL, FlyingSquirrelRenderer::new);
        EntityRenderers.register(Mounts.POPTART_CAT, PoptartCatRenderer::new);
        EntityRenderers.register(Mounts.MAGIC_CARPET, MagicCarpetRenderer::new);
        EntityRenderers.register(Mounts.AIRCRAFT, AircraftRenderer::new);
        EntityRenderers.register(Mounts.GROUND_WALKER, GroundWalkerRenderer::new);
        EntityRenderers.register(Mounts.DRAGON_MOUNT, DragonMountRenderer::new);
        EntityRenderers.register(Mounts.WINGED_UNICORN, WingedUnicornRenderer::new);
        EntityRenderers.register(Mounts.RIDING_GOLEM, RidingGolemRenderer::new);
        EntityRenderers.register(Mounts.SASQUATCH, SasquatchRenderer::new);
        EntityRenderers.register(Mounts.CERBERUS, CerberusRenderer::new);
        EntityRenderers.register(Mounts.LEOPARD, LeopardRenderer::new);
        EntityRenderers.register(Mounts.SEAHORSE, SeahorseRenderer::new);
        EntityRenderers.register(Mounts.GIANT_SCORPION, GiantScorpionRenderer::new);
        EntityRenderers.register(Mounts.CROCODILE, CrocodileRenderer::new);
        EntityRenderers.register(Mounts.GIANT_MOLE, GiantMoleRenderer::new);
	}
}
