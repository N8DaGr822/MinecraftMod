package darkspawn.black.mountkit.client;

import com.mojang.blaze3d.platform.InputConstants;
import darkspawn.black.core.DarkspawnCore;
import darkspawn.black.mountkit.GriffinControlsPayload;
import darkspawn.black.mountkit.MountCompanions;
import darkspawn.black.mountkit.PrototypeMount;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

/** Care screen plus the rider controls every mount shares; clients send buttons, never positions. */
public final class MountKitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(MountCompanions.MENU, MountCareScreen::new);
		var category = KeyMapping.Category.register(DarkspawnCore.id("mounts"));
		KeyMapping ability = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.mount_ability", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, category));
		KeyMapping dig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.mount_dig", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, category));
		KeyMapping descend = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.griffin_descend", InputConstants.Type.KEYBOARD, InputConstants.KEY_X, category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || !(client.player.getVehicle() instanceof PrototypeMount mount)
					|| !ClientPlayNetworking.canSend(GriffinControlsPayload.TYPE)) return;
			int buttons = 0;
			if (client.gui.screen() == null && client.gui.overlay() == null) {
				if (client.options.keyUp.isDown()) buttons |= PrototypeMount.FORWARD;
				if (client.options.keyDown.isDown()) buttons |= PrototypeMount.BACK;
				if (client.options.keyLeft.isDown()) buttons |= PrototypeMount.LEFT;
				if (client.options.keyRight.isDown()) buttons |= PrototypeMount.RIGHT;
				if (client.options.keyJump.isDown()) buttons |= PrototypeMount.ASCEND;
				if (descend.isDown()) buttons |= PrototypeMount.DESCEND;
				if (dig.isDown()) buttons |= PrototypeMount.DIG;
				if (ability.isDown()) buttons |= PrototypeMount.ABILITY;
			}
			ClientPlayNetworking.send(new GriffinControlsPayload(mount.getId(), buttons));
		});
	}
}
