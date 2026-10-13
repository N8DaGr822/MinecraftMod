package darkspawn.black.mount;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelMountModelTest {
	@BeforeAll static void initialize() { TestBootstrap.initialize(); }
	@Test void everyTravelModelBakesAndAnimatesGroundAndFlight() throws Exception {
		for (String[] entry : new String[][] {{"WoollyRhino","woolly_rhino"},{"FlyingSquirrel","flying_squirrel"},{"PoptartCat","poptart_cat"},{"DragonMount","dragon_mount"},{"WingedUnicorn","winged_unicorn"},{"RidingGolem","riding_golem"},{"Sasquatch","sasquatch"},{"Cerberus","cerberus"},{"Leopard","leopard"},{"Seahorse","seahorse"},{"GiantScorpion","giant_scorpion"},{"Crocodile","crocodile"},{"GiantMole","giant_mole"}}) {
        var modelClass = Class.forName("darkspawn.black.client.mount."+entry[0]+"Model");
		var stateClass = Class.forName("darkspawn.black.client.mount.GriffinRenderState");
		var layer = modelClass.getMethod("createBodyLayer").invoke(null);
		var root = layer.getClass().getMethod("bakeRoot").invoke(layer);
		var model = modelClass.getConstructors()[0].newInstance(root);
		var state = stateClass.getConstructor().newInstance();
		for (boolean flying : new boolean[] {false, true}) {
			stateClass.getField("flying").setBoolean(state, flying);
			stateClass.getField("swimming").setBoolean(state, flying);
			stateClass.getField("flightPose").setFloat(state, flying ? 1F : 0F);
			stateClass.getField("walkAnimationSpeed").setFloat(state, .8F);
			stateClass.getField("yRot").setFloat(state, 25F);
			for (int frame = 0; frame < 40; frame++) {
				stateClass.getField("ageInTicks").setFloat(state, frame);
				stateClass.getField("walkAnimationPos").setFloat(state, frame);
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
	@Test void attacksBlendWithLocomotionAndResetBetweenEntities() throws Exception {
		var stateClass = Class.forName("darkspawn.black.client.mount.GriffinRenderState");
		for (String[] entry : new String[][] {{"DragonMount", "head", "jaw"}, {"Crocodile", "head", "jaw"},
				{"WoollyRhino", "head"}, {"GiantScorpion", "tail"}}) {
			var modelClass = Class.forName("darkspawn.black.client.mount." + entry[0] + "Model");
			var layer = modelClass.getMethod("createBodyLayer").invoke(null);
			var root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			var model = modelClass.getConstructors()[0].newInstance(root);
			var state = stateClass.getConstructor().newInstance();
			stateClass.getField("flightPose").setFloat(state, 1F);
			stateClass.getField("swimming").setBoolean(state, true);
			stateClass.getField("walkAnimationSpeed").setFloat(state, .8F);
			Object part = root;
			for (int i = 1; i < entry.length; i++) part = part.getClass().getMethod("getChild", String.class).invoke(part, entry[i]);
			var angle = part.getClass().getField("xRot");
			var animate = modelClass.getMethod("setupAnim", stateClass);
			animate.invoke(model, state);
			float idle = angle.getFloat(part);
			stateClass.getField("attackPose").setFloat(state, 1F); animate.invoke(model, state);
			float strike = angle.getFloat(part);
			assertTrue(Math.abs(strike - idle) > .2F, entry[0] + " has a distinct attack pose");
			stateClass.getField("attackPose").setFloat(state, .5F); animate.invoke(model, state);
			assertEquals((strike + idle) / 2, angle.getFloat(part), .0001F, "Recovery blends with locomotion");
			stateClass.getField("attackPose").setFloat(state, 0F); animate.invoke(model, state);
			assertEquals(idle, angle.getFloat(part), .0001F, "Next entity must not inherit attack pose");
		}
	}
	@Test void cerberusSideHeadsTrackAndWingsBlendWithoutModelStateLeakage() throws Exception {
		var stateClass = Class.forName("darkspawn.black.client.mount.GriffinRenderState");
		for (String name : new String[] { "Cerberus", "DragonMount", "WingedUnicorn", "Griffin" }) {
			var modelClass = Class.forName("darkspawn.black.client.mount." + name + "Model");
			var layer = modelClass.getMethod("createBodyLayer").invoke(null);
			var root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			var model = modelClass.getConstructors()[0].newInstance(root);
			var state = stateClass.getConstructor().newInstance();
			stateClass.getField("yRot").setFloat(state, 30F);
			var animate = modelClass.getMethod("setupAnim", stateClass);
			animate.invoke(model, state);
			if (name.equals("Cerberus")) {
				for (String head : new String[] { "left_head", "right_head" }) {
					var part = root.getClass().getMethod("getChild", String.class).invoke(root, head);
					assertTrue(part.getClass().getField("yRot").getFloat(part) > .2F, head + " follows look direction");
				}
			} else {
				var wing = root.getClass().getMethod("getChild", String.class).invoke(root, "left_wing");
				var rotation = wing.getClass().getField("yRot");
				float grounded = rotation.getFloat(wing);
				stateClass.getField("flightPose").setFloat(state, 1F); animate.invoke(model, state);
				float airborne = rotation.getFloat(wing);
				assertNotEquals(grounded, airborne);
				stateClass.getField("flightPose").setFloat(state, .5F); animate.invoke(model, state);
				assertEquals((grounded + airborne) / 2, rotation.getFloat(wing), .0001F);
				stateClass.getField("flightPose").setFloat(state, 0F); animate.invoke(model, state);
				assertEquals(grounded, rotation.getFloat(wing), .0001F, "Next grounded entity must not inherit flight pose");
			}
		}
	}
}
