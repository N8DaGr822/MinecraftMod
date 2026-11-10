package darkspawn.black.boss;

import darkspawn.black.creaturekit.Region;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionDescriptorTest {
	@Test void everyBossNamesTheMatchingKitRegion() {
		assertEquals(BossKind.values().length, Region.values().length);
		for (var kind : BossKind.values()) assertEquals(kind.id(), kind.region().id());
		for (var profile : BossProfile.values()) {
			var region = profile.kind().region();
			assertEquals(profile.biomes(), region.biomes(), profile.id());
			assertEquals(profile.dimension(), region.dimension(), profile.id());
			assertEquals(profile.underground(), region.underground(), profile.id());
		}
	}
}
