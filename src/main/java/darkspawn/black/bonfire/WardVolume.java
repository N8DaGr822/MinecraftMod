package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finite cylinder. Swept checks prevent fast movement from crossing both sides in one tick. */
public record WardVolume(BlockPos center, int radius) {
	public double bottom() { return center.getY() - 10; }
	public double top() { return center.getY() + 21; }
	public AABB bounds() { return new AABB(center.getX() + .5 - radius, bottom(), center.getZ() + .5 - radius,
		center.getX() + .5 + radius, top(), center.getZ() + .5 + radius); }
	public boolean contains(Vec3 point) {
		double x = point.x - center.getX() - .5, z = point.z - center.getZ() - .5;
		return point.y >= bottom() && point.y < top() && x * x + z * z <= radius * radius;
	}
	public boolean intersects(AABB box) {
		double x = Math.clamp(center.getX() + .5, box.minX, box.maxX) - center.getX() - .5;
		double z = Math.clamp(center.getZ() + .5, box.minZ, box.maxZ) - center.getZ() - .5;
		return box.maxY > bottom() && box.minY < top() && x * x + z * z <= radius * radius;
	}
	public boolean crosses(AABB box, Vec3 movement) {
		if (intersects(box) || intersects(box.move(movement))) return true;
		// Expand the cylinder by the entity's horizontal half-diagonal, conservatively
		// accounting for the full body rather than testing just the feet.
		double lo = 0, hi = 1;
		if (Math.abs(movement.y) < 1e-9) {
			if (box.maxY <= bottom() || box.minY >= top()) return false;
		} else {
			double a = (bottom() - box.maxY) / movement.y, b = (top() - box.minY) / movement.y;
			lo = Math.max(0, Math.min(a, b)); hi = Math.min(1, Math.max(a, b));
			if (lo > hi) return false;
		}
		double x = box.getCenter().x - center.getX() - .5, z = box.getCenter().z - center.getZ() - .5;
		if (movement.y == 0 && x * movement.x + z * movement.z >= 0) return false;
		double length = movement.x * movement.x + movement.z * movement.z;
		double t = length < 1e-9 ? lo : Math.clamp(-(x * movement.x + z * movement.z) / length, lo, hi);
		double expanded = radius + Math.hypot(box.getXsize(), box.getZsize()) / 2;
		return Math.pow(x + movement.x * t, 2) + Math.pow(z + movement.z * t, 2) <= expanded * expanded;
	}
}
