package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardVolumeTest {
	private final WardVolume ward = WardVolume.forTier(new BlockPos(0, 64, 0), 4);
	@Test void tiersHaveExactBlockAlignedVolumesAndIncludeCorners() {
		int[] widths = {12,24,32,50}, heights = {12,12,20,30};
		for (int tier=1;tier<=4;tier++) {
			var volume=WardVolume.forTier(new BlockPos(0,64,0),tier);
			assertEquals(widths[tier-1],volume.bounds().getXsize());
			assertEquals(widths[tier-1],volume.bounds().getZsize());
			assertEquals(heights[tier-1],volume.bounds().getYsize());
			assertTrue(volume.contains(new Vec3(volume.radius()-.01,64,volume.radius()-.01)));
			assertFalse(volume.contains(new Vec3(volume.radius(),64,0)));
			assertTrue(volume.contains(new Vec3(0,volume.bottom(),0)));
			assertFalse(volume.contains(new Vec3(0,volume.bottom()-.01,0)));
			assertFalse(volume.contains(new Vec3(0,volume.top(),0)));
		}
	}

	@Test void fastCrossingsAndFallingCannotTunnelThroughWard() {
		assertTrue(ward.crosses(new AABB(-100, 64, 0, -99, 66, 1), new Vec3(200, 0, 0)));
		assertTrue(ward.crosses(new AABB(0, 110, 0, 1, 112, 1), new Vec3(0, -100, 0)));
		assertFalse(ward.crosses(new AABB(-100, 100, 0, -99, 102, 1), new Vec3(200, 0, 0)));
		assertFalse(ward.crosses(new AABB(-100, 20, 0, -99, 22, 1), new Vec3(200, 0, 0)));
	}
	@Test void bodyContactCountsButEscapeDirectionRemainsOpen() {
		assertTrue(ward.intersects(new AABB(24.5, 64, 0, 25.5, 66, 1)));
		assertFalse(ward.crosses(new AABB(26, 64, 0, 27, 66, 1), new Vec3(2, 0, 0)));
		assertTrue(ward.crosses(new AABB(26, 64, 0, 27, 66, 1), new Vec3(-3, 0, 0)));
	}
}
