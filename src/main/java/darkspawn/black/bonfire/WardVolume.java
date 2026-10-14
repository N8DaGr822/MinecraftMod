package darkspawn.black.bonfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Block-aligned sanctuary. Swept body checks prevent fast movement through its walls. */
public record WardVolume(BlockPos center, int radius, int height) {
	public static WardVolume forTier(BlockPos center, int tier) {
		return switch (tier) {
			case 2 -> new WardVolume(center, 12, 12);
			case 3 -> new WardVolume(center, 16, 20);
			case 4 -> new WardVolume(center, 25, 30);
			default -> new WardVolume(center, 6, 12);
		};
	}
	// Sanctuary Bounds: One third below the core, two thirds above; even widths align to whole blocks.
	public double bottom() { return center.getY() - height / 3; }
	public double top() { return bottom() + height; }
	public AABB bounds() { return new AABB(center.getX() - radius, bottom(), center.getZ() - radius,
		center.getX() + radius, top(), center.getZ() + radius); }
	public boolean contains(Vec3 point) { return bounds().contains(point); }
	public boolean intersects(AABB box) { return bounds().intersects(box); }
	public boolean crosses(AABB box, Vec3 movement) {
		if (intersects(box) || intersects(box.move(movement))) return true;
		// Minkowski Expansion: Trace the entity center against the ward expanded by its half extents.
		AABB expanded = bounds().inflate(box.getXsize() / 2, box.getYsize() / 2, box.getZsize() / 2);
		return expanded.clip(box.getCenter(), box.getCenter().add(movement)).isPresent();
	}
}
