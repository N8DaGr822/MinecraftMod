package darkspawn.black.ecosystem;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForestModelTest {
	@BeforeAll
	static void initialize() { TestBootstrap.initialize(); }

	@Test
	void allForestModelsBakeAndAnimateWalkingHidingAndRootWarnings() throws Exception {
		Class<?> modelClass = Class.forName("darkspawn.black.client.ecosystem.ForestMobModel");
		Class<?> stateClass = Class.forName("darkspawn.black.client.ecosystem.ForestMobRenderState");
		for (ForestMob.Kind kind : ForestMob.Kind.values()) {
			Object layer = modelClass.getMethod("createBodyLayer", ForestMob.Kind.class).invoke(null, kind);
			Object root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			Object model = modelClass.getConstructors()[0].newInstance(root, kind);
			Object state = stateClass.getConstructor().newInstance();
			stateClass.getField("walkAnimationPos").setFloat(state, 8);
			stateClass.getField("walkAnimationSpeed").setFloat(state, 0.7F);
			modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			stateClass.getField("hiding").setBoolean(state, true);
			modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			stateClass.getField("hiding").setBoolean(state, false);
			for (int windup = 25; windup >= 0; windup--) {
				stateClass.getField("windup").setInt(state, windup);
				modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			}
			assertNotNull(ForestEcosystem.MOBS.get(kind));
		}
		Class.forName("darkspawn.black.client.ecosystem.ForestMobRenderer", false, getClass().getClassLoader()).getDeclaredMethods();
	}
}
