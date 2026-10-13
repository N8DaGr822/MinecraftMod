package darkspawn.black.mount;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelMountModelTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	@Test void everyTravelModelBakesAndAnimatesGroundAndFlight() throws Exception {
		for (String[] entry : new String[][] {{"WoollyRhino","woolly_rhino"},{"FlyingSquirrel","flying_squirrel"},{"PoptartCat","poptart_cat"},{"DragonMount","dragon_mount"},{"WingedUnicorn","winged_unicorn"},{"RidingGolem","riding_golem"},{"Sasquatch","sasquatch"},{"Cerberus","cerberus"}}) {
        var modelClass = Class.forName("darkspawn.black.client.mount."+entry[0]+"Model");
		var stateClass = Class.forName("darkspawn.black.client.mount.GriffinRenderState");
		var layer = modelClass.getMethod("createBodyLayer").invoke(null);
		var root = layer.getClass().getMethod("bakeRoot").invoke(layer);
		var model = modelClass.getConstructors()[0].newInstance(root);
		var state = stateClass.getConstructor().newInstance();
		for (boolean flying : new boolean[] {false, true}) {
			stateClass.getField("flying").setBoolean(state, flying);
			for (int frame = 0; frame < 40; frame++) {
				stateClass.getField("ageInTicks").setFloat(state, frame);
				modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			}
		}
		try (var texture = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/"+entry[1]+".png")) {
			assertNotNull(texture);
			var png = javax.imageio.ImageIO.read(texture);
			assertEquals(128, png.getWidth());
			assertEquals(128, png.getHeight());
		}
	}
    }
}
