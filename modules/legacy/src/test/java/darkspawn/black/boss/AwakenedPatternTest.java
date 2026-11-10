package darkspawn.black.boss;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AwakenedPatternTest {
	@Test void everyBossHasDistinctBoundedPatternsAndAnEscapeRoute() {
		var signatures = new HashSet<String>();
		var names = new HashSet<String>();
		for (var kind : BossKind.values()) {
			assertTrue(names.add(AwakenedPattern.name(kind)));
			var signature = new StringBuilder();
			for (int beat = 0; beat < 3; beat++) {
				var zones = AwakenedPattern.zones(kind, beat);
				signature.append(zones);
				assertFalse(zones.isEmpty());
				assertTrue(zones.size() <= 4);
				for (var zone : zones) {
					assertTrue(zone.inner() >= 0 && zone.outer() > zone.inner());
					assertTrue(Math.hypot(zone.x(), zone.z()) + zone.outer() <= 20);
					assertTrue(zone.contains(zone.x() + (zone.inner() + zone.outer()) / 2, zone.z()));
					assertFalse(zone.contains(zone.x() + zone.outer() + .01, zone.z()));
				}
				boolean safe = false;
				for (int x = -18; x <= 18; x++) for (int z = -18; z <= 18; z++) {
					final int px = x, pz = z;
					if (zones.stream().noneMatch(zone -> zone.contains(px, pz))) safe = true;
				}
				assertTrue(safe, kind + " beat " + beat + " needs a safe position");
			}
			assertTrue(signatures.add(signature.toString()), kind + " must have its own sequence");
		}
	}

	@Test void treeRipplesExpandAndLeaveTheCenterSafe() {
		for (int beat = 0; beat < 3; beat++) {
			var ring = AwakenedPattern.zones(BossKind.ANCIENT_TREE_SPIRIT, beat).getFirst();
			assertFalse(ring.contains(0, 0));
			assertTrue(ring.contains(7 + beat * 4, 0));
			assertFalse(ring.contains(5 + beat * 4, 0));
		}
		assertThrows(IllegalArgumentException.class, () -> AwakenedPattern.zones(BossKind.KRAKEN, 3));
	}
}
