package darkspawn.black.creaturekit;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreatureKitTest {
	@org.junit.jupiter.api.BeforeAll static void boot() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}
	@Test void sixteenRegionsWithStableIdsAndDimensions() {
		assertEquals(16, Region.values().length);
		assertEquals(16, Arrays.stream(Region.values()).map(Region::id).collect(Collectors.toSet()).size());
		assertEquals(Region.VOID_EYE, Region.byId("void_eye"));
		assertTrue(Region.CAVE_CRAWLER.underground() && Region.SHADOW_CREEPER_QUEEN.underground() && !Region.KRAKEN.underground());
		assertEquals(net.minecraft.world.level.Level.NETHER, Region.NETHERBORN.dimension());
		assertEquals(net.minecraft.world.level.Level.END, Region.VOID_EYE.dimension());
		assertEquals(net.minecraft.world.level.Level.OVERWORLD, Region.ANCIENT_TREE_SPIRIT.dimension());
	}
	@Test void warningCuesStartOnceAndRigsNeedRegistration() {
		assertTrue(CreatureAudio.startsWarning(0, 20));
		assertFalse(CreatureAudio.startsWarning(20, 19));
		assertFalse(CreatureAudio.startsWarning(0, 0));
		assertFalse(CreatureAssets.animated("creature_kit_unit_rig"));
		CreatureAssets.register("creature_kit_unit_rig");
		assertTrue(CreatureAssets.animated("creature_kit_unit_rig"));
	}
}
