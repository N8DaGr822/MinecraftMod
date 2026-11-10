package darkspawn.black.client.mount;


import darkspawn.black.mount.Mounts;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class GriffinClient {
	private GriffinClient() { }
	public static void initialize() {
        EntityRenderers.register(Mounts.MAGIC_CARPET, MagicCarpetRenderer::new);
        EntityRenderers.register(Mounts.AIRCRAFT, AircraftRenderer::new);
        EntityRenderers.register(Mounts.GROUND_WALKER, GroundWalkerRenderer::new);
	}
}
