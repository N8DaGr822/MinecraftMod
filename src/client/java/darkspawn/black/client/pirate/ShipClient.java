package darkspawn.black.client.pirate;

import com.mojang.blaze3d.platform.InputConstants;
import darkspawn.black.Darkspawn;
import darkspawn.black.pirate.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class ShipClient {
	private ShipClient(){}
	public static void initialize(){
        ShantyPlayback.initialize();MenuScreens.register(SeaMenu.TYPE,SeaScreen::new);
		EntityRenderers.register(Ships.SHIP,ShipRenderer::new);EntityRenderers.register(Ships.BALL,ThrownItemRenderer::new);MenuScreens.register(Ships.MENU,ShipScreen::new);MenuScreens.register(HarborGuide.MENU,HarborScreen::new);
		var category=KeyMapping.Category.register(Darkspawn.id("ships"));
		var port=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.ship_port",InputConstants.Type.KEYBOARD,InputConstants.KEY_Z,category));
		var starboard=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.ship_starboard",InputConstants.Type.KEYBOARD,InputConstants.KEY_C,category));
		var dock=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.ship_dock",InputConstants.Type.KEYBOARD,InputConstants.KEY_R,category));
		var target=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.ship_target",InputConstants.Type.KEYBOARD,InputConstants.KEY_G,category));
		var menu=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.darkspawn.ship_menu",InputConstants.Type.KEYBOARD,InputConstants.KEY_U,category));
		ClientTickEvents.END_CLIENT_TICK.register(client->{
			if(client.player==null||!(client.player.getVehicle() instanceof PirateShip ship)||!ClientPlayNetworking.canSend(ShipControlsPayload.TYPE))return;
			int input=0;if(client.gui.screen()==null&&client.gui.overlay()==null){
				if(client.options.keyUp.isDown())input|=PirateShip.FORWARD;if(client.options.keyDown.isDown())input|=PirateShip.BACK;
				if(client.options.keyLeft.isDown())input|=PirateShip.LEFT;if(client.options.keyRight.isDown())input|=PirateShip.RIGHT;
				if(menu.isDown())input|=PirateShip.MENU;
				if(port.isDown())input|=PirateShip.FIRE_LEFT;if(starboard.isDown())input|=PirateShip.FIRE_RIGHT;if(dock.isDown())input|=PirateShip.DOCK;if(target.isDown())input|=PirateShip.TARGET;
			}ClientPlayNetworking.send(new ShipControlsPayload(ship.getId(),input));
		});
	}
}
