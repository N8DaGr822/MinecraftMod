package darkspawn.black.oneblock;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneBlockProgressTest {
	@Test void phasesRequireSharedCountsAndExplicitActivationAndEndRemainsEndless() {
		var progress = new OneBlockProgress();
		for (int phase = 0; phase < 5; phase++) {
			assertFalse(progress.advance());
			for (int i = 0; i < OneBlockProgress.REQUIRED[phase]; i++) progress.mined();
			assertEquals(phase, progress.phase); assertTrue(progress.ready()); progress.mined();
			assertTrue(progress.advance()); assertEquals(0, progress.breaks);
		}
		for (int i = 0; i < 1000; i++) progress.mined();
		assertEquals(5, progress.phase); assertFalse(progress.advance());
		progress.initialized = true; progress.resource = 19;
		var encoded = OneBlockProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
		var restored = OneBlockProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertEquals(progress.total, restored.total); assertEquals(19, restored.resource); assertTrue(restored.initialized);
	}
}
