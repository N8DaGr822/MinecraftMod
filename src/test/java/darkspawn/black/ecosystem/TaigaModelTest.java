package darkspawn.black.ecosystem;

import darkspawn.black.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TaigaModelTest {
	@BeforeAll
	static void initialize() { TestBootstrap.initialize(); }

	@Test
	void allWolfModelsBakeAndAnimatePackFrenzyAndPounceStates() throws Exception {
		Class<?> modelClass = Class.forName("darkspawn.black.client.ecosystem.TaigaWolfModel");
		Class<?> stateClass = Class.forName("darkspawn.black.client.ecosystem.TaigaWolfRenderState");
		for (TaigaWolf.Kind kind : TaigaWolf.Kind.values()) {
			Object layer = modelClass.getMethod("createBodyLayer", TaigaWolf.Kind.class).invoke(null, kind);
			Object root = layer.getClass().getMethod("bakeRoot").invoke(layer);
			Object model = modelClass.getConstructors()[0].newInstance(root, kind);
			Object state = stateClass.getConstructor().newInstance();
			stateClass.getField("walkAnimationPos").setFloat(state, 8);
			stateClass.getField("walkAnimationSpeed").setFloat(state, 0.7F);
			modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			stateClass.getField("packed").setBoolean(state, true);
			stateClass.getField("frenzy").setInt(state, 100);
			stateClass.getField("windup").setInt(state, 20);
			modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
			stateClass.getField("leaping").setBoolean(state, true);
			modelClass.getMethod("setupAnim", stateClass).invoke(model, state);
		}
		Class.forName("darkspawn.black.client.ecosystem.TaigaWolfRenderer", false, getClass().getClassLoader()).getDeclaredMethods();
	}
}
