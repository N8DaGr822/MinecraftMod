package darkspawn.black.cooking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CulinaryProgressTest {
	@Test void varietyExpiresWithRepetitionAndSaveRestoreKeepsDiscoveries() {
		var progress = new CulinaryProgress();
		for (int i = 0; i < 8; i++) progress.eat("food" + i, true);
		assertEquals(8, progress.variety());
		assertEquals(0.1F, progress.saturationBonus());
		progress.unlock("shadow_stew");
		for (int i = 0; i < 8; i++) progress.eat("food0", true);
		assertEquals(1, progress.variety());
		assertEquals(0, progress.saturationBonus());
		var loaded = new CulinaryProgress();
		loaded.restore(progress.recent(), progress.meals(), progress.recipes());
		assertEquals(8, loaded.meals().size());
		assertTrue(loaded.knows("shadow_stew"));
		assertFalse(loaded.unlock("shadow_stew"));
		assertEquals(1, loaded.variety());
	}
}
