package darkspawn.black.pirate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Moving builds participate in native movement collision on both sides. */
public final class ShipCollision {
    private ShipCollision() { }

    public static List<VoxelShape> append(Entity entity, Level level, AABB area, List<VoxelShape> original) {
        if (entity == null || entity instanceof PirateShip || entity.isPassenger() || !level.dimension().equals(Pirate.LEVEL)) return original;
        List<VoxelShape> result = null;
        // Entity Sections: Query far enough to include the center of the largest rotated hull.
        for (var ship : level.getEntitiesOfClass(PirateShip.class, area.inflate(24), s -> !s.docked() && !s.isRemoved() && s.getBoundingBox().intersects(area))) {
            for (var block : ship.blocks()) {
                for (var box : block.state().getCollisionShape(level, ship.blockPosition()).toAabbs()) {
                    var local = box.move(block.pos().getX() - .5, block.pos().getY(), block.pos().getZ() - .5);
                    var world = bounds(ship, local);
                    if (!world.intersects(area)) continue;
                    if (result == null) result = new ArrayList<>(original);
                    result.add(Shapes.create(world));
                }
            }
        }
        return result == null ? original : result;
    }

    private static AABB bounds(PirateShip ship, AABB box) {
        return bounds(ship.position(), ship.getYRot(), box);
    }

    private static AABB bounds(Vec3 position, float yaw, AABB box) {
        // Rotated Shapes: Conservative boxes preserve slab/stair heights without making the whole hull solid.
        var a = transform(position, yaw, box.minX, box.minY, box.minZ);
        var b = transform(position, yaw, box.maxX, box.maxY, box.maxZ);
        var c = transform(position, yaw, box.minX, box.minY, box.maxZ);
        var d = transform(position, yaw, box.maxX, box.maxY, box.minZ);
        return new AABB(Math.min(Math.min(a.x,b.x),Math.min(c.x,d.x)), a.y,
            Math.min(Math.min(a.z,b.z),Math.min(c.z,d.z)), Math.max(Math.max(a.x,b.x),Math.max(c.x,d.x)), b.y,
            Math.max(Math.max(a.z,b.z),Math.max(c.z,d.z)));
    }

    private static Vec3 transform(Vec3 position, float yaw, double x, double y, double z) {
        double angle = Math.toRadians(yaw);
        return position.add(x*Math.cos(angle)-z*Math.sin(angle), y, x*Math.sin(angle)+z*Math.cos(angle));
    }

    public static boolean intersects(PirateShip ship, AABB area, Vec3 position, float yaw) {
        for (var block : ship.blocks()) for (var box : block.state().getCollisionShape(ship.level(), ship.blockPosition()).toAabbs()) {
            if (bounds(position, yaw, box.move(block.pos().getX()-.5, block.pos().getY(), block.pos().getZ()-.5)).intersects(area)) return true;
        }
        return false;
    }

    public static boolean clearSailing(PirateShip ship, Vec3 position, float yaw) {
        var level = ship.level();
        if (ship.blocks().isEmpty()) return false;
        var others = level.getEntitiesOfClass(PirateShip.class, new AABB(position, position).inflate(48), s -> s != ship && !s.isRemoved());
        // Passenger Clearance: A low bridge must clear occupied seats as well as the build.
        for (int i = 0; i < ship.getPassengers().size(); i++) {
            var passenger = ship.getPassengers().get(i);
            var offset = ship.seatPosition(passenger, i).subtract(ship.position());
            var at = transform(position, yaw - ship.getYRot(), offset.x, offset.y, offset.z);
            var box = passenger.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(at);
            if (level.getBlockCollisions(null, box).iterator().hasNext()) return false;
        }
        // Sailing Clearance: Check captured geometry, not the empty rectangular docking volume.
        for (var block : ship.blocks()) {
            var cell = bounds(position, yaw, new AABB(block.pos()).move(-.5, 0, -.5)).deflate(1.0E-7);
            for (var at : net.minecraft.core.BlockPos.betweenClosed(
                    net.minecraft.core.BlockPos.containing(cell.minX, cell.minY, cell.minZ),
                    net.minecraft.core.BlockPos.containing(cell.maxX, cell.maxY, cell.maxZ))) {
                if (!level.getWorldBorder().isWithinBounds(at) || !level.hasChunkAt(at)) return false;
                if (block.pos().getY() == 0 && !level.getFluidState(at.below()).is(net.minecraft.tags.FluidTags.WATER)) return false;
            }
            for (var box : block.state().getCollisionShape(level, ship.blockPosition()).toAabbs()) {
                var world = bounds(position, yaw, box.move(block.pos().getX()-.5, block.pos().getY(), block.pos().getZ()-.5)).deflate(1.0E-7);
                if (level.getBlockCollisions(null, world).iterator().hasNext()) return false;
                for (var other : others) {
                    if (other.getBoundingBox().intersects(world) && (other.docked() || intersects(other, world, other.position(), other.getYRot()))) return false;
                }
            }
        }
        return true;
    }

    public static PirateShip supporting(Entity entity) {
        if (entity.isPassenger() || !entity.level().dimension().equals(Pirate.LEVEL)) return null;
        var feet = entity.getBoundingBox();
        var below = new AABB(feet.minX, feet.minY - .08, feet.minZ, feet.maxX, feet.minY + .01, feet.maxZ);
        for (var ship : entity.level().getEntitiesOfClass(PirateShip.class, below.inflate(24), s -> !s.docked() && s.getBoundingBox().intersects(below))) {
            for (var block : ship.blocks()) for (var box : block.state().getCollisionShape(entity.level(), ship.blockPosition()).toAabbs()) {
                var world = bounds(ship, box.move(block.pos().getX()-.5, block.pos().getY(), block.pos().getZ()-.5));
                if (Math.abs(world.maxY-feet.minY)<.08 && world.intersects(below)) return ship;
            }
        }
        return null;
    }

    public static Vec3 carriedPosition(Entity entity, Vec3 oldPosition, float oldYaw, PirateShip ship) {
        var offset = entity.position().subtract(oldPosition);
        double angle = Math.toRadians(ship.getYRot()-oldYaw);
        return ship.position().add(offset.x*Math.cos(angle)-offset.z*Math.sin(angle), offset.y,
            offset.x*Math.sin(angle)+offset.z*Math.cos(angle));
    }

    public static boolean climbable(Entity entity) {
        if (entity.isPassenger() || !entity.level().dimension().equals(Pirate.LEVEL)) return false;
        for (var ship : entity.level().getEntitiesOfClass(PirateShip.class, entity.getBoundingBox().inflate(24), s -> !s.docked() && !s.isRemoved() && s.getBoundingBox().contains(entity.position()))) {
            var offset = entity.position().subtract(ship.position());
            double angle = Math.toRadians(-ship.getYRot());
            var pos = net.minecraft.core.BlockPos.containing(offset.x*Math.cos(angle)-offset.z*Math.sin(angle)+.5,
                offset.y, offset.x*Math.sin(angle)+offset.z*Math.cos(angle)+.5);
            if (ship.blocks().stream().anyMatch(b -> b.pos().equals(pos) && b.state().is(net.minecraft.tags.BlockTags.CLIMBABLE))) return true;
        }
        return false;
    }
}
