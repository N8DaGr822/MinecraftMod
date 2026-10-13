package darkspawn.black.client.mount;

import com.mojang.blaze3d.platform.InputConstants;
import darkspawn.black.Darkspawn;
import darkspawn.black.mount.Griffin;
import darkspawn.black.mount.GriffinControlsPayload;
import darkspawn.black.mount.Mounts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class GriffinClient {
	private GriffinClient() { }
	public static void initialize() {
		EntityRenderers.register(Mounts.GRIFFIN, GriffinRenderer::new);
		KeyMapping descend = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.griffin_descend", InputConstants.Type.KEYBOARD, InputConstants.KEY_X,
			KeyMapping.Category.register(Darkspawn.id("mounts"))));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || !(client.player.getVehicle() instanceof Griffin griffin)
					|| !ClientPlayNetworking.canSend(GriffinControlsPayload.TYPE)) return;
			int buttons = 0;
			if (client.gui.screen() == null && client.gui.overlay() == null) {
				if (client.options.keyUp.isDown()) buttons |= Griffin.FORWARD;
				if (client.options.keyDown.isDown()) buttons |= Griffin.BACK;
				if (client.options.keyLeft.isDown()) buttons |= Griffin.LEFT;
				if (client.options.keyRight.isDown()) buttons |= Griffin.RIGHT;
				if (client.options.keyJump.isDown()) buttons |= Griffin.ASCEND;
				if (descend.isDown()) buttons |= Griffin.DESCEND;
			}
			ClientPlayNetworking.send(new GriffinControlsPayload(griffin.getId(), buttons));
		});
	}
}
