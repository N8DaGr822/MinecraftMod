package darkspawn.black.client.mount;

import com.mojang.blaze3d.platform.InputConstants;
import darkspawn.black.Darkspawn;
import darkspawn.black.mount.PrototypeMount;
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
		net.minecraft.client.gui.screens.MenuScreens.register(darkspawn.black.mount.MountCompanions.MENU, MountCareScreen::new);
		EntityRenderers.register(Mounts.GRIFFIN, GriffinRenderer::new);
        EntityRenderers.register(Mounts.WOOLLY_RHINO, WoollyRhinoRenderer::new);
        EntityRenderers.register(Mounts.FLYING_SQUIRREL, FlyingSquirrelRenderer::new);
        EntityRenderers.register(Mounts.POPTART_CAT, PoptartCatRenderer::new);
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
		var category = KeyMapping.Category.register(Darkspawn.id("mounts"));
		KeyMapping ability = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.mount_ability", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, category));
		KeyMapping dig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.mount_dig", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, category));
		KeyMapping descend = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.darkspawn.griffin_descend", InputConstants.Type.KEYBOARD, InputConstants.KEY_X,
			category));
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
