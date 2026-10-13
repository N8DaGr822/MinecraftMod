package darkspawn.black.mount;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GriffinModelTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	@Test void griffinModelBakesAndAnimatesGroundAndFlight() throws Exception {
		var modelClass = Class.forName("darkspawn.black.client.mount.GriffinModel");
		var stateClass = Class.forName("darkspawn.black.client.mount.GriffinRenderState");
		var layer = modelClass.getMethod("createBodyLayer").invoke(null);
		var root = layer.getClass().getMethod("bakeRoot").invoke(layer);
		var model = modelClass.getConstructors()[0].newInstance(root);
		var state = stateClass.getConstructor().newInstance();
		for (boolean flying : new boolean[] {false, true}) {
			stateClass.getField("flying").setBoolean(state, flying);
			stateClass.getField("flightPose").setFloat(state, flying ? 1F : 0F);
			for (int frame = 0; frame < 40; frame++) {
				stateClass.getField("ageInTicks").setFloat(state, frame);
				modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			}
		}
		try (var texture = getClass().getResourceAsStream("/assets/darkspawn/textures/entity/griffin.png")) {
			assertNotNull(texture);
			var png = javax.imageio.ImageIO.read(texture);
			assertEquals(128, png.getWidth());
			assertEquals(128, png.getHeight());
		}
	}
}
