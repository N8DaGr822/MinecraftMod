package darkspawn.black.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Finds a free standing spot near a travel destination without editing the world. */
public final class SafeArrival {
	private SafeArrival() { }
	/** A clear two-block spot on solid, harmless ground within 4 blocks of {@code center}, or null. */
	public static Vec3 position(ServerPlayer player, ServerLevel level, BlockPos center) {
		// Bounded Arrival: Load only the destination neighborhood, never excavate another player's build.
		for (int radius = 0; radius <= 4; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
			for (int y = 0; y <= 4; y++) {
				var pos = center.offset(x, y % 2 == 0 ? y / 2 : -(y + 1) / 2, z);
				if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinY() || pos.getY() + 1 >= level.getMaxY()) continue;
				level.getChunk(pos);
				var floor = level.getBlockState(pos.below());
				if (!floor.isSolidRender() || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
					|| !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
				var at = Vec3.atBottomCenterOf(pos); var box = player.getBoundingBox().move(at.subtract(player.position()));
				if (level.noCollision(player, box) && level.getEntities(player, box).isEmpty()
					&& level.players().stream().noneMatch(other -> other != player && other.getBoundingBox().intersects(box))) return at;
			}
		}
		return null;
	}
}
