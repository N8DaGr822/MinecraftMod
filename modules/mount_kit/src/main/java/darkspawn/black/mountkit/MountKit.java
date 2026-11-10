package darkspawn.black.mountkit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Library entrypoint: rider controls, the care menu, the Companion Capsule and the Veterinary Kit. */
public final class MountKit implements ModInitializer {
	public static final String MOD_ID = "darkspawn_mount_kit";
	private static boolean initialized;
	@Override public void onInitialize() { initialize(); }
	public static synchronized void initialize() {
		if (initialized) return;
		initialized = true;
		MountCompanions.initialize();
		PayloadTypeRegistry.serverboundPlay().register(GriffinControlsPayload.TYPE, GriffinControlsPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GriffinControlsPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof PrototypeMount mount && mount.getId() == payload.entityId()) {
				mount.acceptControls(context.player(), payload.buttons());
			}
		});
	}
}
