package darkspawn.black.forsaken;

import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import darkspawn.black.Darkspawn;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForsakenAssetTest {
	private JsonObject resource(String path) throws Exception {
		try (var stream = getClass().getResourceAsStream("/assets/darkspawn/" + path)) {
			assertNotNull(stream, path);
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
	@Test void infectedRigsAndEveryAnimationBakeWithTheRuntimeLoader() throws Exception {
		darkspawn.black.TestBootstrap.initialize();
		var loader = new GeckoLibGsonLoader();
		for (String id : new String[] {"infected_wanderer", "infected_crawler", "spore_spitter", "assimilated_brute"}) {
			var model = loader.bakeGeckoLibModelFile(Darkspawn.id("entity/" + id), resource("geckolib/models/entity/" + id + ".geo.json"));
			var animations = loader.bakeGeckoLibAnimationsFile(Darkspawn.id("entity/" + id), resource("geckolib/animations/entity/" + id + ".animation.json"), MathParser.create());
			assertFalse(model.isMissingno(), id);
			for (String clip : new String[] {"idle", "walk", "warning", "attack", "death", "active", "hide"}) {
				var animation = animations.getAnimation("animation." + id + "." + clip);
				assertNotNull(animation, id + ": " + clip); assertTrue(animation.length() > 0);
				for (var bone : animation.boneAnimations()) assertTrue(model.getBone(bone.boneName()).isPresent(), bone.boneName());
			}
		}
	}
}
