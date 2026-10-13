package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardVolumeTest {
	private final WardVolume ward = new WardVolume(new BlockPos(0, 64, 0), 75);
	@Test void cylinderHasFiniteVerticalAndHorizontalCoverage() {
		assertTrue(ward.contains(new Vec3(75.5, 64, .5)));
		assertFalse(ward.contains(new Vec3(75.6, 64, .5)));
		assertFalse(ward.contains(new Vec3(60, 64, 60)));
		assertTrue(ward.contains(new Vec3(.5, 54, .5)));
		assertFalse(ward.contains(new Vec3(.5, 53.99, .5)));
		assertTrue(ward.contains(new Vec3(.5, 84.99, .5)));
		assertFalse(ward.contains(new Vec3(.5, 85, .5)));
	}
	@Test void fastCrossingsAndFallingCannotTunnelThroughWard() {
		assertTrue(ward.crosses(new AABB(-100, 64, 0, -99, 66, 1), new Vec3(200, 0, 0)));
		assertTrue(ward.crosses(new AABB(0, 110, 0, 1, 112, 1), new Vec3(0, -100, 0)));
		assertFalse(ward.crosses(new AABB(-100, 100, 0, -99, 102, 1), new Vec3(200, 0, 0)));
		assertFalse(ward.crosses(new AABB(-100, 20, 0, -99, 22, 1), new Vec3(200, 0, 0)));
	}
	@Test void bodyContactCountsButEscapeDirectionRemainsOpen() {
		assertTrue(ward.intersects(new AABB(75, 64, 0, 76, 66, 1)));
		assertFalse(ward.crosses(new AABB(76, 64, 0, 77, 66, 1), new Vec3(2, 0, 0)));
		assertTrue(ward.crosses(new AABB(76, 64, 0, 77, 66, 1), new Vec3(-3, 0, 0)));
	}
}
